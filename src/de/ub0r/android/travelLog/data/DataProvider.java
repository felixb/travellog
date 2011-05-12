/*
 * Copyright (C) 2011 Felix Bechstein
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
package de.ub0r.android.travelLog.data;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import de.ub0r.android.lib.DbUtils;
import de.ub0r.android.lib.Log;
import de.ub0r.android.travelLog.R;

/**
 * @author flx
 */
public final class DataProvider extends ContentProvider {
	/** Tag for output. */
	private static final String TAG = "dp";

	/** Callmeter's package name. */
	public static final String PACKAGE = "de.ub0r.android.travelLog";

	/** Authority. */
	public static final String AUTHORITY = PACKAGE + ".provider";

	/** Name of the {@link SQLiteDatabase}. */
	private static final String DATABASE_NAME = "travellog.db";
	/** Version of the {@link SQLiteDatabase}. */
	private static final int DATABASE_VERSION = 1;

	/** Internal id: Logs. */
	private static final int ID_LOGS = 0;
	/** Internal id: single log. */
	private static final int ID_LOGID = 1;
	/** Internal id: Sum of logs. */
	private static final int ID_LOGSUM = 2;
	/** Internal id: Current open log. */
	private static final int ID_OPENLOG = 3;
	/** Internal id: Log types. */
	private static final int ID_LOGTYPES = 10;
	/** Internal id: single log type. */
	private static final int ID_LOGTYPEID = 11;
	/** Internal id: Cells. */
	private static final int ID_CELLS = 20;
	/** Internal id: single cell. */
	private static final int ID_CELLID = 21;

	/** {@link UriMatcher}. */
	private static final UriMatcher URI_MATCHER;

	static {
		URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		URI_MATCHER.addURI(AUTHORITY, "logs", ID_LOGS);
		URI_MATCHER.addURI(AUTHORITY, "logs/#", ID_LOGID);
		URI_MATCHER.addURI(AUTHORITY, "logsum", ID_LOGSUM);
		URI_MATCHER.addURI(AUTHORITY, "openlog", ID_OPENLOG);
		URI_MATCHER.addURI(AUTHORITY, "logtypes", ID_LOGTYPES);
		URI_MATCHER.addURI(AUTHORITY, "logtypes/#", ID_LOGTYPEID);
		URI_MATCHER.addURI(AUTHORITY, "cells", ID_CELLS);
		URI_MATCHER.addURI(AUTHORITY, "cells/#", ID_CELLID);
	}

	/** Preference's name: travel item count. For migration only! */
	private static final String PREFS_LISTCOUNT = "log_n";
	/** Preference's name: travel item start. For migration only! */
	private static final String PREFS_LIST_START = "log_start_";
	/** Preference's name: travel item end. For migration only! */
	private static final String PREFS_LIST_STOP = "log_stop_";
	/** Preference's name: travel item type. For migration only! */
	private static final String PREFS_LIST_TYPE = "log_type_";

	/**
	 * This class helps open, create, and upgrade the database file.
	 */
	private static class DatabaseHelper extends SQLiteOpenHelper {
		/**
		 * Default Constructor.
		 * 
		 * @param context
		 *            {@link Context}
		 */
		DatabaseHelper(final Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void onCreate(final SQLiteDatabase db) {
			Log.i(TAG, "create database");
			Logs.onCreate(db);
			Logtypes.onCreate(db);
			Cells.onCreate(db);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void onUpgrade(final SQLiteDatabase db, final int oldVersion,
				final int newVersion) {
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
					+ newVersion + ", which will destroy all old data");
			Logs.onUpgrade(db, oldVersion, newVersion);
			Logtypes.onUpgrade(db, oldVersion, newVersion);
			Cells.onUpgrade(db, oldVersion, newVersion);
		}
	}

