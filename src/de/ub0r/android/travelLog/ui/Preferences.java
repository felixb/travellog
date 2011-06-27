package de.ub0r.android.travelLog.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;
import android.widget.Toast;
import de.ub0r.android.lib.Utils;
import de.ub0r.android.travelLog.R;

/**
 * Preferences.
 * 
 * @author flx
 */
public class Preferences extends PreferenceActivity {
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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setTitle(R.string.settings);
		this.addPreferencesFromResource(R.xml.prefs);
		Utils.setLocale(this);
		this.findPreference(PREFS_UPDATE_INTERVAL)
				.setOnPreferenceChangeListener(
						new OnPreferenceChangeListener() {
							@Override
							public boolean onPreferenceChange(
									final Preference preference,
									final Object newValue) {
								return Preferences.this.checkIntervall(newValue
										.toString());
							}
						});
		this.findPreference(PREFS_LIMIT_WARN_HOURS)
				.setOnPreferenceChangeListener(
						new OnPreferenceChangeListener() {
							@Override
							public boolean onPreferenceChange(
									final Preference preference,
									final Object newValue) {
								return Preferences.this.checkWarning(newValue
										.toString());
							}
						});
		this.findPreference(PREFS_LIMIT_ALERT_HOURS)
				.setOnPreferenceChangeListener(
						new OnPreferenceChangeListener() {
							@Override
							public boolean onPreferenceChange(
									final Preference preference,
									final Object newValue) {
								return Preferences.this.checkAlert(newValue
										.toString());
							}
						});

		SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(this);
		this.checkIntervall(p.getString(PREFS_UPDATE_INTERVAL, null));
		this.checkWarning(p.getString(PREFS_LIMIT_WARN_HOURS, null));
		this.checkAlert(p.getString(PREFS_LIMIT_ALERT_HOURS, null));
	}

	/**
	 * Check prefererence: intervall.
	 * 
	 * @param newValue
	 *            new value
	 * @return update preference?
	 */
	private boolean checkIntervall(final String newValue) {
		Preferences.this.findPreference(PREFS_GO_HOME).setEnabled(
				Utils.parseInt(newValue, 0) > 0);
		return true;
	}

	/**
	 * Check prefererence: warning.
	 * 
	 * @param newValue
	 *            new value
	 * @return update preference?
	 */
	private boolean checkWarning(final String newValue) {
		boolean enable = Utils.parseFloat(newValue, 0) > 0;
		Preferences.this.findPreference(PREFS_LIMIT_WARN_DELAY).setEnabled(
				enable);
		Preferences.this.findPreference(PREFS_LIMIT_WARN_SOUND).setEnabled(
				enable);
		return true;
	}

	/**
	 * Check prefererence: alert.
	 * 
	 * @param newValue
	 *            new value
	 * @return update preference?
	 */
	private boolean checkAlert(final String newValue) {
		final float alertHours = Utils.parseFloat(newValue, 0);
		final float warnHours = Utils.parseFloat(PreferenceManager
				.getDefaultSharedPreferences(Preferences.this).getString(
						PREFS_LIMIT_WARN_HOURS, null), 0);
		if (warnHours > 0 && alertHours < warnHours) {
			Toast.makeText(Preferences.this, R.string.limit_alert_gt_warn_,
					Toast.LENGTH_LONG).show();
			return false;
		}
		boolean enable = alertHours > 0;
		Preferences.this.findPreference(PREFS_LIMIT_ALERT_DELAY).setEnabled(
				enable);
		Preferences.this.findPreference(PREFS_LIMIT_ALERT_SOUND).setEnabled(
				enable);
		return true;
	}

	/**
	 * Get round from preferences.
	 * 
	 * @param context
	 *            {@link Context}
	 * @return round
	 */
	public static final int getRound(final Context context) {
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
	public static final int getTheme(final Context context) {
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(context);
		final String s = p.getString(PREFS_THEME, THEME_BLACK);
		if (s != null && THEME_LIGHT.equals(s)) {
			return android.R.style.Theme_Light;
		}
		return android.R.style.Theme_Black;
	}

	/**
	 * Get Text size from Preferences.
	 * 
	 * @param context
	 *            {@link Context}
	 * @return theme
	 */
	public static final float getTextSizeGroup(final Context context) {
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
	public static final float getTextSizeChild(final Context context) {
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(context);
		final String s = p.getString(PREFS_TEXTSIZE_CHILD, "12");
		return Utils.parseFloat(s, DEFAULT_TEXTSIZE_CHILD);
	}
}
