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

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;
import android.view.MenuItem;
import android.widget.Toast;
import de.ub0r.android.lib.Log;
import de.ub0r.android.lib.Utils;
import de.ub0r.android.travelLog.R;

/**
 * Preferences for "go home" settings.
 * 
 * @author flx
 */
public final class PreferencesGoHome extends PreferenceActivity {
	static {
		Log.init("TravelLog");
	}

	/** Tag for output. */
	private static final String TAG = "prefsGH";

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.addPreferencesFromResource(R.xml.prefs_go_home);
		this.setTitle(this.getString(R.string.settings) + " > "
				+ this.getString(R.string.go_home_));
		Utils.setLocale(this);
		this.findPreference(Preferences.PREFS_LIMIT_WARN_HOURS)
				.setOnPreferenceChangeListener(
						new OnPreferenceChangeListener() {
							@Override
							public boolean onPreferenceChange(
									final Preference preference,
									final Object newValue) {
								return PreferencesGoHome.this
										.checkWarning(newValue.toString());
							}
						});
		this.findPreference(Preferences.PREFS_LIMIT_ALERT_HOURS)
				.setOnPreferenceChangeListener(
						new OnPreferenceChangeListener() {
							@Override
							public boolean onPreferenceChange(
									final Preference preference,
									final Object newValue) {
								return PreferencesGoHome.this
										.checkAlert(newValue.toString());
							}
						});

		SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(this);
		this
				.checkWarning(p.getString(Preferences.PREFS_LIMIT_WARN_HOURS,
						null));
		this.checkAlert(p.getString(Preferences.PREFS_LIMIT_ALERT_HOURS, null));
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
	 * Check preference: warning.
	 * 
	 * @param newValue
	 *            new value
	 * @return update preference?
	 */
	private boolean checkWarning(final String newValue) {
		boolean enable = Utils.parseFloat(newValue, 0) > 0;
		this.findPreference(Preferences.PREFS_LIMIT_WARN_DELAY).setEnabled(
				enable);
		this.findPreference(Preferences.PREFS_LIMIT_WARN_SOUND).setEnabled(
				enable);
		return true;
	}

	/**
	 * Check preference: alert.
	 * 
	 * @param newValue
	 *            new value
	 * @return update preference?
	 */
	private boolean checkAlert(final String newValue) {
		final float alertHours = Utils.parseFloat(newValue, 0);
		final float warnHours = Utils.parseFloat(PreferenceManager
				.getDefaultSharedPreferences(this).getString(
						Preferences.PREFS_LIMIT_WARN_HOURS, null), 0);
		if (alertHours > 0f && alertHours < warnHours) {
			Toast.makeText(this, R.string.limit_alert_gt_warn_,
					Toast.LENGTH_LONG).show();
			return false;
		}
		boolean enable = alertHours > 0f;
		this.findPreference(Preferences.PREFS_LIMIT_ALERT_DELAY).setEnabled(
				enable);
		this.findPreference(Preferences.PREFS_LIMIT_ALERT_SOUND).setEnabled(
				enable);
		return true;
	}
}
