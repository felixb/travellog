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
import android.text.TextUtils;
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
	private static final long DELAY = 30; // 30min
	/** Factor for time between location checks. */
	private static final long DELAY_FACTOR = 60000;

	/** LED color for notification. */
	private static final int NOTIFICATION_LED_COLOR = 0xffff0000;
	/** LED blink on (ms) for notification. */
	private static final int NOTIFICATION_LED_ON = 500;
	/** LED blink off (ms) for notification. */
	private static final int NOTIFICATION_LED_OFF = 2000;

	/** Notify user / warning. */
	private static final String ACTION_NOTIFY = LocationChecker.class
			.getPackage() + ".NOTIFY";

	/** Preference's name: last notification. */
	private static final String PREFS_LAST_NOTIFY = "last_notify";
	/** Preference's name: last level. */
	private static final String PREFS_LAST_LEVEL = "last_level";
	/** Normal. */
	private static final int LEVEL_NOTHING = 0;
	/** Warning. */
	private static final int LEVEL_WARN = 1;
	/** Alert. */
	private static final int LEVEL_ALERT = 2;

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
		if (a != null && a.equals(Intent.ACTION_BOOT_COMPLETED)) {
			Preferences.registerLocationChecker(context);
		} else {
			// do actual work
			checkLocation(context);
			long delay = checkWarning(context);
			if (delay > 0L) {
				// schedule next run
				schedNext(context, delay);
			}
		}

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
	private static void checkLocation(final Context context) {
		Log.d(TAG, "checkLocation()");
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
					values.put(DataProvider.Cells.SEEN_LAST,
							System.currentTimeMillis());
					if (cursor.getLong(idFirstSeen) <= 0L) {
						values.put(DataProvider.Cells.SEEN_FIRST,
								System.currentTimeMillis());
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
	 * Get {@link Notification} for current level.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param level
	 *            level
	 * @return {@link Notification}
	 */
	private static Notification getNotification(final Context context,
			final int level) {
		Notification n;
		String ticker, title, text, sound;
		int flags;
		SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(context);
		switch (level) {
		case LEVEL_ALERT:
			ticker = context.getString(R.string.alert_ticker);
			title = context.getString(R.string.alert_title);
			text = context.getString(R.string.alert_text);
			sound = p.getString(Preferences.PREFS_LIMIT_ALERT_SOUND, null);
			flags = Notification.FLAG_NO_CLEAR
					| Notification.FLAG_ONGOING_EVENT;
			break;
		case LEVEL_WARN:
			ticker = context.getString(R.string.warn_ticker);
			title = context.getString(R.string.warn_title);
			text = context.getString(R.string.warn_text);
			sound = p.getString(Preferences.PREFS_LIMIT_WARN_SOUND, null);
			flags = Notification.FLAG_AUTO_CANCEL;
			break;
		default:
			return null;
		}
		n = new Notification(android.R.drawable.stat_notify_error, ticker,
				System.currentTimeMillis());
		n.setLatestEventInfo(context, title, text, PendingIntent.getActivity(
				context, 0, new Intent(context, Logs.class),
				PendingIntent.FLAG_CANCEL_CURRENT));
		n.flags = flags;
		if (TextUtils.isEmpty(sound)) {
			n.sound = null;
		} else {
			n.sound = Uri.parse(sound);
		}
		n.ledARGB = NOTIFICATION_LED_COLOR;
		n.ledOnMS = NOTIFICATION_LED_ON;
		n.ledOffMS = NOTIFICATION_LED_OFF;
		return n;
	}

	/**
	 * Check warnings and notify user.
	 * 
	 * @param context
	 *            {@link Context}
	 * @return delay to next notification
	 */
	private static long checkWarning(final Context context) {
		Log.d(TAG, "checkWarning()");
		SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(context);
		boolean countTravel = p.getBoolean(Preferences.PREFS_COUNT_TRAVEL,
				false);
		final Calendar c = Calendar.getInstance();
		String where = DataProvider.Logs.FROM_D + "=?";
		String[] args;
		if (countTravel) {
			where += " AND (" + DataProvider.Logs.TYPE_TYPE + "=? OR "
					+ DataProvider.Logs.TYPE_TYPE + "=?)";
			args = new String[] { String.valueOf(c.get(Calendar.DAY_OF_YEAR)),
					String.valueOf(DataProvider.Logtypes.TYPE_WORK),
					String.valueOf(DataProvider.Logtypes.TYPE_TRAVEL) };
		} else {
			where += " AND " + DataProvider.Logs.TYPE_TYPE + "=?";
			args = new String[] { String.valueOf(c.get(Calendar.DAY_OF_YEAR)),
					String.valueOf(DataProvider.Logtypes.TYPE_WORK) };
		}

		ContentResolver cr = context.getContentResolver();
		Cursor cursor = cr.query(DataProvider.Logs.CONTENT_URI_OPEN,
				DataProvider.Logs.PROJECTION, where, args, null);
		if (!cursor.moveToFirst()) {
			cursor.close();
			Log.i(TAG, "no open log");
			NotificationManager nm = (NotificationManager) context
					.getSystemService(Context.NOTIFICATION_SERVICE);
			Log.d(TAG, "nm.cancel(0)");
			nm.cancel(0);
			p.edit().remove(PREFS_LAST_LEVEL).remove(PREFS_LAST_NOTIFY)
					.commit();
			return -1L; // no open log. no need to notify
		}
		cursor.close();

		final long warn = (long) (Utils.HOUR_IN_MILLIS * Utils.parseFloat(
				p.getString(Preferences.PREFS_LIMIT_WARN_HOURS, null), 0));
		final long alert = (long) (Utils.HOUR_IN_MILLIS * Utils.parseFloat(
				p.getString(Preferences.PREFS_LIMIT_ALERT_HOURS, null), 0));
		Log.d(TAG, "countTravel: " + countTravel);
		Log.d(TAG, "warn:  " + warn);
		Log.d(TAG, "alert: " + alert);

		if (warn <= 0L && alert <= 0L) {
			Log.i(TAG, "warn=0; alert=0");
			NotificationManager nm = (NotificationManager) context
					.getSystemService(Context.NOTIFICATION_SERVICE);
			Log.d(TAG, "nm.cancel(0)");
			nm.cancel(0);
			p.edit().remove(PREFS_LAST_LEVEL).remove(PREFS_LAST_NOTIFY)
					.commit();
			return -1L; // warn=0; alert=0. no need to notify
		}

		cursor = cr.query(DataProvider.Logs.CONTENT_URI,
				DataProvider.Logs.PROJECTION, where, args, null);

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
				Log.d(TAG, "d:    " + d);
			} while (cursor.moveToNext());
		}
		cursor.close();

		Log.d(TAG, "d:    " + d);

		int lastLevel = p.getInt(PREFS_LAST_LEVEL, LEVEL_NOTHING);
		long lastNotify = p.getLong(PREFS_LAST_NOTIFY, 0L);
		long desiredPeriod = 0L;
		// get current level
		int level = LEVEL_NOTHING;
		if (alert > 0L && d > alert) {
			level = LEVEL_ALERT;
			desiredPeriod = Utils.parseLong(
					p.getString(Preferences.PREFS_LIMIT_ALERT_DELAY, null), 0L)
					* Utils.N_1000;
		} else if (warn > 0L && d > warn) {
			level = LEVEL_WARN;
			desiredPeriod = Utils.parseLong(
					p.getString(Preferences.PREFS_LIMIT_WARN_DELAY, null), 0L)
					* Utils.N_1000;
		}

		final long now = System.currentTimeMillis();
		Notification n = null;
		// show notification?
		if (level > LEVEL_NOTHING && (level != lastLevel || // .
				(desiredPeriod > 0L && lastNotify < now - desiredPeriod))) {
			Log.d(TAG, "level: " + level);
			Log.d(TAG, "lastLevel: " + lastLevel);
			Log.d(TAG, "desiredPeriod: " + desiredPeriod);
			Log.d(TAG, "lastNotify: " + lastNotify);
			Log.d(TAG, "now-p:      " + (now - desiredPeriod));
			n = getNotification(context, level);
		}
		NotificationManager nm = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		if (level == LEVEL_NOTHING) {
			p.edit().remove(PREFS_LAST_LEVEL).remove(PREFS_LAST_NOTIFY)
					.commit();
			Log.d(TAG, "nm.cancel(0)");
			nm.cancel(0);
			return -1L;
		} else if (n != null) {
			p.edit().putInt(PREFS_LAST_LEVEL, level)
					.putLong(PREFS_LAST_NOTIFY, now - Utils.N_100).commit();
			Log.d(TAG, "nm.notify(0, " + n + ")");
			nm.notify(0, n);
			return desiredPeriod;
		} else {
			return lastNotify - now + desiredPeriod;
		}
	}

	/**
	 * Schedule next update.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param delay
	 *            delay for next notification
	 */
	private static void schedNext(final Context context, final long delay) {
		Log.d(TAG, "schedNext(ctx, " + delay + ")");

		final Intent i = new Intent(context, LocationChecker.class);
		long t = SystemClock.elapsedRealtime();
		i.setAction(ACTION_NOTIFY);
		Log.i(TAG, // .
				"next location check in: "
						+ DateFormat.getTimeFormat(context).format(
								new Date(delay)));
		t += delay + Utils.N_100;

		final PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
		final AlarmManager mgr = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		mgr.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, t, pi);
	}
}
