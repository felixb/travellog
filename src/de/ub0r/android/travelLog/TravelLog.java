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
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.TimePicker;
import android.widget.AdapterView.OnItemClickListener;

public class TravelLog extends Activity implements OnClickListener,
		OnItemClickListener, OnDateSetListener, OnTimeSetListener {
	private static final int STATE_NOTHING = 0;
	private static final int STATE_PAUSE = 1;
	private static final int STATE_TRAVEL = 2;
	private static final int STATE_WORK = 3;

	private static final int ACTION_CHG_DATE = 0;
	private static final int ACTION_CHG_START = 1;
	private static final int ACTION_CHG_END = 2;
	private static final int ACTION_CHG_TYPE = 3;
	private static final int ACTION_DELETE = 4;

	private static final int DIALOG_DATE = 0;
	private static final int DIALOG_TIME = 1;
	private static final int DIALOG_TYPE = 2;

	private static final String PREFS_STATE = "state";
	private static final String PREFS_LISTCOUNT = "log_n";
	private static final String PREFS_LIST_START = "log_start_";
	private static final String PREFS_LIST_STOP = "log_stop_";
	private static final String PREFS_LIST_TYPE = "log_type_";

	private static String[] namesStates;

	private int state = STATE_NOTHING;

	private ArrayAdapter<TravelItem> adapter;
	private ArrayList<TravelItem> list;

	private long editDate = 0;
	private int editType = 0;
	private int editItem = 0;

	private class TravelItem {
		private static final String FORMAT_DATE = "dd.MM.";
		private static final String FORMAT_TIME = "kk:mm";

		private long start;
		private long end;
		private int type;

		public TravelItem(final long s, final long e, final int t) {
			this.start = s;
			this.end = e;
			this.type = t;
		}

		public TravelItem(final int t) {
			this.start = System.currentTimeMillis();
			this.type = t;
		}

		public final long getStart() {
			return this.start;
		}

		public final long getEnd() {
			return this.end;
		}

		public final int getType() {
			return this.type;
		}

		public final void setEnd(final long e) {
			this.end = e;
		}

		public final void setStart(final long s) {
			this.start = s;
		}

		public final void setType(final int t) {
			this.type = t;
		}

		public final void start() {
			this.start = System.currentTimeMillis();
		}

		public final void terminate(final long e) {
			if (this.end <= 0) {
				this.end = e;
			}
		}

		public final void terminate() {
			if (this.end <= 0) {
				this.end = System.currentTimeMillis();
			}
		}

		@Override
		public String toString() {
			String ret = null;
			if (this.start > 0) {
				ret = DateFormat.format(FORMAT_DATE, this.start).toString();
				ret += " "
						+ DateFormat.format(FORMAT_TIME, this.start).toString();
			} else {
				ret = "??.??. ???";
			}
			ret += " - ";
			if (this.end > 0) {
				ret += DateFormat.format(FORMAT_TIME, this.end).toString();
			} else {
				ret += "???";
			}
			ret += ": " + TravelLog.namesStates[this.type];
			return ret;
		}
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.main);
		namesStates = this.getResources().getStringArray(R.array.state);
		((Button) this.findViewById(R.id.start_pause_))
				.setOnClickListener(this);
		((Button) this.findViewById(R.id.stop_pause_)).setOnClickListener(this);
		((Button) this.findViewById(R.id.start_travel_))
				.setOnClickListener(this);
		((Button) this.findViewById(R.id.stop_travel_))
				.setOnClickListener(this);
		((Button) this.findViewById(R.id.start_work_)).setOnClickListener(this);
		((Button) this.findViewById(R.id.stop_work_)).setOnClickListener(this);
		((Button) this.findViewById(R.id.add_row_)).setOnClickListener(this);
		((Button) this.findViewById(R.id.clear_)).setOnClickListener(this);
		this.list = new ArrayList<TravelItem>();
		this.adapter = new ArrayAdapter<TravelItem>(this, R.layout.list_item,
				android.R.id.text1, this.list);
		((ListView) this.findViewById(R.id.log)).setAdapter(this.adapter);
		((ListView) this.findViewById(R.id.log)).setOnItemClickListener(this);
	}

	private final void changeState(final int newState, final boolean btnOnly) {
		TravelItem itm = null;
		if (this.list.size() > 0) {
			itm = this.list.get(0);
		}
		switch (newState) {
		case STATE_NOTHING:
			this.findViewById(R.id.start_pause_).setVisibility(View.VISIBLE);
			this.findViewById(R.id.stop_pause_).setVisibility(View.GONE);
			this.findViewById(R.id.start_travel_).setVisibility(View.VISIBLE);
			this.findViewById(R.id.stop_travel_).setVisibility(View.GONE);
			this.findViewById(R.id.start_work_).setVisibility(View.VISIBLE);
			this.findViewById(R.id.stop_work_).setVisibility(View.GONE);
			if (!btnOnly) {
				if (itm != null) {
					itm.terminate();
				}
			}
			break;
		case STATE_PAUSE:
			this.findViewById(R.id.start_pause_).setVisibility(View.GONE);
			this.findViewById(R.id.stop_pause_).setVisibility(View.VISIBLE);
			this.findViewById(R.id.start_travel_).setVisibility(View.VISIBLE);
			this.findViewById(R.id.stop_travel_).setVisibility(View.GONE);
			this.findViewById(R.id.start_work_).setVisibility(View.VISIBLE);
			this.findViewById(R.id.stop_work_).setVisibility(View.GONE);
			if (!btnOnly) {
				if (itm != null) {
					itm.terminate();
				}
				this.list.add(0, new TravelItem(STATE_PAUSE));
			}
			break;
		case STATE_TRAVEL:
			this.findViewById(R.id.start_pause_).setVisibility(View.VISIBLE);
			this.findViewById(R.id.stop_pause_).setVisibility(View.GONE);
			this.findViewById(R.id.start_travel_).setVisibility(View.GONE);
			this.findViewById(R.id.stop_travel_).setVisibility(View.VISIBLE);
			this.findViewById(R.id.start_work_).setVisibility(View.VISIBLE);
			this.findViewById(R.id.stop_work_).setVisibility(View.GONE);
			if (!btnOnly) {
				if (itm != null) {
					itm.terminate();
				}
				this.list.add(0, new TravelItem(STATE_TRAVEL));
			}
			break;
		case STATE_WORK:
			this.findViewById(R.id.start_pause_).setVisibility(View.VISIBLE);
			this.findViewById(R.id.stop_pause_).setVisibility(View.GONE);
			this.findViewById(R.id.start_travel_).setVisibility(View.VISIBLE);
			this.findViewById(R.id.stop_travel_).setVisibility(View.GONE);
			this.findViewById(R.id.start_work_).setVisibility(View.GONE);
			this.findViewById(R.id.stop_work_).setVisibility(View.VISIBLE);
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
	 * A view is clicked!
	 * 
	 * @param view
	 *            the view
	 */
	@Override
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
		case R.id.stop_pause_:
		case R.id.stop_travel_:
		case R.id.stop_work_:
			this.changeState(STATE_NOTHING, false);
			break;
		case R.id.add_row_:
			this.list.add(new TravelItem(0, 0, 0));
			break;
		case R.id.clear_:
			this.list.clear();
			break;
		default:
			break;
		}
		this.adapter.notifyDataSetChanged();
	}

	/**
	 * Handle clicked ListItem.
	 * 
	 * @param parent
	 *            parent AdapterView
	 * @param v
	 *            View
	 * @param position
	 *            Position
	 * @param id
	 *            id
	 */
	@Override
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
						switch (item) {
						case ACTION_CHG_DATE:
							TravelLog.this.editDate = itm.getStart();
							TravelLog.this.showDialog(DIALOG_DATE);
							break;
						case ACTION_CHG_END:
							TravelLog.this.editDate = itm.getEnd();
							TravelLog.this.showDialog(DIALOG_TIME);
							break;
						case ACTION_CHG_START:
							TravelLog.this.editDate = itm.getStart();
							TravelLog.this.showDialog(DIALOG_TIME);
							break;
						case ACTION_CHG_TYPE:
							TravelLog.this.editType = itm.getType();
							TravelLog.this.showDialog(DIALOG_TYPE);
							break;
						case ACTION_DELETE:
							TravelLog.this.list.remove(position);
							TravelLog.this.adapter.notifyDataSetChanged();
							break;
						default:
							break;
						}
					}
				});
		builder.create().show();
	}

	public void onDateSet(final DatePicker view, final int year,
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

	@Override
	public void onTimeSet(final TimePicker view, final int hour,
			final int minutes) {
		final Calendar c = Calendar.getInstance();
		c.setTimeInMillis(this.editDate);
		c.set(Calendar.HOUR_OF_DAY, hour);
		c.set(Calendar.MINUTE, minutes);
		final TravelItem itm = this.list.get(this.editItem);
		if (itm.getStart() == this.editDate) {
			itm.setStart(c.getTimeInMillis());
		} else {
			itm.setEnd(c.getTimeInMillis());
		}
		this.adapter.notifyDataSetChanged();
	}

	protected final void onPrepareDialog(final int id, final Dialog dialog) {
		final Calendar c = Calendar.getInstance();
		c.setTimeInMillis(this.editDate);
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

	@Override
	protected Dialog onCreateDialog(final int id) {
		final Calendar c = Calendar.getInstance();
		c.setTimeInMillis(this.editDate);
		switch (id) {
		case DIALOG_DATE:
			return new DatePickerDialog(this, this, c.get(Calendar.YEAR), c
					.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
		case DIALOG_TIME:
			return new TimePickerDialog(this, this,
					c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true);
		case DIALOG_TYPE:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
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
		}
		return null;
	}

	/** Called on activity resume. */
	@Override
	protected final void onResume() {
		super.onResume();
		this.reloadPreferences();
	}

	/** Called on activity pause. */
	@Override
	public final void onPause() {
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
	}
}
