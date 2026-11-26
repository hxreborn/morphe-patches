/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.exiftool;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class MapPickerScreen {

    private static final int BACKGROUND = 0xFF1E0F12;

    private static final int FOREGROUND = Color.WHITE;

    private static final int ACCENT = 0xFFF4AAB7;

    private static final float INITIAL_ZOOM = 14f;

    private static final float DEFAULT_ZOOM = 2f;

    private static final int MATCH_PARENT = LinearLayout.LayoutParams.MATCH_PARENT;

    private static final int WRAP_CONTENT = LinearLayout.LayoutParams.WRAP_CONTENT;

    private static final int TITLE_TEXT_SP = 20;

    private static final int INPUT_TEXT_SP = 14;

    private static final int TOOLBAR_HEIGHT_DP = 56;

    private static final int ROW_HEIGHT_DP = 48;

    private static final int HORIZONTAL_PADDING_DP = 16;

    private MapPickerScreen() {
    }

    public static void show(Activity activity) {
        Intent intent = activity.getIntent();
        double[] initial = (intent != null) ? intent.getDoubleArrayExtra("location_latlng") : null;
        double latitude = 0.0;
        double longitude = 0.0;
        float zoom = DEFAULT_ZOOM;
        if (initial != null && initial.length == 2) {
            latitude = initial[0];
            longitude = initial[1];
            zoom = INITIAL_ZOOM;
        }

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BACKGROUND);
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(0, insets.getSystemWindowInsetTop(), 0, insets.getSystemWindowInsetBottom());
            return insets;
        });

        LinearLayout toolbar = new LinearLayout(activity);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(activity, HORIZONTAL_PADDING_DP), 0, dp(activity, HORIZONTAL_PADDING_DP), 0);
        Button back = button(activity, "Back");
        back.setOnClickListener((view) -> activity.finish());
        toolbar.addView(back, new LinearLayout.LayoutParams(WRAP_CONTENT, dp(activity, TOOLBAR_HEIGHT_DP)));
        TextView title = new TextView(activity);
        title.setText("GPS Coordinates Editor");
        title.setTextColor(FOREGROUND);
        title.setTextSize(TITLE_TEXT_SP);
        title.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.addView(title, new LinearLayout.LayoutParams(0, dp(activity, TOOLBAR_HEIGHT_DP), 1f));
        Button save = button(activity, "Save");
        toolbar.addView(save, new LinearLayout.LayoutParams(WRAP_CONTENT, dp(activity, TOOLBAR_HEIGHT_DP)));
        root.addView(toolbar);

        MapPickerView map = new MapPickerView(activity);
        map.moveTo(latitude, longitude, zoom);
        root.addView(map, new LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f));

        LinearLayout searchRow = new LinearLayout(activity);
        searchRow.setPadding(dp(activity, HORIZONTAL_PADDING_DP), 0, dp(activity, HORIZONTAL_PADDING_DP), 0);
        searchRow.setGravity(Gravity.CENTER_VERTICAL);
        EditText searchInput = new EditText(activity);
        searchInput.setSingleLine(true);
        searchInput.setTextColor(FOREGROUND);
        searchInput.setHintTextColor(ACCENT);
        searchInput.setTextSize(INPUT_TEXT_SP);
        searchInput.setHint("Search for a place");
        searchRow.addView(searchInput, new LinearLayout.LayoutParams(0, dp(activity, ROW_HEIGHT_DP), 1f));
        Button search = button(activity, "Search");
        searchRow.addView(search, new LinearLayout.LayoutParams(WRAP_CONTENT, dp(activity, ROW_HEIGHT_DP)));
        root.addView(searchRow);

        LinearLayout coordinates = new LinearLayout(activity);
        coordinates.setPadding(dp(activity, HORIZONTAL_PADDING_DP), 0, dp(activity, HORIZONTAL_PADDING_DP), 0);
        coordinates.setGravity(Gravity.CENTER_VERTICAL);
        EditText input = new EditText(activity);
        input.setSingleLine(true);
        input.setTextColor(FOREGROUND);
        input.setHintTextColor(ACCENT);
        input.setTextSize(INPUT_TEXT_SP);
        input.setHint("Latitude, longitude");
        input.setText(format(latitude, longitude));
        coordinates.addView(input, new LinearLayout.LayoutParams(0, dp(activity, ROW_HEIGHT_DP), 1f));
        Button go = button(activity, "Go");
        coordinates.addView(go, new LinearLayout.LayoutParams(WRAP_CONTENT, dp(activity, ROW_HEIGHT_DP)));
        root.addView(coordinates);

        map.setListener(
                (selectedLatitude, selectedLongitude) -> input.setText(format(selectedLatitude, selectedLongitude)));
        search.setOnClickListener((view) -> search(activity, map, input, searchInput, search));
        go.setOnClickListener((view) -> moveToInput(map, input));
        save.setOnClickListener((view) -> {
            if (moveToInput(map, input)) {
                save(activity, map.latitude(), map.longitude());
            }
        });
        ViewGroup content = activity.findViewById(android.R.id.content);
        content.addView(root, new ViewGroup.LayoutParams(MATCH_PARENT, -1));
        root.requestApplyInsets();
    }

    public static void save(Activity activity, double latitude, double longitude) {
        throw new IllegalStateException("Map result stub was not replaced");
    }

    private static void search(Activity activity, MapPickerView map, EditText coordinates, EditText queryInput,
            Button searchButton) {
        String query = queryInput.getText().toString().trim();
        if (query.isEmpty()) {
            queryInput.setError("Enter a place name");
            return;
        }
        searchButton.setEnabled(false);
        new Thread(() -> {
            Address result = null;
            boolean queryFailed = false;
            try {
                List<Address> matches = new Geocoder(activity, Locale.getDefault()).getFromLocationName(query, 1);
                if (matches != null && !matches.isEmpty()) {
                    result = matches.get(0);
                }
            } catch (IOException ex) {
                queryFailed = true;
            }
            Address found = result;
            boolean failed = queryFailed;
            activity.runOnUiThread(() -> {
                searchButton.setEnabled(true);
                if (failed) {
                    queryInput.setError("Place search failed");
                    return;
                }
                if (found == null) {
                    queryInput.setError("No place found");
                    return;
                }
                map.moveTo(found.getLatitude(), found.getLongitude(), INITIAL_ZOOM);
                coordinates.setText(format(map.latitude(), map.longitude()));
            });
        }, "ExifTool place search").start();
    }

    private static boolean moveToInput(MapPickerView map, EditText input) {
        String[] parts = input.getText().toString().split(",", -1);
        if (parts.length != 2) {
            input.setError("Enter latitude and longitude");
            return false;
        }
        try {
            double latitude = Double.parseDouble(parts[0].trim());
            double longitude = Double.parseDouble(parts[1].trim());
            if (Double.isNaN(latitude) || Double.isInfinite(latitude) || Double.isNaN(longitude)
                    || Double.isInfinite(longitude) || Math.abs(latitude) > 85.0511287798
                    || Math.abs(longitude) > 180.0) {
                input.setError("Coordinates are outside the map");
                return false;
            }
            map.moveTo(latitude, longitude, INITIAL_ZOOM);
            return true;
        } catch (NumberFormatException ex) {
            input.setError("Enter numeric coordinates");
            return false;
        }
    }

    private static Button button(Activity activity, String label) {
        Button button = new Button(activity);
        button.setText(label);
        button.setTextColor(ACCENT);
        button.setAllCaps(false);
        return button;
    }

    private static String format(double latitude, double longitude) {
        return String.format(Locale.US, "%.6f, %.6f", latitude, longitude);
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }

}
