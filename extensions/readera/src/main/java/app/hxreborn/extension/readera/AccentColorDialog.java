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
package app.hxreborn.extension.readera;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
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

    private static final float CHECK_SP = 18f;

    private final Activity activity;

    private final SettingsStyle style;

    private final int dialogTheme;

    private final Runnable onSaved;

    private final List<Swatch> swatches = new ArrayList<>();

    private final EditText custom;

    private String selectedPreset;

    private boolean customSelected;

    private AlertDialog dialog;

    AccentColorDialog(Activity activity, SettingsStyle style, int dialogTheme, Runnable onSaved) {
        this.activity = activity;
        this.style = style;
        this.dialogTheme = dialogTheme;
        this.onSaved = onSaved;
        this.selectedPreset = AccentPalette.getPreset(activity);
        this.customSelected = AccentPalette.isCustom(this.selectedPreset);
        this.custom = new EditText(activity);
    }

    void show() {
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
        this.dialog = new AlertDialog.Builder(this.activity, this.dialogTheme).setTitle(AccentColor.TITLE)
            .setView(scroll)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Apply", null)
            .create();
        this.dialog.setOnShowListener(
                (shown) -> this.dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener((view) -> apply()));
        updateSelection();
        this.dialog.show();
    }

    private View grid() {
        LinearLayout grid = new LinearLayout(this.activity);
        grid.setOrientation(LinearLayout.VERTICAL);
        if (AccentPalette.isSystemAvailable()) {
            this.swatches.add(swatch(AccentPalette.label(AccentPalette.SYSTEM), AccentPalette.SYSTEM,
                    AccentPalette.color(this.activity, AccentPalette.SYSTEM)));
        }
        for (AccentPalette.Fixed fixed : AccentPalette.Fixed.values()) {
            this.swatches.add(swatch(fixed.label, fixed.preset, fixed.color));
        }
        this.swatches
            .add(swatch(AccentPalette.label(AccentPalette.STOCK), AccentPalette.STOCK, AccentColor.stockAccent()));
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
        for (int index = this.swatches.size() % COLUMNS; index > 0 && index < COLUMNS; index++) {
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
        TextView check = this.style.text("", CHECK_SP, Color.BLACK, this.style.medium);
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
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable text) {
                AccentColorDialog.this.custom.setError(null);
                AccentColorDialog.this.customSelected = true;
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
            if (!AccentPalette.isHex(preset)) {
                this.custom.setError("Enter a color as #RRGGBB");
                this.custom.requestFocus();
                return;
            }
        }
        if (preset.equalsIgnoreCase(AccentPalette.getPreset(this.activity))) {
            this.dialog.dismiss();
            return;
        }
        if (!AccentPalette.setPreset(this.activity, preset)) {
            Toast.makeText(this.activity, "Could not save the accent color", Toast.LENGTH_SHORT).show();
            return;
        }
        this.dialog.dismiss();
        this.onSaved.run();
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
