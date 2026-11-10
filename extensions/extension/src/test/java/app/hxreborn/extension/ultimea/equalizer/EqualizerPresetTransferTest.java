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
import java.util.Arrays;
import java.util.List;

import android.app.Application;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23, shadows = EqualizerPresetTransferTest.BasesShadow.class)
public class EqualizerPresetTransferTest {

    private final Application application = RuntimeEnvironment.getApplication();

    private final EqualizerPresetStore store = new EqualizerPresetStore(this.application);

    private final EqualizerPresetTransfer transfer = new EqualizerPresetTransfer(this.application);

    @After
    public void clearPresets() {
        this.application.getSharedPreferences("hx_ultimea_equalizer_presets", 0).edit().clear().commit();
    }

    @Test
    public void importsCompleteCollectionWithoutChangingCurrentSelection() throws Exception {
        SoundState base = movie();
        EqualizerPreset original = this.store.createCustom("Movie", base, base);
        SoundState custom = base.withCurve(base.curve.withGain(4, 14).asCustom()).withXupmix(0);
        EqualizerPreset first = EqualizerPreset.create(original.name, custom, "Movie", base);
        EqualizerPreset second = EqualizerPreset.create("Second", base, "Movie", base);
        List<EqualizerPreset> imported = this.transfer
            .importDocument(EqualizerPresetBundleFile.json(Arrays.asList(first, second)).toString());
        Assert.assertEquals(2, imported.size());
        Assert.assertEquals("Movie custom (2)", imported.get(0).name);
        Assert.assertEquals(custom, imported.get(0).state);
        Assert.assertEquals(base, imported.get(0).base);
        Assert.assertEquals("Movie", imported.get(0).baseName);
        Assert.assertEquals(base, imported.get(1).state);
        Assert.assertEquals(original.id, this.store.current());
        Assert.assertEquals(3, this.store.load().size());
        Assert.assertNotEquals(first.id, imported.get(0).id);
    }

    @Test
    public void normalizesMismatchedNativeStateAndBaseBeforeSaving() throws Exception {
        SoundState mismatched = new SoundState(new EqualizerCurve(1, new int[10]), 1);
        EqualizerPreset preset = EqualizerPreset.create("Changed Movie", mismatched, "Movie", mismatched);
        EqualizerPreset imported = this.transfer.importDocument(EqualizerPresetFile.json(preset).toString()).get(0);
        Assert.assertEquals(EqualizerCurve.CUSTOM_MODE, imported.state.curve.mode());
        Assert.assertEquals(EqualizerCurve.CUSTOM_MODE, imported.base.curve.mode());
        Assert.assertEquals(1, imported.state.xupmix);
        Assert.assertEquals(1, imported.base.xupmix);
        Assert.assertEquals(mismatched.curve.asCustom(), imported.state.curve);
        Assert.assertEquals(imported.state, this.store.load().get(0).state);
    }

    @Test
    public void importsLegacySinglePresetWithoutInventingXupmix() throws Exception {
        EqualizerCurve curve = movie().curve.withGain(4, 14);
        EqualizerPreset imported = this.transfer.importDocument(EqualizerPresetFile.json("Legacy", curve).toString())
            .get(0);
        Assert.assertEquals(EqualizerCurve.CUSTOM_MODE, imported.state.curve.mode());
        Assert.assertEquals(-1, imported.state.xupmix);
        Assert.assertEquals(-1, imported.base.xupmix);
    }

    @Test
    public void exportsEveryPresetWithoutLocalSelectionOrIds() throws Exception {
        SoundState base = movie();
        EqualizerPreset first = this.store.createCustom("Movie", base, base);
        this.store.createCustom("Movie", base, base.withXupmix(0));
        this.store.setCurrent(first.id);
        String document = this.transfer.exportDocument();
        List<EqualizerPresetFile> exported = EqualizerPresetBundleFile.read(document);
        Assert.assertEquals(2, exported.size());
        Assert.assertEquals(base, exported.get(0).state);
        Assert.assertEquals(base.withXupmix(0), exported.get(1).state);
        Assert.assertFalse(document.contains(first.id));
        Assert.assertFalse(new JSONObject(document).has("current"));
    }

