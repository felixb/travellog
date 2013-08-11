/*
 * Copyright (C) 2009-2012 Felix Bechstein
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

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Date;

import de.ub0r.android.lib.Log;
import de.ub0r.android.lib.Utils;
import de.ub0r.android.travelLog.R;
import de.ub0r.android.travelLog.data.DataProvider;

/**
 * {@link SherlockActivity} showing all auto logs.
 *
 * @author flx
 */
public final class Map extends SherlockActivity implements GoogleMap.OnMarkerDragListener,
        GoogleMap.OnMarkerClickListener, GoogleMap.OnMapClickListener {

    /**
     * Tag for output.
     */
    private static final String TAG = "Map";

    /**
     * Default zoom level.
     */
    private static final int DEFAULT_ZOOM = 12;

    /**
     * {@link MapView}.
     */
    private MapView mv;

    private GoogleMap googleMap;

    /**
     * Add item on tap.
     */
    private static boolean addItem = false;

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        this.setTheme(Preferences.getTheme(this));
        Utils.setLocale(this);
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.map);
        this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        this.setTitle(R.string.map_);
        if (savedInstanceState == null || this.mv == null) {
            Toast.makeText(this, R.string.map_hint, Toast.LENGTH_LONG).show();

            this.mv = (MapView) this.findViewById(R.id.mapview);
            this.mv.onCreate(savedInstanceState);
            this.googleMap = this.mv.getMap();
            if (this.googleMap != null) {
                this.googleMap.getUiSettings().setAllGesturesEnabled(true);
                this.googleMap.getUiSettings().setCompassEnabled(true);
                this.googleMap.getUiSettings().setMyLocationButtonEnabled(true);
                this.googleMap.getUiSettings().setZoomControlsEnabled(true);
                this.googleMap.setOnMapClickListener(this);
                this.googleMap.setOnMarkerDragListener(this);
                this.googleMap.setOnMarkerClickListener(this);

                loadMarker();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        this.mv.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        this.mv.onLowMemory();
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.mv.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.mv.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.mv.onDestroy();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        this.getSupportMenuInflater().inflate(R.menu.map, menu);
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
            case android.R.id.home:
                Intent intent = new Intent(this, Logs.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                this.startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void loadMarker() {
        this.googleMap.clear();

        Cursor cursor = getContentResolver()
                .query(DataProvider.Cells.CONTENT_URI, DataProvider.Cells.PROJECTION, null, null,
                        null);
        if (cursor == null) {
            return;
        }
        if (!cursor.moveToFirst()) {
            cursor.close();
            return;
        }

        do {
            this.googleMap.addCircle(getCircle(cursor));
            this.googleMap.addMarker(getMarker(cursor));
        } while (cursor.moveToNext());
        cursor.close();
    }

    private MarkerOptions getMarker(final Cursor cursor) {
        MarkerOptions mo = new MarkerOptions();
        mo.position(new LatLng(db2pos(cursor.getLong(4)), db2pos(cursor.getLong(5))));
        mo.title(cursor.getString(8));
        mo.snippet("_id=" + cursor.getLong(0));
        mo.draggable(true);
        return mo;
    }

    private CircleOptions getCircle(final Cursor cursor) {
        CircleOptions co = new CircleOptions();
        co.center(new LatLng(db2pos(cursor.getLong(4)), db2pos(cursor.getLong(5))));
        co.radius(cursor.getLong(6));
        int type = cursor.getInt(1);
        switch (type) {
            case DataProvider.Logtypes.TYPE_PAUSE:
                co.fillColor(0x8000FF00);
                break;
            case DataProvider.Logtypes.TYPE_TRAVEL:
                co.fillColor(0x800000FF);
                break;
            case DataProvider.Logtypes.TYPE_WORK:
                co.fillColor(0x80FF0000);
                break;
            default:
                co.fillColor(0x80FFFFFF);
                break;
        }
        return co;
    }

    @Override
    public void onMapClick(final LatLng latLng) {
        if (addItem) {
            showCreateDialog(latLng, -1);
            addItem = false;
        }
    }

    @Override
    public boolean onMarkerClick(final Marker marker) {
        showItemDialog(marker);
        return true;
    }

    @Override
    public void onMarkerDragStart(final Marker marker) {
        // nothing
    }

    @Override
    public void onMarkerDrag(final Marker marker) {
        // nothing
    }

    @Override
    public void onMarkerDragEnd(final Marker marker) {
        long id = 0;
        ContentValues values = new ContentValues(2);
        values.put(DataProvider.Cells.LATITUDE, pos2db(marker.getPosition().latitude));
        values.put(DataProvider.Cells.LONGITUDE, pos2db(marker.getPosition().longitude));
        Uri u = ContentUris.withAppendedId(DataProvider.Cells.CONTENT_URI, id);
        getContentResolver().update(u, values, null, null);
        loadMarker();
    }

    private long pos2db(final double pos) {
        return (long) (pos * 1000000);
    }

    private double db2pos(final long pos) {
        return ((double) pos) / 1000000;
    }

    /**
     * Show create/edit dialog.
     *
     * @param p       {@link LatLng}
     * @param oldItem old item
     */
    private void showCreateDialog(final LatLng p, final int oldItem) {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        if (oldItem < 0) {
            b.setTitle(R.string.add_cell_);
        } else {
            b.setTitle(R.string.edit_cell_);
        }
        LayoutInflater inflater = LayoutInflater.from(this);
        View v = inflater.inflate(R.layout.map_item_add, null);
        final Spinner sp = (Spinner) v.findViewById(R.id.type);
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
                android.R.layout.simple_spinner_item, this.getContentResolver()
                .query(DataProvider.Logtypes.CONTENT_URI, DataProvider.Logtypes.PROJECTION, null,
                        null, null), new String[]{DataProvider.Logtypes.NAME},
                new int[]{android.R.id.text1});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp.setAdapter(adapter);
        final CheckBox cb = (CheckBox) v.findViewById(R.id.nulltype);
        cb.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(final CompoundButton buttonView,
                    final boolean isChecked) {
                sp.setEnabled(!isChecked);
            }
        });
        final EditText et = (EditText) v.findViewById(R.id.radius);
        if (oldItem >= 0) {
            Log.d(TAG, "fill data from old item: " + oldItem);
            Cursor cursor = getContentResolver()
                    .query(ContentUris.withAppendedId(DataProvider.Cells.CONTENT_URI, oldItem),
                            DataProvider.Cells.PROJECTION, null, null,
                            null);
            if (cursor != null && cursor.moveToFirst()) {
                boolean nothing = cursor.getInt(9) == 0;
                cb.setChecked(nothing);
                et.setText(cursor.getString(6));
            }
            if (cursor != null) {
                cursor.close();
            }
        }
        b.setView(v);
        b.setCancelable(true);
        b.setPositiveButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog,
                            final int which) {
                        final int radius = Utils.parseInt(et.getText().toString(), 0);
                        int type = 0;
                        if (!cb.isChecked()) {
                            type = (int) sp.getSelectedItemId();
                        }
                        if (oldItem >= 0) {
                            ContentValues values = new ContentValues(2);
                            values.put(DataProvider.Cells.TYPE, type);
                            values.put(DataProvider.Cells.RADIUS, radius);
                            Uri u = ContentUris
                                    .withAppendedId(DataProvider.Cells.CONTENT_URI, oldItem);
                            getContentResolver().update(u, values, null, null);
                        } else {
                            ContentValues values = new ContentValues(4);
                            values.put(DataProvider.Cells.LATITUDE, pos2db(p.latitude));
                            values.put(DataProvider.Cells.LONGITUDE, pos2db(p.longitude));
                            values.put(DataProvider.Cells.TYPE, type);
                            values.put(DataProvider.Cells.RADIUS, radius);
                            getContentResolver().insert(DataProvider.Cells.CONTENT_URI, values);
                        }
                        loadMarker();
                    }
                });
        b.setNegativeButton(android.R.string.cancel, null);
        b.show();
    }

    /**
     * Show an item.
     *
     * @param marker marker
     */
    private void showItemDialog(final Marker marker) {
        String s = marker.getSnippet();
        final int item = s == null || !s.contains("=") ? 0 : Integer.parseInt(s.split("=")[1]);
        java.text.DateFormat dFormat = DateFormat.getDateFormat(this);
        java.text.DateFormat tFormat = DateFormat.getTimeFormat(this);
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle(R.string.cell_);
        LayoutInflater inflater = LayoutInflater.from(this);
        View v = inflater.inflate(R.layout.map_item_show, null);
        Cursor cursor = getContentResolver()
                .query(ContentUris.withAppendedId(DataProvider.Cells.CONTENT_URI, item),
                        DataProvider.Cells.PROJECTION, null, null, null);
        String t = null;
        String rad = null;
        long seenFirst = 0;
        long seenLast = 0;
        if (cursor != null && cursor.moveToFirst()) {
            t = cursor.getString(8);
            rad = cursor.getString(6);
            seenFirst = cursor.getLong(2);
            seenLast = cursor.getLong(3);
        }
        if (cursor != null) {
            cursor.close();
        }
        if (t == null) {
            t = this.getString(R.string.nulltype_);
        }
        ((TextView) v.findViewById(R.id.type)).setText(this.getString(R.string.type_) + " " + t);
        ((TextView) v.findViewById(R.id.radius))
                .setText(this.getString(R.string.radius_) + " " + rad);
        Date d = new Date(seenFirst);
        ((TextView) v.findViewById(R.id.first_seen)).setText(
                this.getString(R.string.first_seen_) + " " + dFormat.format(d) + " " + tFormat
                        .format(d));
        d = new Date(seenLast);
        ((TextView) v.findViewById(R.id.last_seen))
                .setText(this.getString(R.string.last_seen_) + " " + dFormat.format(d)
                        + " " + tFormat.format(d));
        b.setView(v);
        b.setCancelable(true);
        b.setPositiveButton(android.R.string.ok, null);
        b.setNeutralButton(R.string.edit_,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog,
                            final int which) {
                        showCreateDialog(null, item);
                    }
                });
        b.setNegativeButton(R.string.delete,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog,
                            final int which) {
                        getContentResolver().delete(
                                ContentUris.withAppendedId(DataProvider.Cells.CONTENT_URI, item),
                                null, null);
                        loadMarker();
                    }
                });
        b.show();
    }
}
