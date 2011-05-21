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

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

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
		/** Used {@link Cursor}. */
		private final Cursor cursor;
		/** Used {@link Context}. */
		private final Context ctx;

		/** Column ids. */
		private final int idId, idLat, idLong, idRad, idSeenFirst, idSeenLast;

		/**
		 * Get the overlay.
		 * 
		 * @param context
		 *            {@link Context}
		 */
		public CellOverlay(final Context context) {
			super(boundCenterBottom(context.getResources().getDrawable(
					R.drawable.icon)));
			this.ctx = context;
			this.cursor = context.getContentResolver().query(
					DataProvider.Cells.CONTENT_URI,
					DataProvider.Cells.PROJECTION, null, null, null);

			this.idId = this.cursor.getColumnIndex(DataProvider.Cells.ID);
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
		 */
		private void add(final GeoPoint point, final int type) {
			ContentValues values = new ContentValues();
			values.put(DataProvider.Cells.LATITUDE, point.getLatitudeE6());
			values.put(DataProvider.Cells.LONGITUDE, point.getLongitudeE6());
			values.put(DataProvider.Cells.TYPE, type);
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
				return false;
			}
			this.add(p, 0);
			return true;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean onTap(final int item) {
			// FIXME
			this.remove(item); // FIXME
			return true;
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
