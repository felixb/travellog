package de.ub0r.android.travelLog.ui;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import de.ub0r.android.lib.IPreferenceContainer;

/**
 * {@link PreferenceFragment} for API>=11.
 * 
 * @author flx
 */
public final class HeaderPreferenceFragment extends PreferenceFragment
		implements IPreferenceContainer {
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Activity a = this.getActivity();
		int res = a.getResources().getIdentifier(
				this.getArguments().getString("resource"), "xml",
				a.getPackageName());

		this.addPreferencesFromResource(res);
		Preferences.registerPreferenceChecker(this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Context getContext() {
		return this.getActivity();
	}
}
