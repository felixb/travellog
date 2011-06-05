/*
 * Copyright (C) 2011 Felix Bechstein
 * 
 * This file is part of TravelLog.
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; If not, see <http://www.gnu.org/licenses/>.
 */
package de.ub0r.android.travelLog.data;

import java.util.Date;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.os.PowerManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import de.ub0r.android.lib.Log;
import de.ub0r.android.lib.Utils;
import de.ub0r.android.travelLog.ui.Preferences;

/**
 * Check location in background.
 * 
 * @author flx
 */
public final class LocationChecker extends BroadcastReceiver {
	static {
		Log.init("TravelLog");
	}

	/** Tag for output. */
	private static final String TAG = "lc";

	/** Time between to location checks. */
	private static final long DELAY = 60; // 30min
	/** Factor for time between location checks. */
	private static final long DELAY_FACTOR = 60000;

	@Override
	public void onReceive(final Context context, final Intent intent) {
		Log.i(TAG, "onReceive(" + intent + ")");

		// get wakelock
		final PowerManager pm = (PowerManager) context
				.getSystemService(Context.POWER_SERVICE);
		final PowerManager.WakeLock wakelock = pm.newWakeLock(
				PowerManager.PARTIAL_WAKE_LOCK, TAG);
		wakelock.acquire();
		Log.i(TAG, "got wakelock");

		final String a = intent.getAction();
		Log.d(TAG, "action: " + a);
		if (a == null || !a.equals(Intent.ACTION_BOOT_COMPLETED)) {
			// do actual work
			this.checkLocation(context);
		}

		// schedule next run
		schedNext(context);
		// release wakelock
		wakelock.release();
		Log.i(TAG, "wakelock released");
	}

	/**
	 * Check the location.
	 * 
	 * @param context
	 *            {@link Context}
	 */
	private void checkLocation(final Context context) {
		final LocationManager lm = (LocationManager) context
				.getSystemService(Context.LOCATION_SERVICE);
		final Location currentLocation = lm
				.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		if (currentLocation == null) {
			Log.i(TAG, "no current location known");
			return;
		}
		boolean foundcell = false;
		final ContentResolver cr = context.getContentResolver();
		final Cursor cursor = cr.query(DataProvider.Cells.CONTENT_URI,
				DataProvider.Cells.PROJECTION, null, null, null);
		if (cursor.moveToFirst()) {
			final int idId = cursor.getColumnIndex(DataProvider.Cells.ID);
			final int idLat = cursor
					.getColumnIndex(DataProvider.Cells.LATITUDE);
			final int idLong = cursor
					.getColumnIndex(DataProvider.Cells.LONGITUDE);
			final int idType = cursor.getColumnIndex(DataProvider.Cells.TYPE);
			final int idRadius = cursor
					.getColumnIndex(DataProvider.Cells.RADIUS);
			final int idFirstSeen = cursor
					.getColumnIndex(DataProvider.Cells.SEEN_FIRST);
			do {
				final Location l = new Location(
						LocationManager.NETWORK_PROVIDER);
				final int cLat = cursor.getInt(idLat);
				final int cLong = cursor.getInt(idLong);
				l.setLatitude(cLat / 1E6);
				l.setLongitude(cLong / 1E6);
				// save last location
				PreferenceManager.getDefaultSharedPreferences(context).edit()
						.putLong(Preferences.PREFS_LAST_LATITUDE, cLat)
						.putLong(Preferences.PREFS_LAST_LONGITUDE, cLong)
						.commit();
				if (currentLocation.distanceTo(l) <= cursor.getInt(idRadius)) {
					final long id = cursor.getLong(idId);
					final int t = cursor.getInt(idType);
					Log.i(TAG, "loc in cell: " + id + " / type: " + t);
					if (t == 0) {
						DataProvider.Logs.closeOpen(context, 0L, false);
					} else {
						final Cursor c = cr.query(
								DataProvider.Logs.CONTENT_URI_OPEN,
								DataProvider.Logs.PROJECTION,
								DataProvider.Logs.TYPE + " = ?",
								new String[] { String.valueOf(t) }, null);
						final boolean openNew = c.getCount() == 0;
						if (!c.isClosed()) {
							c.close();
						}
						if (openNew) { // skip if already running with same type
							DataProvider.Logs.closeOpen(context, 0L, false);
							DataProvider.Logs.openNew(context, 0L, t, true);
						} else {
							Log.i(TAG, "skip open new log with same type");
						}
					}
					ContentValues values = new ContentValues(2);
					values.put(DataProvider.Cells.SEEN_LAST, System
							.currentTimeMillis());
					if (cursor.getLong(idFirstSeen) <= 0L) {
						values.put(DataProvider.Cells.SEEN_FIRST, System
								.currentTimeMillis());
					}
					cr.update(ContentUris.withAppendedId(
							DataProvider.Cells.CONTENT_URI, id), values, null,
							null);
					foundcell = true;
					break;
				}
			} while (cursor.moveToNext());
		}
		if (!cursor.isClosed()) {
			cursor.close();
		}
		if (!foundcell) {
			// close logs opened by automation
			Log.d(TAG, "close all autoopend logs");
			DataProvider.Logs.closeOpen(context, 0L, true);
		}
	}

	/**
	 * Schedule next update.
	 * 
	 * @param context
	 *            {@link Context}
	 */
	private static void schedNext(final Context context) {
		final Intent i = new Intent(context, LocationChecker.class);
		final PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
		final long l = Utils.parseLong(PreferenceManager
				.getDefaultSharedPreferences(context).getString(
						Preferences.PREFS_UPDATE_INTERVAL,
						String.valueOf(DELAY)), DELAY)
				* DELAY_FACTOR;
		if (l == 0L) {
			return;
		}
		final long t = SystemClock.elapsedRealtime() + l;
		final AlarmManager mgr = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		Log.i(TAG, "next location check: "
				+ DateFormat.getTimeFormat(context).format(new Date(t)));
		mgr.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, t, pi);
	}
}
