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

import org.junit.Test;
import org.junit.Assert;

public class EqualizerCurveTest {

    @Test
    public void roundTripSignedTenthsAndMode() {
        int[] gains = { -60, -1, 0, 1, 60, -1, 12, -12, 3, -3 };
        EqualizerCurve curve = new EqualizerCurve(8, gains);
        Assert.assertEquals(curve, EqualizerCurve.fromPayload(curve.payload()));
    }

    @Test
    public void changeBandSelectsCustomWithoutChangingOriginal() {
        EqualizerCurve original = new EqualizerCurve(8, new int[EqualizerCurve.BAND_COUNT]);
        EqualizerCurve changed = original.withGain(5, -1);
        Assert.assertEquals(7, changed.mode());
        Assert.assertEquals(-1, changed.gain(5));
        Assert.assertEquals(0, original.gain(5));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectGainOutsideNativeSliderRange() {
        new EqualizerCurve(7, new int[EqualizerCurve.BAND_COUNT]).withGain(0, 61);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectModeOnlyReply() {
        EqualizerCurve.fromPayload(new byte[] { 8 });
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectChangedFrequency() {
        byte[] payload = new EqualizerCurve(7, new int[EqualizerCurve.BAND_COUNT]).payload();
        payload[1] = 0;
        EqualizerCurve.fromPayload(payload);
    }

    @Test
    public void copyCallerGains() {
        int[] gains = new int[EqualizerCurve.BAND_COUNT];
        EqualizerCurve curve = new EqualizerCurve(7, gains);
        gains[0] = 20;
        Assert.assertEquals(0, curve.gain(0));
    }

}
