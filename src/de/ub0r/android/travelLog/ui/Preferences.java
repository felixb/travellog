package de.ub0r.android.travelLog.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import de.ub0r.android.travelLog.R;

/**
 * Preferences.
 * 
 * @author flx
 */
public class Preferences extends PreferenceActivity {
	/** Preference's name: hide ads. */
	static final String PREFS_HIDEADS = "hideads";
	/** Preference's name: state. */
	private static final String PREFS_STATE = "state";
	/** Preference's name: travel item count. */
	private static final String PREFS_LISTCOUNT = "log_n";
	/** Preference's name: travel item start. */
	private static final String PREFS_LIST_START = "log_start_";
	/** Preference's name: travel item end. */
	private static final String PREFS_LIST_STOP = "log_stop_";
	/** Preference's name: travel item type. */
	private static final String PREFS_LIST_TYPE = "log_type_";
	/** Preference's name: mail. */
	private static final String PREFS_MAIL = "mail";
	/** Preference's name: flip export. */
	private static final String PREFS_FLIP_EXPORT = "export_flip";
	/** Preference's name: round. */
	private static final String PREFS_ROUND = "round";
	/** Preference's name: theme. */
	private static final String PREFS_THEME = "theme";
	/** Theme: black. */
	private static final String THEME_BLACK = "black";
	/** Theme: light. */
	private static final String THEME_LIGHT = "light";
	/** Preference's name: textsize. */
	private static final String PREFS_TEXTSIZE = "textsize";
	/** Textsize: black. */
	private static final String TEXTSIZE_SMALL = "small";
	/** Textsize: light. */
	private static final String TEXTSIZE_MEDIUM = "medium";

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
		this.addPreferencesFromResource(R.xml.prefs);
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
	 * Get Textsize from Preferences.
	 * 
	 * @param context
	 *            {@link Context}
	 * @return theme
	 */
	public static final int getTextsize(final Context context) {
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(context);
		final String s = p.getString(PREFS_TEXTSIZE, TEXTSIZE_SMALL);
		if (s != null && TEXTSIZE_MEDIUM.equals(s)) {
			return android.R.style.TextAppearance_Medium;
		}
		return android.R.style.TextAppearance_Small;
	}
}
