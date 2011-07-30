/*
 * Copyright (C) 2010-2011 Felix Bechstein
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
package de.ub0r.android.travelLog.ui;

import java.util.List;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Criteria;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;
import android.view.MenuItem;
import android.widget.Toast;
import de.ub0r.android.lib.IPreferenceContainer;
import de.ub0r.android.lib.Log;
import de.ub0r.android.lib.Utils;
import de.ub0r.android.travelLog.R;
import de.ub0r.android.travelLog.data.LocationChecker;

/**
 * Preferences.
 * 
 * @author flx
 */
public final class Preferences extends PreferenceActivity implements
		IPreferenceContainer {
	static {
		Log.init("TravelLog");
	}

	/** Tag for output. */
	private static final String TAG = "prefs";

	/** Preference's name: update interval. */
	public static final String PREFS_UPDATE_INTERVAL = "update_interval";
	/** Preference's name: last latitude. */
	public static final String PREFS_LAST_LATITUDE = "last_lat";
	/** Preference's name: last longitude. */
	public static final String PREFS_LAST_LONGITUDE = "last_long";

	/** Preference's name: round. */
	private static final String PREFS_ROUND = "round";
	/** Preference's name: theme. */
	private static final String PREFS_THEME = "theme";
	/** Theme: black. */
	private static final String THEME_BLACK = "black";
	/** Theme: light. */
	private static final String THEME_LIGHT = "light";
	/** Preference's name: text size group. */
	private static final String PREFS_TEXTSIZE_GROUP = "textsize_group";
	/** Preference's name: text size child. */
	private static final String PREFS_TEXTSIZE_CHILD = "textsize_child";
	/** Default text size. */
	private static final float DEFAULT_TEXTSIZE_GROUP = 16f;
	/** Default text size. */
	private static final float DEFAULT_TEXTSIZE_CHILD = 14f;
	/** Preference's name: go home menu. */
	private static final String PREFS_GO_HOME = "go_home";
	/** Preference's name: count travel time. */
	public static final String PREFS_COUNT_TRAVEL = "count_travel";
	/** Preference's name: work hours limit for warning. */
	public static final String PREFS_LIMIT_WARN_HOURS = "limit_warn";
	/** Preference's name: sound for warning. */
	public static final String PREFS_LIMIT_WARN_SOUND = "limit_sound_warn";
	/** Preference's name: delay between repetitions for warning. */
	public static final String PREFS_LIMIT_WARN_DELAY = "limit_delay_warn";

	/** Preference's name: work hours limit for alert. */
	public static final String PREFS_LIMIT_ALERT_HOURS = "limit_alert";
	/** Preference's name: sound for alert. */
	public static final String PREFS_LIMIT_ALERT_SOUND = "limit_sound_alert";
	/** Preference's name: delay between repetitions for alert. */
	public static final String PREFS_LIMIT_ALERT_DELAY = "limit_delay_alert";

	/** Minimal distance in meters between location updates. */
	private static final int MINDISTANCE = 100;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.addPreferencesFromResource(R.xml.prefs_appearance);
		this.addPreferencesFromResource(R.xml.prefs_common);
		this.addPreferencesFromResource(R.xml.prefs_logtypes);
		this.addPreferencesFromResource(R.xml.prefs_go_home);
		this.addPreferencesFromResource(R.xml.prefs_about);

		this.setTitle(R.string.settings);
		Utils.setLocale(this);
		registerPreferenceChecker(this);
	}

	/**
	 * Check all {@link SharedPreferences} and register
	 * {@link OnPreferenceChangeListener}.
	 * 
	 * @param pc
	 *            {@link IPreferenceContainer}
	 */
	static void registerPreferenceChecker(final IPreferenceContainer pc) {
		Preference pr = pc.findPreference(PREFS_UPDATE_INTERVAL);
		if (pr != null) {
			pr.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(final Preference preference,
						final Object newValue) {
					return Preferences.checkIntervall(pc, newValue.toString());
				}
			});
		}

		pr = pc.findPreference("map");
		if (pr != null) {
			if (getLocationProvider(pc.getContext()) == null) {
				pr.setEnabled(false);
				pr.setSummary(R.string.map_unavail);
			} else {
				pr.setSummary(R.string.map_hint);
				pr.setEnabled(true);
			}
		}

		pr = pc.findPreference(Preferences.PREFS_LIMIT_WARN_HOURS);
		if (pr != null) {
			pr.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(final Preference preference,
						final Object newValue) {
					return checkWarning(pc, newValue.toString());
				}
			});
		}
		pr = pc.findPreference(Preferences.PREFS_LIMIT_ALERT_HOURS);
		if (pr != null) {
			pr.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(final Preference preference,
						final Object newValue) {
					return checkAlert(pc, newValue.toString());
				}
			});
		}

		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(pc
				.getContext());
		checkIntervall(pc, sp.getString(PREFS_UPDATE_INTERVAL, null));
		checkWarning(pc, sp.getString(Preferences.PREFS_LIMIT_WARN_HOURS, // .
				null));
		checkAlert(pc, sp.getString(Preferences.PREFS_LIMIT_ALERT_HOURS, null));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Context getContext() {
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			Intent intent = new Intent(this, Logs.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			this.startActivity(intent);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * Check preference: interval.
	 * 
	 * @param pc
	 *            {@link IPreferenceContainer}
	 * @param newValue
	 *            new value
	 * @return update preference?
	 */
	private static boolean checkIntervall(final IPreferenceContainer pc,
			final String newValue) {
		Preference pr = pc.findPreference(PREFS_GO_HOME);
		if (pr != null) {
			pr.setEnabled(Utils.parseInt(newValue, 0) > 0);
		}
		return true;
	}

	/**
	 * Check preference: warning.
	 * 
	 * @param pc
	 *            {@link IPreferenceContainer}
	 * @param newValue
	 *            new value
	 * @return update preference?
	 */
	private static boolean checkWarning(final IPreferenceContainer pc,
			final String newValue) {
		boolean enable = Utils.parseFloat(newValue, 0) > 0;
		Preference pr = pc.findPreference(Preferences.PREFS_LIMIT_WARN_DELAY);
		if (pr != null) {
			pr.setEnabled(enable);
		}
		pr = pc.findPreference(Preferences.PREFS_LIMIT_WARN_SOUND);
		if (pr != null) {
			pr.setEnabled(enable);
		}
		return true;
	}

	/**
	 * Check preference: alert.
	 * 
	 * @param pc
	 *            {@link IPreferenceContainer}
	 * @param newValue
	 *            new value
	 * @return update preference?
	 */
	private static boolean checkAlert(final IPreferenceContainer pc,
			final String newValue) {
		final float alertHours = Utils.parseFloat(newValue, 0);
		final float warnHours = Utils.parseFloat(PreferenceManager
				.getDefaultSharedPreferences(pc.getContext()).getString(
						Preferences.PREFS_LIMIT_WARN_HOURS, null), 0);
		if (alertHours > 0f && alertHours < warnHours) {
			Toast.makeText(pc.getContext(), R.string.limit_alert_gt_warn_,
					Toast.LENGTH_LONG).show();
			return false;
		}
		boolean enable = alertHours > 0f;
		Preference pr = pc.findPreference(Preferences.PREFS_LIMIT_ALERT_DELAY);
		if (pr != null) {
			pr.setEnabled(enable);
		}
		pr = pc.findPreference(Preferences.PREFS_LIMIT_ALERT_SOUND);
		if (pr != null) {
			pr.setEnabled(enable);
		}
		return true;
	}

	/**
	 * Get round from preferences.
	 * 
	 * @param context
	 *            {@link Context}
	 * @return round
	 */
	public static int getRound(final Context context) {
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(context);
		final String s = p.getString(PREFS_ROUND, null);
		return Utils.parseInt(s, 0);

	}

	/**
	 * Get Theme from Preferences.
	 * 
	 * @param context
	 *            {@link Context}
	 * @return theme
	 */
	public static int getTheme(final Context context) {
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(context);
		final String s = p.getString(PREFS_THEME, THEME_LIGHT);
		if (s != null && THEME_BLACK.equals(s)) {
			return R.style.Theme_SherlockUb0r;
		}
		return R.style.Theme_SherlockUb0r_Light;
	}

	/**
	 * Get Text size from Preferences.
	 * 
	 * @param context
	 *            {@link Context}
	 * @return theme
	 */
	public static float getTextSizeGroup(final Context context) {
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(context);
		final String s = p.getString(PREFS_TEXTSIZE_GROUP, "14");
		return Utils.parseFloat(s, DEFAULT_TEXTSIZE_GROUP);
	}

	/**
	 * Get Text size from Preferences.
	 * 
	 * @param context
	 *            {@link Context}
	 * @return theme
	 */
	public static float getTextSizeChild(final Context context) {
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(context);
		final String s = p.getString(PREFS_TEXTSIZE_CHILD, "12");
		return Utils.parseFloat(s, DEFAULT_TEXTSIZE_CHILD);
	}

	/**
	 * Get coarse location provider.
	 * 
	 * @param context
	 *            {@link Context}
	 * @return location provider's name
	 */
	public static String getLocationProvider(final Context context) {
		final LocationManager lm = (LocationManager) context
				.getSystemService(Context.LOCATION_SERVICE);
		Criteria c = new Criteria();
		c.setAccuracy(Criteria.ACCURACY_COARSE);
		List<String> providers = lm.getProviders(c, true);
		if (providers.size() > 0) {
			return providers.get(0);
		}
		Log.w(TAG, "no location provider found");
		return null;
	}

	/**
	 * Register the LocationChecker Service.
	 * 
	 * @param context
	 *            {@link Context}
	 */
	public static void registerLocationChecker(final Context context) {
		Log.d(TAG, "registerLocationChecker()");
		final LocationManager lm = (LocationManager) context
				.getSystemService(Context.LOCATION_SERVICE);
		final PendingIntent pi = PendingIntent.getBroadcast(context, 0,
				new Intent(context, LocationChecker.class),
				PendingIntent.FLAG_UPDATE_CURRENT);
		lm.removeUpdates(pi);
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(context);
		long interval = Utils.parseLong(p
				.getString(PREFS_UPDATE_INTERVAL, null), 0L);
		interval *= Utils.MINUTES_IN_MILLIS;
		String lp = getLocationProvider(context);
		if (lp != null) {
			lm.requestLocationUpdates(lp, interval, MINDISTANCE, pi);
		}
	}
}
