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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import android.content.Context;
import android.util.Log;

@SuppressWarnings("unused")
public final class ReportBundle {

    private static final String TAG = "hxClassDojo";

    private static final long HERMES_MAGIC = 0x1f1903c103bc1fc6L;

    private static final int HERMES_VERSION = 98;

    private static final int VERSION_OFFSET = 8;

    private static final int FILE_LENGTH_OFFSET = 32;

    private static final int FUNCTION_COUNT_OFFSET = 40;

    private static final int STRING_KIND_COUNT_OFFSET = 44;

    private static final int IDENTIFIER_COUNT_OFFSET = 48;

    private static final int STRING_COUNT_OFFSET = 52;

    private static final int OVERFLOW_COUNT_OFFSET = 56;

    private static final int STRING_STORAGE_SIZE_OFFSET = 60;

    private static final int HEADER_LENGTH = 128;

    private static final int FUNCTION_HEADER_LENGTH = 12;

    private static final int WORD_LENGTH = 4;

    private static final int OVERFLOW_ENTRY_LENGTH = 8;

    private static final int FOOTER_LENGTH = 20;

    private static final int STRING_LENGTH_SHIFT = 24;

    private static final int STRING_OFFSET_MASK = 0x7fffff;

    private static final int NON_PLUS_CONTENT_ID = 30782;

    private static final int PLUS_CONTENT_ID = 30783;

    private static final int HEX_RADIX = 16;

    private ReportBundle() {
    }

    public static synchronized String prepare(Context context, String path, String originalHex, String replacementHex) {
        if (path == null || path.startsWith("assets://")) {
            return path;
        }
        try (RandomAccessFile input = new RandomAccessFile(path, "r")) {
            long length = input.length();
            if (length < HEADER_LENGTH + FOOTER_LENGTH || length > Integer.MAX_VALUE) {
                Log.w(TAG, "Unsupported report bundle size");
                return path;
            }
            byte[] bytes = new byte[(int) length];
            input.readFully(bytes);
            ByteBuffer header = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
            if (header.getLong(0) != HERMES_MAGIC || header.getInt(VERSION_OFFSET) != HERMES_VERSION
                    || unsigned(header, FILE_LENGTH_OFFSET) != length
                    || !hasString(header, NON_PLUS_CONTENT_ID, "NonPlusContent")
                    || !hasString(header, PLUS_CONTENT_ID, "PlusContent")) {
                Log.w(TAG, "Unsupported report bundle layout");
                return path;
            }
            int footer = bytes.length - FOOTER_LENGTH;
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update(bytes, 0, footer);
            if (!Arrays.equals(digest.digest(), Arrays.copyOfRange(bytes, footer, bytes.length))) {
                Log.w(TAG, "Report bundle footer mismatch");
                return path;
            }
            byte[] before = decodeHex(originalHex);
            byte[] after = decodeHex(replacementHex);
            int offset = indexOf(bytes, before, HEADER_LENGTH, footer);
            if (before.length != after.length || offset < 0 || indexOf(bytes, before, offset + 1, footer) >= 0) {
                Log.w(TAG, "Report bundle pattern does not match exactly once");
                return path;
            }
            System.arraycopy(after, 0, bytes, offset, after.length);
            digest.update(bytes, 0, footer);
            System.arraycopy(digest.digest(), 0, bytes, footer, FOOTER_LENGTH);
            File output = new File(context.getCodeCacheDir(), "classdojo-reports.bundle");
            File partial = new File(context.getCodeCacheDir(), "classdojo-reports.bundle.tmp");
            try {
                try (FileOutputStream stream = new FileOutputStream(partial)) {
                    stream.write(bytes);
                    stream.getFD().sync();
                }
                if (!partial.renameTo(output)) {
                    Log.w(TAG, "Report bundle publication failed");
                    return path;
                }
                return output.getAbsolutePath();
            } finally {
                if (partial.exists() && !partial.delete()) {
                    Log.w(TAG, "Report bundle temporary file removal failed");
                }
            }
        } catch (IOException | NoSuchAlgorithmException ex) {
            Log.w(TAG, "Report bundle preparation failed");
            return path;
        }
    }

    private static long unsigned(ByteBuffer bytes, int offset) {
        return bytes.getInt(offset) & 0xffffffffL;
    }

    private static boolean hasString(ByteBuffer bytes, int id, String expected) {
        long count = unsigned(bytes, STRING_COUNT_OFFSET);
        if (id >= count) {
            return false;
        }
        long table = HEADER_LENGTH + unsigned(bytes, FUNCTION_COUNT_OFFSET) * FUNCTION_HEADER_LENGTH
                + unsigned(bytes, STRING_KIND_COUNT_OFFSET) * WORD_LENGTH
                + unsigned(bytes, IDENTIFIER_COUNT_OFFSET) * WORD_LENGTH;
        long storage = table + count * WORD_LENGTH + unsigned(bytes, OVERFLOW_COUNT_OFFSET) * OVERFLOW_ENTRY_LENGTH;
        long end = storage + unsigned(bytes, STRING_STORAGE_SIZE_OFFSET);
        if (end > bytes.limit() - FOOTER_LENGTH || table + ((long) id + 1) * WORD_LENGTH > storage) {
            return false;
        }
        int entry = bytes.getInt((int) (table + (long) id * WORD_LENGTH));
        int size = entry >>> STRING_LENGTH_SHIFT;
        long start = storage + ((entry >>> 1) & STRING_OFFSET_MASK);
        byte[] text = expected.getBytes(StandardCharsets.US_ASCII);
        if ((entry & 1) != 0 || size != text.length || start + size > end) {
            return false;
        }
        for (int i = 0; i < size; i++) {
            if (bytes.get((int) start + i) != text[i]) {
                return false;
            }
        }
        return true;
    }

    private static byte[] decodeHex(String hex) {
        String[] parts = hex.split(" ");
        byte[] bytes = new byte[parts.length];
        for (int i = 0; i < parts.length; i++) {
            bytes[i] = (byte) Integer.parseInt(parts[i], HEX_RADIX);
        }
        return bytes;
    }

    private static int indexOf(byte[] bytes, byte[] pattern, int from, int end) {
        for (int i = from; i <= end - pattern.length; i++) {
            int offset = 0;
            while (offset < pattern.length && bytes[i + offset] == pattern[offset]) {
                offset++;
            }
            if (offset == pattern.length) {
                return i;
            }
        }
        return -1;
    }

}
