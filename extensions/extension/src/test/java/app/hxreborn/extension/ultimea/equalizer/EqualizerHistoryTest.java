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

public class EqualizerHistoryTest {

    private final EqualizerCurve original = new EqualizerCurve(8, new int[EqualizerCurve.BAND_COUNT]);

    @Test
    public void undoRestoresFullOriginalModeAndCurve() {
        EqualizerHistory history = new EqualizerHistory(this.original);
        EqualizerCurve changed = this.original.withGain(5, -1);
        history.request(changed);
        Assert.assertFalse(history.canUndo());
        Assert.assertTrue(history.confirm(changed));
        Assert.assertEquals(this.original, history.undoTarget());
        history.request(history.undoTarget());
        Assert.assertTrue(history.confirm(this.original));
        Assert.assertEquals(this.original, history.current());
    }

    @Test
    public void mismatchedReadbackDoesNotConfirmChange() {
        EqualizerHistory history = new EqualizerHistory(this.original);
        history.request(this.original.withGain(5, -1));
        Assert.assertFalse(history.confirm(this.original));
        Assert.assertEquals(this.original, history.current());
        Assert.assertFalse(history.canUndo());
    }

    @Test
    public void failedChangePreservesPreviousUndo() {
        EqualizerHistory history = new EqualizerHistory(this.original);
        EqualizerCurve changed = this.original.withGain(5, -1);
        history.request(changed);
        history.confirm(changed);
        history.request(changed.withGain(0, 1));
        history.failed();
        Assert.assertEquals(this.original, history.undoTarget());
        Assert.assertEquals(changed, history.current());
    }

    @Test(expected = IllegalStateException.class)
    public void rejectOverlappingChange() {
        EqualizerHistory history = new EqualizerHistory(this.original);
        history.request(this.original.withGain(5, -1));
        history.request(this.original.withGain(5, -2));
    }

}
