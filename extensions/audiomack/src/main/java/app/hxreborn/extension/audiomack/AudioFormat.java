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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

enum AudioFormat {

    MP4(".m4a", "audio/mp4"), MP3(".mp3", "audio/mpeg");

    private static final int SIGNATURE_LENGTH = 12;

    private static final int MP4_TYPE_OFFSET = 4;

    private static final byte[] MP4_TYPE = "ftyp".getBytes(StandardCharsets.US_ASCII);

    private static final byte[] ID3_MARKER = "ID3".getBytes(StandardCharsets.US_ASCII);

    private static final int MPEG_SYNC = 0xFFE0;

    private static final int BYTE_MASK = 0xFF;

    private static final int BITS_PER_BYTE = 8;

    final String extension;

    final String mimeType;

    AudioFormat(String extension, String mimeType) {
        this.extension = extension;
        this.mimeType = mimeType;
    }

    static AudioFormat of(File file) throws IOException {
        final byte[] signature = new byte[SIGNATURE_LENGTH];
        try (InputStream in = new FileInputStream(file)) {
            int read = 0;
            while (read < signature.length) {
                final int count = in.read(signature, read, signature.length - read);
                if (count < 0) {
                    break;
                }
                read += count;
            }
            if (read < signature.length) {
                throw new IOException("Audio file is too short");
            }
        }
        if (startsWith(signature, MP4_TYPE_OFFSET, MP4_TYPE)) {
            return MP4;
        }
        final int sync = ((signature[0] & BYTE_MASK) << BITS_PER_BYTE) | (signature[1] & BYTE_MASK);
        if (startsWith(signature, 0, ID3_MARKER) || (sync & MPEG_SYNC) == MPEG_SYNC) {
            return MP3;
        }
        throw new IOException("Unknown audio format");
    }

    void writeTagged(File source, OutputStream out, SongTags tags) throws IOException {
        if (this == MP4) {
            Mp4Tags.write(source, out, tags);
        } else {
            Id3Tags.write(source, out, tags);
        }
    }

    private static boolean startsWith(byte[] bytes, int offset, byte[] prefix) {
        for (int index = 0; index < prefix.length; index++) {
            if (bytes[offset + index] != prefix[index]) {
                return false;
            }
        }
        return true;
    }

}
