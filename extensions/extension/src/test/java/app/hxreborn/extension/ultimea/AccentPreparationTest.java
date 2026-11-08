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
package app.hxreborn.extension.ultimea;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.content.res.AssetManager;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 31,
        shadows = { AccentPreparationTest.AppliedFlagsShadow.class, AccentPreparationTest.PaletteAssetsShadow.class })
public class AccentPreparationTest {

    private Context context;

    @Before
    public void prepareFixture() {
        this.context = RuntimeEnvironment.getApplication();
        this.context.getSharedPreferences(PatchSettings.PREFERENCES_NAME, 0).edit().clear().commit();
        PaletteAssetsShadow.source = ByteBuffer.allocate(16)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putInt(0x31434148)
            .putInt(2)
            .putInt(0xff541597)
            .putInt(0xffc499ff)
            .array();
        PaletteAssetsShadow.opens = 0;
        PaletteAssetsShadow.failedOpen = 0;
        Assert.assertTrue(AccentColor.isPatched());
    }

    @Test
    public void writesPackedColorsAndMatchingFloatChannels() throws Exception {
        Assert.assertTrue(AccentPalette.setPreset(this.context, AccentPalette.Fixed.RED.preset));
        Assert.assertTrue(AccentColor.prepare(this.context));
        ByteBuffer palette = ByteBuffer.wrap(palette()).order(ByteOrder.LITTLE_ENDIAN);
        Assert.assertEquals(32, palette.remaining());
        for (int source : new int[] { 0xff541597, 0xffc499ff }) {
            int color = AccentPalette.transform(this.context, source);
            Assert.assertEquals(color, palette.getInt());
            Assert.assertEquals(((color >>> 16) & 255) / 255f, palette.getFloat(), 0);
            Assert.assertEquals(((color >>> 8) & 255) / 255f, palette.getFloat(), 0);
            Assert.assertEquals((color & 255) / 255f, palette.getFloat(), 0);
        }
    }

    @Test
    public void preservesPublishedPaletteAfterMalformedAsset() throws Exception {
        Assert.assertTrue(AccentColor.prepare(this.context));
        byte[] previous = palette();
        for (byte[] malformed : new byte[][] { new byte[0], new byte[8],
                ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putInt(0x31434148).putInt(2).array() }) {
            PaletteAssetsShadow.source = malformed;
            Assert.assertFalse(AccentColor.prepare(this.context));
            Assert.assertArrayEquals(previous, palette());
        }
    }

    @Test
    public void restoresPaletteEvenWhenSelectionRollbackReportsFailure() throws Exception {
        assertPreparationRollback(true);
    }

    @Test
    public void restoresPaletteAfterTransientAssetFailure() throws Exception {
        assertPreparationRollback(false);
    }

    private void assertPreparationRollback(boolean failRollback) throws Exception {
        Assert.assertTrue(AccentPalette.setPreset(this.context, AccentPalette.Fixed.BLUE.preset));
        Assert.assertTrue(AccentColor.prepare(this.context));
        byte[] blue = palette();
        Assert.assertTrue(AccentPalette.setPreset(this.context, AccentPalette.Fixed.RED.preset));
        Assert.assertTrue(AccentColor.prepare(this.context));
        Assert.assertTrue(AccentPalette.setPreset(this.context, AccentPalette.Fixed.BLUE.preset));
        PaletteAssetsShadow.failedOpen = PaletteAssetsShadow.opens + 1;
        SharedPreferences preferences = this.context.getSharedPreferences(PatchSettings.PREFERENCES_NAME, 0);
        int[] commits = { 0 };
        SharedPreferences wrapped = (SharedPreferences) java.lang.reflect.Proxy.newProxyInstance(
                SharedPreferences.class.getClassLoader(), new Class<?>[] { SharedPreferences.class },
                (proxy, method, arguments) -> {
                    if (!"edit".equals(method.getName())) {
                        return method.invoke(preferences, arguments);
                    }
                    SharedPreferences.Editor editor = preferences.edit();
                    return java.lang.reflect.Proxy.newProxyInstance(SharedPreferences.Editor.class.getClassLoader(),
                            new Class<?>[] { SharedPreferences.Editor.class }, (editorProxy, operation, values) -> {
                                Object result = operation.invoke(editor, values);
                                if ("commit".equals(operation.getName())) {
                                    commits[0]++;
                                    return !failRollback || commits[0] != 2;
                                }
                                return result == editor ? editorProxy : result;
                            });
                });
        Context contextWithCommitResult = new ContextWrapper(this.context) {
            @Override
            public SharedPreferences getSharedPreferences(String name, int mode) {
                return wrapped;
            }
        };
        Assert.assertFalse(AccentColor.setPreset(contextWithCommitResult, AccentPalette.Fixed.RED.preset));
        Assert.assertEquals(AccentPalette.Fixed.BLUE.preset, AccentPalette.getPreset(this.context));
        Assert.assertArrayEquals(blue, palette());
        Assert.assertEquals(2, commits[0]);
        Assert.assertEquals(4, PaletteAssetsShadow.opens);
    }