	/**
	 * Logs.
	 * 
	 * @author flx
	 */
	public static final class Logs {
		/** Table name. */
		private static final String TABLE = "logs";
		/** Joined {@link Logs} with {@link Logtypes}. */
		private static final String JOIN_LOGTYPES = Logs.TABLE
				+ " LEFT OUTER JOIN " + Logtypes.TABLE + " ON (" + Logs.TABLE
				+ "." + Logs.TYPE + " = " + Logtypes.TABLE + "." + Logtypes.ID
				+ ")";
		/** {@link HashMap} for projection. */
		private static final HashMap<String, String> PROJECTION_MAP;

		/** ID. */
		public static final String ID = "_id";
		/** Type. */
		public static final String TYPE = "_type";
		/** From date. */
		public static final String FROM = "_from";
		/** From date (year). */
		public static final String FROM_Y = "_from_y";
		/** From date (month). */
		public static final String FROM_M = "_from_m";
		/** From date (week). */
		public static final String FROM_W = "_from_w";
		/** From date (day). */
		public static final String FROM_D = "_from_d";
		/** To date. */
		public static final String TO = "_to";
		/** Comment. */
		public static final String COMMENT = "_comment";
		/** Started by auto. */
		public static final String STARTBYAUTO = "_startbyauto";
		/** Type's id. */
		public static final String TYPE_ID = "_type_id";
		/** Type's name. */
		public static final String TYPE_NAME = "_type_name";
		/** Type's type. */
		public static final String TYPE_TYPE = "_type_type";

		/** Projection used for query. */
		public static final String[] PROJECTION = new String[] { // .
		TABLE + "." + ID + " AS " + ID, TYPE, FROM, FROM_Y, FROM_M, FROM_W,
				FROM_D, TO, COMMENT, STARTBYAUTO,
				Logtypes.TABLE + "." + Logtypes.ID + " AS " + TYPE_ID,
				Logtypes.TABLE + "." + Logtypes.NAME + " AS " + TYPE_NAME,
				Logtypes.TABLE + "." + Logtypes.TIME_TYPE + " AS " + TYPE_TYPE };
		/** Projection used for query. */
		public static final String[] PROJECTION_SUM = new String[] { // .
		ID, "min(" + FROM + ") AS " + FROM, FROM_Y, FROM_M, FROM_W, FROM_D,
				"max(" + TO + ") AS " + TO };

		/** Content {@link Uri}. */
		public static final Uri CONTENT_URI = Uri.parse("content://"
				+ AUTHORITY + "/logs");
		/** Content {@link Uri}. */
		public static final Uri CONTENT_URI_SUM = Uri.parse("content://"
				+ AUTHORITY + "/logsum");
		/** Content {@link Uri}. */
		public static final Uri CONTENT_URI_OPEN = Uri.parse("content://"
				+ AUTHORITY + "/openlog");

		/**
		 * The MIME type of {@link #CONTENT_URI} providing a list.
		 */
		public static final String CONTENT_TYPE = // .
		"vnd.android.cursor.dir/vnd.ub0r.logs";

		/**
		 * The MIME type of a {@link #CONTENT_URI} single entry.
		 */
		public static final String CONTENT_ITEM_TYPE = // .
		"vnd.android.cursor.item/vnd.ub0r.logs";

		static {
			PROJECTION_MAP = new HashMap<String, String>();
			for (String s : PROJECTION) {
				PROJECTION_MAP.put(s, s);
			}
		}

		/**
		 * Create table in {@link SQLiteDatabase}.
		 * 
		 * @param db
		 *            {@link SQLiteDatabase}
		 */
		public static void onCreate(final SQLiteDatabase db) {
			Log.i(TAG, "create table: " + TABLE);
			db.execSQL("DROP TABLE IF EXISTS " + TABLE);
			db.execSQL("CREATE TABLE " + TABLE + " (" // .
					+ ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " // .
					+ TYPE + " INTEGER,"// .
					+ FROM + " LONG,"// .
					+ FROM_Y + " INTEGER,"// .
					+ FROM_M + " INTEGER,"// .
					+ FROM_W + " INTEGER,"// .
					+ FROM_D + " INTEGER,"// .
					+ TO + " LONG,"// .
					+ COMMENT + " TEXT,"// .
					+ STARTBYAUTO + " INTEGER"// .
					+ ");");
		}

