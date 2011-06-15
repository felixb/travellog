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

import java.util.Calendar;
import java.util.Date;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.PowerManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import de.ub0r.android.lib.Log;
import de.ub0r.android.lib.Utils;
import de.ub0r.android.travelLog.R;
import de.ub0r.android.travelLog.ui.Logs;
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

	/** LED color for notification. */
	private static final int NOTIFICATION_LED_COLOR = 0xffff0000;
	/** LED blink on (ms) for notification. */
	private static final int NOTIFICATION_LED_ON = 500;
	/** LED blink off (ms) for notification. */
	private static final int NOTIFICATION_LED_OFF = 2000;

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
			this.checkWarning(context);
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
	 * Check warnings and notify user.
	 * 
	 * @param context
	 *            {@link Context}
	 */
	private void checkWarning(final Context context) {
		Log.d(TAG, "checkWarning()");
		Cursor cursor = context.getContentResolver().query(
				DataProvider.Logs.CONTENT_URI_OPEN,
				DataProvider.Logs.PROJECTION, null, null, null);
		if (!cursor.moveToFirst()) {
			cursor.close();
			Log.i(TAG, "no open log");
			NotificationManager nm = (NotificationManager) context
					.getSystemService(Context.NOTIFICATION_SERVICE);
			Log.d(TAG, "nm.cancelAll()");
			nm.cancelAll();
			return; // no open log. no need to notify
		}
		cursor.close();

		SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(context);
		boolean countTravel = p.getBoolean(Preferences.PREFS_COUNT_TRAVEL,
				false);
		final long warn = (long) (Utils.HOUR_IN_MILLIS * Utils.parseFloat(p
				.getString(Preferences.PREFS_LIMIT_WARN_HOURS, null), 0));
		final long alert = (long) (Utils.HOUR_IN_MILLIS * Utils.parseFloat(p
				.getString(Preferences.PREFS_LIMIT_ALERT_HOURS, null), 0));
		Log.d(TAG, "countTravel: " + countTravel);
		Log.d(TAG, "warn: " + warn);
		Log.d(TAG, "alert: " + alert);

		final Calendar c = Calendar.getInstance();
		String where = DataProvider.Logs.FROM_D + "=? AND "
				+ DataProvider.Logs.TYPE_TYPE + "=?";
		String[] args;
		if (countTravel) {
			where += " AND " + DataProvider.Logs.TYPE_TYPE + "=?";
			args = new String[] { String.valueOf(c.get(Calendar.DAY_OF_YEAR)),
					String.valueOf(DataProvider.Logtypes.TYPE_WORK),
					String.valueOf(DataProvider.Logtypes.TYPE_TRAVEL) };
		} else {
			args = new String[] { String.valueOf(c.get(Calendar.DAY_OF_YEAR)),
					String.valueOf(DataProvider.Logtypes.TYPE_WORK) };
		}

		cursor = context.getContentResolver().query(
				DataProvider.Logs.CONTENT_URI, DataProvider.Logs.PROJECTION,
				where, args, null);

		long d = 0;
		if (cursor.moveToFirst()) {
			final int idFrom = cursor.getColumnIndex(DataProvider.Logs.FROM);
			final int idTo = cursor.getColumnIndex(DataProvider.Logs.TO);
			do {
				long from = cursor.getLong(idFrom);
				long to = cursor.getLong(idTo);

				if (to <= 0L) {
					to = c.getTimeInMillis();
				}
				d += to - from;
				Log.d(TAG, "d: " + d);
			} while (cursor.moveToNext());
		}
		cursor.close();

		Notification n = null;
		if (d > warn) {
			String ticker, title, text;
			int flags;
			Uri sound;
			if (d > alert) {
				ticker = context.getString(R.string.alert_ticker);
				title = context.getString(R.string.alert_title);
				text = context.getString(R.string.alert_text);
				sound = Uri.parse(p.getString(
						Preferences.PREFS_LIMIT_ALERT_SOUND, null));
				flags = Notification.FLAG_NO_CLEAR
						| Notification.FLAG_ONGOING_EVENT;
			} else {
				ticker = context.getString(R.string.warn_ticker);
				title = context.getString(R.string.warn_title);
				text = context.getString(R.string.warn_text);
				sound = Uri.parse(p.getString(
						Preferences.PREFS_LIMIT_WARN_SOUND, null));
				flags = Notification.FLAG_AUTO_CANCEL;
			}
			n = new Notification(android.R.drawable.stat_notify_error, ticker,
					System.currentTimeMillis());
			n.setLatestEventInfo(context, title, text, PendingIntent
					.getActivity(context, 0, new Intent(context, Logs.class),
							PendingIntent.FLAG_CANCEL_CURRENT));
			n.flags = flags;
			n.sound = sound;
			n.ledARGB = NOTIFICATION_LED_COLOR;
			n.ledOnMS = NOTIFICATION_LED_ON;
			n.ledOffMS = NOTIFICATION_LED_OFF;
		}

		NotificationManager nm = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		if (n == null) {
			Log.d(TAG, "nm.cancelAll()");
			nm.cancelAll();
		} else {
			Log.d(TAG, "nm.notify(0, " + n + ")");
			nm.notify(0, n);
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
