/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package hx;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

public final class Mp4Fixtures {

    private Mp4Fixtures() {
    }

    public static byte[] box(String type, byte[]... children) {
        return Mp4Boxes.box(type, Mp4Boxes.concat(children));
    }

    public static byte[] fullBox(String type, int version, byte[]... children) {
        return box(type, Mp4Boxes.concat(new byte[] { (byte) version, 0, 0, 0 }, Mp4Boxes.concat(children)));
    }

    public static byte[] zeros(int count) {
        return new byte[count];
    }

    public static byte[] u32(long... values) {
        byte[] out = new byte[values.length * 4];
        for (int i = 0; i < values.length; i++) {
            Mp4Boxes.putU32(out, i * 4, values[i]);
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
        return Mp4Boxes.concat(ftyp, box("moov", mvhd, trak, box("mvex", trex)));
    }

    public static byte[] mediaSegment(int trackId, byte[] payload) {
        byte[] mfhd = fullBox("mfhd", 0, u32(1));
        byte[] tfhd = fullBox("tfhd", 0, u32(trackId));
        byte[] moof = box("moof", mfhd, box("traf", tfhd));
        return Mp4Boxes.concat(moof, box("mdat", payload));
    }

    public static byte[] bytes(int length, int seed) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int i = 0; i < length; i++) {
            out.write((seed + i) & 0x7F);
        }
        return out.toByteArray();
    }

}
