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
package app.hxreborn.extension.ultimea.equalizer;

import android.os.Handler;
import android.os.Looper;

final class EqualizerWrites {

    static final int COMBINE_MS = 300;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private final EqualizerSession session;

    private final Runnable changed;

    private final Runnable write = this::flush;

    private EqualizerCurve preview;

    private boolean dragging;

    EqualizerWrites(EqualizerSession session, Runnable changed) {
        this.session = session;
        this.changed = changed;
    }

    EqualizerCurve curve() {
        if (this.preview != null) {
            return this.preview;
        }
        SoundState desired = this.session.state().desired;
        return (desired != null) ? desired.curve : null;
    }

    boolean pending() {
        return this.preview != null;
    }

    void begin() {
        this.dragging = true;
        this.handler.removeCallbacks(this.write);
    }

    void setGain(int band, int gain) {
        EqualizerCurve current = curve();
        if (current != null && this.session.state().canEdit) {
            change(current.withGain(band, gain));
        }
    }

    void step(int band, int direction) {
        EqualizerCurve current = curve();
        if (current != null) {
            int gain = Math.max(EqualizerCurve.MIN_GAIN_TENTHS,
                    Math.min(EqualizerCurve.MAX_GAIN_TENTHS, current.gain(band) + direction));
            setGain(band, gain);
        }
    }

    void group(EqualizerGroup group, int direction) {
        EqualizerCurve current = curve();
        if (current != null && this.session.state().canEdit && group.canAdjust(current, direction)) {
            change(group.adjust(current, direction));
        }
    }

    private void change(EqualizerCurve curve) {
        this.preview = curve;
        this.handler.removeCallbacks(this.write);
        if (!this.dragging) {
            this.handler.postDelayed(this.write, COMBINE_MS);
        }
        this.changed.run();
    }

    void release() {
        this.dragging = false;
        flush();
    }

    void flush() {
        this.handler.removeCallbacks(this.write);
        EqualizerCurve curve = this.preview;
        if (curve != null && !this.session.state().canEdit) {
            return;
        }
        this.preview = null;
        if (curve != null) {
            this.session.setCurve(curve);
        }
        this.changed.run();
    }

    void pause() {
        this.handler.removeCallbacks(this.write);
    }

    void close() {
        this.handler.removeCallbacks(this.write);
        this.preview = null;
    }

}
