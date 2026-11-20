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
package hx.mp4;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class Mp4Boxes {

    public static final int HEADER_BYTES = 8;

    public static final int FULL_HEADER_BYTES = 12;

    public static final int U32_BYTES = 4;

    private static final int TYPE_AT = 4;

    private static final int VERSION_AT = 8;

    private Mp4Boxes() {
    }

    static Box child(byte[] data, int from, int end, String type) throws IOException {
        int at = from;
        while (at + HEADER_BYTES <= end) {
            int size = size(data, at);
            if ((long) at + size > end) {
                throw new IOException("box " + typeAt(data, at) + " overruns its parent");
            }
            if (type.equals(typeAt(data, at))) {
                return new Box(at, size);
            }
            at += size;
        }
        throw new IOException("missing " + type + " box");
    }

    static Box childOf(byte[] data, Box parent, String type) throws IOException {
        return child(data, parent.offset + HEADER_BYTES, parent.end(), type);
    }

    public static int size(byte[] data, int at) throws IOException {
        long size = u32(data, at);
        if (size < HEADER_BYTES || size > Integer.MAX_VALUE) {
            throw new IOException("unsupported box size " + size);
        }
        return (int) size;
    }

    public static String typeAt(byte[] data, int at) {
        return new String(data, at + TYPE_AT, TYPE_AT, StandardCharsets.ISO_8859_1);
    }

    static int versionAt(byte[] data, int boxOffset) {
        return data[boxOffset + VERSION_AT];
    }

    static long u32(byte[] data, int at) {
        return ((data[at] & 0xFFL) << 24) | ((data[at + 1] & 0xFFL) << 16) | ((data[at + 2] & 0xFFL) << 8)
                | (data[at + 3] & 0xFFL);
    }

    static void putU32(byte[] data, int at, long value) {
        data[at] = (byte) (value >>> 24);
        data[at + 1] = (byte) (value >>> 16);
        data[at + 2] = (byte) (value >>> 8);
        data[at + 3] = (byte) value;
    }

    static void putU16(byte[] data, int at, int value) {
        data[at] = (byte) (value >>> 8);
        data[at + 1] = (byte) value;
    }

    static void putDuration(byte[] data, int at, int version, long value) {
        if (version == 1) {
            putU32(data, at, value >>> Integer.SIZE);
            putU32(data, at + U32_BYTES, value);
        } else {
            putU32(data, at, value);
        }
    }

    static byte[] slice(byte[] data, Box box) {
        byte[] out = new byte[box.size];
        System.arraycopy(data, box.offset, out, 0, box.size);
        return out;
    }

    static byte[] box(String type, byte[] payload) {
        byte[] out = new byte[HEADER_BYTES + payload.length];
        putU32(out, 0, out.length);
        System.arraycopy(type.getBytes(StandardCharsets.ISO_8859_1), 0, out, TYPE_AT, TYPE_AT);
        System.arraycopy(payload, 0, out, HEADER_BYTES, payload.length);
        return out;
    }

    static byte[] concat(byte[]... parts) {
        int total = 0;
        for (byte[] part : parts) {
            total += part.length;
        }
        byte[] out = new byte[total];
        int at = 0;
        for (byte[] part : parts) {
            System.arraycopy(part, 0, out, at, part.length);
            at += part.length;
        }
        return out;
    }

    static final class Box {

        final int offset;

        final int size;

        Box(int offset, int size) {
            this.offset = offset;
            this.size = size;
        }

        int end() {
            return this.offset + this.size;
        }

    }

}
