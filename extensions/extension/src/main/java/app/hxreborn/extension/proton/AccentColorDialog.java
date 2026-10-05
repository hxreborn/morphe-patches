/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.proton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

@SuppressLint("SetTextI18n")
final class AccentColorDialog {

    private static final int PREVIEW_RADIUS_DP = 16;

    private static final int SECTION_GAP_DP = 20;

    private static final int ITEM_GAP_DP = 12;

    private static final int PREVIEW_PADDING_DP = 16;

    private static final int LABEL_SP = 14;

    private static final int CAPTION_SP = 12;

    private static final int SWATCH_DP = 34;

    private static final int SWATCH_RING_DP = 46;

    private static final int SWATCH_CHECK_DP = 18;

    private static final int SWATCH_COLUMNS = 4;

    private static final int SWATCH_CELL_HEIGHT_DP = 78;

    private static final int RING_STROKE_DP = 2;

    private static final int TOUCH_TARGET_DP = 48;

    private static final int TRACK_INSET_DP = 18;

    private static final int TRACK_RADIUS_DP = 6;

    private static final int HUE_STOPS = 13;

    private static final int HUE_DEGREES = 360;

    private static final int PERCENT = 100;

    private static final int MIN_SATURATION = 20;

    private static final String STOCK_LABEL = "Proton purple";

    private static final String SYSTEM_LABEL = "Material You";

    private static final String CUSTOM_LABEL = "Custom";

    private static final String[] HUE_NAMES = { "Red", "Orange", "Gold", "Green", "Teal", "Cyan", "Blue", "Pink" };

    private static final int[] HUE_VALUES = { 0, 28, 48, 105, 168, 195, 220, 325 };

    private AccentColorDialog() {

    }

    static String getPresetLabel(String preset) {
        if (AccentColor.SYSTEM.equals(preset)) {
            return SYSTEM_LABEL;
        }
        if (preset == null || preset.isEmpty()) {
            return STOCK_LABEL;
        }

        for (int index = 0; index < HUE_VALUES.length; index++) {
            if (hueToHexColor(HUE_VALUES[index]).equals(preset)) {
                return HUE_NAMES[index];
            }
        }
        return CUSTOM_LABEL + " " + preset.toUpperCase(Locale.US);
    }

