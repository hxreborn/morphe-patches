/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.bereal;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.drawable.GradientDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

final class LocationRows {

    private static final String TAG = "LocationRows";

    private static final int MAP_HEIGHT_DP = 320;

    private static final float MAP_SAVED_ZOOM = 14f;

    private static final int SEARCH_ICON_DP = 24;

    private final Activity activity;

    private final SettingsStyle style;

    private final SettingsRows rows;

    private SettingsRows.Row row;

    private LinearLayout dependents;

    private LinearLayout body;

    private EditText place;

    private MapPickerView map;

    private int placeRequest;

    private TextView hint;

    LocationRows(Activity activity, SettingsStyle style, SettingsRows rows) {
        this.activity = activity;
        this.style = style;
        this.rows = rows;
    }

    View build(SettingsRows.Row switchRow) {
        this.row = switchRow;
        this.dependents = new LinearLayout(this.activity);
        this.dependents.setOrientation(LinearLayout.VERTICAL);
        this.body = new LinearLayout(this.activity);
        this.body.setOrientation(LinearLayout.VERTICAL);
        this.body.setPadding(this.style.dp(SettingsStyle.SCREEN_INSET_DP), 0,
                this.style.dp(SettingsStyle.SCREEN_INSET_DP), this.style.dp(16));
        this.rows.divider(this.dependents);
        this.dependents.addView(this.body, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        placeSearch();
        mapPicker();
        this.hint = this.style.text("", SettingsStyle.BODY_SP, SettingsStyle.SECONDARY_TEXT, this.style.fonts.regular);
        this.hint.setPadding(0, this.style.dp(12), 0, 0);
        this.body.addView(this.hint);
        refresh();
        return this.dependents;
    }

    void refresh() {
        final boolean enabled = PatchSettings.CUSTOM_LOCATION.get();
        this.dependents.setVisibility(enabled ? View.VISIBLE : View.GONE);
        final Coordinates coordinates = Coordinates.parse(PatchSettings.LOCATION_LATITUDE.get(),
                PatchSettings.LOCATION_LONGITUDE.get());
        final LocationSummary summary = LocationSummary.of(PatchSettings.LOCATION_LABEL.get(), coordinates);
        final String value = summary.label.isEmpty() ? summary.coordinates : summary.label;
        this.row.setValue(enabled ? value : "");
        if (coordinates == null) {
            this.hint.setText("Tap or pan the map to place the pin");
            this.hint.setVisibility(View.VISIBLE);
        } else {
            this.hint.setText(summary.coordinates);
            this.hint.setVisibility(summary.label.isEmpty() ? View.GONE : View.VISIBLE);
        }
    }

    private void placeSearch() {
        final FrameLayout frame = new FrameLayout(this.activity);
        this.place = field("Place or address", InputType.TYPE_CLASS_TEXT);
        this.place.setText(PatchSettings.LOCATION_LABEL.get());
        this.place.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        this.place.setPadding(this.style.dp(16), this.style.dp(14), this.style.dp(56), this.style.dp(14));
        this.place.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_SEARCH) {
                return false;
            }
            findPlace(this.place.getText().toString());
            return true;
        });
        frame.addView(this.place,
                new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final ImageView search = new ImageView(this.activity);
        search.setScaleType(ImageView.ScaleType.CENTER);
        search.setImageDrawable(new SearchIconDrawable(SettingsStyle.SECONDARY_TEXT, this.style.dp(SEARCH_ICON_DP)));
        search.setBackground(this.style.ripple(Color.TRANSPARENT, 24));
        search.setContentDescription("Find place");
        search.setClickable(true);
        search.setOnClickListener((view) -> findPlace(this.place.getText().toString()));
        frame.addView(search, new FrameLayout.LayoutParams(this.style.dp(48), this.style.dp(48),
                Gravity.END | Gravity.CENTER_VERTICAL));

        final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = this.style.dp(12);
        this.body.addView(frame, params);
    }

    private void mapPicker() {
        final int radius = this.style.dp(SettingsStyle.FIELD_RADIUS_DP);
        final FrameLayout frame = new FrameLayout(this.activity);
        frame.setBackground(this.style.rounded(SettingsStyle.FIELD_SURFACE, SettingsStyle.FIELD_RADIUS_DP));
        frame.setClipToOutline(true);
        frame.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
            }
        });

        this.map = new MapPickerView(this.activity);
        moveMapToSavedLocation();
        this.map.setListener((latitude, longitude) -> {
            this.placeRequest++;
            PatchSettings.LOCATION_LATITUDE.save(String.format(Locale.US, "%.6f", latitude));
            PatchSettings.LOCATION_LONGITUDE.save(String.format(Locale.US, "%.6f", longitude));
            PatchSettings.LOCATION_LABEL.save("");
            this.place.setText("");
            refresh();
        });
        frame.addView(this.map,
                new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        final LinearLayout zoom = new LinearLayout(this.activity);
        zoom.setOrientation(LinearLayout.VERTICAL);
        zoom.addView(zoomButton("+", "Zoom in", 1f));
        zoom.addView(zoomButton("−", "Zoom out", -1f));
        final FrameLayout.LayoutParams zoomParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.END | Gravity.BOTTOM);
        zoomParams.setMargins(0, 0, this.style.dp(8), this.style.dp(8));
        frame.addView(zoom, zoomParams);

        final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                this.style.dp(MAP_HEIGHT_DP));
        params.topMargin = this.style.dp(12);
        this.body.addView(frame, params);
    }

    private void moveMapToSavedLocation() {
        final Coordinates saved = Coordinates.parse(PatchSettings.LOCATION_LATITUDE.get(),
                PatchSettings.LOCATION_LONGITUDE.get());
        if (saved != null) {
            this.map.moveTo(saved.latitude, saved.longitude, MAP_SAVED_ZOOM);
        }
    }

    private TextView zoomButton(String label, String description, float delta) {
        final TextView button = this.style.text(label, SettingsStyle.TITLE_SP, SettingsStyle.PRIMARY_TEXT,
                this.style.fonts.medium);
        button.setGravity(Gravity.CENTER);
        button.setMinWidth(this.style.dp(48));
        button.setMinHeight(this.style.dp(48));
        button.setBackground(this.style.ripple(SettingsStyle.MAP_CONTROL_SURFACE, SettingsStyle.FIELD_RADIUS_DP));
        button.setContentDescription(description);
        button.setClickable(true);
        button.setOnClickListener((view) -> this.map.zoomBy(delta));
        final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = this.style.dp(4);
        button.setLayoutParams(params);
        return button;
    }

    private void findPlace(String query) {
        final String name = query.trim();
        final int request = ++this.placeRequest;
        if (name.isEmpty()) {
            return;
        }
        if (!Geocoder.isPresent()) {
            toast("This phone has no place search. Use the map.");
            return;
        }
        new Thread(() -> {
            List<Address> results;
            try {
                results = new Geocoder(this.activity).getFromLocationName(name, 1);
            } catch (IOException | RuntimeException ex) {
                Log.e(TAG, "Place search failed", ex);
                results = null;
            }
            final Address address = (results == null || results.isEmpty()) ? null : withCoordinates(results.get(0));
            this.activity.runOnUiThread(() -> {
                if (request != this.placeRequest || !this.dependents.isAttachedToWindow()
                        || !PatchSettings.CUSTOM_LOCATION.get()) {
                    return;
                }
                if (address == null) {
                    toast("No place found for \"" + name + "\"");
                    return;
                }
                PatchSettings.LOCATION_LABEL.save(describe(address, name));
                PatchSettings.LOCATION_LATITUDE.save(String.format(Locale.US, "%.6f", address.getLatitude()));
                PatchSettings.LOCATION_LONGITUDE.save(String.format(Locale.US, "%.6f", address.getLongitude()));
                this.map.moveTo(address.getLatitude(), address.getLongitude(), MAP_SAVED_ZOOM);
                refresh();
            });
        }).start();
    }

    private static Address withCoordinates(Address address) {
        return (address.hasLatitude() && address.hasLongitude()) ? address : null;
    }

    private static String describe(Address address, String fallback) {
        final String line = (address.getMaxAddressLineIndex() >= 0) ? address.getAddressLine(0) : null;
        return (line != null && !line.isEmpty()) ? line : fallback;
    }

    private EditText field(String hint, int inputType) {
        final EditText field = new EditText(this.activity);
        field.setHint(hint);
        field.setInputType(inputType);
        field.setSingleLine(true);
        field.setTextColor(SettingsStyle.PRIMARY_TEXT);
        field.setHintTextColor(SettingsStyle.SECONDARY_TEXT);
        field.setTextSize(TypedValue.COMPLEX_UNIT_SP, SettingsStyle.ROW_TITLE_SP);
        field.setTypeface(this.style.fonts.regular);
        field.setBackground(this.style.rounded(SettingsStyle.FIELD_SURFACE, SettingsStyle.FIELD_RADIUS_DP));
        field.setMinHeight(this.style.dp(48));
        field.setHighlightColor(0x66007AFF);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            final GradientDrawable cursor = new GradientDrawable();
            cursor.setColor(SettingsStyle.PRIMARY_TEXT);
            cursor.setSize(this.style.dp(2), 0);
            field.setTextCursorDrawable(cursor);
        }
        return field;
    }

    private void toast(String message) {
        Toast.makeText(this.activity, message, Toast.LENGTH_SHORT).show();
    }

}
