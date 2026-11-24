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
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class Mp4Tags {

    private static final int HEADER_SIZE = 8;

    private static final int INT_SIZE = 4;

    private static final int LONG_SIZE = 8;

    private static final int LARGE_HEADER_SIZE = 16;

    private static final int FULL_BOX_HEADER_SIZE = 4;

    private static final int EXTENDED_SIZE = 1;

    private static final int SIZE_TO_END = 0;

    private static final int MAX_MOVIE_BOX_SIZE = 64 * 1024 * 1024;

    private static final int UTF8_DATA_TYPE = 1;

    private static final int COPY_BUFFER_SIZE = 64 * 1024;

    private static final long UNSIGNED_INT_MASK = 0xFFFFFFFFL;

    private static final String MOVIE = "moov";

    private static final String MEDIA_DATA = "mdat";

    private static final String USER_DATA = "udta";

    private static final String METADATA = "meta";

    private static final String CHUNK_OFFSETS = "stco";

    private static final String LARGE_CHUNK_OFFSETS = "co64";

    private static final Set<String> CHUNK_OFFSET_PARENTS = new HashSet<>(
            Arrays.asList(MOVIE, "trak", "mdia", "minf", "stbl"));

    private static final byte[] METADATA_HANDLER = { 0, 0, 0, 0, 0, 0, 0, 0, 'm', 'd', 'i', 'r', 'a', 'p', 'p', 'l', 0,
            0, 0, 0, 0, 0, 0, 0, 0 };

    private Mp4Tags() {
    }

    static void write(File source, OutputStream out, SongTags tags) throws IOException {
        try (RandomAccessFile in = new RandomAccessFile(source, "r")) {
            final List<Mp4Box> boxes = topLevelBoxes(in);
            Mp4Box movie = null;
            long firstMediaData = Long.MAX_VALUE;
            for (final Mp4Box box : boxes) {
                if (MOVIE.equals(box.type) && movie == null) {
                    movie = box;
                }
                if (MEDIA_DATA.equals(box.type)) {
                    firstMediaData = Math.min(firstMediaData, box.offset);
                }
            }
            if (movie == null || movie.size > MAX_MOVIE_BOX_SIZE || movie.headerSize != HEADER_SIZE) {
                throw new IOException("No readable movie box");
            }
            final byte[] original = new byte[(int) movie.size];
            in.seek(movie.offset);
            in.readFully(original);
            final byte[] tagged = withTags(original, tags, movie.offset < firstMediaData);
            final byte[] buffer = new byte[COPY_BUFFER_SIZE];
            for (final Mp4Box box : boxes) {
                if (box == movie) {
                    out.write(tagged);
                } else {
                    copy(in, box.offset, box.size, out, buffer);
                }
            }
        }
    }

    static byte[] withTags(byte[] movie, SongTags tags, boolean mediaDataFollows) throws IOException {
        final ByteArrayOutputStream children = new ByteArrayOutputStream();
        byte[] userData = new byte[0];
        int offset = HEADER_SIZE;
        while (offset < movie.length) {
            final int size = childSize(movie, offset, movie.length);
            final String type = type(movie, offset);
            if (USER_DATA.equals(type)) {
                userData = userDataWithoutMetadata(movie, offset, size);
            } else {
                children.write(movie, offset, size);
            }
            offset += size;
        }
        children.write(box(USER_DATA, concat(userData, metadata(tags))));
        final byte[] result = box(MOVIE, children.toByteArray());
        if (mediaDataFollows) {
            shiftChunkOffsets(result, 0, result.length, result.length - movie.length);
        }
        return result;
    }

    private static byte[] userDataWithoutMetadata(byte[] movie, int start, int size) throws IOException {
        final ByteArrayOutputStream kept = new ByteArrayOutputStream();
        int offset = start + HEADER_SIZE;
        final int end = start + size;
        while (offset < end) {
            final int childSize = childSize(movie, offset, end);
            if (!METADATA.equals(type(movie, offset))) {
                kept.write(movie, offset, childSize);
            }
            offset += childSize;
        }
        return kept.toByteArray();
    }

    private static byte[] metadata(SongTags tags) throws IOException {
        final ByteArrayOutputStream items = new ByteArrayOutputStream();
        writeItem(items, "\u00A9nam", tags.title);
        writeItem(items, "\u00A9ART", tags.artist);
        writeItem(items, "\u00A9alb", tags.album);
        final byte[] handler = box("hdlr", METADATA_HANDLER);
        final byte[] list = box("ilst", items.toByteArray());
        return box(METADATA, concat(new byte[FULL_BOX_HEADER_SIZE], concat(handler, list)));
    }

    private static void writeItem(ByteArrayOutputStream items, String name, String value) throws IOException {
        if (value.isEmpty()) {
            return;
        }
        final ByteArrayOutputStream data = new ByteArrayOutputStream();
        final DataOutputStream fields = new DataOutputStream(data);
        fields.writeInt(UTF8_DATA_TYPE);
        fields.writeInt(0);
        fields.write(value.getBytes(StandardCharsets.UTF_8));
        items.write(box(name, box("data", data.toByteArray())));
    }

    private static void shiftChunkOffsets(byte[] bytes, int start, int end, int delta) throws IOException {
        int offset = start;
        while (offset < end) {
            final int size = childSize(bytes, offset, end);
            final String type = type(bytes, offset);
            if (CHUNK_OFFSET_PARENTS.contains(type)) {
                shiftChunkOffsets(bytes, offset + HEADER_SIZE, offset + size, delta);
            } else if (CHUNK_OFFSETS.equals(type)) {
                shiftTable(bytes, offset, size, delta, false);
            } else if (LARGE_CHUNK_OFFSETS.equals(type)) {
                shiftTable(bytes, offset, size, delta, true);
            }
            offset += size;
        }
    }

    private static void shiftTable(byte[] bytes, int offset, int size, int delta, boolean large) throws IOException {
        final ByteBuffer table = ByteBuffer.wrap(bytes, offset, size);
        final int countPosition = offset + HEADER_SIZE + FULL_BOX_HEADER_SIZE;
        final int count = table.getInt(countPosition);
        final int entrySize = large ? LONG_SIZE : INT_SIZE;
        int position = countPosition + INT_SIZE;
        if (count < 0 || (long) count * entrySize > offset + size - position) {
            throw new IOException("Chunk offset table is truncated");
        }
        for (int index = 0; index < count; index++) {
            if (large) {
                table.putLong(position, table.getLong(position) + delta);
            } else {
                table.putInt(position, (int) ((table.getInt(position) & UNSIGNED_INT_MASK) + delta));
            }
            position += entrySize;
        }
    }

    private static List<Mp4Box> topLevelBoxes(RandomAccessFile in) throws IOException {
        final List<Mp4Box> boxes = new ArrayList<>();
        final long length = in.length();
        long offset = 0;
        while (offset + HEADER_SIZE <= length) {
            in.seek(offset);
            long size = in.readInt() & UNSIGNED_INT_MASK;
            final byte[] type = new byte[INT_SIZE];
            in.readFully(type);
            int headerSize = HEADER_SIZE;
            if (size == EXTENDED_SIZE) {
                size = in.readLong();
                headerSize = LARGE_HEADER_SIZE;
            } else if (size == SIZE_TO_END) {
                size = length - offset;
            }
            if (size < headerSize || offset + size > length) {
                throw new IOException("Box size is out of range");
            }
            boxes.add(new Mp4Box(new String(type, StandardCharsets.ISO_8859_1), offset, size, headerSize));
            offset += size;
        }
        return boxes;
    }

    private static int childSize(byte[] bytes, int offset, int end) throws IOException {
        if (offset + HEADER_SIZE > end) {
            throw new IOException("Box header is truncated");
        }
        final long size = ByteBuffer.wrap(bytes).getInt(offset) & UNSIGNED_INT_MASK;
        if (size < HEADER_SIZE || offset + size > end) {
            throw new IOException("Box size is out of range");
        }
        return (int) size;
    }

    private static String type(byte[] bytes, int offset) {
        return new String(bytes, offset + INT_SIZE, INT_SIZE, StandardCharsets.ISO_8859_1);
    }

    private static byte[] box(String type, byte[] content) throws IOException {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream(HEADER_SIZE + content.length);
        final DataOutputStream out = new DataOutputStream(bytes);
        out.writeInt(HEADER_SIZE + content.length);
        out.write(type.getBytes(StandardCharsets.ISO_8859_1));
        out.write(content);
        return bytes.toByteArray();
    }

    private static byte[] concat(byte[] first, byte[] second) {
        final byte[] joined = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, joined, first.length, second.length);
        return joined;
    }

    private static void copy(RandomAccessFile in, long offset, long size, OutputStream out, byte[] buffer)
            throws IOException {
        in.seek(offset);
        long remaining = size;
        while (remaining > 0) {
            final int read = in.read(buffer, 0, (int) Math.min(buffer.length, remaining));
            if (read < 0) {
                throw new IOException("Audio file ended early");
            }
            out.write(buffer, 0, read);
            remaining -= read;
        }
    }

}
