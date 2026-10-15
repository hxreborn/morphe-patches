/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.readera;

import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

@SuppressWarnings("unused")
public final class SpeechRateSlider implements SeekBar.OnSeekBarChangeListener {

    private static final int MIN_PROGRESS = 10;

    private static final int PROGRESS_STEP = 10;

    private final TextView label;

    private final SpeechRateTarget target;

    private int appliedProgress = -1;

    private SpeechRateSlider(TextView label, SpeechRateTarget target) {
        this.label = label;
        this.target = target;
    }

    public static void attach(View root, int seekBarId, int labelId, SpeechRateTarget target) {
        SeekBar seekBar = (SeekBar) root.findViewById(seekBarId);
        TextView label = (TextView) root.findViewById(labelId);
        seekBar.setOnSeekBarChangeListener(new SpeechRateSlider(label, target));
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (!fromUser) {
            return;
        }
        int stepped = Math.max(MIN_PROGRESS, (progress / PROGRESS_STEP) * PROGRESS_STEP);
        if (stepped == this.appliedProgress) {
            return;
        }
        this.appliedProgress = stepped;
        float rate = stepped / 100.0f;
        this.label.setText(rate + "x");
        this.target.applySpeechRate(rate);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        this.appliedProgress = seekBar.getProgress();
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

}
