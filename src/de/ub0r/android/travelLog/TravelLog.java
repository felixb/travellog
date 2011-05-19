package de.ub0r.android.travelLog;

import android.app.Application;
import de.ub0r.android.lib.Log;
import de.ub0r.android.lib.Utils;

/**
 * Main Activity.
 * 
 * @author flx
 */
public final class TravelLog extends Application {
	/** Tag for output. */
	private static final String TAG = "TravelLog";

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreate() {
		super.onCreate();
		Log.init(TAG);
		Utils.setLocale(this);
	}
}
