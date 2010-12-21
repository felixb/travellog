package de.ub0r.android.travelLog;

import java.util.ArrayList;
import java.util.Calendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.TimePicker;
import android.widget.AdapterView.OnItemClickListener;
import de.ub0r.android.lib.Changelog;
import de.ub0r.android.lib.DonationHelper;

/**
 * Main Activity.
 * 
 * @author flx
 */
public class TravelLog extends Activity implements OnClickListener,
		OnItemClickListener, OnDateSetListener, OnTimeSetListener {
	/** Tag for output. */
	// private static final String TAG = "TravelLog";

	/** State: nothing. */
	private static final int STATE_NOTHING = 0;
	/** State: pause. */
	private static final int STATE_PAUSE = 1;
	/** State: travel. */
	private static final int STATE_TRAVEL = 2;
	/** State: work. */
	private static final int STATE_WORK = 3;

	/** Action: change date. */
	private static final int ACTION_CHG_DATE = 0;
	/** Action: change start time. */
	private static final int ACTION_CHG_START = 1;
	/** Action: change end time. */
	private static final int ACTION_CHG_END = 2;
	/** Action: change type. */
	private static final int ACTION_CHG_TYPE = 3;
	/** Action: delete. */
	private static final int ACTION_DELETE = 4;

	/** Dialog: change date. */
	private static final int DIALOG_DATE = 0;
	/** Dialog: change time. */
	private static final int DIALOG_TIME = 1;
	/** Dialog: change type. */
	private static final int DIALOG_TYPE = 2;
	/** Dialog: about. */
	private static final int DIALOG_ABOUT = 4;

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

	/** States as String[]. */
	static String[] namesStates;

	/** State. */
	private int state = STATE_NOTHING;

	/** ArrayAdapter for ListView. */
	private ArrayAdapter<TravelItem> adapter;
	/** The List itself. */
	private ArrayList<TravelItem> list;

	/** Date/Time for editing. */
	private long editDate = 0;
	/** Item to edit. */
	private int editItem = 0;
	/** Action selected. */
	private int editAction = ACTION_CHG_START;

	/** Display ads? */
	private boolean prefsNoAds;

	/** Round time to this. */
	int prefsRound = 0;

	/**
	 * Preferences.
	 * 
	 * @author flx
	 */
	public static class Preferences extends PreferenceActivity {
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
		static final int getTheme(final Context context) {
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
		static final int getTextsize(final Context context) {
			final SharedPreferences p = PreferenceManager
					.getDefaultSharedPreferences(context);
			final String s = p.getString(PREFS_TEXTSIZE, TEXTSIZE_SMALL);
			if (s != null && TEXTSIZE_MEDIUM.equals(s)) {
				return android.R.style.TextAppearance_Medium;
			}
			return android.R.style.TextAppearance_Small;
		}
	}

	/**
	 * A single TravelLog item.
	 * 
	 * @author flx
	 */
	private class TravelItem {
		/** Time: start. */
		private long start;
		/** Time: end. */
		private long end;
		/** Type. */
		private int type;

		/**
		 * Create a TravelItem.
		 * 
		 * @param s
		 *            start time/date
		 * @param e
		 *            end time/date
		 * @param t
		 *            type
		 */
		public TravelItem(final long s, final long e, final int t) {
			this.start = s;
			this.end = e;
			this.type = t;
		}

		/**
		 * Create a TravelItem.
		 * 
		 * @param t
		 *            type
		 */
		public TravelItem(final int t) {
			this.start = TravelLog.this.roundTime(System.currentTimeMillis());
			this.type = t;
		}

		/**
		 * @return start date/time.
		 */
		public final long getStart() {
			return this.start;
		}

		/**
		 * @return end date/time
		 */
		public final long getEnd() {
			return this.end;
		}

		/**
		 * @return type
		 */
		public final int getType() {
			return this.type;
		}

		/**
		 * Set start date/time.
		 * 
		 * @param s
		 *            start
		 */
		public final void setStart(final long s) {
			this.start = s;
		}

		/**
		 * Set end date/time.
		 * 
		 * @param e
		 *            end
		 */
		public final void setEnd(final long e) {
			this.end = e;
		}

		/**
		 * Set type.
		 * 
		 * @param t
		 *            type
		 */
		public final void setType(final int t) {
			this.type = t;
		}

		/**
		 * Terminate open TravelItem now.
		 */
		public final void terminate() {
			if (this.end <= this.start) {
				this.end = TravelLog.this.roundTime(System.currentTimeMillis());
			}
		}

		/**
		 *{@inheritDoc}
		 */
		@Override
		public final String toString() {
			final long s = this.start;
			final long e = this.end;
			StringBuilder ret = new StringBuilder();
			if (s > 0) {
				ret.append(DateFormat.format(FORMAT_DATE, s).toString());
				ret.append(" " + DateFormat.format(FORMAT_TIME, s).toString());
			} else {
				ret.append("??.??. ???");
			}
			ret.append(" - ");
			if (e >= s && e != 0) {
				ret.append(DateFormat.format(FORMAT_TIME, e).toString());
			} else {
				ret.append("???");
			}
			ret.append(": " + TravelLog.namesStates[this.type]);
			if (s > 0 && (s < e || e == 0)) {
				ret.append(" " + TravelLog.this.getString(R.string.for_) + " ");
				if (e >= s) {
					ret.append(TravelLog.getTime(e - s));
				} else {
					ret.append(TravelLog
							.getTime(System.currentTimeMillis() - s));
				}
			}
			return ret.toString();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setTheme(Preferences.getTheme(this));
		this.setContentView(R.layout.main);
		FORMAT_DATE = this.getString(R.string.format_date);
		FORMAT_TIME = this.getString(R.string.format_time);
		FORMAT_AMPM = !FORMAT_TIME.endsWith("aa");
		namesStates = this.getResources().getStringArray(R.array.state);
		((Button) this.findViewById(R.id.stop)).setOnClickListener(this);
		((Button) this.findViewById(R.id.start_pause_))
				.setOnClickListener(this);
		((Button) this.findViewById(R.id.start_travel_))
				.setOnClickListener(this);
		((Button) this.findViewById(R.id.start_work_)).setOnClickListener(this);
		((Button) this.findViewById(R.id.add_row_)).setOnClickListener(this);
		this.list = new ArrayList<TravelItem>();
		int textSize = Preferences.getTextsize(this);
		int itemLayout;
		if (textSize == android.R.style.TextAppearance_Medium) {
			itemLayout = R.layout.list_item_medium;
		} else {
			itemLayout = R.layout.list_item_small;
		}
		this.adapter = new ArrayAdapter<TravelItem>(this, itemLayout,
				android.R.id.text1, this.list);
		final ListView lv = (ListView) this.findViewById(R.id.log);
		lv.setAdapter(this.adapter);
		lv.setOnItemClickListener(this);

		Changelog.showChangelog(this);

		this.prefsNoAds = DonationHelper.hideAds(this);
	}

	/**
	 * Change to new state.
	 * 
	 * @param newState
	 *            new state
	 * @param btnOnly
	 *            set buttons only, do not modify items
	 */
	private void changeState(final int newState, final boolean btnOnly) {
		TravelItem itm = null;
		if (this.list.size() > 0) {
			itm = this.list.get(0);
		}
		switch (newState) {
		case STATE_NOTHING:
			this.findViewById(R.id.stop).setVisibility(View.GONE);
			this.findViewById(R.id.start_pause_).setVisibility(View.VISIBLE);
			this.findViewById(R.id.start_travel_).setVisibility(View.VISIBLE);

			this.findViewById(R.id.start_work_).setVisibility(View.VISIBLE);

			if (!btnOnly) {
				if (itm != null) {
					itm.terminate();
				}
			}
			break;
		case STATE_PAUSE:
			((Button) this.findViewById(R.id.stop))
					.setText(R.string.stop_pause);
			this.findViewById(R.id.stop).setVisibility(View.VISIBLE);
			this.findViewById(R.id.start_pause_).setVisibility(View.GONE);
			this.findViewById(R.id.start_travel_).setVisibility(View.VISIBLE);
			this.findViewById(R.id.start_work_).setVisibility(View.VISIBLE);
			if (!btnOnly) {
				if (itm != null) {
					itm.terminate();
				}
				this.list.add(0, new TravelItem(STATE_PAUSE));
			}
			break;
		case STATE_TRAVEL:
			((Button) this.findViewById(R.id.stop))
					.setText(R.string.stop_travel);
			this.findViewById(R.id.stop).setVisibility(View.VISIBLE);
			this.findViewById(R.id.start_pause_).setVisibility(View.VISIBLE);
			this.findViewById(R.id.start_travel_).setVisibility(View.GONE);
			this.findViewById(R.id.start_work_).setVisibility(View.VISIBLE);
			if (!btnOnly) {
				if (itm != null) {
					itm.terminate();
				}
				this.list.add(0, new TravelItem(STATE_TRAVEL));
			}
			break;
		case STATE_WORK:
			((Button) this.findViewById(R.id.stop)).setText(R.string.stop_work);
			this.findViewById(R.id.stop).setVisibility(View.VISIBLE);
			this.findViewById(R.id.start_pause_).setVisibility(View.VISIBLE);
			this.findViewById(R.id.start_travel_).setVisibility(View.VISIBLE);
			this.findViewById(R.id.start_work_).setVisibility(View.GONE);
			if (!btnOnly) {
				if (itm != null) {
					itm.terminate();
				}
				this.list.add(0, new TravelItem(STATE_WORK));
			}
			break;
		default:
			break;
		}
		this.state = newState;
	}

	/**
	 * {@inheritDoc}
	 */
	public final void onClick(final View view) {
		switch (view.getId()) {
		case R.id.start_pause_:
			this.changeState(STATE_PAUSE, false);
			break;
		case R.id.start_travel_:
			this.changeState(STATE_TRAVEL, false);
			break;
		case R.id.start_work_:
			this.changeState(STATE_WORK, false);
			break;
		case R.id.stop:
			this.changeState(STATE_NOTHING, false);
			break;
		case R.id.add_row_:
			this.list.add(new TravelItem(0, 0, 0));
			break;
		default:
			break;
		}
		this.adapter.notifyDataSetChanged();
	}

	/**
	 * {@inheritDoc}
	 */
	public final void onItemClick(final AdapterView<?> parent, final View v,
			final int position, final long id) {
		this.editItem = position;
		final TravelItem itm = this.list.get(position);
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(itm.toString());
		builder.setItems(this.getResources().getStringArray(R.array.action),
				new DialogInterface.OnClickListener() {
					public void onClick(final DialogInterface dialog,
							final int item) {
						TravelLog.this.editAction = item;
						switch (item) {
						case ACTION_CHG_DATE:
							TravelLog.this.editDate = itm.getStart();
							TravelLog.this.showDialog(DIALOG_DATE);
							return;
						case ACTION_CHG_END:
							final long e = itm.getEnd();
							final long s = itm.getStart();
							if (e < s) {
								TravelLog.this.editDate = s;
							} else {
								TravelLog.this.editDate = e;
							}
							TravelLog.this.showDialog(DIALOG_TIME);
							return;
						case ACTION_CHG_START:
							TravelLog.this.editDate = itm.getStart();
							TravelLog.this.showDialog(DIALOG_TIME);
							return;
						case ACTION_CHG_TYPE:
							TravelLog.this.showDialog(DIALOG_TYPE);
							return;
						case ACTION_DELETE:
							TravelLog.this.list.remove(position);
							TravelLog.this.adapter.notifyDataSetChanged();
							return;
						default:
							return;
						}
					}
				});
		builder.create().show();
	}

	/**
	 * {@inheritDoc}
	 */
	public final void onDateSet(final DatePicker view, final int year,
			final int monthOfYear, final int dayOfMonth) {
		final Calendar c = Calendar.getInstance();
		c.setTimeInMillis(this.editDate);
		c.set(Calendar.YEAR, year);
		c.set(Calendar.MONTH, monthOfYear);
		c.set(Calendar.DAY_OF_MONTH, dayOfMonth);
		this.list.get(this.editItem).setStart(c.getTimeInMillis());
		c.setTimeInMillis(this.list.get(this.editItem).getEnd());
		c.set(Calendar.YEAR, year);
		c.set(Calendar.MONTH, monthOfYear);
		c.set(Calendar.DAY_OF_MONTH, dayOfMonth);
		this.list.get(this.editItem).setEnd(c.getTimeInMillis());
		this.adapter.notifyDataSetChanged();
	}

	/**
	 * {@inheritDoc}
	 */
	public final void onTimeSet(final TimePicker view, final int hour,
			final int minutes) {
		final Calendar c = Calendar.getInstance();
		c.setTimeInMillis(this.editDate);
		c.set(Calendar.HOUR_OF_DAY, hour);
		c.set(Calendar.MINUTE, minutes);
		final TravelItem itm = this.list.get(this.editItem);
		if (this.editAction == ACTION_CHG_START) {
			itm.setStart(c.getTimeInMillis());
		} else if (this.editAction == ACTION_CHG_END) {
			itm.setEnd(c.getTimeInMillis());
		}
		this.adapter.notifyDataSetChanged();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void onPrepareDialog(final int id, final Dialog dialog) {
		final Calendar c = Calendar.getInstance();
		if (this.editDate != 0) {
			c.setTimeInMillis(this.editDate);
		}
		switch (id) {
		case DIALOG_DATE:
			((DatePickerDialog) dialog).updateDate(c.get(Calendar.YEAR), c
					.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
			break;
		case DIALOG_TIME:
			((TimePickerDialog) dialog).updateTime(c.get(Calendar.HOUR_OF_DAY),
					c.get(Calendar.MINUTE));
			break;
		default:
			break;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final Dialog onCreateDialog(final int id) {
		final Calendar c = Calendar.getInstance();
		c.setTimeInMillis(this.editDate);
		Dialog d;
		AlertDialog.Builder builder;
		switch (id) {
		case DIALOG_ABOUT:
			d = new Dialog(this);
			d.setContentView(R.layout.about);
			d.setTitle(this.getString(R.string.about_) + " v"
					+ this.getString(R.string.app_version));
			return d;
		case DIALOG_DATE:
			return new DatePickerDialog(this, this, c.get(Calendar.YEAR), c
					.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
		case DIALOG_TIME:
			return new TimePickerDialog(this, this,
					c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE),
					FORMAT_AMPM);
		case DIALOG_TYPE:
			builder = new AlertDialog.Builder(this);
			builder.setItems(this.getResources().getStringArray(R.array.state),
					new DialogInterface.OnClickListener() {
						public void onClick(final DialogInterface dialog,
								final int item) {
							TravelLog.this.list.get(TravelLog.this.editItem)
									.setType(item);
							TravelLog.this.adapter.notifyDataSetChanged();
						}
					});
			return builder.create();
		default:
			return null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final boolean onCreateOptionsMenu(final Menu menu) {
		MenuInflater inflater = this.getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		if (this.prefsNoAds) {
			menu.removeItem(R.id.item_donate);
		}
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case R.id.item_settings: // start settings activity
			this.startActivity(new Intent(this, Preferences.class));
			return true;
		case R.id.item_clear:
			this.list.clear();
			this.adapter.notifyDataSetChanged();
			return true;
		case R.id.item_donate:
			this.startActivity(new Intent(this, DonationHelper.class));
			return true;
		case R.id.item_export:
			SharedPreferences p = PreferenceManager
					.getDefaultSharedPreferences(this);
			final String mail = p.getString(PREFS_MAIL,
					"nobody@set-your-prefs.com");
			final Intent in = new Intent(Intent.ACTION_SEND);
			in.putExtra(Intent.EXTRA_EMAIL, new String[] { mail, "" });
			// FIXME: "" is a k9 hack.
			final boolean flip = p.getBoolean(PREFS_FLIP_EXPORT, false);
			final StringBuilder buf = new StringBuilder();
			final int c = this.list.size();
			if (flip) {
				for (int i = c - 1; i >= 0; i--) {
					buf.append(this.list.get(i) + "\n");
				}
			} else {
				for (int i = 0; i < c; i++) {
					buf.append(this.list.get(i) + "\n");
				}
			}
			buf.append("\n");
			buf.append(this.getString(R.string.export_footer));
			buf.append(" ");
			buf.append(this.getString(R.string.website));
			buf.append("\n");
			in.putExtra(Intent.EXTRA_TEXT, buf.toString());
			in.putExtra(Intent.EXTRA_SUBJECT, this
					.getString(R.string.export_subject));
			in.setType("text/plain");
			this.startActivity(in);
			return true;
		default:
			return false;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void onResume() {
		super.onResume();
		this.reloadPreferences();
		if (!this.prefsNoAds) {
			this.findViewById(R.id.ad).setVisibility(View.VISIBLE);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void onPause() {
		super.onPause();
		this.savePreferences();
	}

	/** Save prefs. */
	private void savePreferences() {
		// save user preferences
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putInt(PREFS_STATE, this.state);
		int count = prefs.getInt(PREFS_LISTCOUNT, 0);
		for (int i = 0; i < count; i++) {
			editor.remove(PREFS_LIST_START + i);
			editor.remove(PREFS_LIST_STOP + i);
			editor.remove(PREFS_LIST_TYPE + i);
		}
		count = this.list.size();
		for (int i = 0; i < count; i++) {
			TravelItem itm = this.list.get(i);
			editor.putLong(PREFS_LIST_START + i, itm.getStart());
			editor.putLong(PREFS_LIST_STOP + i, itm.getEnd());
			editor.putInt(PREFS_LIST_TYPE + i, itm.getType());
		}
		editor.putInt(PREFS_LISTCOUNT, count);
		// commit changes
		editor.commit();
	}

	/**
	 * Read static vars holding preferences.
	 */
	private void reloadPreferences() {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		this.state = prefs.getInt(PREFS_STATE, STATE_NOTHING);
		this.list.clear();
		final int count = prefs.getInt(PREFS_LISTCOUNT, 0);
		for (int i = 0; i < count; i++) {
			final long start = prefs.getLong(PREFS_LIST_START + i, 0);
			final long end = prefs.getLong(PREFS_LIST_STOP + i, 0);
			final int type = prefs.getInt(PREFS_LIST_TYPE + i, 0);
			this.list.add(new TravelItem(start, end, type));
		}
		this.changeState(this.state, true);
		this.prefsRound = Integer.parseInt(prefs.getString(PREFS_ROUND, "0"));
	}

	/**
	 * Parse number of seconds to a readable time format.
	 * 
	 * @param milliseconds
	 *            milliseconds
	 * @return parsed string
	 */
	static final String getTime(final long milliseconds) {
		String ret;
		int seconds = (int) (milliseconds / 1000);
		int d = seconds / 86400;
		int h = (seconds % 86400) / 3600;
		int m = (seconds % 3600) / 60;
		if (d > 0) {
			ret = d + "d ";
		} else {
			ret = "";
		}
		if (h > 0 || d > 0) {
			if (h < 10) {
				ret += "0";
			}
			ret += h + ":";
		}
		if (m > 0 || h > 0 || d > 0) {
			if (m < 10 && h > 0) {
				ret += "0";
			}
		}
		ret += m;
		if (d == 0 && h == 0) {
			ret += "min";
		}
		return ret;
	}

	/**
	 * Round time as set in preferences.
	 * 
	 * @param time
	 *            unrounded time
	 * @return rounded time
	 */
	final long roundTime(final long time) {
		final int roundTo = this.prefsRound;
		long m = time / MILLIS_A_MINUTE; // cut down to full minutes
		if (roundTo == 0) {
			return m * MILLIS_A_MINUTE;
		}
		final Calendar c = Calendar.getInstance();
		c.setTimeInMillis(m * MILLIS_A_MINUTE);
		m = c.get(Calendar.MINUTE);
		final int r = (int) (m % roundTo);
		if (r != 0) {
			if (r >= roundTo / 2) {
				c.add(Calendar.MINUTE, -r + roundTo);
			} else {
				c.add(Calendar.MINUTE, -r);
			}
		}
		return c.getTimeInMillis();
	}
}
