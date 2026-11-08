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

import java.io.IOException;
import java.util.List;

import android.app.Application;
import org.json.JSONException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23)
public class EqualizerPresetStoreTest {

    private final Application application = RuntimeEnvironment.getApplication();

    private final EqualizerPresetStore store = new EqualizerPresetStore(this.application);

    private final EqualizerCurve curve = new EqualizerCurve(8, new int[10]).withGain(5, -1);

    @After
    public void clearPresets() {
        this.application.getSharedPreferences("hx_ultimea_equalizer_presets", 0).edit().clear().commit();
    }

    @Test
    public void duplicatePreservesOriginalAndGains() throws JSONException, IOException {
        EqualizerPreset original = this.store.add("Dialogue", this.curve);
        EqualizerPreset duplicate = this.store.add(original.name, original.curve);
        Assert.assertNotEquals(original.id, duplicate.id);
        Assert.assertEquals("Dialogue (2)", duplicate.name);
        Assert.assertEquals(this.curve, duplicate.curve);
        Assert.assertEquals("Dialogue", this.store.load().get(0).name);
    }

    @Test
    public void renamePreservesIdentityAndCurve() throws JSONException, IOException {
        EqualizerPreset original = this.store.add("Dialogue", this.curve);
        this.store.rename(original.id, "  Movie dialogue  ");
        EqualizerPreset renamed = this.store.load().get(0);
        Assert.assertEquals(original.id, renamed.id);
        Assert.assertEquals("Movie dialogue", renamed.name);
        Assert.assertEquals(this.curve, renamed.curve);
    }

    @Test
    public void rejectNameCollisionWithoutChangingPresets() throws JSONException, IOException {
        EqualizerPreset first = this.store.add("First", this.curve);
        this.store.add("Second", this.curve);
        try {
            this.store.rename(first.id, "Second");
            Assert.fail("Expected duplicate name rejection");
        } catch (IllegalArgumentException ex) {
            List<EqualizerPreset> presets = this.store.load();
            Assert.assertEquals("First", presets.get(0).name);
            Assert.assertEquals("Second", presets.get(1).name);
        }
    }

    @Test
    public void keepCustomModeForSavedCurves() throws JSONException, IOException {
        this.store.add("Flat", new EqualizerCurve(5, new int[10]));
        Assert.assertEquals(EqualizerCurve.CUSTOM_MODE, this.store.load().get(0).curve.mode());
    }

    @Test
    public void rejectMalformedImportWithoutChangingStore() throws JSONException, IOException {
        this.store.add("Original", this.curve);
        try {
            EqualizerPresetFile file = EqualizerPresetFile.parse("{}");
            this.store.add(file.name, file.curve);
            Assert.fail("Expected invalid file rejection");
        } catch (JSONException ex) {
            Assert.assertEquals(1, this.store.load().size());
            Assert.assertEquals("Original", this.store.load().get(0).name);
            Assert.assertEquals(this.curve, this.store.load().get(0).curve);
        }
    }

    @Test
    public void persistAcrossStoreInstances() throws JSONException, IOException {
        this.store.add("Dialogue", this.curve);
        EqualizerPresetStore reopened = new EqualizerPresetStore(this.application);
        Assert.assertEquals(this.curve, reopened.load().get(0).curve);
    }

    @Test
    public void retainExportAcrossStoreRecreation() throws JSONException, IOException {
        EqualizerPreset preset = this.store.add("Dialogue", this.curve);
        this.store.prepareExport(preset);
        EqualizerPresetStore recreated = new EqualizerPresetStore(this.application);
        EqualizerPresetFile pending = recreated.takeExport();
        Assert.assertEquals(preset.name, pending.name);
        Assert.assertEquals(this.curve, pending.curve);
        Assert.assertNull(recreated.takeExport());
    }

    @Test
    public void keepSavedPresetsWhenExportIsConsumed() throws JSONException, IOException {
        EqualizerPreset preset = this.store.add("Dialogue", this.curve);
        this.store.prepareExport(preset);
        this.store.takeExport();
        Assert.assertEquals(1, this.store.load().size());
        Assert.assertEquals(preset.id, this.store.load().get(0).id);
    }

    @Test
    public void removeOnlySelectedPreset() throws JSONException, IOException {
        EqualizerPreset first = this.store.add("First", this.curve);
        EqualizerPreset second = this.store.add("Second", this.curve);
        this.store.remove(first.id);
        Assert.assertEquals(1, this.store.load().size());
        Assert.assertEquals(second.id, this.store.load().get(0).id);
        Assert.assertEquals(this.curve, this.store.load().get(0).curve);
    }

}
