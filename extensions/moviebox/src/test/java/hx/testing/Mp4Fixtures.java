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
package hx.testing;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

public final class Mp4Fixtures {

    private Mp4Fixtures() {
    }

    public static byte[] concat(byte[]... parts) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] part : parts) {
            out.write(part, 0, part.length);
        }
        return out.toByteArray();
    }

    public static void putU32(byte[] data, int at, long value) {
        data[at] = (byte) (value >>> 24);
        data[at + 1] = (byte) (value >>> 16);
        data[at + 2] = (byte) (value >>> 8);
        data[at + 3] = (byte) value;
    }

    public static long readU32(byte[] data, int at) {
        return ((data[at] & 0xFFL) << 24) | ((data[at + 1] & 0xFFL) << 16) | ((data[at + 2] & 0xFFL) << 8)
                | (data[at + 3] & 0xFFL);
    }

    public static int sizeAt(byte[] data, int at) {
        return (int) readU32(data, at);
    }

    public static String typeAt(byte[] data, int at) {
        return new String(data, at + 4, 4, StandardCharsets.ISO_8859_1);
    }

    public static byte[] box(String type, byte[]... children) {
        byte[] payload = concat(children);
        byte[] header = new byte[8];
        putU32(header, 0, payload.length + header.length);
        System.arraycopy(type.getBytes(StandardCharsets.ISO_8859_1), 0, header, 4, 4);
        return concat(header, payload);
    }

    public static byte[] fullBox(String type, int version, byte[]... children) {
        return box(type, concat(new byte[] { (byte) version, 0, 0, 0 }, concat(children)));
    }

    public static byte[] zeros(int count) {
        return new byte[count];
    }

    public static byte[] u32(long... values) {
        byte[] out = new byte[values.length * 4];
        for (int i = 0; i < values.length; i++) {
            putU32(out, i * 4, values[i]);
        }
        return out;
    }

    public static byte[] initSegment(int trackId, long movieTimescale, long mediaTimescale) {
        byte[] ftyp = box("ftyp", "iso6".getBytes(StandardCharsets.ISO_8859_1), zeros(4));
        byte[] mvhd = fullBox("mvhd", 0, u32(0, 0, movieTimescale, 0, 0x00010000), zeros(2 + 10 + 36 + 24),
                u32(trackId + 1));
        byte[] tkhd = fullBox("tkhd", 0, u32(0, 0, trackId, 0, 0), zeros(8 + 2 + 2 + 2 + 2 + 36 + 4 + 4));
        byte[] mdhd = fullBox("mdhd", 0, u32(0, 0, mediaTimescale, 0), zeros(2 + 2));
        byte[] trak = box("trak", tkhd, box("mdia", mdhd));
        byte[] trex = fullBox("trex", 0, u32(trackId, 1, 0, 0, 0));
        return concat(ftyp, box("moov", mvhd, trak, box("mvex", trex)));
    }

    public static byte[] mediaSegment(int trackId, byte[] payload) {
        byte[] mfhd = fullBox("mfhd", 0, u32(1));
        byte[] tfhd = fullBox("tfhd", 0, u32(trackId));
        byte[] moof = box("moof", mfhd, box("traf", tfhd));
        return concat(moof, box("mdat", payload));
    }

    public static byte[] bytes(int length, int seed) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int i = 0; i < length; i++) {
            out.write((seed + i) & 0x7F);
        }
        return out.toByteArray();
    }

}
