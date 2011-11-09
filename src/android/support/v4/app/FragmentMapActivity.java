/*
 * Copyright (C) 2011 The Android Open Source Project
 * 
 * Copyright (C) 2011 Jake Wharton <jakewharton@gmail.com>
 * 
 * Copyright (C) 2011 Felix Bechstein <f@ub0r.de>
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package android.support.v4.app;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.support.v4.view.ActionMode;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuInflater;
import android.support.v4.view.MenuItem;
import android.support.v4.view.Window;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;

import com.actionbarsherlock.R;
import com.actionbarsherlock.internal.app.ActionBarImpl;
import com.actionbarsherlock.internal.app.ActionBarWrapper;
import com.actionbarsherlock.internal.view.menu.MenuBuilder;
import com.actionbarsherlock.internal.view.menu.MenuInflaterWrapper;
import com.actionbarsherlock.internal.view.menu.MenuItemImpl;
import com.actionbarsherlock.internal.view.menu.MenuItemWrapper;
import com.actionbarsherlock.internal.view.menu.MenuWrapper;
import com.google.android.maps.MapActivity;

/**
 * Base class for activities that want to use the support-based ActionBar,
 * Fragment, and Loader APIs.
 * <p>
 * Known limitations:
 * </p>
 * <ul>
 * <li>
 * <p>
 * When using the &lt;fragment> tag, this implementation can not use the parent
 * view's ID as the new fragment's ID. You must explicitly specify an ID (or
 * tag) in the &lt;fragment>.
 * </p>
 * <li>
 * <p>
 * Prior to Honeycomb (3.0), an activity's state was saved before pausing.
 * Fragments are a significant amount of new state, and dynamic enough that one
 * often wants them to change between pausing and stopping. These classes throw
 * an exception if you try to change the fragment state after it has been saved,
 * to avoid accidental loss of UI state. However this is too restrictive prior
 * to Honeycomb, where the state is saved before pausing. To address this, when
 * running on platforms prior to Honeycomb an exception will not be thrown if
 * you change fragments between the state save and the activity being stopped.
 * This means that is some cases if the activity is restored from its last saved
 * state, this may be a snapshot slightly before what the user last saw.
 * </p>
 * </ul>
 */
