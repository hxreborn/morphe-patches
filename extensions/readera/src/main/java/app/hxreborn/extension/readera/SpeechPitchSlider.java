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

import android.content.Context;
import android.content.SharedPreferences;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

@SuppressWarnings("unused")
public final class SpeechPitchSlider implements SeekBar.OnSeekBarChangeListener {

    private static final String PITCH = "pitch";

    private static final String ROW_TAG = "hx_readera_pitch";

    private static final int MIN_PITCH_PERCENT = 50;

    private static final int MAX_PITCH_PERCENT = 200;

    private static final int PROGRESS_STEP = 10;

    private static final float PERCENT_SCALE = 100.0f;

    private static final float DEFAULT_PITCH = 1.0f;

    private final SharedPreferences preferences;

    private final SpeechPitchTarget target;

    private String appliedPreferenceKey;

    private final TextView label;

    private int appliedProgress;

    private SpeechPitchSlider(Context context, SpeechPitchTarget target, TextView label) {
        this.preferences = PatchSettings.speechPreferences(context);
        this.target = target;
        this.label = label;
    }

    public static void attach(View root, SpeechPitchTarget target) {
        if (root == null) {
            return;
        }
        View row = root.findViewWithTag(ROW_TAG);
        if (row == null) {
            return;
        }
        View rateRow = root.findViewById(speechRateRowId());
        if (rateRow != null && rateRow.getParent() instanceof ViewGroup) {
            ViewGroup parent = (ViewGroup) rateRow.getParent();
            if (row.getParent() == parent && parent.indexOfChild(row) != parent.indexOfChild(rateRow) + 1) {
                ViewGroup.LayoutParams layoutParameters = row.getLayoutParams();
                parent.removeView(row);
                parent.addView(row, parent.indexOfChild(rateRow) + 1, layoutParameters);
            }
        }
        SeekBar seekBar = (SeekBar) row.findViewById(android.R.id.progress);
        TextView label = (TextView) row.findViewById(android.R.id.text2);
        Voice voice = target.speechPitchVoice();
        SpeechPitchSlider slider = new SpeechPitchSlider(root.getContext(), target, label);
        seekBar.setOnSeekBarChangeListener(null);
        float pitch = readPitch(slider.preferences, voice);
        seekBar.setProgress(Math.round(pitch * PERCENT_SCALE) - MIN_PITCH_PERCENT);
        slider.appliedProgress = seekBar.getProgress();
        slider.appliedPreferenceKey = preferenceKey(voice);
        label.setText(pitch + "x");
        seekBar.setOnSeekBarChangeListener(slider);
    }

    public static void applyPitch(Context context, TextToSpeech speech, SpeechPitchTarget target) {
        speech.setPitch(readPitch(PatchSettings.speechPreferences(context), target.speechPitchVoice()));
    }

    private static int speechRateRowId() {
        return 0;
    }

    private static String preferenceKey(Voice voice) {
        return (voice != null) ? PITCH + ":" + voice.getName() : PITCH;
    }

    private static float readPitch(SharedPreferences preferences, Voice voice) {
        float pitch = preferences.getFloat(preferenceKey(voice), preferences.getFloat(PITCH, DEFAULT_PITCH));
        if (Float.isNaN(pitch) || Float.isInfinite(pitch)) {
            return DEFAULT_PITCH;
        }
        return Math.max(MIN_PITCH_PERCENT / PERCENT_SCALE, Math.min(MAX_PITCH_PERCENT / PERCENT_SCALE, pitch));
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (!fromUser) {
            return;
        }
        String preferenceKey = preferenceKey(this.target.speechPitchVoice());
        int stepped = (progress / PROGRESS_STEP) * PROGRESS_STEP;
        if (stepped == this.appliedProgress && preferenceKey.equals(this.appliedPreferenceKey)) {
            return;
        }
        this.appliedProgress = stepped;
        this.appliedPreferenceKey = preferenceKey;
        float pitch = (MIN_PITCH_PERCENT + stepped) / PERCENT_SCALE;
        this.label.setText(pitch + "x");
        this.preferences.edit().putFloat(preferenceKey, pitch).apply();
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

}
