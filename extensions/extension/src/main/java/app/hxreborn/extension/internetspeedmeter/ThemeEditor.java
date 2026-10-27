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
package app.hxreborn.extension.internetspeedmeter;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.Spinner;

final class ThemeEditor {

    private static final float PERCENT = 100f;

    private static final int[] PRESET_HUES = { 215, 195, 90, 30, 140, 270 };

    private static final int HEADER_SATURATION = 70;

    private static final int HEADER_VALUE = 93;

    private static final int VALUES_SATURATION = 40;

    private static final int VALUES_VALUE = 95;

    private final Activity activity;

    private final View content;

    private final SharedPreferences preferences;

    private final Spinner presets;

    private final SeekBar hue;

    private final SeekBar headerSaturation;

    private final SeekBar headerValue;

    private final SeekBar valuesSaturation;

    private final SeekBar valuesValue;

    private final CheckBox whiteText;

    private final View headerPreview;

    private final View valuesPreview;

    private boolean applyingPreset;

    private int selectedPreset;

    private ThemeEditor(Activity activity) {
        this.activity = activity;
        this.content = View.inflate(activity, AppResources.id(activity, "layout", "custom_theme"), null);
        this.preferences = AppResources.preferences(activity);
        this.presets = find("theme_spinner");
        this.hue = find("Global_hue_slider");
        this.headerSaturation = find("Header_saturation_slider");
        this.headerValue = find("Header_value_slider");
        this.valuesSaturation = find("Values_saturation_slider");
        this.valuesValue = find("Values_value_slider");
        this.whiteText = find("WhiteText");
        this.headerPreview = find("rel_layout_1");
        this.valuesPreview = find("rel_layout_2");
    }

    static void show(Activity activity) {
        new ThemeEditor(activity).open();
    }

    private void open() {
        Dialog dialog = new Dialog(this.activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(this.content);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        this.hue
            .setProgressDrawable(this.activity.getDrawable(AppResources.id(this.activity, "drawable", "full_rainbow")));
        this.hue.setProgress((int) this.preferences.getFloat("global_hue", PRESET_HUES[0]));
        setPercent(this.headerSaturation, this.preferences.getFloat("header_saturation", HEADER_SATURATION / PERCENT));
        setPercent(this.headerValue, this.preferences.getFloat("header_value", HEADER_VALUE / PERCENT));
        setPercent(this.valuesSaturation, this.preferences.getFloat("values_saturation", VALUES_SATURATION / PERCENT));
        setPercent(this.valuesValue, this.preferences.getFloat("values_value", VALUES_VALUE / PERCENT));
        this.whiteText.setChecked(this.preferences.getBoolean("whitetext", false));

        bindGlobalSliders();
        bindPresets();
        bindPreview();

        find("Peek").setVisibility(View.GONE);
        find("OK").setOnClickListener((view) -> {
            save();
            dialog.dismiss();
            restartMainScreen();
        });
        find("Cancel").setOnClickListener((view) -> dialog.dismiss());
        preview();
        dialog.show();
    }

    private void bindGlobalSliders() {
        SeekBar saturation = find("Global_saturation_slider");
        SeekBar value = find("Global_value_slider");
        saturation.setProgress(this.headerSaturation.getProgress());
        value.setProgress(this.headerValue.getProgress());
        saturation.setOnSeekBarChangeListener(new SliderListener(() -> {
            this.headerSaturation.setProgress(saturation.getProgress());
            this.valuesSaturation
                .setProgress(Math.round(saturation.getProgress() * VALUES_SATURATION / (float) HEADER_SATURATION));
        }));
        value.setOnSeekBarChangeListener(new SliderListener(() -> {
            this.headerValue.setProgress(value.getProgress());
            this.valuesValue.setProgress(Math.min(100, value.getProgress() + VALUES_VALUE - HEADER_VALUE));
        }));
    }

    private void bindPresets() {
        String[] names = AppResources.array(this.activity, "theme_array");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this.activity, android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.presets.setAdapter(adapter);
        this.selectedPreset = this.preferences.getInt("theme_pos", 0);
        this.presets.setSelection(this.selectedPreset, false);
        this.presets.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position != ThemeEditor.this.selectedPreset && position < PRESET_HUES.length) {
                    applyPreset(PRESET_HUES[position]);
                }
                ThemeEditor.this.selectedPreset = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }

        });
    }

    private void bindPreview() {
        SliderListener listener = new SliderListener(this::onSliderMoved);
        this.hue.setOnSeekBarChangeListener(listener);
        this.headerSaturation.setOnSeekBarChangeListener(listener);
        this.headerValue.setOnSeekBarChangeListener(listener);
        this.valuesSaturation.setOnSeekBarChangeListener(listener);
        this.valuesValue.setOnSeekBarChangeListener(listener);
    }

    private void onSliderMoved() {
        if (!this.applyingPreset) {
            this.selectedPreset = this.presets.getCount() - 1;
            this.presets.setSelection(this.selectedPreset, false);
        }
        preview();
    }

    private void applyPreset(int presetHue) {
        this.applyingPreset = true;
        this.hue.setProgress(presetHue);
        this.headerSaturation.setProgress(HEADER_SATURATION);
        this.headerValue.setProgress(HEADER_VALUE);
        this.valuesSaturation.setProgress(VALUES_SATURATION);
        this.valuesValue.setProgress(VALUES_VALUE);
        this.applyingPreset = false;
        preview();
    }

    private void preview() {
        this.headerPreview.setBackgroundColor(color(this.headerSaturation, this.headerValue));
        this.valuesPreview.setBackgroundColor(color(this.valuesSaturation, this.valuesValue));
    }

    private int color(SeekBar saturation, SeekBar value) {
        return Color.HSVToColor(new float[] { this.hue.getProgress(), saturation.getProgress() / PERCENT,
                value.getProgress() / PERCENT });
    }

    private void save() {
        this.preferences.edit()
            .putFloat("global_hue", this.hue.getProgress())
            .putFloat("header_saturation", this.headerSaturation.getProgress() / PERCENT)
            .putFloat("header_value", this.headerValue.getProgress() / PERCENT)
            .putFloat("values_saturation", this.valuesSaturation.getProgress() / PERCENT)
            .putFloat("values_value", this.valuesValue.getProgress() / PERCENT)
            .putBoolean("whitetext", this.whiteText.isChecked())
            .putInt("theme_pos", this.presets.getSelectedItemPosition())
            .apply();
    }

    private void restartMainScreen() {
        Intent main = new Intent().setClassName(this.activity, AppResources.MAIN_ACTIVITY)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        this.activity.startActivity(main);
    }

    private static void setPercent(SeekBar bar, float fraction) {
        bar.setProgress(Math.round(fraction * PERCENT));
    }

    @SuppressWarnings("unchecked")
    private <T extends View> T find(String id) {
        return (T) this.content.findViewById(AppResources.id(this.activity, "id", id));
    }

    private static final class SliderListener implements SeekBar.OnSeekBarChangeListener {

        private final Runnable onChange;

        SliderListener(Runnable onChange) {
            this.onChange = onChange;
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            this.onChange.run();
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }

    }

}
