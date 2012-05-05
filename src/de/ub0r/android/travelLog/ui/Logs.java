/*
 * Copyright (C) 2010-2012 Felix Bechstein
 * 
 * This file is part of TravelLog.
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; If not, see <http://www.gnu.org/licenses/>.
 */
package de.ub0r.android.travelLog.ui;

import java.util.Calendar;
import java.util.Date;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.View;
import android.view.Window;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ResourceCursorTreeAdapter;
import android.widget.TextView;
import android.widget.TimePicker;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import de.ub0r.android.lib.ChangelogHelper;
import de.ub0r.android.lib.Log;
import de.ub0r.android.lib.Utils;
import de.ub0r.android.travelLog.R;
import de.ub0r.android.travelLog.data.DataProvider;

/**
 * Main {@link SherlockActivity}.
 * 
 * @author flx
 */
public final class Logs extends SherlockActivity implements
		OnChildClickListener {
	static {
		Log.init("TravelLog");
	}

	/** Tag for output. */
	private static final String TAG = "Logs";

	/** Dialog: clear all data. */
	private static final int DIALOG_CLEAR = 1;

	/**
	 * Adapter showing log entries.
	 * 
	 * @author flx
	 */
	private final class LogAdapter extends ResourceCursorTreeAdapter {
		/** {@link ContentResolver}. */
		private final ContentResolver cr;
		/** Where clause used for inner {@link Cursor}. */
		private static final String INNER_SELECT = DataProvider.Logs.FROM_D
				+ "= ?";
		/** {@link DateFormat}. */
		private final java.text.DateFormat dateFormat;
		/** {@link DateFormat}. */
		private final java.text.DateFormat timeFormat;
		/** {@link TextView}'s text size. */
		private final float textSizeGroup, textSizeChild;
		/** Count travel time in sum. */
		private final boolean countTravel;

		/**
		 * Constructor.
		 * 
		 * @param context
		 *            {@link Context}
		 */
		public LogAdapter(final Context context) {
			super(context, null, R.layout.logs_group, R.layout.logs_child);
			this.cr = context.getContentResolver();
			this.dateFormat = DateFormat.getDateFormat(context);
			this.timeFormat = DateFormat.getTimeFormat(context);
			this.textSizeGroup = Preferences.getTextSizeGroup(context);
			this.textSizeChild = Preferences.getTextSizeChild(context);
			this.countTravel = PreferenceManager.getDefaultSharedPreferences(
					context).getBoolean(Preferences.PREFS_COUNT_TRAVEL, false);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		protected void bindChildView(final View view, final Context context,
				final Cursor cursor, final boolean isLastChild) {
			final int idFrom = cursor.getColumnIndex(DataProvider.Logs.FROM);
			final int idTo = cursor.getColumnIndex(DataProvider.Logs.TO);
			final int idComment = cursor
					.getColumnIndex(DataProvider.Logs.COMMENT);
			final int idTypeName = cursor
					.getColumnIndex(DataProvider.Logs.TYPE_NAME);
			final long from = cursor.getLong(idFrom);
			final long to = cursor.getLong(idTo);
			final String comment = cursor.getString(idComment);
			final String typeName = cursor.getString(idTypeName);
			long dur = to - from;
			if (to <= 0L) {
				dur = System.currentTimeMillis() - from;
			}

			TextView tv = (TextView) view.findViewById(R.id.time);
			tv.setText(getTime(dur));
			tv.setTextSize(this.textSizeChild);

			String s = this.timeFormat.format(new Date(from));
			if (to > 0L) {
				s += " - " + this.timeFormat.format(new Date(to));
			}
			tv = (TextView) view.findViewById(R.id.from_to);
			tv.setText(s);
			tv.setTextSize(this.textSizeChild);

			tv = (TextView) view.findViewById(R.id.type);
			tv.setText(typeName);
			tv.setTextSize(this.textSizeChild);

			if (TextUtils.isEmpty(comment)) {
				view.findViewById(R.id.comment).setVisibility(View.GONE);
			} else {
				tv = (TextView) view.findViewById(R.id.comment);
				tv.setText(comment);
				tv.setTextSize(this.textSizeChild);
				tv.setVisibility(View.VISIBLE);
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		protected void bindGroupView(final View view, final Context context,
				final Cursor cursor, final boolean isExpanded) {
			final int idFrom = cursor.getColumnIndex(DataProvider.Logs.FROM);
			final int idTo = cursor.getColumnIndex(DataProvider.Logs.TO);
			final int idSumWork = cursor
					.getColumnIndex(DataProvider.Logs.SUM_WORK);
			final int idSumTravel = cursor
					.getColumnIndex(DataProvider.Logs.SUM_TRAVEL);
			final int idSumPause = cursor
					.getColumnIndex(DataProvider.Logs.SUM_PAUSE);
			long to = cursor.getLong(idTo);
			if (to == 0L) {
				to = System.currentTimeMillis();
			}
			final long from = cursor.getLong(idFrom);
			final long sumWork = cursor.getLong(idSumWork);
			final long sumTravel = cursor.getLong(idSumTravel);
			final long sumPause = cursor.getLong(idSumPause);
			final long time = this.countTravel ? sumWork + sumTravel : sumWork;

			TextView tv = (TextView) view.findViewById(R.id.date);
			tv.setText(this.dateFormat.format(new Date(from)));
			tv.setTextSize(this.textSizeGroup);

			tv = (TextView) view.findViewById(R.id.time);
			if (tv != null) {
				tv.setText(getTime(time));
				tv.setTextSize(this.textSizeGroup);
			}

			tv = (TextView) view.findViewById(R.id.work);
			if (sumWork > 0L) {
				tv.setText(context.getString(R.string.work) + ": "
						+ getTime(sumWork));
				tv.setTextSize(this.textSizeChild);
				tv.setVisibility(View.VISIBLE);
			} else {
				tv.setVisibility(View.GONE);
			}

			tv = (TextView) view.findViewById(R.id.travel);
			if (sumTravel > 0L) {
				tv.setText(context.getString(R.string.travel) + ": "
						+ getTime(sumTravel));
				tv.setTextSize(this.textSizeChild);
				tv.setVisibility(View.VISIBLE);
			} else {
				tv.setVisibility(View.GONE);
			}

			tv = (TextView) view.findViewById(R.id.pause);
			if (sumPause > 0L) {
				tv.setText(context.getString(R.string.pause) + ": "
						+ getTime(sumPause));
				tv.setTextSize(this.textSizeChild);
				tv.setVisibility(View.VISIBLE);
			} else {
				tv.setVisibility(View.GONE);
			}

		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		protected Cursor getChildrenCursor(final Cursor groupCursor) {
			// final int idFromY = groupCursor
			// .getColumnIndex(DataProvider.Logs.FROM_Y);
			// final int idFromM = groupCursor
			// .getColumnIndex(DataProvider.Logs.FROM_M);
			final int idFromD = groupCursor
					.getColumnIndex(DataProvider.Logs.FROM_D);
			return this.cr.query(DataProvider.Logs.CONTENT_URI,
					DataProvider.Logs.PROJECTION, INNER_SELECT,
					new String[] { groupCursor.getString(idFromD) },
					DataProvider.Logs.FROM + " DESC");
		}
	}

	/**
	 * Handle queries in background.
	 * 
	 * @author flx
	 */
	private final class BackgroundQueryHandler extends AsyncQueryHandler {
		/** Token for {@link BackgroundQueryHandler}. */
		private static final int LIST_QUERY_TOKEN = 1;

		/**
		 * A helper class to help make handling asynchronous
		 * {@link ContentResolver} queries easier.
		 * 
		 * @param contentResolver
		 *            {@link ContentResolver}
		 */
		public BackgroundQueryHandler(final ContentResolver contentResolver) {
			super(contentResolver);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		protected void onQueryComplete(final int token, final Object cookie,
				final Cursor cursor) {
			Log.d(TAG, "onQueryComplete(" + token + "," + cookie + ",c)");
			switch (token) {
			case LIST_QUERY_TOKEN:
				Logs.this.requery(cursor);
				Logs.this.setProgressBarIndeterminateVisibility(false);
				return;
			default:
				return;
			}
		}
	}

	/** Action: add comment. */
	private static final int ACTION_CHILD_COMMENT = 0;
	/** Action: change date. */
	private static final int ACTION_CHILD_CHG_DATE = 1;
	/** Action: change start time. */
	private static final int ACTION_CHILD_CHG_START = 2;
	/** Action: change end time. */
	private static final int ACTION_CHILD_CHG_END = 3;
	/** Action: change type. */
	private static final int ACTION_CHILD_CHG_TYPE = 4;
	/** Action: delete. */
	private static final int ACTION_CHILD_DELETE = 5;

	/** Action: change date. */
	// private static final int ACTION_GROUP_CHG_DATE = 0;
	/** Action: delete. */
	// private static final int ACTION_GROUP_DELETE = 4;

	/** Preference's name: mail. */
	private static final String PREFS_MAIL = "mail";
	/** Preference's name: flip export. */
	private static final String PREFS_FLIP_EXPORT = "export_flip";

	/** {@link BackgroundQueryHandler}. */
	private BackgroundQueryHandler queryHandler = null;

	/** {@link MenuItem}s . */
	private MenuItem stopItem, workItem, pauseItem, travelItem;
	/** Show {@link MenuItem}s. */
	private boolean showStopItem, showWorkItem, showPauseItem, showTravelItem;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		this.setTheme(Preferences.getTheme(this));
		this.requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.logs);

		if (this.queryHandler == null) {
			this.queryHandler = new BackgroundQueryHandler(
					this.getContentResolver());
		}
		final ExpandableListView lv = (ExpandableListView) this
				.findViewById(android.R.id.list);
		lv.setAdapter(new LogAdapter(this));
		lv.setOnChildClickListener(this);

		if (savedInstanceState == null) {
			ChangelogHelper.showChangelog(this, true);
		}
		this.changeState(0, 0, true);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		this.getSupportMenuInflater().inflate(R.menu.menu, menu);
		this.stopItem = menu.findItem(R.id.item_stop);
		if (this.stopItem != null) {
			this.stopItem.setVisible(this.showStopItem);
			if (this.showStopItem) {
				this.changeState(0, 0, true);
			}
		}
		this.pauseItem = menu.findItem(R.id.start_pause_);
		this.travelItem = menu.findItem(R.id.start_travel_);
		this.workItem = menu.findItem(R.id.start_work_);
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case R.id.item_settings:
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
				this.startActivity(new Intent(this, Preferences11.class));
			} else {
				this.startActivity(new Intent(this, Preferences.class));
			}
			return true;
		case R.id.item_map:
			this.startActivity(new Intent(this, Map.class));
			return true;
		case R.id.item_clear:
			this.showDialog(DIALOG_CLEAR);
			return true;
		case R.id.item_export:
			this.export();
			return true;
		case R.id.start_pause_:
			this.changeState(DataProvider.Logtypes.TYPE_PAUSE);
			return true;
		case R.id.start_travel_:
			this.changeState(DataProvider.Logtypes.TYPE_TRAVEL);
			return true;
		case R.id.start_work_:
			this.changeState(DataProvider.Logtypes.TYPE_WORK);
			return true;
		case R.id.item_stop:
			this.changeState(0);
			return true;
		default:
			return false;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Dialog onCreateDialog(final int id) {
		Builder b;
		switch (id) {
		case DIALOG_CLEAR:
			b = new Builder(this);
			b.setTitle(R.string.clear_);
			b.setMessage(R.string.clear_hint);
			b.setPositiveButton(android.R.string.ok,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(final DialogInterface dialog,
								final int which) {
							Logs.this.getContentResolver().delete(
									DataProvider.Logs.CONTENT_URI, null, null);
							Logs.this.requery();
						}
					});
			b.setNegativeButton(android.R.string.cancel, null);
			return b.create();
		default:
			return null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onResume() {
		super.onResume();
		Utils.setLocale(this);

		// update logs from cells
		Preferences.registerLocationChecker(this);

		// refresh query
		this.requery();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onPause() {
		super.onPause();
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean onChildClick(final ExpandableListView parent, final View v,
			final int groupPosition, final int childPosition, final long id) {
		AlertDialog.Builder b = new AlertDialog.Builder(this);
		b.setItems(R.array.action_child, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(final DialogInterface dialog, final int which) {
				final Uri target = ContentUris.withAppendedId(
						DataProvider.Logs.CONTENT_URI, id);
				switch (which) {
				case ACTION_CHILD_COMMENT:
					Logs.this.setComment(target);
					return;
				case ACTION_CHILD_CHG_DATE:
					Logs.this.changeDate(target);
					return;
				case ACTION_CHILD_CHG_END:
					Logs.this.changeTime(target, DataProvider.Logs.TO);
					return;
				case ACTION_CHILD_CHG_START:
					Logs.this.changeTime(target, DataProvider.Logs.FROM);
					return;
				case ACTION_CHILD_CHG_TYPE:
					Logs.this.changeType(target);
					return;
				case ACTION_CHILD_DELETE:
					Logs.this.delete(target);
					return;
				default:
					return;
				}
			}
		});
		b.show();
		return true;
	}

	/**
	 * Export data.
	 */
	private void export() {
		SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(this);
		final String mail = p
				.getString(PREFS_MAIL, "nobody@set-your-prefs.com");
		final Intent in = new Intent(Intent.ACTION_SEND);
		in.putExtra(Intent.EXTRA_EMAIL, new String[] { mail, "" });
		String sortOrder = DataProvider.Logs.FROM;
		if (p.getBoolean(PREFS_FLIP_EXPORT, false)) {
			sortOrder += " ASC";
		} else {
			sortOrder += " DESC";
		}
		final StringBuilder buf = new StringBuilder();

		final Cursor cursor = this.getContentResolver().query(
				DataProvider.Logs.CONTENT_URI, DataProvider.Logs.PROJECTION,
				null, null, sortOrder);
		if (cursor.moveToFirst()) {
			final int idFrom = cursor.getColumnIndex(DataProvider.Logs.FROM);
			final int idTo = cursor.getColumnIndex(DataProvider.Logs.TO);
			final int idComment = cursor
					.getColumnIndex(DataProvider.Logs.COMMENT);
			final int idTypeName = cursor
					.getColumnIndex(DataProvider.Logs.TYPE_NAME);
			final java.text.DateFormat dateFormat = DateFormat
					.getDateFormat(this);
			final java.text.DateFormat timeFormat = DateFormat
					.getTimeFormat(this);
			do {
				final long from = cursor.getLong(idFrom);
				final long to = cursor.getLong(idTo);
				long dur = to - from;
				if (to <= 0L) {
					dur = System.currentTimeMillis() - from;
				}

				buf.append(dateFormat.format(new Date(from)));
				buf.append(": ");
				buf.append(timeFormat.format(new Date(from)));
				buf.append(" - ");
				if (to > 0L) {
					buf.append(timeFormat.format(new Date(to)));
				} else {
					buf.append("??:??");
				}
				buf.append("\t ");
				buf.append(getTime(dur));
				buf.append("\t ");
				buf.append(cursor.getString(idTypeName));
				String s = cursor.getString(idComment);
				if (!TextUtils.isEmpty(s)) {
					buf.append("\t ");
					buf.append(cursor.getString(idComment));
				}
				buf.append("\n");
			} while (cursor.moveToNext());
		}

		buf.append("\n");
		buf.append(this.getString(R.string.export_footer));
		buf.append(" ");
		buf.append(this.getString(R.string.website));
		buf.append("\n");
		in.putExtra(Intent.EXTRA_TEXT, buf.toString());
		in.putExtra(Intent.EXTRA_SUBJECT,
				this.getString(R.string.export_subject));
		in.setType("text/plain");
		this.startActivity(in);
	}

	/**
	 * Parse number of seconds to a readable time format.
	 * 
	 * @param milliseconds
	 *            milliseconds
	 * @return parsed string
	 */
	static String getTime(final long milliseconds) {
		String ret;
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(milliseconds);
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
		if (d == 0 && h == 0) {
			if (m > 0) {
				ret += m + "min";
			} else {
				ret += seconds + "s";
			}
		} else {
			ret += m;
		}
		return ret;
	}

	/**
	 * Change state.
	 * 
	 * @param logTypeType
	 *            type of log type
	 */
	private void changeState(final int logTypeType) {
		Log.d(TAG, "changeState(" + logTypeType + ")");
		if (logTypeType == 0) {
			this.changeState(0, 0, false);
		} else {
			Cursor cursor = this.getContentResolver().query(
					DataProvider.Logtypes.CONTENT_URI,
					DataProvider.Logtypes.PROJECTION,
					DataProvider.Logtypes.TIME_TYPE + " = ?",
					new String[] { String.valueOf(logTypeType) }, null);
			int l = cursor.getCount();
			final int[] ids = new int[l];
			final String[] names = new String[l];
			final int idId = cursor.getColumnIndex(DataProvider.Logtypes.ID);
			final int idName = cursor
					.getColumnIndex(DataProvider.Logtypes.NAME);
			if (cursor.moveToFirst()) {
				int i = 0;
				do {
					ids[i] = cursor.getInt(idId);
					names[i] = cursor.getString(idName);
					++i;
				} while (cursor.moveToNext() && i <= l);
			}
			if (!cursor.isClosed()) {
				cursor.close();
			}
			cursor = null;
			if (l == 1) {
				Log.d(TAG, "choose the only existing logtype: " + ids[0]);
				Logs.this.changeState(logTypeType, ids[0], false);
			} else {
				AlertDialog.Builder b = new AlertDialog.Builder(this);
				switch (logTypeType) {
				case DataProvider.Logtypes.TYPE_PAUSE:
					b.setTitle(R.string.pause);
					break;
				case DataProvider.Logtypes.TYPE_TRAVEL:
					b.setTitle(R.string.travel);
					break;
				case DataProvider.Logtypes.TYPE_WORK:
					b.setTitle(R.string.work);
					break;
				default:
					break;
				}
				b.setItems(names, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog,
							final int which) {
						Logs.this.changeState(logTypeType, ids[which], false);
					}
				});
				b.show();
			}
		}
	}

	/**
	 * Change to state.
	 * 
	 * @param logTypeType
	 *            type of log type
	 * @param logTypeId
	 *            id of log type
	 * @param btnOnly
	 *            set buttons only, do not modify items
	 */
	private void changeState(final int logTypeType, final int logTypeId,
			final boolean btnOnly) {
		Log.d(TAG, "changeState(" + logTypeType + "," + logTypeId + ")");
		final ContentResolver cr = this.getContentResolver();
		if (!btnOnly) { // change state of logs
			DataProvider.Logs.closeOpen(this, 0L, false);
			if (logTypeType > 0) {
				DataProvider.Logs.openNew(this, 0L, logTypeId, false);
			}
		}

		// set buttons
		Cursor cursor = cr.query(DataProvider.Logs.CONTENT_URI_OPEN,
				DataProvider.Logs.PROJECTION, null, null, null);
		if (cursor.moveToFirst()) { // a log is open
			final int idLogTypeType = cursor
					.getColumnIndex(DataProvider.Logs.TYPE_TYPE);
			final int logType = cursor.getInt(idLogTypeType);
			int resId = -1;
			this.showStopItem = true;
			switch (logType) {
			case DataProvider.Logtypes.TYPE_PAUSE:
				this.showPauseItem = false;
				this.showTravelItem = true;
				this.showWorkItem = true;
				break;
			case DataProvider.Logtypes.TYPE_TRAVEL:
				this.showPauseItem = true;
				this.showTravelItem = false;
				this.showWorkItem = true;
				break;
			case DataProvider.Logtypes.TYPE_WORK:
				this.showPauseItem = true;
				this.showTravelItem = true;
				this.showWorkItem = false;
				break;
			default:
				this.showStopItem = false;
				this.showPauseItem = true;
				this.showTravelItem = true;
				this.showWorkItem = true;
			}
		} else { // no log is open
			this.showStopItem = false;
			this.showPauseItem = true;
			this.showTravelItem = true;
			this.showWorkItem = true;
		}
		if (!cursor.isClosed()) {
			cursor.close();
		}
		if (this.stopItem != null) {
			this.stopItem.setVisible(this.showStopItem);
		}
		if (this.pauseItem != null) {
			this.pauseItem.setVisible(this.showPauseItem);
		}
		if (this.travelItem != null) {
			this.travelItem.setVisible(this.showTravelItem);
		}
		if (this.workItem != null) {
			this.workItem.setVisible(this.showWorkItem);
		}

		if (!btnOnly) {
			this.requery();
		}
	}

	/**
	 * Requery data.
	 */
	private void requery() {
		Log.d(TAG, "requery()");
		// Cancel any pending queries
		this.queryHandler
				.cancelOperation(BackgroundQueryHandler.LIST_QUERY_TOKEN);
		try {
			// Kick off the new query
			this.setProgressBarIndeterminateVisibility(true);
			final String v = String.valueOf(System.currentTimeMillis());
			final String[] p = DataProvider.Logs.PROJECTION_SUM.clone();
			final int l = p.length;
			for (int i = 0; i < l; i++) {
				p[i] = p[i].replace("?", v);
				Log.d(TAG, "p[" + i + "] = " + p[i]);
			}
			this.queryHandler.startQuery(
					BackgroundQueryHandler.LIST_QUERY_TOKEN, null,
					DataProvider.Logs.CONTENT_URI_SUM, p, null, null, null);
		} catch (SQLiteException e) {
			Log.e(TAG, "error starting query", e);
		}
	}

	/**
	 * Requery data.
	 * 
	 * @param cursor
	 *            new {@link Cursor}
	 */
	private void requery(final Cursor cursor) {
		final ExpandableListView lv = (ExpandableListView) this
				.findViewById(android.R.id.list);
		Cursor c = ((LogAdapter) lv.getExpandableListAdapter()).getCursor();
		if (c != null && !c.isClosed()) {
			c.close();
		}
		c = null;
		if (cursor != null && !cursor.isClosed() && cursor.getCount() > 0) {
			this.findViewById(R.id.hint).setVisibility(View.GONE);
			try {
				final boolean expandFirst = lv.isGroupExpanded(0)
						|| ((LogAdapter) lv.getExpandableListAdapter())
								.getGroupCount() == 0;
				((LogAdapter) lv.getExpandableListAdapter())
						.setGroupCursor(cursor);
				if (expandFirst) {
					lv.collapseGroup(0);
					lv.expandGroup(0);
				}
			} catch (NullPointerException e) {
				Log.d(TAG, "NPE", e);
				// nothing to do here
			}
			lv.setVisibility(View.VISIBLE);
		} else {
			lv.setVisibility(View.GONE);
			this.findViewById(R.id.hint).setVisibility(View.VISIBLE);
		}
		this.changeState(0, 0, true);
	}

	/**
	 * Set comment.
	 * 
	 * @param target
	 *            target log entry
	 */
	private void setComment(final Uri target) {
		final AlertDialog.Builder b = new AlertDialog.Builder(this);
		String[] res = this.getResources().getStringArray(R.array.action_child);
		b.setTitle(res[ACTION_CHILD_COMMENT]);
		res = null;

		final EditText et = new EditText(this);
		Cursor cursor = this.getContentResolver().query(target,
				DataProvider.Logs.PROJECTION, null, null, null);
		int idComment = cursor.getColumnIndex(DataProvider.Logs.COMMENT);
		if (cursor.moveToFirst()) {
			et.setText(cursor.getString(idComment));
		}
		if (!cursor.isClosed()) {
			cursor.close();
		}
		cursor = null;

		b.setView(et);
		b.setNegativeButton(android.R.string.cancel, null);
		b.setPositiveButton(android.R.string.ok,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog,
							final int which) {
						final ContentValues values = new ContentValues(1);
						values.put(DataProvider.Logs.COMMENT, et.getText()
								.toString());
						Logs.this.getContentResolver().update(target, values,
								null, null);
						Logs.this.requery();
					}
				});
		b.show();
	}

	/**
	 * Change date.
	 * 
	 * @param target
	 *            target log entry
	 */
	private void changeDate(final Uri target) {
		Cursor cursor = this.getContentResolver().query(target,
				DataProvider.Logs.PROJECTION, null, null, null);
		int idFrom = cursor.getColumnIndex(DataProvider.Logs.FROM);
		int idTo = cursor.getColumnIndex(DataProvider.Logs.TO);
		final Calendar cal = Calendar.getInstance();
		if (cursor.moveToFirst()) {
			final long from = cursor.getLong(idFrom);
			final long to = cursor.getLong(idTo);
			cal.setTimeInMillis(from);
			new DatePickerDialog(Logs.this, new OnDateSetListener() {
				@Override
				public void onDateSet(final DatePicker view, final int year,
						final int monthOfYear, final int dayOfMonth) {
					final ContentValues values = new ContentValues();
					cal.set(year, monthOfYear, dayOfMonth);
					values.put(DataProvider.Logs.FROM, cal.getTimeInMillis());
					cal.setTimeInMillis(to);
					cal.set(year, monthOfYear, dayOfMonth);
					values.put(DataProvider.Logs.TO, cal.getTimeInMillis());
					Logs.this.getContentResolver().update(target, values, null,
							null);
					Logs.this.requery();
				}
			}, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH),
					cal.get(Calendar.DAY_OF_MONTH)).show();
		}
	}

	/**
	 * Change time: from/to.
	 * 
	 * @param target
	 *            target log entry.
	 * @param field
	 *            FROM or TO
	 */
	private void changeTime(final Uri target, final String field) {
		Cursor cursor = this.getContentResolver().query(target,
				DataProvider.Logs.PROJECTION, null, null, null);
		if (cursor.moveToFirst()) {
			final int idTime = cursor.getColumnIndex(field);
			final long time = cursor.getLong(idTime);
			final Calendar cal = Calendar.getInstance();
			if (time > 0L) {
				cal.setTimeInMillis(time);
			}
			TimePickerDialog d = new TimePickerDialog(this,
					new OnTimeSetListener() {
						@Override
						public void onTimeSet(final TimePicker view,
								final int hourOfDay, final int minute) {
							cal.set(Calendar.HOUR_OF_DAY, hourOfDay);
							cal.set(Calendar.MINUTE, minute);
							ContentValues values = new ContentValues(1);
							values.put(field, cal.getTimeInMillis());
							Logs.this.getContentResolver().update(target,
									values, null, null);
							Logs.this.requery();
						}
					}, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE),
					DateFormat.is24HourFormat(Logs.this));
			String[] res = this.getResources().getStringArray(
					R.array.action_child);
			if (field.equals(DataProvider.Logs.FROM)) {
				d.setTitle(res[ACTION_CHILD_CHG_START]);
			} else {
				d.setTitle(res[ACTION_CHILD_CHG_END]);
			}
			res = null;
			d.show();
		}
	}

	/**
	 * Change type of log entry.
	 * 
	 * @param target
	 *            target log entry.
	 */
	private void changeType(final Uri target) {
		Cursor cursor = this.getContentResolver().query(
				DataProvider.Logtypes.CONTENT_URI,
				DataProvider.Logtypes.PROJECTION, null, null, null);
		int l = cursor.getCount();
		final int[] ids = new int[l];
		final String[] names = new String[l];
		final int idId = cursor.getColumnIndex(DataProvider.Logtypes.ID);
		final int idName = cursor.getColumnIndex(DataProvider.Logtypes.NAME);
		if (cursor.moveToFirst()) {
			int i = 0;
			do {
				ids[i] = cursor.getInt(idId);
				names[i] = cursor.getString(idName);
				++i;
			} while (cursor.moveToNext() && i <= l);
		}
		if (!cursor.isClosed()) {
			cursor.close();
		}
		cursor = null;
		AlertDialog.Builder b = new AlertDialog.Builder(Logs.this);
		String[] res = Logs.this.getResources().getStringArray(
				R.array.action_child);
		b.setTitle(res[ACTION_CHILD_CHG_TYPE]);
		res = null;
		b.setItems(names, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(final DialogInterface dialog, final int which) {
				final ContentValues values = new ContentValues(1);
				values.put(DataProvider.Logs.TYPE, ids[which]);
				Logs.this.getContentResolver().update(target, values, null,
						null);
				Logs.this.requery();
			}
		});
		b.show();
	}

	/**
	 * Delete a log entry. * @param target target log entry.
	 */
	private void delete(final Uri target) {
		this.getContentResolver().delete(target, null, null);
		Logs.this.requery();
	}
}
