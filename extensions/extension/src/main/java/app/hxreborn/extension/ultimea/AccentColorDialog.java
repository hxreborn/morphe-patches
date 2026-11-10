/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Additional term under GPLv3 Section 7(b): When conveying this material
 * or modified versions, preserve this notice and credit hxreborn for this
 * material in any Appropriate Legal Notices displayed by works containing it.
 */
package app.hxreborn.extension.ultimea;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

final class AccentColorDialog {

    private static final int COLUMNS = 4;

    private static final int CELL_MIN_HEIGHT_DP = 80;

    private static final int CELL_PADDING_DP = 4;

    private static final int SWATCH_TOUCH_DP = 48;

    private static final int SWATCH_RING_DP = 40;

    private static final int SWATCH_COLOR_DP = 28;

    private static final int SWATCH_STROKE_DP = 2;

    private static final int CONTENT_GAP_DP = 8;

    private static final int INPUT_MIN_HEIGHT_DP = 48;

    private static final int CHECK_SP = 18;

    private static final int STOCK_COLOR = 0xff541597;

    private static final double SRGB_THRESHOLD = 0.04045;

    private static final double SRGB_DIVISOR = 12.92;

    private static final double SRGB_OFFSET = 0.055;

    private static final double SRGB_SCALE = 1.055;

    private static final double SRGB_EXPONENT = 2.4;

    private static final double DARK_TEXT_THRESHOLD = 0.179;

    private static final double RED_LUMINANCE = 0.2126;

    private static final double GREEN_LUMINANCE = 0.7152;

    private static final double BLUE_LUMINANCE = 0.0722;

    private static final int CHANNEL_MAX = 255;

    private static final Pattern HEX_COLOR = Pattern.compile("#[0-9a-fA-F]{6}");

    private final Activity activity;

    private final SettingsStyle style;

    private final List<Swatch> swatches = new ArrayList<>();

    private final EditText custom;

    private String selectedPreset;

    private boolean customSelected;

    private AlertDialog dialog;

    private AccentColorDialog(Activity activity) {
        this.activity = activity;
        this.style = new SettingsStyle(activity);
        this.selectedPreset = AccentPalette.getPreset(activity);
        this.custom = new EditText(activity);
    }

    static void show(Activity activity) {
        if (!activity.isFinishing() && !activity.isDestroyed()) {
            new AccentColorDialog(activity).show();
        }
    }

