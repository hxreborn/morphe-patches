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
package app.hxreborn.extension.audiomack;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;

final class Id3Tags {

    private static final int HEADER_SIZE = 10;

    private static final int FOOTER_SIZE = 10;

    private static final int SIZE_OFFSET = 6;

    private static final int FLAGS_OFFSET = 5;

    private static final int FOOTER_FLAG = 0x10;

    private static final int SYNCSAFE_BITS = 7;

    private static final int SYNCSAFE_MASK = 0x7F;

    private static final int BYTE_MASK = 0xFF;

    private static final int VERSION_MAJOR = 3;

    private static final int UTF16_ENCODING = 1;

    private static final byte[] UTF16_LITTLE_ENDIAN_MARK = { (byte) 0xFF, (byte) 0xFE };

    private static final int COPY_BUFFER_SIZE = 64 * 1024;

    private static final byte[] MARKER = "ID3".getBytes(StandardCharsets.US_ASCII);

    private Id3Tags() {
    }

    static void write(File source, OutputStream out, SongTags tags) throws IOException {
        try (RandomAccessFile in = new RandomAccessFile(source, "r")) {
            out.write(tag(tags));
            in.seek(existingTagSize(in));
            final byte[] buffer = new byte[COPY_BUFFER_SIZE];
            int read;
            while ((read = in.read(buffer)) >= 0) {
                out.write(buffer, 0, read);
            }
        }
    }

    static byte[] tag(SongTags tags) throws IOException {
        final ByteArrayOutputStream frames = new ByteArrayOutputStream();
        writeFrame(frames, "TIT2", tags.title);
        writeFrame(frames, "TPE1", tags.artist);
        writeFrame(frames, "TALB", tags.album);
        final ByteArrayOutputStream tag = new ByteArrayOutputStream();
        tag.write(MARKER);
        tag.write(VERSION_MAJOR);
        tag.write(0);
        tag.write(0);
        final int size = frames.size();
        for (int shift = 3 * SYNCSAFE_BITS; shift >= 0; shift -= SYNCSAFE_BITS) {
            tag.write((size >> shift) & SYNCSAFE_MASK);
        }
        frames.writeTo(tag);
        return tag.toByteArray();
    }

    private static long existingTagSize(RandomAccessFile in) throws IOException {
        if (in.length() < HEADER_SIZE) {
            return 0;
        }
        final byte[] header = new byte[HEADER_SIZE];
        in.seek(0);
        in.readFully(header);
        for (int index = 0; index < MARKER.length; index++) {
            if (header[index] != MARKER[index]) {
                return 0;
            }
        }
        long size = 0;
        for (int index = SIZE_OFFSET; index < HEADER_SIZE; index++) {
            size = (size << SYNCSAFE_BITS) | (header[index] & SYNCSAFE_MASK);
        }
        final boolean footer = (header[FLAGS_OFFSET] & BYTE_MASK & FOOTER_FLAG) != 0;
        return Math.min(in.length(), HEADER_SIZE + size + (footer ? FOOTER_SIZE : 0));
    }

    private static void writeFrame(ByteArrayOutputStream frames, String id, String value) throws IOException {
        if (value.isEmpty()) {
            return;
        }
        final byte[] text = value.getBytes(StandardCharsets.UTF_16LE);
        final DataOutputStream out = new DataOutputStream(frames);
        out.write(id.getBytes(StandardCharsets.US_ASCII));
        out.writeInt(1 + UTF16_LITTLE_ENDIAN_MARK.length + text.length);
        out.writeShort(0);
        out.write(UTF16_ENCODING);
        out.write(UTF16_LITTLE_ENDIAN_MARK);
        out.write(text);
    }

}