public abstract class FragmentMapActivity extends MapActivity implements
		SupportActivity {
	private static final String TAG = "FragmentMapActivity";
	private static final boolean DEBUG = false;

	private static final String FRAGMENTS_TAG = "android:support:fragments";

	static final boolean IS_HONEYCOMB = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;

	static final int MSG_REALLY_STOPPED = 1;
	static final int MSG_RESUME_PENDING = 2;

	private static final int WINDOW_FLAG_ACTION_BAR = 1 << Window.FEATURE_ACTION_BAR;
	private static final int WINDOW_FLAG_ACTION_BAR_ITEM_TEXT = 1 << Window.FEATURE_ACTION_BAR_ITEM_TEXT;
	private static final int WINDOW_FLAG_ACTION_BAR_OVERLAY = 1 << Window.FEATURE_ACTION_BAR_OVERLAY;
	private static final int WINDOW_FLAG_ACTION_MODE_OVERLAY = 1 << Window.FEATURE_ACTION_MODE_OVERLAY;
	private static final int WINDOW_FLAG_INDETERMINANTE_PROGRESS = 1 << Window.FEATURE_INDETERMINATE_PROGRESS;

	final SupportActivity.InternalCallbacks mInternalCallbacks = new SupportActivity.InternalCallbacks() {
		@Override
		void invalidateSupportFragmentIndex(final int index) {
			FragmentMapActivity.this.invalidateSupportFragmentIndex(index);
		}

		@Override
		LoaderManagerImpl getLoaderManager(final int index,
				final boolean started, final boolean create) {
			return FragmentMapActivity.this.getLoaderManager(index, started,
					create);
		}

		@Override
		Handler getHandler() {
			return FragmentMapActivity.this.mHandler;
		}

		@Override
		FragmentManagerImpl getFragments() {
			return FragmentMapActivity.this.mFragments;
		}

		@Override
		void ensureSupportActionBarAttached() {
			FragmentMapActivity.this.ensureSupportActionBarAttached();
		}

		@Override
		boolean getRetaining() {
			return FragmentMapActivity.this.mRetaining;
		}
	};

	final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(final Message msg) {
			switch (msg.what) {
			case MSG_REALLY_STOPPED:
				if (FragmentMapActivity.this.mStopped) {
					FragmentMapActivity.this.doReallyStop(false);
				}
				break;
			case MSG_RESUME_PENDING:
				FragmentMapActivity.this.mFragments.dispatchResume();
				FragmentMapActivity.this.mFragments.execPendingActions();
				break;
			default:
				super.handleMessage(msg);
			}
		}
	};
	final FragmentManagerImpl mFragments = new FragmentManagerImpl();

	ActionBar mActionBar;
	boolean mIsActionBarImplAttached;
	long mWindowFlags = 0;

	final MenuBuilder mSupportMenu;
	final MenuBuilder.Callback mSupportMenuCallback = new MenuBuilder.Callback() {
		@Override
		public boolean onMenuItemSelected(final MenuBuilder menu,
				final MenuItem item) {
			return FragmentMapActivity.this.onMenuItemSelected(
					android.view.Window.FEATURE_OPTIONS_PANEL, item);
		}
	};

	boolean mCreated;
	boolean mResumed;
	boolean mStopped;
	boolean mReallyStopped;
	boolean mRetaining;

	boolean mOptionsMenuInvalidated;
	boolean mOptionsMenuCreateResult;

	boolean mCheckedForLoaderManager;
	boolean mLoadersStarted;
	HCSparseArray<LoaderManagerImpl> mAllLoaderManagers;
	LoaderManagerImpl mLoaderManager;

	static final class NonConfigurationInstances {
		Object activity;
		Object custom;
		HashMap<String, Object> children;
		ArrayList<Fragment> fragments;
		HCSparseArray<LoaderManagerImpl> loaders;
	}

	static class FragmentTag {
		public static final int[] Fragment = { 0x01010003, 0x010100d0,
				0x010100d1 };
		public static final int Fragment_id = 1;
		public static final int Fragment_name = 0;
		public static final int Fragment_tag = 2;
	}

	public FragmentMapActivity() {
		super();

		if (IS_HONEYCOMB) {
			this.mActionBar = ActionBarWrapper.createFor(this);
			this.mSupportMenu = null; // Everything should be done natively
		} else {
			this.mSupportMenu = new MenuBuilder(this);
			this.mSupportMenu.setCallback(this.mSupportMenuCallback);
		}
	}

	@Override
	public SupportActivity.InternalCallbacks getInternalCallbacks() {
		return this.mInternalCallbacks;
	}

	@Override
	public Activity asActivity() {
		return this;
	}

	protected void ensureSupportActionBarAttached() {
		if (IS_HONEYCOMB || this.mIsActionBarImplAttached) {
			return;
		}
		if (!this.isChild()) {
			if ((this.mWindowFlags & WINDOW_FLAG_ACTION_BAR) == WINDOW_FLAG_ACTION_BAR) {
				if ((this.mWindowFlags & WINDOW_FLAG_ACTION_BAR_OVERLAY) == WINDOW_FLAG_ACTION_BAR_OVERLAY) {
					super.setContentView(R.layout.abs__screen_action_bar_overlay);
				} else {
					super.setContentView(R.layout.abs__screen_action_bar);
				}

				this.mActionBar = new ActionBarImpl(this);
				((ActionBarImpl) this.mActionBar).init();

				final boolean textEnabled = ((this.mWindowFlags & WINDOW_FLAG_ACTION_BAR_ITEM_TEXT) == WINDOW_FLAG_ACTION_BAR_ITEM_TEXT);
				this.mSupportMenu.setShowsActionItemText(textEnabled);

				if ((this.mWindowFlags & WINDOW_FLAG_INDETERMINANTE_PROGRESS) == WINDOW_FLAG_INDETERMINANTE_PROGRESS) {
					((ActionBarImpl) this.mActionBar)
							.setProgressBarIndeterminateVisibility(false);
				}

				// TODO set other flags
			} else {
				if ((this.mWindowFlags & WINDOW_FLAG_INDETERMINANTE_PROGRESS) == WINDOW_FLAG_INDETERMINANTE_PROGRESS) {
					super.requestWindowFeature((int) Window.FEATURE_INDETERMINATE_PROGRESS);
				}
				super.setContentView(R.layout.abs__screen_simple);
			}
		}

		this.invalidateOptionsMenu();
		this.mIsActionBarImplAttached = true;
	}

	// ------------------------------------------------------------------------
	// HOOKS INTO ACTIVITY
	// ------------------------------------------------------------------------

	/**
	 * Enable extended window features.
	 * 
	 * @param featureId
	 *            The desired feature as defined in
	 *            {@link android.support.v4.view.Window}.
	 * @return Returns {@code true} if the requested feature is supported and
	 *         now enabled.
	 */
	@Override
	public boolean requestWindowFeature(final long featureId) {
		if (!IS_HONEYCOMB) {
			switch ((int) featureId) {
			case (int) Window.FEATURE_ACTION_BAR:
			case (int) Window.FEATURE_ACTION_BAR_ITEM_TEXT:
			case (int) Window.FEATURE_ACTION_BAR_OVERLAY:
			case (int) Window.FEATURE_ACTION_MODE_OVERLAY:
			case (int) Window.FEATURE_INDETERMINATE_PROGRESS:
				this.mWindowFlags |= (1 << featureId);
				return true;
			}
		}
		return super.requestWindowFeature((int) featureId);
	}

	@Override
	public android.view.MenuInflater getMenuInflater() {
		if (IS_HONEYCOMB) {
			if (DEBUG) {
				Log.d(TAG, "getMenuInflater(): Wrapping native inflater.");
			}

			// Wrap the native inflater so it can unwrap the native menu first
			return new MenuInflaterWrapper(this, super.getMenuInflater());
		}

		if (DEBUG) {
			Log.d(TAG, "getMenuInflater(): Returning support inflater.");
		}

		// Use our custom menu inflater
		return new MenuInflater(this, super.getMenuInflater());
	}

	@Override
	public void setContentView(final int layoutResId) {
		this.ensureSupportActionBarAttached();
		if (IS_HONEYCOMB || this.isChild()) {
			super.setContentView(layoutResId);
		} else {
			FrameLayout contentView = (FrameLayout) this
					.findViewById(R.id.abs__content);
			contentView.removeAllViews();
			this.getLayoutInflater().inflate(layoutResId, contentView, true);
		}
	}

	@Override
	public void setContentView(final View view, final LayoutParams params) {
		this.ensureSupportActionBarAttached();
		if (IS_HONEYCOMB || this.isChild()) {
			super.setContentView(view, params);
		} else {
			FrameLayout contentView = (FrameLayout) this
					.findViewById(R.id.abs__content);
			contentView.removeAllViews();
			contentView.addView(view, params);
		}
	}

	@Override
	public void setContentView(final View view) {
		this.ensureSupportActionBarAttached();
		if (IS_HONEYCOMB || this.isChild()) {
			super.setContentView(view);
		} else {
			FrameLayout contentView = (FrameLayout) this
					.findViewById(R.id.abs__content);
			contentView.removeAllViews();
			contentView.addView(view);
		}
	}

	@Override
	public void setTitle(final CharSequence title) {
		if (IS_HONEYCOMB || (this.getSupportActionBar() == null)) {
			super.setTitle(title);
		} else {
			this.getSupportActionBar().setTitle(title);
		}
	}

	@Override
	public void setTitle(final int titleId) {
		if (IS_HONEYCOMB || (this.getSupportActionBar() == null)) {
			super.setTitle(titleId);
		} else {
			this.getSupportActionBar().setTitle(titleId);
		}
	}

	/**
	 * Dispatch incoming result to the correct fragment.
	 */
	@Override
	protected void onActivityResult(final int requestCode,
			final int resultCode, final Intent data) {
		int index = requestCode >> 16;
		if (index != 0) {
			index--;
			if (this.mFragments.mActive == null || index < 0
					|| index >= this.mFragments.mActive.size()) {
				Log.w(TAG, "Activity result fragment index out of range: 0x"
						+ Integer.toHexString(requestCode));
				return;
			}
			Fragment frag = this.mFragments.mActive.get(index);
			if (frag == null) {
				Log.w(TAG, "Activity result no fragment exists for index: 0x"
						+ Integer.toHexString(requestCode));
			} else {
				frag.onActivityResult(requestCode & 0xffff, resultCode, data);
			}
			return;
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected void onApplyThemeResource(final Theme theme, final int resid,
			final boolean first) {
		TypedArray attrs = theme.obtainStyledAttributes(resid,
				R.styleable.SherlockTheme);

		final boolean actionBar = attrs.getBoolean(
				R.styleable.SherlockTheme_windowActionBar, false);
		this.mWindowFlags |= actionBar ? WINDOW_FLAG_ACTION_BAR : 0;

		final boolean actionModeOverlay = attrs.getBoolean(
				R.styleable.SherlockTheme_windowActionModeOverlay, false);
		this.mWindowFlags |= actionModeOverlay ? WINDOW_FLAG_ACTION_MODE_OVERLAY
				: 0;

		attrs.recycle();
		super.onApplyThemeResource(theme, resid, first);
	}

	/**
	 * Take care of popping the fragment back stack or finishing the activity as
	 * appropriate.
	 */
	@Override
	public void onBackPressed() {
		if (!this.mFragments.popBackStackImmediate()) {
			this.finish();
		}
	}

	/**
	 * Dispatch configuration change to all fragments.
	 */
	@Override
	public void onConfigurationChanged(final Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		this.mFragments.dispatchConfigurationChanged(newConfig);
	}

	/**
	 * Perform initialization of all fragments and loaders.
	 */
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		this.mFragments.attachActivity(this);
		// Old versions of the platform didn't do this!
		if (this.getLayoutInflater().getFactory() == null) {
			this.getLayoutInflater().setFactory(this);
		}

		super.onCreate(savedInstanceState);

		NonConfigurationInstances nc = (NonConfigurationInstances) this
				.getLastNonConfigurationInstance();
		if (nc != null) {
			this.mAllLoaderManagers = nc.loaders;
		}
		if (savedInstanceState != null) {
			Parcelable p = savedInstanceState.getParcelable(FRAGMENTS_TAG);
			this.mFragments
					.restoreAllState(p, nc != null ? nc.fragments : null);
		}
		this.mFragments.dispatchCreate();
	}

	/**
	 * <p>
	 * Initialize the contents of the Activity's standard options menu. You
	 * should place your menu items in to menu.
	 * </p>
	 * <p>
	 * The default implementation populates the menu with standard system menu
	 * items. These are placed in the {@link Menu.CATEGORY_SYSTEM} group so that
	 * they will be correctly ordered with application-defined menu items.
	 * Deriving classes should always call through to the base implementation.
	 * </p>
	 * <p>
	 * You can safely hold on to menu (and any items created from it), making
	 * modifications to it as desired, until the next time
	 * {@code onCreateOptionsMenu()} is called.
	 * </p>
	 * <p>
	 * When you add items to the menu, you can implement the Activity's
	 * {@link #onOptionsItemSelected(MenuItem)} method to handle them there.
	 * </p>
	 * 
	 * @param menu
	 *            The options menu in which you place your items.
	 * @return You must return true for the menu to be displayed; if you return
	 *         false it will not be shown.
	 */
	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		if (DEBUG) {
			Log.d(TAG,
					"onCreateOptionsMenu(Menu): Returning "
							+ menu.hasVisibleItems());
		}
		return menu.hasVisibleItems();
	}

	@Override
	public final boolean onCreateOptionsMenu(final android.view.Menu menu) {
		// Prior to Honeycomb, the framework can't invalidate the options
		// menu, so we must always say we have one in case the app later
		// invalidates it and needs to have it shown.
		boolean result = true;

		if (IS_HONEYCOMB) {
			if (DEBUG) {
				Log.d(TAG,
						"onCreateOptionsMenu(android.view.Menu): Calling support method with wrapped native menu.");
			}
			MenuWrapper wrapped = new MenuWrapper(menu);
			result = this.onCreateOptionsMenu(wrapped);
			result |= this.mFragments.dispatchCreateOptionsMenu(wrapped,
					this.getMenuInflater());
		}

		if (DEBUG) {
			Log.d(TAG, "onCreateOptionsMenu(android.view.Menu): Returning "
					+ result);
		}
		return result;
	}

	/**
	 * Add support for inflating the &lt;fragment> tag.
	 */
	@Override
	public View onCreateView(final String name, final Context context,
			final AttributeSet attrs) {
		if (!"fragment".equals(name)) {
			return super.onCreateView(name, context, attrs);
		}

		String fname = attrs.getAttributeValue(null, "class");
		TypedArray a = context.obtainStyledAttributes(attrs,
				FragmentTag.Fragment);
		if (fname == null) {
			fname = a.getString(FragmentTag.Fragment_name);
		}
		int id = a.getResourceId(FragmentTag.Fragment_id, View.NO_ID);
		String tag = a.getString(FragmentTag.Fragment_tag);
		a.recycle();

		View parent = null; // NOTE: no way to get parent pre-Honeycomb.
		int containerId = parent != null ? parent.getId() : 0;
		if (containerId == View.NO_ID && id == View.NO_ID && tag == null) {
			throw new IllegalArgumentException(
					attrs.getPositionDescription()
							+ ": Must specify unique android:id, android:tag, or have a parent with an id for "
							+ fname);
		}

		// If we restored from a previous state, we may already have
		// instantiated this fragment from the state and should use
		// that instance instead of making a new one.
		Fragment fragment = id != View.NO_ID ? this.mFragments
				.findFragmentById(id) : null;
		if (fragment == null && tag != null) {
			fragment = this.mFragments.findFragmentByTag(tag);
		}
		if (fragment == null && containerId != View.NO_ID) {
			fragment = this.mFragments.findFragmentById(containerId);
		}

		if (FragmentManagerImpl.DEBUG) {
			Log.v(TAG, "onCreateView: id=0x" + Integer.toHexString(id)
					+ " fname=" + fname + " existing=" + fragment);
		}
		if (fragment == null) {
			fragment = Fragment.instantiate(this, fname);
			fragment.mFromLayout = true;
			fragment.mFragmentId = id != 0 ? id : containerId;
			fragment.mContainerId = containerId;
			fragment.mTag = tag;
			fragment.mInLayout = true;
			fragment.mFragmentManager = this.mFragments;
			fragment.onInflate(this, attrs, fragment.mSavedFragmentState);
			this.mFragments.addFragment(fragment, true);

		} else if (fragment.mInLayout) {
			// A fragment already exists and it is not one we restored from
			// previous state.
			throw new IllegalArgumentException(attrs.getPositionDescription()
					+ ": Duplicate id 0x" + Integer.toHexString(id) + ", tag "
					+ tag + ", or parent id 0x"
					+ Integer.toHexString(containerId)
					+ " with another fragment for " + fname);
		} else {
			// This fragment was retained from a previous instance; get it
			// going now.
			fragment.mInLayout = true;
			// If this fragment is newly instantiated (either right now, or
			// from last saved state), then give it the attributes to
			// initialize itself.
			if (!fragment.mRetaining) {
				fragment.onInflate(this, attrs, fragment.mSavedFragmentState);
			}
			this.mFragments.moveToState(fragment);
		}

		if (fragment.mView == null) {
			throw new IllegalStateException("Fragment " + fname
					+ " did not create a view.");
		}
		if (id != 0) {
			fragment.mView.setId(id);
		}
		if (fragment.mView.getTag() == null) {
			fragment.mView.setTag(tag);
		}
		return fragment.mView;
	}

	@Override
	public void invalidateOptionsMenu() {
		if (DEBUG) {
			Log.d(TAG, "supportInvalidateOptionsMenu(): Invalidating menu.");
		}

		if (IS_HONEYCOMB) {
			HoneycombInvalidateOptionsMenu.invoke(this);
		} else {
			this.mSupportMenu.clear();

			this.mOptionsMenuCreateResult = this
					.onCreateOptionsMenu(this.mSupportMenu);
			this.mOptionsMenuCreateResult |= this.mFragments
					.dispatchCreateOptionsMenu(this.mSupportMenu,
							this.getMenuInflater());

			if (this.getSupportActionBar() != null) {
				if (this.onPrepareOptionsMenu(this.mSupportMenu)) {
					this.mFragments
							.dispatchPrepareOptionsMenu(this.mSupportMenu);
				}

				// Since we now know we are using a custom action bar, perform
				// the
				// inflation callback to allow it to display any items it wants.
				((ActionBarImpl) this.mActionBar)
						.onMenuInflated(this.mSupportMenu);
			}

			// Whoops, older platform... we'll use a hack, to manually rebuild
			// the options menu the next time it is prepared.
			this.mOptionsMenuInvalidated = true;
		}
	}

	private static final class HoneycombInvalidateOptionsMenu {
		static void invoke(final Activity activity) {
			activity.getWindow().invalidatePanelMenu(
					android.view.Window.FEATURE_OPTIONS_PANEL);
		}
	}

	/**
	 * Destroy all fragments and loaders.
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();

		this.doReallyStop(false);

		this.mFragments.dispatchDestroy();
		if (this.mLoaderManager != null) {
			this.mLoaderManager.doDestroy();
		}
	}

	/**
	 * Take care of calling onBackPressed() for pre-Eclair platforms.
	 */
	@Override
	public boolean onKeyDown(final int keyCode, final KeyEvent event) {
		if (android.os.Build.VERSION.SDK_INT < 5 /* ECLAIR */
				&& keyCode == KeyEvent.KEYCODE_BACK
				&& event.getRepeatCount() == 0) {
			// Take care of calling this method on earlier versions of
			// the platform where it doesn't exist.
			this.onBackPressed();
			return true;
		}

		return super.onKeyDown(keyCode, event);
	}

	/**
	 * Dispatch onLowMemory() to all fragments.
	 */
	@Override
	public void onLowMemory() {
		super.onLowMemory();
		this.mFragments.dispatchLowMemory();
	}

	/**
	 * Dispatch context and options menu to fragments.
	 */
	@Override
	public final boolean onMenuItemSelected(final int featureId,
			final android.view.MenuItem item) {
		if (super.onMenuItemSelected(featureId, item)) {
			return true;
		}

		switch (featureId) {
		case android.view.Window.FEATURE_OPTIONS_PANEL:
			return this.mFragments
					.dispatchOptionsItemSelected(new MenuItemWrapper(item));

		case android.view.Window.FEATURE_CONTEXT_MENU:
			return this.mFragments
					.dispatchContextItemSelected(new MenuItemWrapper(item));

		default:
			return false;
		}
	}

	@Override
	public boolean onMenuItemSelected(final int featureId, final MenuItem item) {
		if (this.onOptionsItemSelected(item)) {
			return true;
		}

		switch (featureId) {
		case android.view.Window.FEATURE_OPTIONS_PANEL:
			return this.mFragments.dispatchOptionsItemSelected(item);

		case android.view.Window.FEATURE_CONTEXT_MENU:
			return this.mFragments.dispatchContextItemSelected(item);

		default:
			return false;
		}
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		return super.onOptionsItemSelected(item);
	}

	@Override
	public final boolean onOptionsItemSelected(final android.view.MenuItem item) {
		return this.onOptionsItemSelected(new MenuItemWrapper(item));
	}

	/**
	 * Call onOptionsMenuClosed() on fragments.
	 */
	@Override
	public void onPanelClosed(final int featureId, final android.view.Menu menu) {
		switch (featureId) {
		case android.view.Window.FEATURE_OPTIONS_PANEL:
			this.mFragments.dispatchOptionsMenuClosed(new MenuWrapper(menu));

			if (!IS_HONEYCOMB && (this.getSupportActionBar() != null)) {
				if (DEBUG) {
					Log.d(TAG,
							"onPanelClosed(int, android.view.Menu): Dispatch menu visibility false to custom action bar.");
				}
				((ActionBarImpl) this.mActionBar)
						.onMenuVisibilityChanged(false);
			}
			break;
		}
		super.onPanelClosed(featureId, menu);
	}

	/**
	 * Dispatch onPause() to fragments.
	 */
	@Override
	protected void onPause() {
		super.onPause();
		this.mResumed = false;
		if (this.mHandler.hasMessages(MSG_RESUME_PENDING)) {
			this.mHandler.removeMessages(MSG_RESUME_PENDING);
			this.mFragments.dispatchResume();
		}
		this.mFragments.dispatchPause();
	}

	/**
	 * Dispatch onResume() to fragments.
	 */
	@Override
	protected void onResume() {
		super.onResume();
		this.mHandler.sendEmptyMessage(MSG_RESUME_PENDING);
		this.mResumed = true;
		this.mFragments.execPendingActions();
	}

	/**
	 * Dispatch onResume() to fragments.
	 */
	@Override
	protected void onPostResume() {
		super.onPostResume();
		this.mHandler.removeMessages(MSG_RESUME_PENDING);
		this.mFragments.dispatchResume();
		this.mFragments.execPendingActions();
	}

	@Override
	public boolean onPrepareOptionsMenu(final Menu menu) {
		boolean result = menu.hasVisibleItems();
		if (DEBUG) {
			Log.d(TAG, "onPrepareOptionsMenu(Menu): Returning " + result);
		}
		return result;
	}

	@Override
	public final boolean onPrepareOptionsMenu(final android.view.Menu menu) {
		boolean result = super.onPrepareOptionsMenu(menu);

		if (!IS_HONEYCOMB) {
			if (DEBUG) {
				Log.d(TAG,
						"onPrepareOptionsMenu(android.view.Menu): mOptionsMenuCreateResult = "
								+ this.mOptionsMenuCreateResult);
				Log.d(TAG,
						"onPrepareOptionsMenu(android.view.Menu): mOptionsMenuInvalidated = "
								+ this.mOptionsMenuInvalidated);
			}

			boolean prepareResult = true;
			if (this.mOptionsMenuCreateResult) {
				if (DEBUG) {
					Log.d(TAG,
							"onPrepareOptionsMenu(android.view.Menu): Calling support method with custom menu.");
				}
				prepareResult = this.onPrepareOptionsMenu(this.mSupportMenu);
				if (DEBUG) {
					Log.d(TAG,
							"onPrepareOptionsMenu(android.view.Menu): Support method result returned "
									+ prepareResult);
				}
				if (prepareResult) {
					if (DEBUG) {
						Log.d(TAG,
								"onPrepareOptionsMenu(android.view.Menu): Dispatching fragment method with custom menu.");
					}
					this.mFragments
							.dispatchPrepareOptionsMenu(this.mSupportMenu);
				}
			}

			if (this.mOptionsMenuInvalidated) {
				if (DEBUG) {
					Log.d(TAG,
							"onPrepareOptionsMenu(android.view.Menu): Clearing existing options menu.");
				}
				menu.clear();
				this.mOptionsMenuInvalidated = false;

				if (this.mOptionsMenuCreateResult && prepareResult) {
					if (DEBUG) {
						Log.d(TAG,
								"onPrepareOptionsMenu(android.view.Menu): Adding any action items that are not displayed on the action bar.");
					}
					// Only add items that have not already been added to our
					// custom
					// action bar implementation
					for (MenuItemImpl item : this.mSupportMenu.getItems()) {
						if (!item.isShownOnActionBar()) {
							item.addTo(menu);
						}
					}
				}
			}

			if (this.mOptionsMenuCreateResult && prepareResult
					&& menu.hasVisibleItems()) {
				if (this.getSupportActionBar() != null) {
					if (DEBUG) {
						Log.d(TAG,
								"onPrepareOptionsMenu(android.view.Menu): Dispatch menu visibility true to custom action bar.");
					}
					((ActionBarImpl) this.mActionBar)
							.onMenuVisibilityChanged(true);
				}
				result = true;
			}
		} else {
			if (DEBUG) {
				Log.d(TAG,
						"onPrepareOptionsMenu(android.view.Menu): Calling support method with wrapped native menu.");
			}
			final MenuWrapper wrappedMenu = new MenuWrapper(menu);
			result = this.onPrepareOptionsMenu(wrappedMenu);
			if (result) {
				if (DEBUG) {
					Log.d(TAG,
							"onPrepareOptionsMenu(android.view.Menu): Dispatching fragment method with wrapped native menu.");
				}
				this.mFragments.dispatchPrepareOptionsMenu(wrappedMenu);
			}
		}

		if (DEBUG) {
			Log.d(TAG, "onPrepareOptionsMenu(android.view.Menu): Returning "
					+ result);
		}
		return result;
	}

	/**
	 * Cause this Activity to be recreated with a new instance. This results in
	 * essentially the same flow as when the Activity is created due to a
	 * configuration change -- the current instance will go through its
	 * lifecycle to onDestroy() and a new instance then created after it.
	 */
	@Override
	public void recreate() {
		// This SUCKS! Figure out a way to call the super method and support
		// Android 1.6
		/*
		 * if (IS_HONEYCOMB) { super.recreate(); } else {
		 */
		final Intent intent = this.getIntent();
		intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

		this.startActivity(intent);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR) {
			OverridePendingTransition.invoke(this);
		}

		this.finish();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR) {
			OverridePendingTransition.invoke(this);
		}
		/*
		 * }
		 */
	}

	private static final class OverridePendingTransition {
		static void invoke(final Activity activity) {
			activity.overridePendingTransition(0, 0);
		}
	}

	/**
	 * Retain all appropriate fragment and loader state. You can NOT override
	 * this yourself! Use {@link #onRetainCustomNonConfigurationInstance()} if
	 * you want to retain your own state.
	 */
	@Override
	public final Object onRetainNonConfigurationInstance() {
		if (this.mStopped) {
			this.doReallyStop(true);
		}

		Object custom = this.onRetainCustomNonConfigurationInstance();

		ArrayList<Fragment> fragments = this.mFragments.retainNonConfig();
		boolean retainLoaders = false;
		if (this.mAllLoaderManagers != null) {
			// prune out any loader managers that were already stopped and so
			// have nothing useful to retain.
			for (int i = this.mAllLoaderManagers.size() - 1; i >= 0; i--) {
				LoaderManagerImpl lm = this.mAllLoaderManagers.valueAt(i);
				if (lm.mRetaining) {
					retainLoaders = true;
				} else {
					lm.doDestroy();
					this.mAllLoaderManagers.removeAt(i);
				}
			}
		}
		if (fragments == null && !retainLoaders && custom == null) {
			return null;
		}

		NonConfigurationInstances nci = new NonConfigurationInstances();
		nci.activity = null;
		nci.custom = custom;
		nci.children = null;
		nci.fragments = fragments;
		nci.loaders = this.mAllLoaderManagers;
		return nci;
	}

	/**
	 * Save all appropriate fragment state.
	 */
	@Override
	protected void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		Parcelable p = this.mFragments.saveAllState();
		if (p != null) {
			outState.putParcelable(FRAGMENTS_TAG, p);
		}
	}

	/**
	 * Dispatch onStart() to all fragments. Ensure any created loaders are now
	 * started.
	 */
	@Override
	protected void onStart() {
		super.onStart();

		this.mStopped = false;
		this.mReallyStopped = false;
		this.mHandler.removeMessages(MSG_REALLY_STOPPED);

		if (!this.mCreated) {
			this.mCreated = true;
			this.ensureSupportActionBarAttached(); // Needed for retained
													// fragments
			this.mFragments.dispatchActivityCreated();
		}

		this.mFragments.noteStateNotSaved();
		this.mFragments.execPendingActions();

		if (!this.mLoadersStarted) {
			this.mLoadersStarted = true;
			if (this.mLoaderManager != null) {
				this.mLoaderManager.doStart();
			} else if (!this.mCheckedForLoaderManager) {
				this.mLoaderManager = this.getLoaderManager(-1,
						this.mLoadersStarted, false);
			}
			this.mCheckedForLoaderManager = true;
		}
		// NOTE: HC onStart goes here.

		this.mFragments.dispatchStart();
		if (this.mAllLoaderManagers != null) {
			for (int i = this.mAllLoaderManagers.size() - 1; i >= 0; i--) {
				LoaderManagerImpl lm = this.mAllLoaderManagers.valueAt(i);
				lm.finishRetain();
				lm.doReportStart();
			}
		}
	}

	/**
	 * Dispatch onStop() to all fragments. Ensure all loaders are stopped.
	 */
	@Override
	protected void onStop() {
		super.onStop();

		this.mStopped = true;
		this.mHandler.sendEmptyMessage(MSG_REALLY_STOPPED);

		this.mFragments.dispatchStop();
	}

	/**
	 * <p>
	 * Sets the visibility of the indeterminate progress bar in the title.
	 * </p>
	 * <p>
	 * In order for the progress bar to be shown, the feature must be requested
	 * via {@link #requestWindowFeature(long)}.
	 * </p>
	 * <p>
	 * <strong>This method must be used instead of
	 * {@link #setProgressBarIndeterminateVisibility(boolean)} for
	 * ActionBarSherlock.</strong> Pass {@link Boolean.TRUE} or
	 * {@link Boolean.FALSE} to ensure the appropriate one is called.
	 * </p>
	 * 
	 * @param visible
	 *            Whether to show the progress bars in the title.
	 */
	@Override
	public void setProgressBarIndeterminateVisibility(final Boolean visible) {
		if (IS_HONEYCOMB || (this.getSupportActionBar() == null)) {
			super.setProgressBarIndeterminateVisibility(visible);
		} else if ((this.mWindowFlags & WINDOW_FLAG_INDETERMINANTE_PROGRESS) == WINDOW_FLAG_INDETERMINANTE_PROGRESS) {
			((ActionBarImpl) this.mActionBar)
					.setProgressBarIndeterminateVisibility(visible);
		}
	}

	// ------------------------------------------------------------------------
	// NEW METHODS
	// ------------------------------------------------------------------------

	/**
	 * Use this instead of {@link #onRetainNonConfigurationInstance()}. Retrieve
	 * later with {@link #getLastCustomNonConfigurationInstance()}.
	 */
	public Object onRetainCustomNonConfigurationInstance() {
		return null;
	}

	/**
	 * Return the value previously returned from
	 * {@link #onRetainCustomNonConfigurationInstance()}.
	 */
	public Object getLastCustomNonConfigurationInstance() {
		NonConfigurationInstances nc = (NonConfigurationInstances) this
				.getLastNonConfigurationInstance();
		return nc != null ? nc.custom : null;
	}

	/**
	 * @deprecated Use {@link invalidateOptionsMenu}.
	 */
	@Deprecated
	void supportInvalidateOptionsMenu() {
		this.invalidateOptionsMenu();
	}

	/**
	 * Print the Activity's state into the given stream. This gets invoked if
	 * you run "adb shell dumpsys activity <activity_component_name>".
	 * 
	 * @param prefix
	 *            Desired prefix to prepend at each line of output.
	 * @param fd
	 *            The raw file descriptor that the dump is being sent to.
	 * @param writer
	 *            The PrintWriter to which you should dump your state. This will
	 *            be closed for you after you return.
	 * @param args
	 *            additional arguments to the dump request.
	 */
	@Override
	public void dump(final String prefix, final FileDescriptor fd,
			final PrintWriter writer, final String[] args) {
		if (IS_HONEYCOMB) {
			// This can only work if we can call the super-class impl. :/
			// ActivityCompatHoneycomb.dump(this, prefix, fd, writer, args);
		}
		writer.print(prefix);
		writer.print("Local FragmentActivity ");
		writer.print(Integer.toHexString(System.identityHashCode(this)));
		writer.println(" State:");
		String innerPrefix = prefix + "  ";
		writer.print(innerPrefix);
		writer.print("mCreated=");
		writer.print(this.mCreated);
		writer.print("mResumed=");
		writer.print(this.mResumed);
		writer.print(" mStopped=");
		writer.print(this.mStopped);
		writer.print(" mReallyStopped=");
		writer.println(this.mReallyStopped);
		writer.print(innerPrefix);
		writer.print("mLoadersStarted=");
		writer.println(this.mLoadersStarted);
		if (this.mLoaderManager != null) {
			writer.print(prefix);
			writer.print("Loader Manager ");
			writer.print(Integer.toHexString(System
					.identityHashCode(this.mLoaderManager)));
			writer.println(":");
			this.mLoaderManager.dump(prefix + "  ", fd, writer, args);
		}
		this.mFragments.dump(prefix, fd, writer, args);
	}

	void doReallyStop(final boolean retaining) {
		if (!this.mReallyStopped) {
			this.mReallyStopped = true;
			this.mRetaining = retaining;
			this.mHandler.removeMessages(MSG_REALLY_STOPPED);
			this.onReallyStop();
		}
	}

	/**
	 * Pre-HC, we didn't have a way to determine whether an activity was being
	 * stopped for a config change or not until we saw
	 * onRetainNonConfigurationInstance() called after onStop(). However we need
	 * to know this, to know whether to retain fragments. This will tell us what
	 * we need to know.
	 */
	void onReallyStop() {
		if (this.mLoadersStarted) {
			this.mLoadersStarted = false;
			if (this.mLoaderManager != null) {
				if (!this.mRetaining) {
					this.mLoaderManager.doStop();
				} else {
					this.mLoaderManager.doRetain();
				}
			}
		}

		this.mFragments.dispatchReallyStop();
	}

	// ------------------------------------------------------------------------
	// ACTION BAR AND ACTION MODE SUPPORT
	// ------------------------------------------------------------------------

	/**
	 * Retrieve a reference to this activity's action bar handler.
	 * 
	 * @return The handler for the appropriate action bar, or null.
	 */
	@Override
	public ActionBar getSupportActionBar() {
		return (this.mActionBar != null) ? this.mActionBar.getPublicInstance()
				: null;
	}

	/**
	 * Notifies the activity that an action mode has finished. Activity
	 * subclasses overriding this method should call the superclass
	 * implementation.
	 * 
	 * @param mode
	 *            The action mode that just finished.
	 */
	@Override
	public void onActionModeFinished(final ActionMode mode) {
	}

	/**
	 * Notifies the Activity that an action mode has been started. Activity
	 * subclasses overriding this method should call the superclass
	 * implementation.
	 * 
	 * @param mode
	 *            The new action mode.
	 */
	@Override
	public void onActionModeStarted(final ActionMode mode) {
	}

	/**
	 * <p>
	 * Give the Activity a chance to control the UI for an action mode requested
	 * by the system.
	 * </p>
	 * <p>
	 * Note: If you are looking for a notification callback that an action mode
	 * has been started for this activity, see
	 * {@link #onActionModeStarted(ActionMode)}.
	 * </p>
	 * 
	 * @param callback
	 *            The callback that should control the new action mode
	 * @return The new action mode, or null if the activity does not want to
	 *         provide special handling for this action mode. (It will be
	 *         handled by the system.)
	 */
	@Override
	public ActionMode onWindowStartingActionMode(
			final ActionMode.Callback callback) {
		return null;
	}

	/**
	 * Start an action mode.
	 * 
	 * @param callback
	 *            Callback that will manage lifecycle events for this context
	 *            mode
	 * @return The ContextMode that was started, or null if it was cancelled
	 * @see android.support.v4.view.ActionMode
	 */
	@Override
	public final ActionMode startActionMode(final ActionMode.Callback callback) {
		// Give the activity override a chance to handle the action mode
		ActionMode actionMode = this.onWindowStartingActionMode(callback);

		if (actionMode == null) {
			// If the activity did not handle, send to action bar for platform-
			// specific implementation
			actionMode = this.mActionBar.startActionMode(callback);
		}
		if (actionMode != null) {
			// Send the activity callback that our action mode was started
			this.onActionModeStarted(actionMode);
		}

		// Return to the caller
		return actionMode;
	}

	// ------------------------------------------------------------------------
	// FRAGMENT SUPPORT
	// ------------------------------------------------------------------------

	/**
	 * Called when a fragment is attached to the activity.
	 */
	@Override
	public void onAttachFragment(final Fragment fragment) {
	}

	/**
	 * Return the FragmentManager for interacting with fragments associated with
	 * this activity.
	 */
	@Override
	public FragmentManager getSupportFragmentManager() {
		return this.mFragments;
	}

	/**
	 * Modifies the standard behavior to allow results to be delivered to
	 * fragments. This imposes a restriction that requestCode be <= 0xffff.
	 */
	@Override
	public void startActivityForResult(final Intent intent,
			final int requestCode) {
		if (requestCode != -1 && (requestCode & 0xffff0000) != 0) {
			throw new IllegalArgumentException(
					"Can only use lower 16 bits for requestCode");
		}
		super.startActivityForResult(intent, requestCode);
	}

	/**
	 * Called by Fragment.startActivityForResult() to implement its behavior.
	 */
	@Override
	public void startActivityFromFragment(final Fragment fragment,
			final Intent intent, final int requestCode) {
		if (requestCode == -1) {
			super.startActivityForResult(intent, -1);
			return;
		}
		if ((requestCode & 0xffff0000) != 0) {
			throw new IllegalArgumentException(
					"Can only use lower 16 bits for requestCode");
		}
		super.startActivityForResult(intent, ((fragment.mIndex + 1) << 16)
				+ (requestCode & 0xffff));
	}

	void invalidateSupportFragmentIndex(final int index) {
		// Log.v(TAG, "invalidateFragmentIndex: index=" + index);
		if (this.mAllLoaderManagers != null) {
			LoaderManagerImpl lm = this.mAllLoaderManagers.get(index);
			if (lm != null && !lm.mRetaining) {
				lm.doDestroy();
				this.mAllLoaderManagers.remove(index);
			}
		}
	}

	// ------------------------------------------------------------------------
	// LOADER SUPPORT
	// ------------------------------------------------------------------------

	/**
	 * Return the LoaderManager for this fragment, creating it if needed.
	 */
	@Override
	public LoaderManager getSupportLoaderManager() {
		if (this.mLoaderManager != null) {
			return this.mLoaderManager;
		}
		this.mCheckedForLoaderManager = true;
		this.mLoaderManager = this.getLoaderManager(-1, this.mLoadersStarted,
				true);
		return this.mLoaderManager;
	}

	LoaderManagerImpl getLoaderManager(final int index, final boolean started,
			final boolean create) {
		if (this.mAllLoaderManagers == null) {
			this.mAllLoaderManagers = new HCSparseArray<LoaderManagerImpl>();
		}
		LoaderManagerImpl lm = this.mAllLoaderManagers.get(index);
		if (lm == null) {
			if (create) {
				lm = new LoaderManagerImpl(this, started);
				this.mAllLoaderManagers.put(index, lm);
			}
		} else {
			lm.updateActivity(this);
		}
		return lm;
	}
}
