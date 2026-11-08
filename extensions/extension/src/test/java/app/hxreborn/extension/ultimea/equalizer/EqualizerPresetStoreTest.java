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
import java.util.Collections;
import java.util.List;

import android.app.Application;
import org.json.JSONException;
import org.json.JSONArray;
import org.json.JSONObject;
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
    public void recoverReadablePresetsAndKeepOriginalData() throws Exception {
        EqualizerPreset original = this.store.add("Retained", this.curve);
        this.store.setCurrent(original.id);
        android.content.SharedPreferences preferences = this.application
            .getSharedPreferences("hx_ultimea_equalizer_presets", 0);
        String malformed = new JSONArray(preferences.getString("presets", "[]"))
            .put(new JSONObject().put("id", "broken"))
            .toString();
        preferences.edit().putString("presets", malformed).commit();
        Assert.assertThrows(JSONException.class, () -> this.store.load());
        Assert.assertEquals(1, this.store.recover());
        Assert.assertEquals(original.id, this.store.load().get(0).id);
        Assert.assertEquals(original.id, this.store.current());
        Assert.assertEquals(malformed, new JSONArray(preferences.getString("recovery_backups", "[]")).getString(0));
        this.store.add("New", this.curve);
        Assert.assertEquals(2, this.store.load().size());
        Assert.assertEquals(malformed, new JSONArray(preferences.getString("recovery_backups", "[]")).getString(0));
    }

    @Test
    public void recoverMalformedCollectionWithoutDiscardingItsBytes() throws Exception {
        android.content.SharedPreferences preferences = this.application
            .getSharedPreferences("hx_ultimea_equalizer_presets", 0);
        preferences.edit().putString("presets", "[{broken").putString("current", "broken").commit();
        Assert.assertEquals(0, this.store.recover());
        Assert.assertTrue(this.store.load().isEmpty());
        Assert.assertNull(this.store.current());
        Assert.assertEquals("[{broken", new JSONArray(preferences.getString("recovery_backups", "[]")).getString(0));
    }

    @Test
    public void mergePresetBundleWithoutChangingCurrentSelection() throws JSONException, IOException {
        SoundState base = new SoundState(new EqualizerCurve(1, new int[] { 30, 40, 20, 10, 0, 10, 10, 0, -20, -20 }),
                1);
        EqualizerPreset original = this.store.createCustom("Movie", base, base);
        EqualizerPreset first = EqualizerPreset.create(original.name, new SoundState(base.curve, 0), "Movie", base);
        SoundState custom = new SoundState(base.curve.withGain(2, 17), 0);
        EqualizerPreset second = EqualizerPreset.create(original.name, custom, "Movie", base);
        List<EqualizerPreset> added = this.store.addAll(EqualizerPresetBundleFile
            .read(EqualizerPresetBundleFile.json(Arrays.asList(first, second)).toString()));
        Assert.assertEquals(2, added.size());
        List<EqualizerPreset> restored = new EqualizerPresetStore(this.application).load();
        Assert.assertEquals(3, restored.size());
        Assert.assertEquals(original.id, this.store.current());
        Assert.assertEquals(original.id, restored.get(0).id);
        Assert.assertEquals("Movie custom (2)", restored.get(1).name);
        Assert.assertEquals(first.state, restored.get(1).state);
        Assert.assertEquals("Movie custom (3)", restored.get(2).name);
        Assert.assertEquals(custom, restored.get(2).state);
        Assert.assertEquals(base, restored.get(2).base);
        Assert.assertEquals("Movie", restored.get(2).baseName);
        Assert.assertNotEquals(first.id, restored.get(1).id);
        Assert.assertNotEquals(second.id, restored.get(2).id);
    }

    @Test
    public void rejectMalformedSecondBundleEntryWithoutChangingStore() throws JSONException, IOException {
        EqualizerPreset original = this.store.add("Original", this.curve);
        this.store.setCurrent(original.id);
        EqualizerPreset imported = EqualizerPreset.create("Imported", this.curve);
        JSONObject json = EqualizerPresetBundleFile.json(Arrays.asList(imported, imported));
        json.getJSONArray("presets").getJSONObject(1).put("gainsTenthsDb", new JSONArray(new int[9]));
        try {
            this.store.addAll(EqualizerPresetBundleFile.read(json.toString()));
            Assert.fail("Expected malformed bundle rejection");
        } catch (JSONException ex) {
            Assert.assertEquals(1, this.store.load().size());
            Assert.assertEquals(original.id, this.store.load().get(0).id);
            Assert.assertEquals(this.curve, this.store.load().get(0).curve);
            Assert.assertEquals(original.id, this.store.current());
        }
    }

    @Test
    public void rejectBundleBeyondRemainingCapacityWithoutChangingStore() throws JSONException, IOException {
        EqualizerPreset original = this.store.add("Original", this.curve);
        this.store.setCurrent(original.id);
        EqualizerPresetFile imported = EqualizerPresetFile
            .parse(EqualizerPresetFile.json("Imported", this.curve).toString());
        try {
            this.store.addAll(Collections.nCopies(64, imported));
            Assert.fail("Expected preset limit rejection");
        } catch (IOException ex) {
            Assert.assertEquals(1, this.store.load().size());
            Assert.assertEquals(original.id, this.store.load().get(0).id);
            Assert.assertEquals(this.curve, this.store.load().get(0).curve);
            Assert.assertEquals(original.id, this.store.current());
        }
    }

    @Test
    public void rejectInvalidSecondPresetWithoutSavingFirst() throws JSONException, IOException {
        EqualizerPreset original = this.store.add("Original", this.curve);
        this.store.setCurrent(original.id);
        EqualizerPresetFile imported = EqualizerPresetFile
            .parse(EqualizerPresetFile.json("Imported", this.curve).toString());
        try {
            this.store.addAll(Arrays.asList(imported, null));
            Assert.fail("Expected invalid preset rejection");
        } catch (IOException ex) {
            Assert.assertEquals(1, this.store.load().size());
            Assert.assertEquals(original.id, this.store.load().get(0).id);
            Assert.assertEquals(original.id, this.store.current());
        }
    }

    @Test
    public void createSequentialNamesFromBase() throws JSONException, IOException {
        SoundState base = new SoundState(this.curve, 1);
        EqualizerPreset first = this.store.createCustom("Movie", base, base);
        EqualizerPreset second = this.store.createCustom("Movie", base, base);
        Assert.assertEquals("Movie custom", first.name);
        Assert.assertEquals("Movie custom 2", second.name);
        Assert.assertEquals("Movie", second.baseName);
        Assert.assertEquals(second.id, this.store.current());
    }

    @Test
    public void updateStateWithoutChangingNameOrBase() throws JSONException, IOException {
        SoundState base = new SoundState(new EqualizerCurve(1, new int[] { 30, 40, 20, 10, 0, 10, 10, 0, -20, -20 }),
                1);
        EqualizerPreset original = this.store.createCustom("Movie", base, base);
        SoundState state = new SoundState(base.curve.withGain(2, 17), 0);
        this.store.rename(original.id, "Dialogue");
        this.store.update(original.id, state);
        EqualizerPreset updated = this.store.load().get(0);
        Assert.assertEquals(original.id, updated.id);
        Assert.assertEquals("Dialogue", updated.name);
        Assert.assertEquals(state.curve, updated.state.curve);
        Assert.assertEquals(EqualizerCurve.CUSTOM_MODE, updated.state.curve.mode());
        Assert.assertEquals(0, updated.state.xupmix);
        Assert.assertEquals(base.curve, updated.base.curve);
        Assert.assertEquals(1, updated.base.curve.mode());
        Assert.assertEquals(1, updated.base.xupmix);
        Assert.assertEquals("Movie", updated.baseName);
    }

    @Test
    public void retainMovieModeWhenOnlyXupmixChanges() throws JSONException, IOException {
        SoundState base = new SoundState(new EqualizerCurve(1, new int[] { 30, 40, 20, 10, 0, 10, 10, 0, -20, -20 }),
                1);
        EqualizerPreset preset = this.store.createCustom("Movie", base, new SoundState(base.curve, 0));
        EqualizerPreset loaded = new EqualizerPresetStore(this.application).load().get(0);
        Assert.assertEquals(preset.id, loaded.id);
        Assert.assertEquals(base.curve, loaded.state.curve);
        Assert.assertEquals(1, loaded.state.curve.mode());
        Assert.assertEquals(0, loaded.state.xupmix);
        Assert.assertEquals(base.curve, loaded.base.curve);
        Assert.assertEquals(1, loaded.base.xupmix);
    }

    @Test
    public void retainImportedStateAndBase() throws JSONException, IOException {
        SoundState base = new SoundState(this.curve, 0);
        SoundState state = new SoundState(this.curve.withGain(2, 17), 1);
        EqualizerPreset exported = EqualizerPreset.create("Imported", state, "Movie", base);
        EqualizerPreset imported = this.store
            .add(EqualizerPresetFile.parse(EqualizerPresetFile.json(exported).toString()));
        EqualizerPreset restored = new EqualizerPresetStore(this.application).load().get(0);
        Assert.assertEquals(imported.id, restored.id);
        Assert.assertEquals(state.curve, restored.state.curve);
        Assert.assertEquals(1, restored.state.xupmix);
        Assert.assertEquals("Movie", restored.baseName);
        Assert.assertEquals(base.curve, restored.base.curve);
        Assert.assertEquals(0, restored.base.xupmix);
    }

    @Test
    public void preserveCurrentSelectionUntilSelectedPresetIsDeleted() throws JSONException, IOException {
        EqualizerPreset first = this.store.add("First", this.curve);
        EqualizerPreset second = this.store.add("Second", this.curve);
        this.store.setCurrent(second.id);
        EqualizerPresetStore reopened = new EqualizerPresetStore(this.application);
        Assert.assertEquals(second.id, reopened.current());
        reopened.remove(first.id);
        Assert.assertEquals(second.id, reopened.current());
        reopened.remove(second.id);
        Assert.assertNull(reopened.current());
    }

    @Test
    public void retainWholeStateInPendingExport() throws JSONException, IOException {
        SoundState base = new SoundState(this.curve, 1);
        SoundState state = new SoundState(this.curve.withGain(2, 17), 0);
        EqualizerPreset preset = this.store.createCustom("Movie", base, state);
        this.store.prepareExport(preset);
        EqualizerPresetFile file = new EqualizerPresetStore(this.application).takeExport();
        Assert.assertEquals(state.curve, file.state.curve);
        Assert.assertEquals(0, file.state.xupmix);
        Assert.assertEquals(base.curve, file.base.curve);
        Assert.assertEquals(1, file.base.xupmix);
        Assert.assertEquals("Movie", file.baseName);
    }

    @Test(expected = IOException.class)
    public void rejectMissingCurrentSelection() throws JSONException, IOException {
        this.store.setCurrent("missing");
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