		/**
		 * Upgrade table.
		 * 
		 * @param db
		 *            {@link SQLiteDatabase}
		 * @param oldVersion
		 *            old version
		 * @param newVersion
		 *            new version
		 */
		public static void onUpgrade(final SQLiteDatabase db,
				final int oldVersion, final int newVersion) {
			Log.w(TAG, "Upgrading table: " + TABLE);
			final ContentValues[] values = backup(db, TABLE, PROJECTION, null);
			onCreate(db);
			reload(db, TABLE, values);
		}

		/** Default constructor. */
		private Logs() {
			// nothing here.
		}

		/**
		 * Fix {@link ContentValues}.
		 * 
		 * @param values
		 *            {@link ContentValues}
		 */
		public static void fixValues(final ContentValues values) {
			long from = 0L;
			if (!values.containsKey(Logs.FROM)) {
				from = System.currentTimeMillis();
				values.put(Logs.FROM, from);
			} else {
				from = values.getAsLong(Logs.FROM);
			}
			Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(from);
			values.put(Logs.FROM_Y, cal.get(Calendar.YEAR));
			values.put(Logs.FROM_M, cal.get(Calendar.MONTH));
			values.put(Logs.FROM_W, cal.get(Calendar.WEEK_OF_YEAR));
			values.put(Logs.FROM_D, cal.get(Calendar.DAY_OF_YEAR));
		}
	}

	/**
	 * Log types.
	 * 
	 * @author flx
	 */
	public static final class Logtypes {
		/** Table name. */
		private static final String TABLE = "logtypes";
		/** {@link HashMap} for projection. */
		private static final HashMap<String, String> PROJECTION_MAP;

		/** Time type: PAUSE. */
		public static final int TYPE_PAUSE = 1;
		/** Time type: Travel. */
		public static final int TYPE_TRAVEL = 2;
		/** Time type: Work. */
		public static final int TYPE_WORK = 3;

		/** ID. */
		public static final String ID = "_id";
		/** Name. */
		public static final String NAME = "_name";
		/** Time type. */
		public static final String TIME_TYPE = "_timetype";

		/** Projection used for query. */
		public static final String[] PROJECTION = new String[] { // .
		ID, NAME, TIME_TYPE };

		/** Content {@link Uri}. */
		public static final Uri CONTENT_URI = Uri.parse("content://"
				+ AUTHORITY + "/logtypes");
		/**
		 * The MIME type of {@link #CONTENT_URI} providing a list.
		 */
		public static final String CONTENT_TYPE = // .
		"vnd.android.cursor.dir/vnd.ub0r.logtypes";

		/**
		 * The MIME type of a {@link #CONTENT_URI} single entry.
		 */
		public static final String CONTENT_ITEM_TYPE = // .
		"vnd.android.cursor.item/vnd.ub0r.logtypes";

		static {
			PROJECTION_MAP = new HashMap<String, String>();
			for (String s : PROJECTION) {
				PROJECTION_MAP.put(s, s);
			}
		}

		/**
		 * Create table in {@link SQLiteDatabase}.
		 * 
		 * @param db
		 *            {@link SQLiteDatabase}
		 */
		public static void onCreate(final SQLiteDatabase db) {
			Log.i(TAG, "create table: " + TABLE);
			db.execSQL("DROP TABLE IF EXISTS " + TABLE);
			db.execSQL("CREATE TABLE " + TABLE + " (" // .
					+ ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " // .
					+ NAME + " TEXT," // .
					+ TIME_TYPE + " INTEGER"// .
					+ ");");
		}

