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

import org.junit.Assert;
import org.junit.Test;

public class EqualizerGroupTest {

    @Test
    public void adjustOnlyNamedMidrangeBands() {
        EqualizerCurve original = new EqualizerCurve(8, new int[10]);
        EqualizerCurve changed = EqualizerGroup.MIDRANGE.adjust(original, 1);
        Assert.assertEquals(EqualizerCurve.CUSTOM_MODE, changed.mode());
        for (int band = 0; band < EqualizerCurve.BAND_COUNT; band++) {
            Assert.assertEquals((band >= 4 && band <= 6) ? 1 : 0, changed.gain(band));
            Assert.assertEquals(0, original.gain(band));
        }
    }

    @Test
    public void preserveDifferencesBetweenBands() {
        EqualizerCurve original = new EqualizerCurve(8, new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 });
        EqualizerCurve changed = EqualizerGroup.BASS.adjust(original, -1);
        for (int band = 0; band < EqualizerCurve.BAND_COUNT; band++) {
            Assert.assertEquals(band - ((band <= 3) ? 1 : 0), changed.gain(band));
        }
    }

    @Test
    public void clampBandAtLimitWhileMovingOtherBands() {
        EqualizerCurve original = new EqualizerCurve(8, new int[] { 0, 0, 0, 0, 0, 60, 0, 0, 0, 0 });
        Assert.assertTrue(EqualizerGroup.MIDRANGE.canAdjust(original, 1));
        EqualizerCurve changed = EqualizerGroup.MIDRANGE.adjust(original, 1);
        Assert.assertEquals(1, changed.gain(4));
        Assert.assertEquals(60, changed.gain(5));
        Assert.assertEquals(1, changed.gain(6));
        Assert.assertEquals(0, original.gain(4));
        EqualizerCurve lower = EqualizerGroup.MIDRANGE
            .adjust(new EqualizerCurve(8, new int[] { 0, 0, 0, 0, 0, -60, 0, 0, 0, 0 }), -1);
        Assert.assertEquals(-1, lower.gain(4));
        Assert.assertEquals(-60, lower.gain(5));
        Assert.assertEquals(-1, lower.gain(6));
    }

    @Test
    public void rejectNonStepDirection() {
        EqualizerCurve original = new EqualizerCurve(8, new int[10]);
        Assert.assertFalse(EqualizerGroup.TREBLE.canAdjust(original, 2));
        Assert.assertThrows(IllegalArgumentException.class, () -> EqualizerGroup.TREBLE.adjust(original, 2));
        Assert.assertThrows(IllegalArgumentException.class, () -> EqualizerGroup.TREBLE.adjust(original, 0));
    }

    @Test
    public void undoRestoresOriginalModeAndEntireCurve() {
        EqualizerCurve original = new EqualizerCurve(8, new int[10]);
        EqualizerHistory history = new EqualizerHistory(new SoundState(original, 0));
        EqualizerCurve changed = EqualizerGroup.TREBLE.adjust(original, -1);
        for (int band = 0; band < EqualizerCurve.BAND_COUNT; band++) {
            Assert.assertEquals(band >= 7 ? -1 : 0, changed.gain(band));
        }
        history.request(new SoundState(changed, 0));
        Assert.assertTrue(history.confirm(new SoundState(changed, 0)));
        Assert.assertEquals(new SoundState(original, 0), history.undoTarget());
        EqualizerCurve second = EqualizerGroup.TREBLE.adjust(changed, -1);
        history.request(new SoundState(second, 0));
        Assert.assertTrue(history.confirm(new SoundState(second, 0)));
        Assert.assertEquals(new SoundState(changed, 0), history.undoTarget());
    }

}