    @Test
    public void publishCompletePaletteWithoutChangingPreviousMapping() throws Exception {
        Assert.assertTrue(AccentPalette.setPreset(this.context, AccentPalette.Fixed.BLUE.preset));
        Assert.assertTrue(AccentColor.prepare(this.context));
        File published = new File(this.context.getFilesDir(), "hx_ultimea_accent");
        byte[] previous = Files.readAllBytes(published.toPath());
        try (java.io.RandomAccessFile original = new java.io.RandomAccessFile(published, "r")) {
            java.nio.MappedByteBuffer mapped = original.getChannel()
                .map(java.nio.channels.FileChannel.MapMode.READ_ONLY, 0, previous.length);
            Assert.assertTrue(AccentPalette.setPreset(this.context, AccentPalette.Fixed.RED.preset));
            Assert.assertTrue(AccentColor.prepare(this.context));
            byte[] retained = new byte[previous.length];
            mapped.get(retained);
            Assert.assertArrayEquals(previous, retained);
            byte[] current = Files.readAllBytes(published.toPath());
            Assert.assertFalse(java.util.Arrays.equals(previous, current));
            long version = ByteBuffer.wrap(current).order(ByteOrder.LITTLE_ENDIAN).getLong();
            Assert.assertEquals(ByteBuffer.wrap(previous).order(ByteOrder.LITTLE_ENDIAN).getLong() + 1, version);
            byte[] marker = Files
                .readAllBytes(new File(this.context.getFilesDir(), "hx_ultimea_accent_version").toPath());
            Assert.assertEquals(version, ByteBuffer.wrap(marker).order(ByteOrder.LITTLE_ENDIAN).getLong());
            Assert.assertEquals(40, current.length);
            Assert.assertTrue(AccentColor.prepare(this.context));
            Assert.assertArrayEquals(current, Files.readAllBytes(published.toPath()));
        }
    }

    private byte[] palette() throws IOException {
        byte[] bytes = Files.readAllBytes(new File(this.context.getFilesDir(), "hx_ultimea_accent").toPath());
        return java.util.Arrays.copyOfRange(bytes, Long.BYTES, bytes.length);
    }

    @Implements(value = AppliedPatches.class, isInAndroidSdk = false)
    public static class AppliedFlagsShadow {

        @Implementation
        protected static boolean accentColor() {
            return true;
        }

    }

    @Implements(AssetManager.class)
    public static class PaletteAssetsShadow extends org.robolectric.shadows.ShadowArscAssetManager10 {

        private static byte[] source;

        private static int opens;

        private static int failedOpen;

        @Implementation
        protected InputStream open(String name) throws IOException {
            if (!"hx_ultimea/accent_colors".equals(name) || ++opens == failedOpen) {
                throw new IOException("Palette asset read failed");
            }
            return new ByteArrayInputStream(source);
        }

    }

}