		/**
		 * Upgrade table.
		 * 
		 * @param db
		 *            {@link SQLiteDatabase}
		 * @param oldVersion
		 *            old version
		 * @param newVersion
		 *            new version
		 */
		public static void onUpgrade(final SQLiteDatabase db,
				final int oldVersion, final int newVersion) {
			Log.w(TAG, "Upgrading table: " + TABLE);
			final ContentValues[] values = backup(db, TABLE, PROJECTION, null);
			onCreate(db);
			reload(db, TABLE, values);
		}

		/** Default constructor. */
		private Logtypes() {
			// nothing here.
		}
	}

	/**
	 * Cells.
	 * 
	 * @author flx
	 */
	public static final class Cells {
		/** Table name. */
		private static final String TABLE = "cells";
		/** {@link HashMap} for projection. */
		private static final HashMap<String, String> PROJECTION_MAP;

		/** ID. */
		public static final String ID = "_id";
		/** Type. */
		public static final String TYPE = "_type";
		/** First seen. */
		public static final String SEEN_FIRST = "_seen_first";
		/** Last seen. */
		public static final String SEEN_LAST = "_seen_last";

		/** Projection used for query. */
		public static final String[] PROJECTION = new String[] { // .
		ID, TYPE, SEEN_FIRST, SEEN_LAST };

		/** Content {@link Uri}. */
		public static final Uri CONTENT_URI = Uri.parse("content://"
				+ AUTHORITY + "/cells");
		/**
		 * The MIME type of {@link #CONTENT_URI} providing a list.
		 */
		public static final String CONTENT_TYPE = // .
		"vnd.android.cursor.dir/vnd.ub0r.cells";

		/**
		 * The MIME type of a {@link #CONTENT_URI} single entry.
		 */
		public static final String CONTENT_ITEM_TYPE = // .
		"vnd.android.cursor.item/vnd.ub0r.cells";

		static {
			PROJECTION_MAP = new HashMap<String, String>();
			for (String s : PROJECTION) {
				PROJECTION_MAP.put(s, s);
			}
		}

		/**
		 * Create table in {@link SQLiteDatabase}.
		 * 
		 * @param db
		 *            {@link SQLiteDatabase}
		 */
		public static void onCreate(final SQLiteDatabase db) {
			Log.i(TAG, "create table: " + TABLE);
			db.execSQL("DROP TABLE IF EXISTS " + TABLE);
			db.execSQL("CREATE TABLE " + TABLE + " (" // .
					+ ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " // .
					+ TYPE + " INTEGER,"// .
					+ SEEN_FIRST + " LONG,"// .
					+ SEEN_LAST + " LONG"// .
					+ ");");
		}

		/**
		 * Upgrade table.
		 * 
		 * @param db
		 *            {@link SQLiteDatabase}
		 * @param oldVersion
		 *            old version
		 * @param newVersion
		 *            new version
		 */
		public static void onUpgrade(final SQLiteDatabase db,
				final int oldVersion, final int newVersion) {
			Log.w(TAG, "Upgrading table: " + TABLE);
			final ContentValues[] values = backup(db, TABLE, PROJECTION, null);
			onCreate(db);
			reload(db, TABLE, values);
		}

		/** Default constructor. */
		private Cells() {
			// nothing here.
		}
	}

	/** {@link DatabaseHelper}. */
	private DatabaseHelper mOpenHelper;

