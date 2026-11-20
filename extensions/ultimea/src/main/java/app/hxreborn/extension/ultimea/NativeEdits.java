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

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.Arrays;

final class NativeEdits {

    private static final int MAX_EDIT_BYTES = 4096;

    private NativeEdits() {
    }

    static void apply(InputStream source, RandomAccessFile library) throws IOException {
        try (DataInputStream edits = new DataInputStream(source)) {
            int count = edits.readInt();
            if (count < 1 || count > MAX_EDIT_BYTES) {
                throw new IOException("Invalid native settings edit count");
            }
            for (int index = 0; index < count; index++) {
                int offset = edits.readInt();
                int length = edits.readInt();
                if (offset < 0 || length < 1 || length > MAX_EDIT_BYTES || offset > library.length() - length) {
                    throw new IOException("Invalid native settings edit range");
                }
                byte[] before = new byte[length];
                byte[] after = new byte[length];
                byte[] actual = new byte[length];
                edits.readFully(before);
                edits.readFully(after);
                library.seek(offset);
                library.readFully(actual);
                if (!Arrays.equals(before, actual)) {
                    throw new IOException("Native settings original bytes do not match");
                }
                library.seek(offset);
                library.write(after);
            }
            if (edits.read() != -1) {
                throw new IOException("Unexpected native settings edit data");
            }
        }
    }

}