    private void show() {
        LinearLayout content = new LinearLayout(this.activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setFocusableInTouchMode(true);
        content.setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
        content.setPadding(this.style.dp(SettingsStyle.SCREEN_INSET_DP), this.style.dp(CONTENT_GAP_DP),
                this.style.dp(SettingsStyle.SCREEN_INSET_DP), this.style.dp(CONTENT_GAP_DP));
        content.addView(grid());
        TextView customLabel = this.style.text("Custom", SettingsStyle.BODY_SP, this.style.primaryText,
                this.style.medium);
        LinearLayout.LayoutParams labelLayout = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        labelLayout.topMargin = this.style.dp(CONTENT_GAP_DP);
        content.addView(customLabel, labelLayout);
        configureInput();
        content.addView(this.custom, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        ScrollView scroll = new ScrollView(this.activity);
        scroll.addView(content);
        this.dialog = new AlertDialog.Builder(this.activity, dialogTheme()).setTitle("Accent color")
            .setView(scroll)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Apply", null)
            .create();
        this.dialog.setOnShowListener((dialog) -> {
            this.dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setAllCaps(false);
            this.dialog.getButton(AlertDialog.BUTTON_POSITIVE).setAllCaps(false);
            this.dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(this.style.secondaryText);
            this.dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(AccentPalette.primary(this.activity, this.style.dark));
            this.dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener((view) -> apply());
        });
        updateSelection();
        this.dialog.show();
    }

    private View grid() {
        LinearLayout grid = new LinearLayout(this.activity);
        grid.setOrientation(LinearLayout.VERTICAL);
        this.swatches.add(swatch("ULTIMEA", AccentPalette.STOCK, STOCK_COLOR));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            this.swatches.add(swatch("Material You", AccentPalette.SYSTEM, systemColor()));
        }
        for (AccentPalette.Fixed fixed : AccentPalette.Fixed.values()) {
            this.swatches.add(swatch(fixed.label, fixed.preset, Color.parseColor(fixed.preset)));
        }
        LinearLayout row = null;
        for (int index = 0; index < this.swatches.size(); index++) {
            if (index % COLUMNS == 0) {
                row = new LinearLayout(this.activity);
                row.setBaselineAligned(false);
                grid.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
            }
            row.addView(this.swatches.get(index).view,
                    new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        }
        int remainder = this.swatches.size() % COLUMNS;
        for (int index = remainder; index > 0 && index < COLUMNS; index++) {
            row.addView(new View(this.activity), new LinearLayout.LayoutParams(0, 0, 1f));
        }
        return grid;
    }

    private Swatch swatch(String label, String preset, int color) {
        LinearLayout cell = new LinearLayout(this.activity);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER_HORIZONTAL);
        cell.setMinimumHeight(this.style.dp(CELL_MIN_HEIGHT_DP));
        cell.setPadding(this.style.dp(CELL_PADDING_DP), this.style.dp(CELL_PADDING_DP), this.style.dp(CELL_PADDING_DP),
                this.style.dp(CELL_PADDING_DP));
        cell.setFocusable(true);
        cell.setContentDescription(label);
        cell.setBackground(this.style.ripple(SettingsStyle.CARD_RADIUS_DP));
        FrameLayout circle = new FrameLayout(this.activity);
        circle.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        GradientDrawable ring = new GradientDrawable();
        ring.setShape(GradientDrawable.OVAL);
        ring.setColor(Color.TRANSPARENT);
        View outline = new View(this.activity);
        outline.setBackground(ring);
        circle.addView(outline, new FrameLayout.LayoutParams(this.style.dp(SWATCH_RING_DP),
                this.style.dp(SWATCH_RING_DP), Gravity.CENTER));
        GradientDrawable fill = new GradientDrawable();
        fill.setShape(GradientDrawable.OVAL);
        fill.setColor(color);
        TextView check = this.style.text("✓", CHECK_SP, foreground(color), this.style.medium);
        check.setGravity(Gravity.CENTER);
        check.setBackground(fill);
        circle.addView(check, new FrameLayout.LayoutParams(this.style.dp(SWATCH_COLOR_DP),
                this.style.dp(SWATCH_COLOR_DP), Gravity.CENTER));
        cell.addView(circle,
                new LinearLayout.LayoutParams(this.style.dp(SWATCH_TOUCH_DP), this.style.dp(SWATCH_TOUCH_DP)));
        TextView title = this.style.text(label, SettingsStyle.BODY_SP, this.style.primaryText, null);
        title.setGravity(Gravity.CENTER);
        title.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        cell.addView(title, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        Swatch swatch = new Swatch(preset, cell, ring, check);
        cell.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override
            public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo node) {
                super.onInitializeAccessibilityNodeInfo(host, node);
                node.setClassName("android.widget.RadioButton");
                node.setCheckable(true);
                node.setChecked(isSelected(swatch));
            }
        });
        cell.setOnClickListener((view) -> {
            this.customSelected = false;
            this.selectedPreset = preset;
            this.custom.clearFocus();
            this.custom.setError(null);
            updateSelection();
            cell.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);
        });
        return swatch;
    }

    private void configureInput() {
        this.custom.setSingleLine(true);
        this.custom.setHint("#RRGGBB");
        this.custom.setContentDescription("Custom accent color");
        this.custom.setTextSize(TypedValue.COMPLEX_UNIT_SP, SettingsStyle.BODY_SP);
        this.custom.setTextColor(this.style.primaryText);
        this.custom.setHintTextColor(this.style.secondaryText);
        this.custom.setMinHeight(this.style.dp(INPUT_MIN_HEIGHT_DP));
        this.custom.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        boolean fixed = false;
        for (Swatch swatch : this.swatches) {
            fixed |= swatch.preset.equals(this.selectedPreset);
        }
        this.customSelected = !fixed && HEX_COLOR.matcher(this.selectedPreset).matches();
        if (this.customSelected) {
            this.custom.setText(this.selectedPreset);
        }
        this.custom.setOnFocusChangeListener((view, focused) -> {
            if (focused) {
                this.customSelected = true;
                updateSelection();
            }
        });
        this.custom.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
                AccentColorDialog.this.custom.setError(null);
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
                AccentColorDialog.this.customSelected = true;
            }

            @Override
            public void afterTextChanged(Editable text) {
                updateSelection();
            }
        });

    }

    private boolean isSelected(Swatch swatch) {
        return !this.customSelected && swatch.preset.equals(this.selectedPreset);
    }

    private void updateSelection() {
        for (Swatch swatch : this.swatches) {
            boolean selected = isSelected(swatch);
            swatch.view.setSelected(selected);
            swatch.ring.setStroke(this.style.dp(SWATCH_STROKE_DP),
                    selected ? this.style.primaryText : Color.TRANSPARENT);
            swatch.check.setText(selected ? "✓" : "");
        }
    }

    private void apply() {
        String preset = this.selectedPreset;
        if (this.customSelected) {
            preset = this.custom.getText().toString().trim();
            if (!HEX_COLOR.matcher(preset).matches()) {
                this.custom.setError("Enter a color as #RRGGBB");
                this.custom.requestFocus();
                return;
            }
            preset = preset.toUpperCase(Locale.ROOT);
        }
        if (preset.equals(AccentPalette.getPreset(this.activity))) {
            this.dialog.dismiss();
            return;
        }
        if (!AccentColor.setPreset(this.activity, preset)) {
            Toast.makeText(this.activity, "Could not save accent color", Toast.LENGTH_SHORT).show();
            return;
        }
        this.dialog.dismiss();
        this.activity.recreate();
    }

    private int dialogTheme() {
        return this.style.dark ? android.R.style.Theme_DeviceDefault_Dialog_Alert
                : android.R.style.Theme_DeviceDefault_Light_Dialog_Alert;
    }

    @TargetApi(Build.VERSION_CODES.S)
    private int systemColor() {
        try {
            return this.activity.getColor(android.R.color.system_accent1_500);
        } catch (Resources.NotFoundException ex) {
            Log.w(PatchSettings.LOG_TAG, "System accent swatch could not be read", ex);
            return STOCK_COLOR;
        }
    }

    private static int foreground(int color) {
        double luminance = RED_LUMINANCE * linear(Color.red(color)) + GREEN_LUMINANCE * linear(Color.green(color))
                + BLUE_LUMINANCE * linear(Color.blue(color));
        return (luminance > DARK_TEXT_THRESHOLD) ? Color.BLACK : Color.WHITE;
    }

    private static double linear(int channel) {
        double component = channel / (double) CHANNEL_MAX;
        return (component <= SRGB_THRESHOLD) ? component / SRGB_DIVISOR
                : Math.pow((component + SRGB_OFFSET) / SRGB_SCALE, SRGB_EXPONENT);
    }

    private static final class Swatch {

        private final String preset;

        private final LinearLayout view;

        private final GradientDrawable ring;

        private final TextView check;

        private Swatch(String preset, LinearLayout view, GradientDrawable ring, TextView check) {
            this.preset = preset;
            this.view = view;
            this.ring = ring;
            this.check = check;
        }

    }

}
