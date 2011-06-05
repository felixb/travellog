package de.ub0r.android.travelLog.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import de.ub0r.android.lib.Utils;
import de.ub0r.android.travelLog.R;

/**
 * Preferences.
 * 
 * @author flx
 */
public class Preferences extends PreferenceActivity {
	/** Preference's name: hide ads. */
	static final String PREFS_HIDEADS = "hideads";
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

	/** Milliseconds per minute. */
	static final long MILLIS_A_MINUTE = 60000;

	/** DateFormat: date. */
	static String FORMAT_DATE = "dd.MM.";
	/** DateFormat: time. */
	static String FORMAT_TIME = "kk:mm";
	/** DateFormat: am/pm */
	static boolean FORMAT_AMPM = false;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setTitle(R.string.settings);
		this.addPreferencesFromResource(R.xml.prefs);
		Utils.setLocale(this);
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