    static void show(Activity activity, Runnable onChanged) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        new Picker(activity, onChanged).show();
    }

    private static String hueToHexColor(int hueDegrees) {
        return hsvToHexColor(new float[] { hueDegrees, 1f, 1f });
    }

    private static String hsvToHexColor(float[] selectedHsv) {
        return String.format(Locale.US, "#%06X", Color.HSVToColor(selectedHsv) & 0xFFFFFF);
    }

    private static final class Picker {

        private final Activity activity;

        private final Runnable onChanged;

        private final int dialogBackgroundColor;

        private final int previewBackgroundColor;

        private final int textNormColor;

        private final int textWeakColor;

        private String selectedPreset;

        private final float[] selectedHsv = new float[3];

        private final List<SwatchCell> swatchCells = new ArrayList<>();

        private final GradientDrawable previewButtonBackground;

        private final GradientDrawable saturationTrack;

        private final TextView previewLink;

        private final Switch previewSwitch;

        private final TextView hueValueLabel;

        private final TextView saturationValueLabel;

        private final TextView applyButton;

        private final SeekBar hueSlider;

        private final SeekBar saturationSlider;

        Picker(Activity activity, Runnable onChanged) {
            this.activity = activity;
            this.onChanged = onChanged;

            this.dialogBackgroundColor = PatchesTheme.resolveColorAttribute(activity,
                    PatchesTheme.BACKGROUND_SECONDARY);
            this.previewBackgroundColor = PatchesTheme.resolveColorAttribute(activity, PatchesTheme.BACKGROUND_NORM);
            this.textNormColor = PatchesTheme.resolveColorAttribute(activity, PatchesTheme.TEXT_NORM);
            this.textWeakColor = PatchesTheme.resolveColorAttribute(activity, PatchesTheme.TEXT_WEAK);

            this.selectedPreset = AccentColor.getPreset();
            final int picked = AccentColor.resolvePresetColor(this.selectedPreset);
            Color.colorToHSV((picked != 0) ? picked : AccentColor.STOCK_LIGHT_ACCENT, this.selectedHsv);

            this.previewButtonBackground = new GradientDrawable();
            this.previewButtonBackground.setCornerRadius(dp(PREVIEW_RADIUS_DP));
            this.previewLink = createTextView(CUSTOM_LABEL, LABEL_SP, this.textNormColor);
            this.previewSwitch = new Switch(activity);
            this.saturationTrack = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                    new int[] { Color.GRAY, Color.GRAY });
            this.saturationTrack.setCornerRadius(dp(TRACK_RADIUS_DP));
            this.hueValueLabel = createTextView("", LABEL_SP, this.textWeakColor);
            this.saturationValueLabel = createTextView("", LABEL_SP, this.textWeakColor);
            this.applyButton = createTextView("Apply", LABEL_SP, this.textNormColor);
            this.hueSlider = createSlider(HUE_DEGREES - 1, createHueGradient());
            this.saturationSlider = createSlider(PERCENT - MIN_SATURATION, this.saturationTrack);
        }

        void show() {
            final LinearLayout contentLayout = PatchesDialog.createContentLayout(this.activity);
            contentLayout.addView(PatchesDialog.createTitle(this.activity, AppliedPatches.ACCENT_COLOR));

            final LinearLayout content = new LinearLayout(this.activity);
            content.setOrientation(LinearLayout.VERTICAL);
            content.addView(createPreviewRow(), fullWidthLayoutParamsWithTopMargin(SECTION_GAP_DP));
            content.addView(createSwatchGrid(), fullWidthLayoutParamsWithTopMargin(SECTION_GAP_DP));
            content.addView(createSliderRow("Hue", this.hueValueLabel, this.hueSlider),
                    fullWidthLayoutParamsWithTopMargin(SECTION_GAP_DP));
            content.addView(createSliderRow("Saturation", this.saturationValueLabel, this.saturationSlider),
                    fullWidthLayoutParamsWithTopMargin(ITEM_GAP_DP));

            final ScrollView scroll = new ScrollView(this.activity);
            scroll.setVerticalScrollBarEnabled(false);
            scroll.addView(content, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            contentLayout.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

            final Dialog dialog = PatchesDialog.createDialog(this.activity, contentLayout);
            contentLayout.addView(createActionButtons(dialog), fullWidthLayoutParamsWithTopMargin(ITEM_GAP_DP));

            updatePreviewAndSelection();
            dialog.show();
        }

        private View createPreviewRow() {
            final LinearLayout previewRow = new LinearLayout(this.activity);
            previewRow.setOrientation(LinearLayout.HORIZONTAL);
            previewRow.setGravity(Gravity.CENTER_VERTICAL);
            previewRow.setBackground(
                    PatchesTheme.createRoundedRectangle(this.activity, this.previewBackgroundColor, PREVIEW_RADIUS_DP));
            previewRow.setPadding(dp(PREVIEW_PADDING_DP), dp(PREVIEW_PADDING_DP), dp(PREVIEW_PADDING_DP),
                    dp(PREVIEW_PADDING_DP));

            final TextView button = createTextView("Send", LABEL_SP, Color.WHITE);
            button.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            button.setBackground(this.previewButtonBackground);
            button.setGravity(Gravity.CENTER);
            button.setPadding(dp(PREVIEW_PADDING_DP), dp(ITEM_GAP_DP / 2), dp(PREVIEW_PADDING_DP), dp(ITEM_GAP_DP / 2));
            previewRow.addView(button);

            this.previewLink.setText("Learn more");
            this.previewLink.setPaintFlags(this.previewLink.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
            this.previewLink.setGravity(Gravity.CENTER);
            this.previewLink.setSingleLine(true);
            final LinearLayout.LayoutParams linkParams = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            linkParams.setMarginStart(dp(ITEM_GAP_DP));
            linkParams.setMarginEnd(dp(ITEM_GAP_DP));
            previewRow.addView(this.previewLink, linkParams);

            this.previewSwitch.setChecked(true);
            this.previewSwitch.setClickable(false);
            previewRow.addView(this.previewSwitch);

            previewRow.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
            return previewRow;
        }

        private View createSwatchGrid() {
            final List<String> presets = new ArrayList<>();
            presets.add(AccentColor.STOCK);
            if (AccentColor.isSystemAccentAvailable()) {
                presets.add(AccentColor.SYSTEM);
            }
            for (int index = 0; index < HUE_VALUES.length; index++) {
                presets.add(hueToHexColor(HUE_VALUES[index]));
            }

            final LinearLayout grid = new LinearLayout(this.activity);
            grid.setOrientation(LinearLayout.VERTICAL);

            LinearLayout row = null;
            for (int index = 0; index < presets.size(); index++) {
                if (index % SWATCH_COLUMNS == 0) {
                    row = new LinearLayout(this.activity);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    grid.addView(row, (index != 0) ? fullWidthLayoutParamsWithTopMargin(ITEM_GAP_DP / 2)
                            : fullWidthLayoutParams());
                }

                final String preset = presets.get(index);
                final SwatchCell cell = new SwatchCell(preset, getPresetLabel(preset));
                this.swatchCells.add(cell);
                row.addView(cell.view, swatchLayoutParams());
            }

            final int remainder = presets.size() % SWATCH_COLUMNS;
            for (int index = remainder; index > 0 && index < SWATCH_COLUMNS; index++) {
                row.addView(new View(this.activity), swatchLayoutParams());
            }
            return grid;
        }

        private LinearLayout.LayoutParams swatchLayoutParams() {
            return new LinearLayout.LayoutParams(0, dp(SWATCH_CELL_HEIGHT_DP), 1f);
        }

        private View createSliderRow(String name, TextView value, SeekBar slider) {
            final LinearLayout row = new LinearLayout(this.activity);
            row.setOrientation(LinearLayout.VERTICAL);

            final LinearLayout header = new LinearLayout(this.activity);
            header.setOrientation(LinearLayout.HORIZONTAL);
            header.addView(createTextView(name, LABEL_SP, this.textNormColor),
                    new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            value.setGravity(Gravity.END);
            header.addView(value);
            row.addView(header, fullWidthLayoutParams());

            slider.setContentDescription(name);
            row.addView(slider);
            return row;
        }

        private View createActionButtons(Dialog dialog) {
            final LinearLayout buttons = PatchesDialog.createButtonRow(this.activity);
            buttons.addView(PatchesDialog.configureActionButton(this.activity,
                    createTextView("Cancel", LABEL_SP, this.textWeakColor), () -> dialog.dismiss()));
            buttons.addView(PatchesDialog.configureActionButton(this.activity, this.applyButton, () -> {
                dialog.dismiss();
                if (this.selectedPreset.equals(AccentColor.getPreset())) {
                    return;
                }

                AccentColor.setPreset(this.selectedPreset);
                this.onChanged.run();
            }));
            return buttons;
        }

        private SeekBar createSlider(int max, GradientDrawable track) {
            final SeekBar slider = new SeekBar(this.activity);
            slider.setMax(max);
            slider.setProgressDrawable(new ColorDrawable(Color.TRANSPARENT));
            slider.setBackground(new InsetDrawable(track, 0, dp(TRACK_INSET_DP), 0, dp(TRACK_INSET_DP)));
            slider.setLayoutParams(
                    new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(TOUCH_TARGET_DP)));
            slider.setOnSeekBarChangeListener(new SliderListener());
            return slider;
        }

        private GradientDrawable createHueGradient() {
            final int[] colors = new int[HUE_STOPS];
            for (int index = 0; index < colors.length; index++) {
                colors[index] = Color.HSVToColor(
                        new float[] { index * (float) HUE_DEGREES / (colors.length - 1) % HUE_DEGREES, 1f, 1f });
            }

            final GradientDrawable track = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, colors);
            track.setCornerRadius(dp(TRACK_RADIUS_DP));
            return track;
        }

        private void updatePreviewAndSelection() {
            final boolean dark = PatchesTheme.isDark(this.dialogBackgroundColor);
            final int accent = AccentColor.resolveAccentColor(this.selectedPreset, dark);

            this.previewButtonBackground.setColor(accent);
            this.previewLink.setTextColor(accent);
            SwitchStyle.apply(this.previewSwitch, accent);
            this.applyButton.setTextColor(accent);

            this.hueSlider.setThumbTintList(ColorStateList.valueOf(accent));
            this.saturationSlider.setThumbTintList(ColorStateList.valueOf(accent));
            this.saturationTrack.setColors(new int[] {
                    Color.HSVToColor(new float[] { this.selectedHsv[0], MIN_SATURATION / (float) PERCENT, 1f }),
                    Color.HSVToColor(new float[] { this.selectedHsv[0], 1f, 1f }), });

            this.hueValueLabel.setText(Math.round(this.selectedHsv[0]) + "°");
            this.saturationValueLabel.setText(Math.round(this.selectedHsv[1] * PERCENT) + "%");

            for (SwatchCell cell : this.swatchCells) {
                cell.setSelected(cell.preset.equals(this.selectedPreset));
            }
        }

        private void setSliderProgressFromHsv() {
            this.hueSlider.setProgress(Math.round(this.selectedHsv[0]));
            this.saturationSlider.setProgress(Math.max(0, Math.round(this.selectedHsv[1] * PERCENT) - MIN_SATURATION));
        }

        private TextView createTextView(String value, int sizeSp, int color) {
            return PatchesDialog.createTextView(this.activity, value, sizeSp, color);
        }

        private LinearLayout.LayoutParams fullWidthLayoutParams() {
            return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        private LinearLayout.LayoutParams fullWidthLayoutParamsWithTopMargin(int marginDp) {
            final LinearLayout.LayoutParams params = fullWidthLayoutParams();
            params.topMargin = dp(marginDp);
            return params;
        }

        private int dp(int value) {
            return PatchesTheme.dpToPx(this.activity, value);
        }

        private final class SliderListener implements SeekBar.OnSeekBarChangeListener {

            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if (!fromUser) {
                    return;
                }

                if (bar == Picker.this.hueSlider) {
                    Picker.this.selectedHsv[0] = progress;
                }
                else {
                    Picker.this.selectedHsv[1] = (progress + MIN_SATURATION) / (float) PERCENT;
                }
                Picker.this.selectedPreset = hsvToHexColor(Picker.this.selectedHsv);
                updatePreviewAndSelection();
            }

            @Override
            public void onStartTrackingTouch(SeekBar bar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar bar) {
            }

        }

        private final class SwatchCell {

            private final String preset;

            private final View view;

            private final ImageView checkmark;

            private final GradientDrawable selectionOutline;

            SwatchCell(String preset, String name) {
                this.preset = preset;
                final int color = AccentColor.resolveAccentColor(preset,
                        PatchesTheme.isDark(Picker.this.dialogBackgroundColor));

                final View swatch = new View(Picker.this.activity);
                swatch.setBackground(PatchesTheme.createCircle(color));

                this.checkmark = new ImageView(Picker.this.activity);
                this.checkmark
                    .setImageDrawable(PatchesTheme.getDrawableByName(Picker.this.activity, "ic_proton_checkmark"));
                this.checkmark.setColorFilter((PatchesTheme.isDark(color)) ? Color.WHITE : Color.BLACK);
                this.checkmark.setScaleType(ImageView.ScaleType.FIT_CENTER);

                this.selectionOutline = new GradientDrawable();
                this.selectionOutline.setShape(GradientDrawable.OVAL);
                this.selectionOutline.setColor(Color.TRANSPARENT);

                final FrameLayout swatchContainer = new FrameLayout(Picker.this.activity);
                swatchContainer.setBackground(this.selectionOutline);
                swatchContainer.addView(swatch, centeredSquareLayoutParams(SWATCH_DP));
                swatchContainer.addView(this.checkmark, centeredSquareLayoutParams(SWATCH_CHECK_DP));

                final TextView caption = createTextView(name, CAPTION_SP, Picker.this.textWeakColor);
                caption.setGravity(Gravity.CENTER_HORIZONTAL);
                caption.setSingleLine(true);

                final LinearLayout cell = new LinearLayout(Picker.this.activity);
                cell.setOrientation(LinearLayout.VERTICAL);
                cell.setGravity(Gravity.CENTER_HORIZONTAL);
                cell.addView(swatchContainer, new LinearLayout.LayoutParams(dp(SWATCH_RING_DP), dp(SWATCH_RING_DP)));
                cell.addView(caption, fullWidthLayoutParams());
                cell.setContentDescription(name);
                PatchesTheme.makeClickable(cell);
                cell.setOnClickListener((ignored) -> {
                    Picker.this.selectedPreset = preset;
                    final int picked = AccentColor.resolvePresetColor(preset);
                    if (picked != 0) {
                        Color.colorToHSV(picked, Picker.this.selectedHsv);
                        setSliderProgressFromHsv();
                    }
                    updatePreviewAndSelection();
                });
                this.view = cell;
            }

            private FrameLayout.LayoutParams centeredSquareLayoutParams(int sizeDp) {
                final FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(dp(sizeDp), dp(sizeDp));
                params.gravity = Gravity.CENTER;
                return params;
            }

            void setSelected(boolean selected) {
                this.selectionOutline.setStroke((selected) ? dp(RING_STROKE_DP) : 0, Picker.this.textNormColor);
                this.checkmark.setVisibility((selected) ? View.VISIBLE : View.GONE);
                this.view.setSelected(selected);
            }

        }

    }

}
