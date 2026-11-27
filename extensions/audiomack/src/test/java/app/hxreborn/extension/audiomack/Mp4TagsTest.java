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
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public final class Mp4TagsTest {

    private static final byte[] AUDIO = "audio-frames".getBytes(StandardCharsets.US_ASCII);

    private static final SongTags TAGS = new SongTags("1", "Thanks and Praise", "Evang. John Okah", "Praise", null);

    private static final byte[] COVER = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 7, 8, 9 };

    private static final int JPEG_DATA_TYPE = 13;

    private static final int DATA_TYPE_AFTER_ITEM_TYPE = 12;

    private static final int PAYLOAD_AFTER_ITEM_TYPE = 20;

    private static final int HEADER_SIZE = 8;

    private static final int FIRST_CHUNK_OFFSET_AFTER_TYPE = 12;

    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void shiftsChunkOffsetsWhenTheMovieBoxPrecedesTheAudio() throws IOException {
        // given
        final File source = mp4(true);

        // when
        final byte[] tagged = tagged(source);

        // then
        final int audio = indexOf(tagged, AUDIO);
        Assert.assertEquals(audio, chunkOffset(tagged));
    }

    @Test
    public void keepsChunkOffsetsWhenTheAudioPrecedesTheMovieBox() throws IOException {
        // given
        final File source = mp4(false);

        // when
        final byte[] tagged = tagged(source);

        // then
        Assert.assertEquals(indexOf(tagged, AUDIO), chunkOffset(tagged));
    }

    @Test
    public void writesTitleArtistAndAlbumItems() throws IOException {
        // given
        final File source = mp4(true);

        // when
        final String tagged = new String(tagged(source), StandardCharsets.UTF_8);

        // then
        Assert.assertTrue(tagged.contains("Thanks and Praise"));
        Assert.assertTrue(tagged.contains("Evang. John Okah"));
        Assert.assertTrue(tagged.contains("Praise"));
    }

    @Test
    public void writesTheCoverAsAJpegItem() throws IOException {
        // given
        final File source = mp4(true);
        final SongTags tags = new SongTags("1", "Thanks and Praise", "Evang. John Okah", "Praise", COVER);

        // when
        final byte[] tagged = tagged(source, tags);

        // then
        final int coverTypeOffset = indexOf(tagged, "covr".getBytes(StandardCharsets.US_ASCII));
        final ByteBuffer file = ByteBuffer.wrap(tagged);
        Assert.assertEquals(JPEG_DATA_TYPE, file.getInt(coverTypeOffset + DATA_TYPE_AFTER_ITEM_TYPE));
        Assert.assertEquals(coverTypeOffset + PAYLOAD_AFTER_ITEM_TYPE, indexOf(tagged, COVER));
        Assert.assertEquals(indexOf(tagged, AUDIO), chunkOffset(tagged));
    }

    @Test
    public void replacesTheExistingMetadataBox() throws IOException {
        // given
        final File source = mp4(true);

        // when
        final String tagged = new String(tagged(source), StandardCharsets.ISO_8859_1);

        // then
        Assert.assertFalse(tagged.contains("Lavf"));
        Assert.assertEquals(tagged.indexOf("meta"), tagged.lastIndexOf("meta"));
    }

    @Test
    public void keepsTheAudioBytes() throws IOException {
        // given
        final File source = mp4(true);

        // when
        final byte[] tagged = tagged(source);

        // then
        Assert.assertTrue(indexOf(tagged, AUDIO) > 0);
    }

    @Test
    public void rejectsAFileWithoutAMovieBox() throws IOException {
        // given
        final File source = this.folder.newFile();
        Files.write(source.toPath(), box("mdat", AUDIO));

        // when
        final IOException failure = Assert.assertThrows(IOException.class, () -> tagged(source));

        // then
        Assert.assertEquals("No readable movie box", failure.getMessage());
    }

    private File mp4(boolean movieFirst) throws IOException {
        final byte[] type = box("ftyp", "isom".getBytes(StandardCharsets.US_ASCII));
        final byte[] media = box("mdat", AUDIO);
        final byte[] encoder = box("udta", box("meta", "Lavf59".getBytes(StandardCharsets.US_ASCII)));
        final int movieSize = box("moov", concat(trak(0), encoder)).length;
        final int audio = movieFirst ? type.length + movieSize + HEADER_SIZE : type.length + HEADER_SIZE;
        final byte[] movie = box("moov", concat(trak(audio), encoder));
        final ByteArrayOutputStream file = new ByteArrayOutputStream();
        file.write(type);
        file.write(movieFirst ? movie : media);
        file.write(movieFirst ? media : movie);
        final File source = this.folder.newFile();
        Files.write(source.toPath(), file.toByteArray());
        return source;
    }

    private static byte[] trak(int chunkOffset) throws IOException {
        final ByteArrayOutputStream table = new ByteArrayOutputStream();
        final DataOutputStream out = new DataOutputStream(table);
        out.writeInt(0);
        out.writeInt(1);
        out.writeInt(chunkOffset);
        return box("trak", box("mdia", box("minf", box("stbl", box("stco", table.toByteArray())))));
    }

    private static int chunkOffset(byte[] file) {
        final int table = indexOf(file, "stco".getBytes(StandardCharsets.US_ASCII));
        return ByteBuffer.wrap(file).getInt(table + FIRST_CHUNK_OFFSET_AFTER_TYPE);
    }

    private static byte[] tagged(File source) throws IOException {
        return tagged(source, TAGS);
    }

    private static byte[] tagged(File source, SongTags tags) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        Mp4Tags.write(source, out, tags);
        return out.toByteArray();
    }

    private static byte[] box(String type, byte[] content) throws IOException {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        final DataOutputStream out = new DataOutputStream(bytes);
        out.writeInt(HEADER_SIZE + content.length);
        out.write(type.getBytes(StandardCharsets.US_ASCII));
        out.write(content);
        return bytes.toByteArray();
    }

    private static byte[] concat(byte[] first, byte[] second) {
        final byte[] joined = new byte[first.length + second.length];
        System.arraycopy(first, 0, joined, 0, first.length);
        System.arraycopy(second, 0, joined, first.length, second.length);
        return joined;
    }

    private static int indexOf(byte[] bytes, byte[] needle) {
        for (int start = 0; start <= bytes.length - needle.length; start++) {
            boolean found = true;
            for (int index = 0; index < needle.length && found; index++) {
                found = bytes[start + index] == needle[index];
            }
            if (found) {
                return start;
            }
        }
        return -1;
    }

}
