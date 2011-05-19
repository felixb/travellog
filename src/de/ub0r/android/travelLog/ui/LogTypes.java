/*
 * Copyright (C) 2009-2010 Felix Bechstein, The Android Open Source Project
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

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import de.ub0r.android.lib.Utils;
import de.ub0r.android.travelLog.R;
import de.ub0r.android.travelLog.data.DataProvider;

/**
 * {@link ListActivity} for setting plans.
 * 
 * @author flx
 */
public class LogTypes extends ListActivity implements OnClickListener,
		OnItemClickListener {
	/**
	 * Adapter binding plans to View.
	 * 
	 * @author flx
	 */
	private class LogTypeAdapter extends ResourceCursorAdapter {
		/**
		 * Default Constructor.
		 * 
		 * @param context
		 *            {@link Context}
		 */
		public LogTypeAdapter(final Context context) {
			super(
					context,
					R.layout.logtypes_item,
					context.getContentResolver().query(
							DataProvider.Logtypes.CONTENT_URI,
							DataProvider.Logtypes.PROJECTION, null, null, null),
					true);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public final void bindView(final View view, final Context context,
				final Cursor cursor) {
			final int idName = cursor
					.getColumnIndex(DataProvider.Logtypes.NAME);
			final int idType = cursor
					.getColumnIndex(DataProvider.Logtypes.TIME_TYPE);
			final int type = cursor.getInt(idType);
			TextView tv = (TextView) view.findViewById(R.id.name);
			tv.setText(cursor.getString(idName));

			tv = (TextView) view.findViewById(R.id.type);
			String typeName;
			switch (type) {
			case DataProvider.Logtypes.TYPE_PAUSE:
				typeName = context.getString(R.string.pause);
				break;
			case DataProvider.Logtypes.TYPE_TRAVEL:
				typeName = context.getString(R.string.travel);
				break;
			case DataProvider.Logtypes.TYPE_WORK:
				typeName = context.getString(R.string.work);
				break;
			default:
				typeName = null;
				break;
			}
			tv.setText(typeName);
		}
	}

	/** Item menu: edit. */
	private static final int ACTION_RENAME = 0;
	/** Item menu: delete. */
	private static final int ACTION_DELETE = 1;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Utils.setLocale(this);
		this.setTitle(this.getString(R.string.settings) + " > "
				+ this.getString(R.string.logtypes_));
		this.setContentView(R.layout.list_ok_add);
		this.setListAdapter(new LogTypeAdapter(this));
		this.getListView().setOnItemClickListener(this);
		this.findViewById(R.id.ok).setOnClickListener(this);
		this.findViewById(R.id.add).setOnClickListener(this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void onResume() {
		super.onResume();
		Utils.setLocale(this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onClick(final View v) {
		switch (v.getId()) {
		case R.id.add:
			this.add();
			break;
		case R.id.ok:
			this.finish();
			break;
		default:
			break;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onItemClick(final AdapterView<?> parent, final View view,
			final int position, final long id) {

		final AlertDialog.Builder b = new AlertDialog.Builder(this);
		b.setItems(R.array.action_logtype,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog,
							final int which) {
						switch (which) {
						case ACTION_RENAME:
							LogTypes.this.rename(position);
							break;
						case ACTION_DELETE:
							LogTypes.this.getContentResolver().delete(
									ContentUris.withAppendedId(
											DataProvider.Logtypes.CONTENT_URI,
											id), null, null);
							break;
						default:
							break;
						}
					}
				});
		b.show();
	}

	/**
	 * Show dialog to add a log type.
	 */
	private void add() {
		final AlertDialog.Builder b = new AlertDialog.Builder(this);
		b.setTitle(R.string.add_);
		final String[] items = new String[] { // .
		this.getString(R.string.pause), // .
				this.getString(R.string.travel), // .
				this.getString(R.string.work) };
		b.setItems(items, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(final DialogInterface dialog, final int which) {
				LogTypes.this.rename(null, null, which + 1);
			}
		});
		b.show();
	}

	/**
	 * Show dialog to rename .
	 * 
	 * @param position
	 *            position in list
	 */
	private void rename(final int position) {
		final Cursor cursor = (Cursor) this.getListAdapter().getItem(position);
		final int idName = cursor.getColumnIndex(DataProvider.Logtypes.NAME);
		final long id = this.getListAdapter().getItemId(position);
		final Uri target = ContentUris.withAppendedId(
				DataProvider.Logtypes.CONTENT_URI, id);
		this.rename(target, cursor.getString(idName), 0);
	}

	/**
	 * Show dialog to rename or add a log type.
	 * 
	 * @param target
	 *            target {@link Uri}
	 * @param currentName
	 *            current name
	 * @param type
	 *            type of new log type, ignored if target is not null
	 */
	private void rename(final Uri target, final String currentName,
			final int type) {
		final AlertDialog.Builder b = new AlertDialog.Builder(this);
		final EditText et = new EditText(this);
		if (target == null) {
			String[] res = this.getResources().getStringArray(
					R.array.action_logtype);
			b.setTitle(res[ACTION_RENAME]);
			res = null;
			et.setText(currentName);
		} else {
			b.setTitle(R.string.add_);
		}
		b.setView(et);
		b.setNegativeButton(android.R.string.cancel, null);
		b.setNegativeButton(android.R.string.ok,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog,
							final int which) {
						final ContentValues values = new ContentValues(2);
						values.put(DataProvider.Logtypes.NAME, et.getText()
								.toString());
						if (target != null) {
							LogTypes.this.getContentResolver().update(target,
									values, null, null);
						} else {
							values.put(DataProvider.Logtypes.TIME_TYPE, type);
							LogTypes.this.getContentResolver().insert(
									DataProvider.Logtypes.CONTENT_URI, values);
						}
					}
				});
		b.show();
	}
}
