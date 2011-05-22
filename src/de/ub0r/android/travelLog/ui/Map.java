/*
 * Copyright (C) 2009-2011 Felix Bechstein
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

import java.util.List;

import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.google.android.maps.Projection;

import de.ub0r.android.lib.Log;
import de.ub0r.android.lib.Utils;
import de.ub0r.android.travelLog.R;
import de.ub0r.android.travelLog.data.DataProvider;

/**
 * {@link MapActivity} showing all auto logs.
 * 
 * @author flx
 */
public final class Map extends MapActivity {
	/** Tag for output. */
	private static final String TAG = "Map";

	/**
	 * Class holding all cells in an overlay.
	 * 
	 * @author flx
	 */
	private static class CellOverlay extends ItemizedOverlay<OverlayItem> {
		/** Transparency for fill. */
		private final int TRANS_FILL = 0x80;
		/** Transparency for stroke. */
		private final int TRANS_STROKE = 0xD0;
		/** Color for circles. */
		private final int[][] COLOR = new int[][] {
				new int[] { 0xFF, 0xFF, 0xFF }, // nothing
				new int[] { 0x00, 0xFF, 0x00 }, // pause
				new int[] { 0x00, 0x00, 0xFF }, // travel
				new int[] { 0xFF, 0x00, 0x00 }, // work
		};

		/** Used {@link Cursor}. */
		private final Cursor cursor;
		/** Used {@link Context}. */
		private final Context ctx;

		/** Column ids. */
		private final int idId, idType, idTypeName, idTypeType, idLat, idLong,
				idRad, idSeenFirst, idSeenLast;

		/**
		 * Get the overlay.
		 * 
		 * @param context
		 *            {@link Context}
		 */
		public CellOverlay(final Context context) {
			super(boundCenterBottom(context.getResources().getDrawable(
					R.drawable.marker)));
			this.ctx = context;
			this.cursor = context.getContentResolver().query(
					DataProvider.Cells.CONTENT_URI,
					DataProvider.Cells.PROJECTION, null, null, null);

			this.idId = this.cursor.getColumnIndex(DataProvider.Cells.ID);
			this.idType = this.cursor.getColumnIndex(DataProvider.Cells.TYPE);
			this.idTypeName = this.cursor
					.getColumnIndex(DataProvider.Cells.TYPE_NAME);
			this.idTypeType = this.cursor
					.getColumnIndex(DataProvider.Cells.TYPE_TYPE);
			this.idLat = this.cursor
					.getColumnIndex(DataProvider.Cells.LATITUDE);
			this.idLong = this.cursor
					.getColumnIndex(DataProvider.Cells.LONGITUDE);
			this.idRad = this.cursor.getColumnIndex(DataProvider.Cells.RADIUS);
			this.idSeenFirst = this.cursor
					.getColumnIndex(DataProvider.Cells.SEEN_FIRST);
			this.idSeenLast = this.cursor
					.getColumnIndex(DataProvider.Cells.SEEN_LAST);
			this.populate();
		}

		/**
		 * Add a {@link OverlayItem}.
		 * 
		 * @param point
		 *            {@link GeoPoint}
		 * @param type
		 *            log type
		 * @param radius
		 *            radius
		 */
		private void add(final GeoPoint point, final int type, // .
				final int radius) {
			ContentValues values = new ContentValues();
			values.put(DataProvider.Cells.LATITUDE, point.getLatitudeE6());
			values.put(DataProvider.Cells.LONGITUDE, point.getLongitudeE6());
			values.put(DataProvider.Cells.TYPE, type);
			values.put(DataProvider.Cells.RADIUS, radius);

			this.ctx.getContentResolver().insert(
					DataProvider.Cells.CONTENT_URI, values);
			this.cursor.requery();
			this.populate();
			addItem = false;
		}

