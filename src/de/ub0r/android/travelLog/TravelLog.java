package de.ub0r.android.travelLog;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class TravelLog extends Activity implements OnClickListener {
	private static final int STATE_NOTHING = 0;
	private static final int STATE_PAUSE = 1;
	private static final int STATE_TRAVEL = 2;
	private static final int STATE_WORK = 3;

	private static final String PREFS_STARTE = "state";

	private int state = STATE_NOTHING;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.main);
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
	}

	private final void changeState(final int newState) {
		switch (newState) {
		case STATE_NOTHING:
			this.findViewById(R.id.start_pause_).setVisibility(View.VISIBLE);
			this.findViewById(R.id.stop_pause_).setVisibility(View.GONE);
			this.findViewById(R.id.start_travel_).setVisibility(View.VISIBLE);
			this.findViewById(R.id.stop_travel_).setVisibility(View.GONE);
			this.findViewById(R.id.start_work_).setVisibility(View.VISIBLE);
			this.findViewById(R.id.stop_work_).setVisibility(View.GONE);
			break;
		case STATE_PAUSE:
			this.findViewById(R.id.start_pause_).setVisibility(View.GONE);
			this.findViewById(R.id.stop_pause_).setVisibility(View.VISIBLE);
			this.findViewById(R.id.start_travel_).setVisibility(View.VISIBLE);
			this.findViewById(R.id.stop_travel_).setVisibility(View.GONE);
			this.findViewById(R.id.start_work_).setVisibility(View.VISIBLE);
			this.findViewById(R.id.stop_work_).setVisibility(View.GONE);
			break;
		case STATE_TRAVEL:
			this.findViewById(R.id.start_pause_).setVisibility(View.VISIBLE);
			this.findViewById(R.id.stop_pause_).setVisibility(View.GONE);
			this.findViewById(R.id.start_travel_).setVisibility(View.GONE);
			this.findViewById(R.id.stop_travel_).setVisibility(View.VISIBLE);
			this.findViewById(R.id.start_work_).setVisibility(View.VISIBLE);
			this.findViewById(R.id.stop_work_).setVisibility(View.GONE);
			break;
		case STATE_WORK:
			this.findViewById(R.id.start_pause_).setVisibility(View.VISIBLE);
			this.findViewById(R.id.stop_pause_).setVisibility(View.GONE);
			this.findViewById(R.id.start_travel_).setVisibility(View.VISIBLE);
			this.findViewById(R.id.stop_travel_).setVisibility(View.GONE);
			this.findViewById(R.id.start_work_).setVisibility(View.GONE);
			this.findViewById(R.id.stop_work_).setVisibility(View.VISIBLE);
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
		default:
			break;
		}
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
		editor.putInt(PREFS_STARTE, this.state);
		// commit changes
		editor.commit();
	}

	/**
	 * Read static vars holding preferences.
	 */
	private void reloadPreferences() {
		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		this.state = preferences.getInt(PREFS_STARTE, STATE_NOTHING);
		this.changeState(this.state);
	}
}
