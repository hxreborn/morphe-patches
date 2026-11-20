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

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.android.controller.ActivityController;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23)
public class PresetShareProviderTest {

    @Test
    public void sharesCompleteSoundStateWithReadGrant() throws Exception {
        try (ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            SoundState state = new SoundState(new EqualizerCurve(7, new int[10]).withGain(5, -1), 1);
            SoundState base = new SoundState(new EqualizerCurve(1, new int[10]), 0);
            EqualizerPreset preset = EqualizerPreset.create("Movie custom", state, "Movie", base);
            PresetShareProvider.share(activity, preset);
            Intent chooser = Shadows.shadowOf(activity).getNextStartedActivity();
            Assert.assertEquals(Intent.ACTION_CHOOSER, chooser.getAction());
            Intent send = chooser.getParcelableExtra(Intent.EXTRA_INTENT);
            Assert.assertEquals(Intent.ACTION_SEND, send.getAction());
            Assert.assertTrue((send.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0);
            Uri uri = send.getParcelableExtra(Intent.EXTRA_STREAM);
            Assert.assertTrue((chooser.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0);
            Assert.assertEquals(uri, chooser.getClipData().getItemAt(0).getUri());
            Assert.assertEquals(uri, send.getClipData().getItemAt(0).getUri());
            PresetShareProvider provider = Robolectric.buildContentProvider(PresetShareProvider.class).create().get();
            try (android.database.Cursor metadata = provider.query(uri, null, null, null, null)) {
                Assert.assertTrue(metadata.moveToFirst());
                Assert.assertEquals("Movie custom.json", metadata.getString(0));
            }
            try (android.database.Cursor metadata = provider.query(uri,
                    new String[] { android.provider.OpenableColumns.SIZE }, null, null, null)) {
                Assert.assertTrue(metadata.moveToFirst());
                Assert.assertEquals(1, metadata.getColumnCount());
                Assert.assertEquals(android.provider.OpenableColumns.SIZE, metadata.getColumnName(0));
                Assert.assertTrue(metadata.getLong(0) > 0);
            }
            try (InputStream input = new ParcelFileDescriptor.AutoCloseInputStream(provider.openFile(uri, "r"))) {
                java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    bytes.write(buffer, 0, count);
                }
                EqualizerPresetFile shared = EqualizerPresetFile
                    .parse(new String(bytes.toByteArray(), StandardCharsets.UTF_8));
                Assert.assertEquals(state, shared.state);
                Assert.assertEquals(base, shared.base);
                Assert.assertEquals("Movie", shared.baseName);
            }
        }
    }

    @Test
    public void sharesCollectionWithWholeStateAndCollectionFilename() throws Exception {
        try (ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            SoundState base = new SoundState(new EqualizerCurve(1, new int[10]), 1);
            SoundState state = new SoundState(new EqualizerCurve(7, new int[10]).withGain(4, 16), 0);
            EqualizerPreset first = EqualizerPreset.create("Movie custom", state, "Movie", base);
            EqualizerPreset second = EqualizerPreset.create("Movie custom 2", base, "Movie", base);
            PresetShareProvider.shareAll(activity, Arrays.asList(first, second));
            Intent chooser = Shadows.shadowOf(activity).getNextStartedActivity();
            Assert.assertEquals("Share presets", chooser.getCharSequenceExtra(Intent.EXTRA_TITLE));
            Intent send = chooser.getParcelableExtra(Intent.EXTRA_INTENT);
            Assert.assertEquals("application/json", send.getType());
            Assert.assertTrue((send.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0);
            Uri uri = send.getParcelableExtra(Intent.EXTRA_STREAM);
            Assert.assertTrue((chooser.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0);
            Assert.assertEquals(uri, chooser.getClipData().getItemAt(0).getUri());
            Assert.assertEquals(uri, send.getClipData().getItemAt(0).getUri());
            PresetShareProvider provider = Robolectric.buildContentProvider(PresetShareProvider.class).create().get();
            try (android.database.Cursor metadata = provider.query(uri, null, null, null, null)) {
                Assert.assertTrue(metadata.moveToFirst());
                Assert.assertEquals("ULTIMEA presets.json", metadata.getString(0));
                Assert.assertTrue(metadata.getLong(1) > 0);
            }
            try (InputStream input = new ParcelFileDescriptor.AutoCloseInputStream(provider.openFile(uri, "r"))) {
                java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    bytes.write(buffer, 0, count);
                }
                List<EqualizerPresetFile> shared = EqualizerPresetBundleFile
                    .read(new String(bytes.toByteArray(), StandardCharsets.UTF_8));
                Assert.assertEquals(2, shared.size());
                Assert.assertEquals(state, shared.get(0).state);
                Assert.assertEquals(base, shared.get(0).base);
                Assert.assertEquals("Movie", shared.get(0).baseName);
                Assert.assertEquals(base, shared.get(1).state);
            }
        }
    }

    @Test
    public void givesSingleEntryCollectionCollectionFilename() throws Exception {
        try (ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            EqualizerPreset preset = EqualizerPreset.create("Single", new EqualizerCurve(7, new int[10]));
            PresetShareProvider.shareAll(activity, Arrays.asList(preset));
            Intent chooser = Shadows.shadowOf(activity).getNextStartedActivity();
            Intent send = chooser.getParcelableExtra(Intent.EXTRA_INTENT);
            Uri uri = send.getParcelableExtra(Intent.EXTRA_STREAM);
            Assert.assertTrue((chooser.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0);
            Assert.assertEquals(uri, chooser.getClipData().getItemAt(0).getUri());
            Assert.assertEquals(uri, send.getClipData().getItemAt(0).getUri());
            PresetShareProvider provider = Robolectric.buildContentProvider(PresetShareProvider.class).create().get();
            try (android.database.Cursor metadata = provider.query(uri, null, null, null, null)) {
                Assert.assertTrue(metadata.moveToFirst());
                Assert.assertEquals("ULTIMEA presets.json", metadata.getString(0));
            }
        }
    }

    @Test
    public void readsCollectionMetadataBeyondSinglePresetSizeLimit() throws Exception {
        try (ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            char[] characters = new char[EqualizerPreset.MAX_NAME_LENGTH];
            Arrays.fill(characters, '\u97f3');
            SoundState state = new SoundState(new EqualizerCurve(7, new int[10]), 1);
            EqualizerPreset preset = EqualizerPreset.create(new String(characters), state, new String(characters),
                    state);
            EqualizerPreset[] presets = new EqualizerPreset[64];
            Arrays.fill(presets, preset);
            PresetShareProvider.shareAll(activity, Arrays.asList(presets));
            Intent chooser = Shadows.shadowOf(activity).getNextStartedActivity();
            Intent send = chooser.getParcelableExtra(Intent.EXTRA_INTENT);
            Uri uri = send.getParcelableExtra(Intent.EXTRA_STREAM);
            Assert.assertTrue((chooser.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0);
            Assert.assertEquals(uri, chooser.getClipData().getItemAt(0).getUri());
            Assert.assertEquals(uri, send.getClipData().getItemAt(0).getUri());
            PresetShareProvider provider = Robolectric.buildContentProvider(PresetShareProvider.class).create().get();
            try (android.database.Cursor metadata = provider.query(uri, null, null, null, null)) {
                Assert.assertTrue(metadata.moveToFirst());
                Assert.assertEquals("ULTIMEA presets.json", metadata.getString(0));
                Assert.assertTrue(metadata.getLong(1) > EqualizerPresetFile.MAX_BYTES);
                Assert.assertTrue(metadata.getLong(1) <= EqualizerPresetBundleFile.MAX_BYTES);
            }
        }
    }

    @Test(expected = FileNotFoundException.class)
    public void rejectsCollectionWriteAccess() throws Exception {
        try (ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            PresetShareProvider.shareAll(activity,
                    Arrays.asList(EqualizerPreset.create("Single", new EqualizerCurve(7, new int[10]))));
            Intent chooser = Shadows.shadowOf(activity).getNextStartedActivity();
            Intent send = chooser.getParcelableExtra(Intent.EXTRA_INTENT);
            Uri uri = send.getParcelableExtra(Intent.EXTRA_STREAM);
            Assert.assertTrue((chooser.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0);
            Assert.assertEquals(uri, chooser.getClipData().getItemAt(0).getUri());
            Assert.assertEquals(uri, send.getClipData().getItemAt(0).getUri());
            PresetShareProvider provider = Robolectric.buildContentProvider(PresetShareProvider.class).create().get();
            provider.openFile(uri, "w");
        }
    }

    @Test(expected = FileNotFoundException.class)
    public void rejectsSharePathTraversal() throws Exception {
        PresetShareProvider provider = Robolectric.buildContentProvider(PresetShareProvider.class).create().get();
        provider.openFile(Uri.parse("content://org.robolectric.default.hx_equalizer_presets/../private.json"), "r");
    }

}
