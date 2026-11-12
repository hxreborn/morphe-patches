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
package app.hxreborn.extension.readera;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public final class AccentResourceTableTest {

    private static final String PACKAGE_NAME = "org.readera";

    private static final String TABLE_ENTRY = "resources.arsc";

    private static final String MANIFEST_ENTRY = "AndroidManifest.xml";

    private static final String APK_NAME = "accent.apk";

    private static final short SDK_QUALIFIER = 21;

    private static final int ACCENT_ID = 0x7f060019;

    private static final int ACCENT_PACKAGE_ID = 0x7f;

    private static final byte ACCENT_TYPE_ID = 0x06;

    private static final int ACCENT_ENTRY_COUNT = 0x1A;

    private static final int STOCK_COLOR_ENTRY_COUNT = 267;

    private static final int TYPE_ENTRY_COUNT_OFFSET = 12;

    private static final int TYPE_ENTRIES_START_OFFSET = 16;

    private static final int NO_ENTRY = 0xFFFFFFFF;

    private static final int FIRST_COLOR_ID = 0x7f020000;

    private static final int ID_WITHOUT_TYPE = 0x7f000019;

    private static final int COLOR = 0xFFA5D6A7;

    private static final int AAPT2_TABLE_SIZE = 796;

    private static final short PACKAGE_CHUNK = 0x0200;

    private static final short TYPE_SPEC_CHUNK = 0x0202;

    private static final short TYPE_CHUNK = 0x0201;

    private static final short XML_CHUNK = 0x0003;

    private static final int TABLE_HEADER_SIZE = 12;

    private static final int CHUNK_HEADER_SIZE_OFFSET = 2;

    private static final int CHUNK_SIZE_OFFSET = 4;

    private static final int PACKAGE_ID_OFFSET = 8;

    private static final int ENTRY_COUNT_OFFSET = 12;

    private static final int TYPE_ID_OFFSET = 8;

    private static final int CONFIG_SDK_VERSION_OFFSET = 44;

    private static final int TYPE_HEADER_END = 84;

    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void writeAccentColorEntryInSdk21Configuration() throws IOException {
        ByteBuffer table = entry(writeApk(ACCENT_ID, "z", ACCENT_ENTRY_COUNT), TABLE_ENTRY);

        Assert.assertEquals(AAPT2_TABLE_SIZE, table.capacity());
        Assert.assertEquals(AAPT2_TABLE_SIZE, table.getInt(CHUNK_SIZE_OFFSET));
        int packageChunk = chunk(table, PACKAGE_CHUNK);
        Assert.assertEquals(ACCENT_PACKAGE_ID, table.getInt(packageChunk + PACKAGE_ID_OFFSET));
        int typeSpec = chunk(table, TYPE_SPEC_CHUNK);
        Assert.assertEquals(ACCENT_ENTRY_COUNT, table.getInt(typeSpec + ENTRY_COUNT_OFFSET));
        int type = chunk(table, TYPE_CHUNK);
        Assert.assertEquals(ACCENT_TYPE_ID, table.get(type + TYPE_ID_OFFSET));
        Assert.assertEquals(SDK_QUALIFIER, table.getShort(type + CONFIG_SDK_VERSION_OFFSET));
        Assert.assertEquals(COLOR, table.getInt(table.capacity() - Integer.BYTES));
    }

    @Test
    public void writeFirstColorEntry() throws IOException {
        ByteBuffer table = entry(writeApk(FIRST_COLOR_ID, "accent", 1), TABLE_ENTRY);

        Assert.assertEquals(1, table.getInt(chunk(table, TYPE_SPEC_CHUNK) + ENTRY_COUNT_OFFSET));
        Assert.assertEquals(COLOR, table.getInt(table.capacity() - Integer.BYTES));
    }

    @Test
    public void sizeTypeEntriesToStockEntryCount() throws IOException {
        ByteBuffer table = entry(writeApk(ACCENT_ID, "z", STOCK_COLOR_ENTRY_COUNT), TABLE_ENTRY);

        Assert.assertEquals(STOCK_COLOR_ENTRY_COUNT, table.getInt(chunk(table, TYPE_SPEC_CHUNK) + ENTRY_COUNT_OFFSET));
        int type = chunk(table, TYPE_CHUNK);
        Assert.assertEquals(STOCK_COLOR_ENTRY_COUNT, table.getInt(type + TYPE_ENTRY_COUNT_OFFSET));
        int entriesStart = type + table.getInt(type + TYPE_ENTRIES_START_OFFSET);
        Assert.assertEquals(entriesStart, type + TYPE_HEADER_END + STOCK_COLOR_ENTRY_COUNT * Integer.BYTES);
        Assert.assertEquals(NO_ENTRY, table.getInt(entriesStart - Integer.BYTES));
        Assert.assertEquals(COLOR, table.getInt(table.capacity() - Integer.BYTES));
    }

    @Test
    public void rejectEntryCountBelowResourceIndex() {
        Assert.assertThrows(IllegalArgumentException.class, () -> writeApk(ACCENT_ID, "z", ACCENT_ENTRY_COUNT - 1));
    }

    @Test
    public void rejectResourceIdWithoutType() {
        Assert.assertThrows(IllegalArgumentException.class, () -> writeApk(ID_WITHOUT_TYPE, "z", ACCENT_ENTRY_COUNT));
    }

    @Test
    public void writeManifestAsOneXmlDocument() throws IOException {
        ByteBuffer manifest = entry(writeApk(ACCENT_ID, "z", ACCENT_ENTRY_COUNT), MANIFEST_ENTRY);

        Assert.assertEquals(XML_CHUNK, manifest.getShort(0));
        Assert.assertEquals(manifest.capacity(), manifest.getInt(CHUNK_SIZE_OFFSET));
    }

    @Test
    public void writeApkWithStoredTableFirst() throws IOException {
        File target = writeApk(ACCENT_ID, "z", ACCENT_ENTRY_COUNT);

        List<String> names = new ArrayList<>();
        try (ZipFile zip = new ZipFile(target)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                Assert.assertEquals(ZipEntry.STORED, entry.getMethod());
                names.add(entry.getName());
            }
        }
        Assert.assertEquals(List.of(TABLE_ENTRY, MANIFEST_ENTRY), names);
        Assert.assertArrayEquals(new String[] { APK_NAME }, this.folder.getRoot().list());
    }

    private File writeApk(int resourceId, String entryName, int entryCount) throws IOException {
        File target = new File(this.folder.getRoot(), APK_NAME);
        AccentResourceTable.writeApk(target, PACKAGE_NAME, resourceId, "color", entryName, entryCount, COLOR);
        return target;
    }

    private static ByteBuffer entry(File apk, String name) throws IOException {
        try (ZipFile zip = new ZipFile(apk); InputStream input = zip.getInputStream(zip.getEntry(name))) {
            return ByteBuffer.wrap(input.readAllBytes()).order(ByteOrder.LITTLE_ENDIAN);
        }
    }

    private static int chunk(ByteBuffer table, short type) {
        int offset = TABLE_HEADER_SIZE;
        while (offset < table.capacity()) {
            if (table.getShort(offset) == type) {
                return offset;
            }
            boolean container = table.getShort(offset) == PACKAGE_CHUNK;
            offset += container ? table.getShort(offset + CHUNK_HEADER_SIZE_OFFSET)
                    : table.getInt(offset + CHUNK_SIZE_OFFSET);
        }
        throw new AssertionError("No chunk of type " + type);
    }

}