	/**
	 * Try to backup fields from table.
	 * 
	 * @param db
	 *            {@link SQLiteDatabase}
	 * @param table
	 *            table
	 * @param cols
	 *            columns
	 * @param strip
	 *            column to forget on backup, eg. _id
	 * @return array of rows
	 */
	private static ContentValues[] backup(final SQLiteDatabase db,
			final String table, final String[] cols, final String strip) {
		ArrayList<ContentValues> ret = new ArrayList<ContentValues>();
		String[] proj = cols;
		if (strip != null) {
			proj = new String[cols.length - 1];
			int i = 0;
			for (String c : cols) {
				if (strip.equals(c)) {
					continue;
				}
				proj[i] = c;
				++i;
			}
		}
		final int l = proj.length;
		Cursor cursor = null;
		try {
			cursor = db.query(table, proj, null, null, null, null, null);
		} catch (SQLException e) {
			if (l == 1) {
				return null;
			}
			final String err = e.getMessage();
			if (!err.startsWith("no such column:")) {
				return null;
			}
			final String str = err.split(":", 3)[1].trim();
			return backup(db, table, proj, str);
		}
		if (cursor != null && cursor.moveToFirst()) {
			do {
				final ContentValues cv = new ContentValues();
				for (int i = 0; i < l; i++) {
					final String s = cursor.getString(i);
					if (s != null) {
						cv.put(proj[i], s);
					}
				}
				ret.add(cv);
			} while (cursor.moveToNext());
		}
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
		return ret.toArray(new ContentValues[0]);
	}