    @Test
    public void reportsRejectedPresetWhileImportingValidPresetAtomically() throws Exception {
        EqualizerPreset original = this.store.createCustom("Movie", movie(), movie());
        EqualizerPreset preset = EqualizerPreset.create("Imported", movie(), "Movie", movie());
        JSONObject document = EqualizerPresetBundleFile.json(Arrays.asList(preset, preset));
        document.getJSONArray("presets").put(1, new JSONObject().put("name", "Rejected"));
        EqualizerPresetTransfer.ImportResult result = this.transfer.importDocumentWithReport(document.toString());
        Assert.assertEquals(1, result.imported.size());
        Assert.assertEquals(1, result.rejected.size());
        Assert.assertEquals(2, result.rejected.get(0).position);
        Assert.assertEquals("Rejected", result.rejected.get(0).name);
        Assert.assertEquals(2, this.store.load().size());
        Assert.assertEquals(original.id, this.store.current());
    }

    @Test
    public void reportsInvalidSinglePresetWithoutWriting() throws Exception {
        JSONObject document = EqualizerPresetFile.json("Broken", movie().curve);
        document.getJSONArray("gainsTenthsDb").put(4, 99);
        EqualizerPresetTransfer.ImportResult result = this.transfer.importDocumentWithReport(document.toString());
        Assert.assertTrue(result.imported.isEmpty());
        Assert.assertEquals(1, result.rejected.size());
        Assert.assertEquals("Broken", result.rejected.get(0).name);
        Assert.assertEquals(EqualizerPresetBundleFile.RejectionReason.INVALID_SOUND_STATE,
                result.rejected.get(0).reason);
        Assert.assertTrue(this.store.load().isEmpty());
    }

    @Test(expected = JSONException.class)
    public void rejectsWrongFileMarker() throws Exception {
        JSONObject document = EqualizerPresetFile.json("Other", movie().curve);
        document.put("format", "other-presets");
        this.transfer.importDocumentWithReport(document.toString());
    }

    @Test(expected = JSONException.class)
    public void rejectsNewerFileVersion() throws Exception {
        JSONObject document = EqualizerPresetBundleFile
            .json(Arrays.asList(EqualizerPreset.create("Imported", movie(), "Movie", movie())));
        document.put("formatVersion", 2);
        this.transfer.importDocumentWithReport(document.toString());
    }

    @Test
    public void countsIgnoredFieldsFromBundleAndAcceptedPresets() throws Exception {
        EqualizerPreset preset = EqualizerPreset.create("Imported", movie(), "Movie", movie());
        JSONObject document = EqualizerPresetBundleFile.json(Arrays.asList(preset));
        document.put("future", true);
        document.getJSONArray("presets").getJSONObject(0).put("futureSound", true);
        EqualizerPresetTransfer.ImportResult result = this.transfer.importDocumentWithReport(document.toString());
        Assert.assertEquals(1, result.imported.size());
        Assert.assertEquals(2, result.ignoredFields);
        Assert.assertTrue(result.rejected.isEmpty());
    }

    @Test
    public void keepsStoreWhenEveryPresetIsRejected() throws Exception {
        EqualizerPreset original = this.store.createCustom("Movie", movie(), movie());
        EqualizerPreset preset = EqualizerPreset.create("Imported", movie(), "Movie", movie());
        JSONObject document = EqualizerPresetBundleFile.json(Arrays.asList(preset));
        document.getJSONArray("presets").put(0, new JSONObject());
        EqualizerPresetTransfer.ImportResult result = this.transfer.importDocumentWithReport(document.toString());
        Assert.assertTrue(result.imported.isEmpty());
        Assert.assertEquals(1, result.rejected.size());
        Assert.assertEquals(1, this.store.load().size());
        Assert.assertEquals(original.id, this.store.current());
    }

    @Test
    public void rejectsCollectionBeyondRemainingCapacityAtomically() throws Exception {
        EqualizerPreset original = this.store.createCustom("Movie", movie(), movie());
        EqualizerPreset preset = EqualizerPreset.create("Imported", movie(), "Movie", movie());
        EqualizerPreset[] imports = new EqualizerPreset[64];
        Arrays.fill(imports, preset);
        String document = EqualizerPresetBundleFile.json(Arrays.asList(imports)).toString();
        try {
            this.transfer.importDocument(document);
            Assert.fail("Collection exceeded preset limit");
        } catch (IOException ex) {
            Assert.assertEquals(1, this.store.load().size());
            Assert.assertEquals(original.id, this.store.load().get(0).id);
            Assert.assertEquals(original.id, this.store.current());
        }
    }

    private static SoundState movie() {
        return new SoundState(new EqualizerCurve(1, new int[] { 30, 40, 20, 10, 0, 10, 10, 0, -20, -20 }), 1);
    }

    @Implements(EqualizerBases.class)
    public static class BasesShadow {

        @Implementation
        protected static String definitions() {
            return "{\"default_mode\":[{\"name\":\"movie\",\"db\":[3,4,2,1,0,1,1,0,-2,-2]}]}";
        }

    }

}
