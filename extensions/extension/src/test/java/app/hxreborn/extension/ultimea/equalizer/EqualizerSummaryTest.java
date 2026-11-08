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

import android.app.Application;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23)
public class EqualizerSummaryTest {

    private final Application application = RuntimeEnvironment.getApplication();

    @After
    public void clearState() {
        EqualizerSummary.clear();
        this.application.getSharedPreferences("hx_ultimea_equalizer_presets", 0).edit().clear().commit();
    }

    @Test
    public void waitsForConfirmedCurve() {
        Assert.assertNull(EqualizerSummary.current(this.application));
    }

    @Test
    public void showsConfirmedModeWithoutSelectedPreset() {
        EqualizerSummary.curve(new EqualizerCurve(8, new int[10]));
        Assert.assertEquals("Style", EqualizerSummary.current(this.application));
    }

    @Test
    public void showsMatchingPresetOnlyAfterWholeStateReadback() throws Exception {
        SoundState state = new SoundState(new EqualizerCurve(7, new int[10]), 1);
        EqualizerPresetStore store = new EqualizerPresetStore(this.application);
        store.createCustom("Movie", state, state);
        EqualizerSummary.curve(state.curve);
        Assert.assertEquals("Custom", EqualizerSummary.current(this.application));
        EqualizerSummary.xupmix(1);
        Assert.assertEquals("Custom: Movie custom", EqualizerSummary.current(this.application));
        EqualizerSummary.xupmix(0);
        Assert.assertEquals("Custom", EqualizerSummary.current(this.application));
    }

    @Test
    public void readsRenamedAndDeletedSelection() throws Exception {
        SoundState state = new SoundState(new EqualizerCurve(7, new int[10]), 1);
        EqualizerPresetStore store = new EqualizerPresetStore(this.application);
        EqualizerPreset preset = store.createCustom("Movie", state, state);
        EqualizerSummary.curve(state.curve);
        EqualizerSummary.xupmix(1);
        store.rename(preset.id, "Evening");
        Assert.assertEquals("Custom: Evening", EqualizerSummary.current(this.application));
        store.remove(preset.id);
        Assert.assertEquals("Custom", EqualizerSummary.current(this.application));
    }

    @Test
    public void skipsUnknownModeLabel() {
        EqualizerSummary.curve(new EqualizerCurve(9, new int[10]));
        Assert.assertNull(EqualizerSummary.current(this.application));
    }

    @Test
    public void clearsConnectionReading() {
        EqualizerSummary.curve(new EqualizerCurve(7, new int[10]));
        EqualizerSummary.xupmix(1);
        EqualizerSummary.clear();
        Assert.assertNull(EqualizerSummary.current(this.application));
    }

}
