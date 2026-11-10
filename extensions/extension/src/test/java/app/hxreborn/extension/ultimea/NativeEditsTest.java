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
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public final class NativeEditsTest {

    private static final byte[] ORIGINAL = { 12, 34, 56, 78 };

    @Rule
    public TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void replaceOnlyMatchedBytes() throws IOException {
        File file = library();
        try (RandomAccessFile library = new RandomAccessFile(file, "rw")) {
            NativeEdits.apply(new ByteArrayInputStream(table(1, new byte[] { 34, 56 }, new byte[] { 1, 2 })), library);
        }
        Assert.assertArrayEquals(new byte[] { 12, 1, 2, 78 }, Files.readAllBytes(file.toPath()));
    }

    @Test
    public void rejectMismatchedOriginalBytes() throws IOException {
        reject(table(1, new byte[] { 35 }, new byte[] { 1 }));
    }

    @Test
    public void rejectNegativeOffsets() throws IOException {
        reject(table(-1, new byte[] { 12 }, new byte[] { 1 }));
    }

    @Test
    public void rejectOffsetsBeyondLibrary() throws IOException {
        reject(table(Integer.MAX_VALUE, new byte[] { 12 }, new byte[] { 1 }));
    }

    @Test
    public void rejectTruncatedEdits() throws IOException {
        byte[] table = table(0, new byte[] { 12 }, new byte[] { 1 });
        reject(java.util.Arrays.copyOf(table, table.length - 1));
    }

    @Test
    public void rejectTrailingData() throws IOException {
        byte[] table = table(0, new byte[] { 12 }, new byte[] { 1 });
        byte[] trailing = java.util.Arrays.copyOf(table, table.length + 1);
        try (RandomAccessFile library = new RandomAccessFile(library(), "rw")) {
            Assert.assertThrows(IOException.class,
                    () -> NativeEdits.apply(new ByteArrayInputStream(trailing), library));
        }
    }

    private void reject(byte[] table) throws IOException {
        File file = library();
        try (RandomAccessFile library = new RandomAccessFile(file, "rw")) {
            Assert.assertThrows(IOException.class, () -> NativeEdits.apply(new ByteArrayInputStream(table), library));
        }
        Assert.assertArrayEquals(ORIGINAL, Files.readAllBytes(file.toPath()));
    }

    private File library() throws IOException {
        File file = this.temporary.newFile();
        Files.write(file.toPath(), ORIGINAL);
        return file;
    }

    private byte[] table(int offset, byte[] before, byte[] after) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(buffer)) {
            output.writeInt(1);
            output.writeInt(offset);
            output.writeInt(before.length);
            output.write(before);
            output.write(after);
        }
        return buffer.toByteArray();
    }

}