		/**
		 * Remove an item.
		 * 
		 * @param item
		 *            {@link OverlayItem}
		 */
		private void remove(final int item) {
			if (!this.cursor.moveToPosition(item)) {
				return;
			}
			final long id = this.cursor.getLong(this.idId);
			this.ctx.getContentResolver().delete(
					ContentUris.withAppendedId(DataProvider.Cells.CONTENT_URI,
							id), null, null);
			this.cursor.requery();
			this.populate();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		protected OverlayItem createItem(final int item) {
			if (!this.cursor.moveToPosition(item)) {
				return null;
			}
			OverlayItem ret = new OverlayItem(new GeoPoint(this.cursor
					.getInt(this.idLat), this.cursor.getInt(this.idLong)), "",
					"");
			return ret;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public int size() {
			if (this.cursor == null) {
				return 0;
			}
			return this.cursor.getCount();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean onTap(final GeoPoint p, final MapView mapView) {
			if (!addItem) {
				return super.onTap(p, mapView);
			}
			AlertDialog.Builder b = new AlertDialog.Builder(this.ctx);
			b.setTitle(R.string.add_cell_);
			LayoutInflater inflater = LayoutInflater.from(this.ctx);
			View v = inflater.inflate(R.layout.map_item_add, null);
			final Spinner sp = (Spinner) v.findViewById(R.id.type);
			sp.setAdapter(new SimpleCursorAdapter(this.ctx,
					android.R.layout.simple_spinner_item, this.ctx
							.getContentResolver().query(
									DataProvider.Logtypes.CONTENT_URI,
									DataProvider.Logtypes.PROJECTION, null,
									null, null),
					new String[] { DataProvider.Logtypes.NAME },
					new int[] { android.R.id.text1 }));
			final CheckBox cb = (CheckBox) v.findViewById(R.id.nulltype);
			cb.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(final CompoundButton buttonView,
						final boolean isChecked) {
					sp.setEnabled(!isChecked);
				}
			});
			final EditText et = (EditText) v.findViewById(R.id.radius);
			b.setView(v);
			b.setCancelable(true);
			b.setPositiveButton(android.R.string.ok,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(final DialogInterface dialog,
								final int which) {
							final int radius = Utils.parseInt(et.getText()
									.toString(), 0);
							int type = 0;
							if (!cb.isChecked()) {
								type = (int) sp.getSelectedItemId();
							}
							CellOverlay.this.add(p, type, radius);
						}
					});
			b.setNegativeButton(android.R.string.cancel, null);
			b.show();

			return true;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean onTap(final int item) {
			if (!this.cursor.moveToPosition(item)) {
				return super.onTap(item);
			}
			AlertDialog.Builder b = new AlertDialog.Builder(this.ctx);
			b.setTitle(R.string.cell_);
			LayoutInflater inflater = LayoutInflater.from(this.ctx);
			View v = inflater.inflate(R.layout.map_item_show, null);
			((TextView) v.findViewById(R.id.type)).setText(this.ctx
					.getString(R.string.type_)
					+ " " + this.cursor.getString(this.idTypeName));
			((TextView) v.findViewById(R.id.radius)).setText(this.ctx
					.getString(R.string.radius_)
					+ " " + this.cursor.getString(this.idRad));
			b.setView(v);
			b.setCancelable(true);
			b.setPositiveButton(android.R.string.ok, null);
			b.setNegativeButton(R.string.delete,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(final DialogInterface dialog,
								final int which) {
							CellOverlay.this.remove(item);
						}
					});
			b.show();
			return true;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void draw(final Canvas canvas, final MapView mapView,
				final boolean shadow) {
			if (!shadow && this.cursor.moveToFirst()) {
				Projection projection = mapView.getProjection();
				do {
					int latitude = this.cursor.getInt(this.idLat);
					int longitude = this.cursor.getInt(this.idLong);
					float radius = this.cursor.getInt(this.idRad);
					int type = this.cursor.getInt(this.idTypeType);
					GeoPoint centerGeo = new GeoPoint(latitude, longitude);
					double dLat = latitude / 1E6;
					double dLong = longitude / 1E6;

					// convert radius
					float[] result = new float[1];
					Location.distanceBetween(dLat, dLong, dLat, dLong + 1,
							result);
					float longitudeLineDistance = result[0];
					GeoPoint leftGeo = new GeoPoint(latitude, // .
							(int) ((dLong - // .
							(radius / longitudeLineDistance)) * 1E6));
					Point left = projection.toPixels(leftGeo, null);
					Point center = projection.toPixels(centerGeo, null);
					radius = center.x - left.x;

					// paint circles
					Paint paint;
					paint = new Paint();
					paint.setARGB(this.TRANS_FILL, this.COLOR[type][0],
							this.COLOR[type][1], this.COLOR[type][2]);
					paint.setAntiAlias(true);
					paint.setStyle(Paint.Style.FILL);
					canvas.drawCircle(center.x, center.y, radius, paint);
					paint.setARGB(this.TRANS_STROKE, this.COLOR[type][0],
							this.COLOR[type][1], this.COLOR[type][2]);
					paint.setStyle(Paint.Style.STROKE);
					canvas.drawCircle(center.x, center.y, radius, paint);
				} while (this.cursor.moveToNext());
			}

			// paint original drawable
			super.draw(canvas, mapView, shadow);
		}
	}

	/** Default zoom level. */
	private static final int DEFAULT_ZOOM = 12;

	/** Cell overlay. */
	private CellOverlay cellOverlay;
	/** {@link MyLocationOverlay}. */
	private MyLocationOverlay myLocationOverly;

	/** Add item on tap. */
	private static boolean addItem = false;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Utils.setLocale(this);
		this.setTitle(this.getString(R.string.settings) + " > "
				+ this.getString(R.string.map_));
		this.setContentView(R.layout.map);
		MapView mv = (MapView) this.findViewById(R.id.mapview);
		mv.setBuiltInZoomControls(true);
		final MapController mc = mv.getController();
		mc.setZoom(DEFAULT_ZOOM);

		List<Overlay> overlays = mv.getOverlays();

		this.cellOverlay = new CellOverlay(this);

		Log.d(TAG, "autoLogOverlay.size: " + this.cellOverlay.size());
		overlays.add(this.cellOverlay);

		this.myLocationOverly = new MyLocationOverlay(this, mv);
		this.myLocationOverly.runOnFirstFix(new Runnable() {
			public void run() {
				mc.animateTo(Map.this.myLocationOverly.getMyLocation());
			}
		});
		overlays.add(this.myLocationOverly);
		mv.invalidate();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	@Override
	protected void onResume() {
		super.onResume();
		this.myLocationOverly.enableMyLocation();
	}

	@Override
	protected void onPause() {
		super.onPause();
		this.myLocationOverly.disableMyLocation();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		MenuInflater inflater = this.getMenuInflater();
		inflater.inflate(R.menu.map, menu);
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case R.id.item_add_cell:
			Toast.makeText(this, R.string.add_cell_hint, Toast.LENGTH_LONG)
					.show();
			addItem = true;
			return true;
		default:
			return false;
		}
	}

}
