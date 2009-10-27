package de.ub0r.android.travelLog;

import java.util.ArrayList;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

public class TravelLog extends Activity implements OnClickListener {
	private static final int STATE_NOTHING = 0;
	private static final int STATE_PAUSE = 1;
	private static final int STATE_TRAVEL = 2;
	private static final int STATE_WORK = 3;

	private static final String PREFS_STATE = "state";

	private int state = STATE_NOTHING;

	private ArrayAdapter<TravelItem> adapter;
	private ArrayList<TravelItem> list;

	private static String[] namesStates;

	private class TravelItem {
		private static final String FORMAT = "dd.MM. hh:mm";

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

		public void setEnd(final long e) {
			this.end = e;
		}

		public void setStart(final long s) {
			this.start = s;
		}

		public void start() {
			this.start = System.currentTimeMillis();
		}

		public void terminate(final long e) {
			if (this.end <= 0) {
				this.end = e;
			}
		}

		public void terminate() {
			if (this.end <= 0) {
				this.end = System.currentTimeMillis();
			}
		}

		@Override
		public String toString() {
			String ret = null;
			if (this.start > 0) {
				ret = DateFormat.format(FORMAT, this.start).toString();
			} else {
				ret = "???";
			}
			ret += " - ";
			if (this.end > 0) {
				ret += DateFormat.format(FORMAT, this.end).toString();
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
		this.list = new ArrayList<TravelItem>();
		this.adapter = new ArrayAdapter<TravelItem>(this, R.layout.list_item,
				android.R.id.text1, this.list);
		((ListView) this.findViewById(R.id.log)).setAdapter(this.adapter);
	}

	private final void changeState(final int newState) {
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
			if (itm != null) {
				itm.terminate();
			}
			break;
		case STATE_PAUSE:
			this.findViewById(R.id.start_pause_).setVisibility(View.GONE);
			this.findViewById(R.id.stop_pause_).setVisibility(View.VISIBLE);
			this.findViewById(R.id.start_travel_).setVisibility(View.VISIBLE);
			this.findViewById(R.id.stop_travel_).setVisibility(View.GONE);
			this.findViewById(R.id.start_work_).setVisibility(View.VISIBLE);
			this.findViewById(R.id.stop_work_).setVisibility(View.GONE);
			if (itm != null) {
				itm.terminate();
			}
			this.list.add(0, new TravelItem(STATE_PAUSE));
			break;
		case STATE_TRAVEL:
			this.findViewById(R.id.start_pause_).setVisibility(View.VISIBLE);
			this.findViewById(R.id.stop_pause_).setVisibility(View.GONE);
			this.findViewById(R.id.start_travel_).setVisibility(View.GONE);
			this.findViewById(R.id.stop_travel_).setVisibility(View.VISIBLE);
			this.findViewById(R.id.start_work_).setVisibility(View.VISIBLE);
			this.findViewById(R.id.stop_work_).setVisibility(View.GONE);
			if (itm != null) {
				itm.terminate();
			}
			this.list.add(0, new TravelItem(STATE_TRAVEL));
			break;
		case STATE_WORK:
			this.findViewById(R.id.start_pause_).setVisibility(View.VISIBLE);
			this.findViewById(R.id.stop_pause_).setVisibility(View.GONE);
			this.findViewById(R.id.start_travel_).setVisibility(View.VISIBLE);
			this.findViewById(R.id.stop_travel_).setVisibility(View.GONE);
			this.findViewById(R.id.start_work_).setVisibility(View.GONE);
			this.findViewById(R.id.stop_work_).setVisibility(View.VISIBLE);
			if (itm != null) {
				itm.terminate();
			}
			this.list.add(0, new TravelItem(STATE_WORK));
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
			this.changeState(STATE_PAUSE);
			break;
		case R.id.start_travel_:
			this.changeState(STATE_TRAVEL);
			break;
		case R.id.start_work_:
			this.changeState(STATE_WORK);
			break;
		case R.id.stop_pause_:
		case R.id.stop_travel_:
		case R.id.stop_work_:
			this.changeState(STATE_NOTHING);
			break;
		case R.id.add_row_:
			this.list.add(new TravelItem(0, 0, 0));
			break;
		default:
			break;
		}
		this.adapter.notifyDataSetChanged();
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
	final void savePreferences() {
		// save user preferences
		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = preferences.edit();
		// common
		editor.putInt(PREFS_STATE, this.state);
		// commit changes
		editor.commit();
	}

	/**
	 * Read static vars holding preferences.
	 */
	private void reloadPreferences() {
		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		this.state = preferences.getInt(PREFS_STATE, STATE_NOTHING);
		this.changeState(this.state);
	}
}