	/**
	 * Reload backup into table.
	 * 
	 * @param db
	 *            {@link SQLiteDatabase}
	 * @param table
	 *            table
	 * @param values
	 *            {@link ContentValues}[] backed up with backup()
	 */
	private static void reload(final SQLiteDatabase db, final String table,
			final ContentValues[] values) {
		if (values == null || values.length == 0) {
			return;
		}
		Log.d(TAG, "reload(db, " + table + ", cv[" + values.length + "])");
		for (ContentValues cv : values) {
			db.insert(table, null, cv);
		}
		return;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int delete(final Uri uri, final String selection,
			final String[] selectionArgs) {
		Log.d(TAG, "delete(" + uri + "," + selection + ")");
		final SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
		int ret = 0;
		switch (URI_MATCHER.match(uri)) {
		case ID_LOGS:
			ret = db.delete(Logs.TABLE, selection, selectionArgs);
			break;
		case ID_LOGID:
			ret = db.delete(Logs.TABLE, DbUtils.sqlAnd(Logs.ID + "="
					+ ContentUris.parseId(uri), selection), selectionArgs);
			break;
		case ID_OPENLOG:
			ret = db.delete(Logs.TABLE, DbUtils.sqlAnd(Logs.TO + "= 0",
					selection), selectionArgs);
			break;
		case ID_LOGTYPES:
			ret = db.delete(Logtypes.TABLE, selection, selectionArgs);
			break;
		case ID_LOGTYPEID:
			ret = db.delete(Logtypes.TABLE, DbUtils.sqlAnd(Logtypes.ID + "="
					+ ContentUris.parseId(uri), selection), selectionArgs);
			break;
		case ID_CELLS:
			ret = db.delete(Cells.TABLE, selection, selectionArgs);
			break;
		case ID_CELLID:
			ret = db.delete(Cells.TABLE, DbUtils.sqlAnd(Cells.ID + "="
					+ ContentUris.parseId(uri), selection), selectionArgs);
			break;
		default:
			throw new IllegalArgumentException("Unknown Uri " + uri);
		}
		if (ret > 0) {
			this.getContext().getContentResolver().notifyChange(uri, null);
		}
		Log.d(TAG, "deleted: " + ret);
		return ret;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getType(final Uri uri) {
		switch (URI_MATCHER.match(uri)) {
		case ID_LOGS:
			return Logs.CONTENT_TYPE;
		case ID_LOGID:
			return Logs.CONTENT_ITEM_TYPE;
		case ID_LOGTYPES:
			return Logtypes.CONTENT_TYPE;
		case ID_LOGTYPEID:
			return Logtypes.CONTENT_ITEM_TYPE;
		case ID_CELLS:
			return Cells.CONTENT_TYPE;
		case ID_CELLID:
			return Cells.CONTENT_ITEM_TYPE;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Uri insert(final Uri uri, final ContentValues values) {
		Log.d(TAG, "insert(" + uri + "," + values + ")");
		final SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
		long ret = -1;
		switch (URI_MATCHER.match(uri)) {
		case ID_LOGS:
			if (!values.containsKey(Logs.TYPE)) {
				throw new IllegalArgumentException("Type not set.");
			}
			Logs.fixValues(values);
			Log.d(TAG, "insert: " + values);
			ret = db.insert(Logs.TABLE, null, values);
			break;
		case ID_LOGTYPES:
			ret = db.insert(Logtypes.TABLE, null, values);
			break;
		case ID_CELLS:
			ret = db.insert(Cells.TABLE, null, values);
			break;
		default:
			throw new IllegalArgumentException("Unknown Uri " + uri);
		}
		Uri ruri = null;
		if (ret >= 0) {
			this.getContext().getContentResolver().notifyChange(uri, null);
			ruri = ContentUris.withAppendedId(uri, ret);
		}
		Log.d(TAG, "inserted: " + ruri);
		return ruri;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onCreate() {
		final Context context = this.getContext();
		this.mOpenHelper = new DatabaseHelper(context);
		SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
		if (db.query(Logtypes.TABLE, Logtypes.PROJECTION, null, null, null,
				null, null).getCount() == 0) {
			Log.i(TAG, "insert default types");
			ContentValues values = new ContentValues();
			values.put(Logtypes.NAME, context.getString(R.string.pause));
			values.put(Logtypes.TIME_TYPE, Logtypes.TYPE_PAUSE);
			values.put(Logtypes.ID, Logtypes.TYPE_PAUSE);
			db.insert(Logtypes.TABLE, null, values);
			values = new ContentValues();
			values.put(Logtypes.NAME, context.getString(R.string.travel));
			values.put(Logtypes.TIME_TYPE, Logtypes.TYPE_TRAVEL);
			values.put(Logtypes.ID, Logtypes.TYPE_TRAVEL);
			db.insert(Logtypes.TABLE, null, values);
			values = new ContentValues();
			values.put(Logtypes.NAME, context.getString(R.string.work));
			values.put(Logtypes.TIME_TYPE, Logtypes.TYPE_WORK);
			values.put(Logtypes.ID, Logtypes.TYPE_WORK);
			db.insert(Logtypes.TABLE, null, values);
		}
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(context);
		final int l = p.getInt(PREFS_LISTCOUNT, 0);
		for (int i = 0; i < l; i++) {
			Log.i(TAG, "migrate old data: " + i);
			final ContentValues values = new ContentValues();
			values.put(Logs.FROM, p.getLong(PREFS_LIST_START + i, 0));
			values.put(Logs.TO, p.getLong(PREFS_LIST_STOP + i, 0));
			values.put(Logs.TYPE, p.getInt(PREFS_LIST_TYPE + i, 0));
			Logs.fixValues(values);
			db.insert(Logs.TABLE, null, values);
		}
		p.edit().remove(PREFS_LISTCOUNT).commit();
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Cursor query(final Uri uri, final String[] projection,
			final String selection, final String[] selectionArgs,
			final String sortOrder) {
		Log.d(TAG, "query(" + uri + "," + selection + ")");
		final SQLiteDatabase db = this.mOpenHelper.getReadableDatabase();
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		final int uid = URI_MATCHER.match(uri);
		String groupBy = null;
		String orderBy = null;
		Cursor c = null;
		switch (uid) {
		case ID_LOGID:
			c = db.query(Logs.JOIN_LOGTYPES, projection, DbUtils.sqlAnd(
					selection, Logs.TABLE + "." + Logs.ID + " = "
							+ ContentUris.parseId(uri)), selectionArgs,
					groupBy, null, orderBy);
			break;
		case ID_LOGS:
			orderBy = Logs.FROM + " DESC";
			if (!TextUtils.isEmpty(sortOrder)) {
				orderBy = sortOrder;
			}
			c = db.query(Logs.JOIN_LOGTYPES, projection, selection,
					selectionArgs, groupBy, null, orderBy);
			break;
		case ID_OPENLOG:
			orderBy = Logs.FROM + " DESC";
			if (!TextUtils.isEmpty(sortOrder)) {
				orderBy = sortOrder;
			}
			c = db.query(Logs.JOIN_LOGTYPES, projection, DbUtils.sqlAnd(
					selection, Logs.TO + " = 0"), selectionArgs, groupBy, null,
					orderBy);
			break;
		case ID_LOGSUM:
			groupBy = Logs.FROM_D;
			orderBy = Logs.FROM + " DESC";
			if (!TextUtils.isEmpty(sortOrder)) {
				orderBy = sortOrder;
			}
			c = db.query(Logs.TABLE, projection, selection, selectionArgs,
					groupBy, null, orderBy);
			break;
		case ID_LOGTYPEID:
			qb.appendWhere(Logtypes.ID + "=" + ContentUris.parseId(uri));
		case ID_LOGTYPES:
			orderBy = Logtypes.TIME_TYPE + " ASC, " + Logtypes.NAME + " ASC";
			qb.setTables(Logtypes.TABLE);
			qb.setProjectionMap(Logtypes.PROJECTION_MAP);
			break;
		case ID_CELLID:
			qb.appendWhere(Cells.ID + "=" + ContentUris.parseId(uri));
		case ID_CELLS:
			qb.setTables(Cells.TABLE);
			qb.setProjectionMap(Cells.PROJECTION_MAP);
			break;
		default:
			throw new IllegalArgumentException("Unknown Uri " + uri);
		}

		if (c == null) { // get the cursor from query builder
			// If no sort order is specified use the default
			if (!TextUtils.isEmpty(sortOrder)) {
				orderBy = sortOrder;
			}

			// Run the query
			c = qb.query(db, projection, selection, selectionArgs, groupBy,
					null, orderBy);
		}
		// Tell the cursor what uri to watch, so it knows when its source data
		// changes
		c.setNotificationUri(this.getContext().getContentResolver(), uri);
		Log.d(TAG, "got: " + c.getCount());
		return c;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int update(final Uri uri, final ContentValues values,
			final String selection, final String[] selectionArgs) {
		Log.d(TAG, "update(" + uri + "," + selection + "," + values + ")");
		final SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
		int ret = 0;
		switch (URI_MATCHER.match(uri)) {
		case ID_LOGS:
			if (values.containsKey(Logs.FROM)) {
				Logs.fixValues(values);
			}
			ret = db.update(Logs.TABLE, values, selection, selectionArgs);
			break;
		case ID_LOGID:
			if (values.containsKey(Logs.FROM)) {
				Logs.fixValues(values);
			}
			ret = db.update(Logs.TABLE, values, DbUtils.sqlAnd(Logs.ID + "="
					+ ContentUris.parseId(uri), selection), selectionArgs);
			break;
		case ID_OPENLOG:
			if (values.containsKey(Logs.FROM)) {
				Logs.fixValues(values);
			}
			ret = db.update(Logs.TABLE, values, DbUtils.sqlAnd(Logs.TO + "= 0",
					selection), selectionArgs);
			break;
		case ID_LOGTYPES:
			ret = db.update(Logtypes.TABLE, values, selection, selectionArgs);
			break;
		case ID_LOGTYPEID:
			ret = db
					.update(Logtypes.TABLE, values, DbUtils.sqlAnd(Logtypes.ID
							+ "=" + ContentUris.parseId(uri), selection),
							selectionArgs);
			break;
		case ID_CELLS:
			ret = db.update(Cells.TABLE, values, selection, selectionArgs);
			break;
		case ID_CELLID:
			ret = db.update(Cells.TABLE, values, DbUtils.sqlAnd(Cells.ID + "="
					+ ContentUris.parseId(uri), selection), selectionArgs);
			break;
		default:
			throw new IllegalArgumentException("Unknown Uri " + uri);
		}
		if (ret > 0) {
			this.getContext().getContentResolver().notifyChange(uri, null);
		}
		Log.d(TAG, "updated: " + ret);
		return ret;
	}

}
