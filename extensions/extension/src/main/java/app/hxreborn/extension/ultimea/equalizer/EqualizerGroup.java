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

import java.util.Locale;

enum EqualizerGroup {

    BASS("Bass EQ", 0, 3), MIDRANGE("Midrange EQ", 4, 6), TREBLE("Treble EQ", 7, 9);

    final String title;

    private final int first;

    private final int last;

    EqualizerGroup(String title, int first, int last) {
        this.title = title;
        this.first = first;
        this.last = last;
    }

    boolean canAdjust(EqualizerCurve curve, int direction) {
        if (direction != -1 && direction != 1) {
            return false;
        }
        for (int band = this.first; band <= this.last; band++) {
            int gain = curve.gain(band) + direction;
            if (gain < EqualizerCurve.MIN_GAIN_TENTHS || gain > EqualizerCurve.MAX_GAIN_TENTHS) {
                return false;
            }
        }
        return true;
    }

    EqualizerCurve adjust(EqualizerCurve curve, int direction) {
        if (!canAdjust(curve, direction)) {
            throw new IllegalArgumentException("Equalizer group exceeds slider range");
        }
        EqualizerCurve changed = curve;
        for (int band = this.first; band <= this.last; band++) {
            changed = changed.withGain(band, curve.gain(band) + direction);
        }
        return changed;
    }

    String values(EqualizerCurve curve) {
        StringBuilder text = new StringBuilder(this.title);
        for (int band = this.first; band <= this.last; band++) {
            text.append('\n')
                .append(EqualizerCurve.frequency(band))
                .append(" Hz  ")
                .append(String.format(Locale.ROOT, "%+.1f dB", curve.gain(band) / 10.0));
        }
        return text.toString();
    }

}
