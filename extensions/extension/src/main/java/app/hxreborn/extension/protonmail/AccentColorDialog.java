/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.protonmail;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

    private static final String[] HUE_NAMES =
            {"Red", "Orange", "Gold", "Green", "Teal", "Cyan", "Blue", "Pink"};
    private static final int[] HUE_VALUES = {0, 28, 48, 105, 168, 195, 220, 325};

    private AccentColorDialog() {}

    static String getPresetLabel(String preset) {
        if (AccentColor.SYSTEM.equals(preset)) return SYSTEM_LABEL;
        if (preset == null || preset.isEmpty()) return STOCK_LABEL;

        for (int index = 0; index < HUE_VALUES.length; index++) {
            if (hueToHexColor(HUE_VALUES[index]).equals(preset)) return HUE_NAMES[index];
        }
        return CUSTOM_LABEL + " " + preset.toUpperCase(Locale.US);
    }

    static void show(Activity activity, Runnable onChanged) {
        if (activity.isFinishing() || activity.isDestroyed()) return;

        new Picker(activity, onChanged).show();
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

            dialogBackgroundColor = PatchesTheme.resolveColorAttribute(activity, PatchesTheme.BACKGROUND_SECONDARY);
            previewBackgroundColor = PatchesTheme.resolveColorAttribute(activity, PatchesTheme.BACKGROUND_NORM);
            textNormColor = PatchesTheme.resolveColorAttribute(activity, PatchesTheme.TEXT_NORM);
            textWeakColor = PatchesTheme.resolveColorAttribute(activity, PatchesTheme.TEXT_WEAK);

            selectedPreset = AccentColor.getPreset();
            final int picked = AccentColor.resolvePresetColor(selectedPreset);
            Color.colorToHSV(picked == 0 ? AccentColor.STOCK_LIGHT_ACCENT : picked, selectedHsv);

            previewButtonBackground = new GradientDrawable();
            previewButtonBackground.setCornerRadius(dp(PREVIEW_RADIUS_DP));
            previewLink = createTextView(CUSTOM_LABEL, LABEL_SP, textNormColor);
            previewSwitch = new Switch(activity);
            saturationTrack = new GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT, new int[] {Color.GRAY, Color.GRAY});
            saturationTrack.setCornerRadius(dp(TRACK_RADIUS_DP));
            hueValueLabel = createTextView("", LABEL_SP, textWeakColor);
            saturationValueLabel = createTextView("", LABEL_SP, textWeakColor);
            applyButton = createTextView("Apply", LABEL_SP, textNormColor);
            hueSlider = createSlider(HUE_DEGREES - 1, createHueGradient());
            saturationSlider = createSlider(PERCENT - MIN_SATURATION, saturationTrack);
        }

        void show() {
            final LinearLayout contentLayout = PatchesDialog.createContentLayout(activity);
            contentLayout.addView(PatchesDialog.createTitle(activity, AppliedPatches.ACCENT_COLOR));

            final LinearLayout content = new LinearLayout(activity);
            content.setOrientation(LinearLayout.VERTICAL);
            content.addView(createPreviewRow(), fullWidthLayoutParamsWithTopMargin(SECTION_GAP_DP));
            content.addView(createSwatchGrid(), fullWidthLayoutParamsWithTopMargin(SECTION_GAP_DP));
            content.addView(createSliderRow("Hue", hueValueLabel, hueSlider), fullWidthLayoutParamsWithTopMargin(SECTION_GAP_DP));
            content.addView(createSliderRow("Saturation", saturationValueLabel, saturationSlider),
                    fullWidthLayoutParamsWithTopMargin(ITEM_GAP_DP));

            final ScrollView scroll = new ScrollView(activity);
            scroll.setVerticalScrollBarEnabled(false);
            scroll.addView(content, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            contentLayout.addView(scroll, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

            final Dialog dialog = PatchesDialog.createDialog(activity, contentLayout);
            contentLayout.addView(createActionButtons(dialog), fullWidthLayoutParamsWithTopMargin(ITEM_GAP_DP));

            updatePreviewAndSelection();
            dialog.show();
        }

        private View createPreviewRow() {
            final LinearLayout previewRow = new LinearLayout(activity);
            previewRow.setOrientation(LinearLayout.HORIZONTAL);
            previewRow.setGravity(Gravity.CENTER_VERTICAL);
            previewRow.setBackground(
                    PatchesTheme.createRoundedRectangle(activity, previewBackgroundColor, PREVIEW_RADIUS_DP));
            previewRow.setPadding(dp(PREVIEW_PADDING_DP), dp(PREVIEW_PADDING_DP),
                    dp(PREVIEW_PADDING_DP), dp(PREVIEW_PADDING_DP));

            final TextView button = createTextView("Send", LABEL_SP, Color.WHITE);
            button.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            button.setBackground(previewButtonBackground);
            button.setGravity(Gravity.CENTER);
            button.setPadding(dp(PREVIEW_PADDING_DP), dp(ITEM_GAP_DP / 2),
                    dp(PREVIEW_PADDING_DP), dp(ITEM_GAP_DP / 2));
            previewRow.addView(button);

            previewLink.setText("Learn more");
            previewLink.setPaintFlags(previewLink.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
            previewLink.setGravity(Gravity.CENTER);
            previewLink.setSingleLine(true);
            final LinearLayout.LayoutParams linkParams = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            linkParams.setMarginStart(dp(ITEM_GAP_DP));
            linkParams.setMarginEnd(dp(ITEM_GAP_DP));
            previewRow.addView(previewLink, linkParams);

            previewSwitch.setChecked(true);
            previewSwitch.setClickable(false);
            previewRow.addView(previewSwitch);

            previewRow.setImportantForAccessibility(
                    View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
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

            final LinearLayout grid = new LinearLayout(activity);
            grid.setOrientation(LinearLayout.VERTICAL);

            LinearLayout row = null;
            for (int index = 0; index < presets.size(); index++) {
                if (index % SWATCH_COLUMNS == 0) {
                    row = new LinearLayout(activity);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    grid.addView(row, index == 0
                            ? fullWidthLayoutParams()
                            : fullWidthLayoutParamsWithTopMargin(ITEM_GAP_DP / 2));
                }

                final String preset = presets.get(index);
                final SwatchCell cell = new SwatchCell(preset, getPresetLabel(preset));
                swatchCells.add(cell);
                row.addView(cell.view, swatchLayoutParams());
            }

            final int remainder = presets.size() % SWATCH_COLUMNS;
            for (int index = remainder; index > 0 && index < SWATCH_COLUMNS; index++) {
                row.addView(new View(activity), swatchLayoutParams());
            }
            return grid;
        }

        private LinearLayout.LayoutParams swatchLayoutParams() {
            return new LinearLayout.LayoutParams(0, dp(SWATCH_CELL_HEIGHT_DP), 1f);
        }

        private View createSliderRow(String name, TextView value, SeekBar slider) {
            final LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.VERTICAL);

            final LinearLayout header = new LinearLayout(activity);
            header.setOrientation(LinearLayout.HORIZONTAL);
            header.addView(createTextView(name, LABEL_SP, textNormColor),
                    new LinearLayout.LayoutParams(0,
                            ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            value.setGravity(Gravity.END);
            header.addView(value);
            row.addView(header, fullWidthLayoutParams());

            slider.setContentDescription(name);
            row.addView(slider);
            return row;
        }

        private View createActionButtons(Dialog dialog) {
            final LinearLayout buttons = PatchesDialog.createButtonRow(activity);
            buttons.addView(PatchesDialog.configureActionButton(activity,
                    createTextView("Cancel", LABEL_SP, textWeakColor), () -> dialog.dismiss()));
            buttons.addView(PatchesDialog.configureActionButton(activity, applyButton, () -> {
                dialog.dismiss();
                if (selectedPreset.equals(AccentColor.getPreset())) return;

                AccentColor.setPreset(selectedPreset);
                onChanged.run();
            }));
            return buttons;
        }

        private SeekBar createSlider(int max, GradientDrawable track) {
            final SeekBar slider = new SeekBar(activity);
            slider.setMax(max);
            slider.setProgressDrawable(new ColorDrawable(Color.TRANSPARENT));
            slider.setBackground(new InsetDrawable(track, 0, dp(TRACK_INSET_DP), 0,
                    dp(TRACK_INSET_DP)));
            slider.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(TOUCH_TARGET_DP)));
            slider.setOnSeekBarChangeListener(new SliderListener());
            return slider;
        }

        private GradientDrawable createHueGradient() {
            final int[] colors = new int[HUE_STOPS];
            for (int index = 0; index < colors.length; index++) {
                colors[index] = Color.HSVToColor(new float[] {
                        index * (float) HUE_DEGREES / (colors.length - 1) % HUE_DEGREES, 1f, 1f});
            }

            final GradientDrawable track =
                    new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, colors);
            track.setCornerRadius(dp(TRACK_RADIUS_DP));
            return track;
        }

        private void updatePreviewAndSelection() {
            final boolean dark = PatchesTheme.isDark(dialogBackgroundColor);
            final int accent = AccentColor.resolveAccentColor(selectedPreset, dark);

            previewButtonBackground.setColor(accent);
            previewLink.setTextColor(accent);
            PatchesTheme.tintSwitch(previewSwitch, accent);
            applyButton.setTextColor(accent);

            hueSlider.setThumbTintList(ColorStateList.valueOf(accent));
            saturationSlider.setThumbTintList(ColorStateList.valueOf(accent));
            saturationTrack.setColors(new int[] {
                    Color.HSVToColor(new float[] {selectedHsv[0], MIN_SATURATION / (float) PERCENT, 1f}),
                    Color.HSVToColor(new float[] {selectedHsv[0], 1f, 1f}),
            });

            hueValueLabel.setText(Math.round(selectedHsv[0]) + "°");
            saturationValueLabel.setText(Math.round(selectedHsv[1] * PERCENT) + "%");

            for (SwatchCell cell : swatchCells) {
                cell.setSelected(cell.preset.equals(selectedPreset));
            }
        }

        private void setSliderProgressFromHsv() {
            hueSlider.setProgress(Math.round(selectedHsv[0]));
            saturationSlider.setProgress(
                    Math.max(0, Math.round(selectedHsv[1] * PERCENT) - MIN_SATURATION));
        }

        private TextView createTextView(String value, int sizeSp, int color) {
            return PatchesDialog.createTextView(activity, value, sizeSp, color);
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
            return PatchesTheme.dpToPx(activity, value);
        }

        private final class SliderListener implements SeekBar.OnSeekBarChangeListener {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if (!fromUser) return;

                if (bar == hueSlider) {
                    selectedHsv[0] = progress;
                } else {
                    selectedHsv[1] = (progress + MIN_SATURATION) / (float) PERCENT;
                }
                selectedPreset = hsvToHexColor(selectedHsv);
                updatePreviewAndSelection();
            }

            @Override
            public void onStartTrackingTouch(SeekBar bar) {}

            @Override
            public void onStopTrackingTouch(SeekBar bar) {}
        }

        private final class SwatchCell {
            private final String preset;
            private final View view;
            private final ImageView checkmark;
            private final GradientDrawable selectionOutline;

            SwatchCell(String preset, String name) {
                this.preset = preset;
                final int color = AccentColor.resolveAccentColor(preset, PatchesTheme.isDark(dialogBackgroundColor));

                final View swatch = new View(activity);
                swatch.setBackground(PatchesTheme.createCircle(color));

                checkmark = new ImageView(activity);
                checkmark.setImageDrawable(PatchesTheme.getDrawableByName(activity, "ic_proton_checkmark"));
                checkmark.setColorFilter(PatchesTheme.isDark(color) ? Color.WHITE : Color.BLACK);
                checkmark.setScaleType(ImageView.ScaleType.FIT_CENTER);

                selectionOutline = new GradientDrawable();
                selectionOutline.setShape(GradientDrawable.OVAL);
                selectionOutline.setColor(Color.TRANSPARENT);

                final FrameLayout swatchContainer = new FrameLayout(activity);
                swatchContainer.setBackground(selectionOutline);
                swatchContainer.addView(swatch, centeredSquareLayoutParams(SWATCH_DP));
                swatchContainer.addView(checkmark, centeredSquareLayoutParams(SWATCH_CHECK_DP));

                final TextView caption = createTextView(name, CAPTION_SP, textWeakColor);
                caption.setGravity(Gravity.CENTER_HORIZONTAL);
                caption.setSingleLine(true);

                final LinearLayout cell = new LinearLayout(activity);
                cell.setOrientation(LinearLayout.VERTICAL);
                cell.setGravity(Gravity.CENTER_HORIZONTAL);
                cell.addView(swatchContainer, new LinearLayout.LayoutParams(
                        dp(SWATCH_RING_DP), dp(SWATCH_RING_DP)));
                cell.addView(caption, fullWidthLayoutParams());
                cell.setContentDescription(name);
                PatchesTheme.makeClickable(cell);
                cell.setOnClickListener(ignored -> {
                    selectedPreset = preset;
                    final int picked = AccentColor.resolvePresetColor(preset);
                    if (picked != 0) {
                        Color.colorToHSV(picked, selectedHsv);
                        setSliderProgressFromHsv();
                    }
                    updatePreviewAndSelection();
                });
                view = cell;
            }

            private FrameLayout.LayoutParams centeredSquareLayoutParams(int sizeDp) {
                final FrameLayout.LayoutParams params =
                        new FrameLayout.LayoutParams(dp(sizeDp), dp(sizeDp));
                params.gravity = Gravity.CENTER;
                return params;
            }

            void setSelected(boolean selected) {
                selectionOutline.setStroke(selected ? dp(RING_STROKE_DP) : 0, textNormColor);
                checkmark.setVisibility(selected ? View.VISIBLE : View.GONE);
                view.setSelected(selected);
            }
        }
    }

    private static String hueToHexColor(int hueDegrees) {
        return hsvToHexColor(new float[] {hueDegrees, 1f, 1f});
    }

    private static String hsvToHexColor(float[] selectedHsv) {
        return String.format(Locale.US, "#%06X", Color.HSVToColor(selectedHsv) & 0xFFFFFF);
    }

}
