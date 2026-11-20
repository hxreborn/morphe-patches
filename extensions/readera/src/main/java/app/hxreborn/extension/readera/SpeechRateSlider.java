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
