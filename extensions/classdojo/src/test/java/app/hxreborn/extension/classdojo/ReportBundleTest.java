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
package app.hxreborn.extension.classdojo;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Arrays;

import android.content.Context;
import android.content.ContextWrapper;
import org.robolectric.RuntimeEnvironment;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 28)
@SuppressWarnings("deprecation")
public final class ReportBundleTest {

    private static final int STRING_COUNT = 30784;

    private static final int STRING_TABLE = 128;

    private static final int STRING_STORAGE = STRING_TABLE + STRING_COUNT * 4;

    private static final int CODE = STRING_STORAGE + 25;

    private static final byte[] ORIGINAL = { 2, 4, 6, 8 };

    private static final String ORIGINAL_HEX = "02 04 06 08";

    private static final String REPLACEMENT_HEX = "02 05 06 09";

    private final Context context = RuntimeEnvironment.getApplication();

    @Test
    public void writesDerivedBundleWithoutChangingSource() throws Exception {
        byte[] bytes = bundle();
        File source = source(bytes);
        String result = ReportBundle.prepare(this.context, source.getPath(), ORIGINAL_HEX, REPLACEMENT_HEX);
        Assert.assertNotEquals(source.getPath(), result);
        Assert.assertArrayEquals(bytes, Files.readAllBytes(source.toPath()));
        byte[] derived = Files.readAllBytes(new File(result).toPath());
        Assert.assertEquals(5, derived[CODE + 1]);
        Assert.assertEquals(9, derived[CODE + 3]);
        Assert.assertArrayEquals(seal(derived.clone()), derived);
    }

    @Test
    public void rejectsRepeatedReportPattern() throws Exception {
        byte[] bytes = bundle();
        System.arraycopy(ORIGINAL, 0, bytes, CODE + ORIGINAL.length, ORIGINAL.length);
        assertOriginal(seal(bytes));
    }

    @Test
    public void rejectsChangedComponentIdentifier() throws Exception {
        byte[] bytes = bundle();
        bytes[STRING_STORAGE] = 'X';
        assertOriginal(seal(bytes));
    }

    @Test
    public void rejectsBrokenFooter() throws Exception {
        byte[] bytes = bundle();
        bytes[bytes.length - 1] ^= 1;
        assertOriginal(bytes);
    }

    @Test
    public void rejectsDifferentHermesVersion() throws Exception {
        byte[] bytes = bundle();
        bytes[8] = 97;
        assertOriginal(seal(bytes));
    }

    @Test
    public void rejectsTruncatedTables() throws Exception {
        byte[] bytes = bundle();
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).putInt(40, Integer.MAX_VALUE);
        assertOriginal(seal(bytes));
    }

    @Test
    public void keepsBundleWithoutOriginalPattern() throws Exception {
        byte[] bytes = bundle();
        bytes[CODE + 1] = 5;
        assertOriginal(seal(bytes));
    }

    @Test
    public void keepsAssetSelection() {
        Assert.assertEquals("assets://index.android.bundle",
                ReportBundle.prepare(this.context, "assets://index.android.bundle", ORIGINAL_HEX, REPLACEMENT_HEX));
    }

    @Test
    public void keepsSourceWhenCacheCannotBeWritten() throws Exception {
        File cacheFile = source(new byte[] { 0 });
        Context blockedCache = new ContextWrapper(this.context) {
            @Override
            public File getCodeCacheDir() {
                return cacheFile;
            }
        };
        File source = source(bundle());
        Assert.assertEquals(source.getPath(),
                ReportBundle.prepare(blockedCache, source.getPath(), ORIGINAL_HEX, REPLACEMENT_HEX));
    }

    @Test
    public void keepsSourceWhenCachePublicationFails() throws Exception {
        File output = new File(this.context.getCodeCacheDir(), "classdojo-reports.bundle");
        Files.deleteIfExists(output.toPath());
        Assert.assertTrue(output.mkdir());
        try {
            assertOriginal(bundle());
            Assert.assertFalse(new File(this.context.getCodeCacheDir(), "classdojo-reports.bundle.tmp").exists());
        } finally {
            Assert.assertTrue(output.delete());
        }
    }

    private void assertOriginal(byte[] bytes) throws Exception {
        File source = source(bytes);
        Assert.assertEquals(source.getPath(),
                ReportBundle.prepare(this.context, source.getPath(), ORIGINAL_HEX, REPLACEMENT_HEX));
        Assert.assertArrayEquals(bytes, Files.readAllBytes(source.toPath()));
    }

    private File source(byte[] bytes) throws Exception {
        File file = File.createTempFile("download-", ".bundle", this.context.getFilesDir());
        Files.write(file.toPath(), bytes);
        return file;
    }

    private byte[] bundle() throws Exception {
        byte[] bytes = new byte[CODE + ORIGINAL.length * 2 + 20];
        ByteBuffer header = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        header.putLong(0, 0x1f1903c103bc1fc6L);
        header.putInt(8, 98);
        header.putInt(32, bytes.length);
        header.putInt(52, STRING_COUNT);
        header.putInt(60, 25);
        header.putInt(STRING_TABLE + 30782 * 4, 14 << 24);
        header.putInt(STRING_TABLE + 30783 * 4, (11 << 24) | (14 << 1));
        byte[] names = "NonPlusContentPlusContent".getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(names, 0, bytes, STRING_STORAGE, names.length);
        System.arraycopy(ORIGINAL, 0, bytes, CODE, ORIGINAL.length);
        return seal(bytes);
    }

    private byte[] seal(byte[] bytes) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-1").digest(Arrays.copyOf(bytes, bytes.length - 20));
        System.arraycopy(digest, 0, bytes, bytes.length - 20, digest.length);
        return bytes;
    }

}
