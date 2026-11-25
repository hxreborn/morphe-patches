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
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public final class Id3TagsTest {

    private static final byte[] AUDIO = { (byte) 0xFF, (byte) 0xFB, 1, 2, 3 };

    private static final byte[] OLD_TAG = { 'I', 'D', '3', 3, 0, 0, 0, 0, 0, 2, 'x', 'y' };

    private static final SongTags TAGS = new SongTags("1", "Corny", "Rema", "");

    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void replacesTheExistingTagAndKeepsTheAudio() throws IOException {
        // given
        final File source = this.folder.newFile();
        final ByteArrayOutputStream file = new ByteArrayOutputStream();
        file.write(OLD_TAG);
        file.write(AUDIO);
        Files.write(source.toPath(), file.toByteArray());

        // when
        final byte[] tagged = tagged(source);

        // then
        final byte[] expected = new byte[tagged.length - AUDIO.length];
        System.arraycopy(tagged, 0, expected, 0, expected.length);
        Assert.assertArrayEquals(Id3Tags.tag(TAGS), expected);
        Assert.assertArrayEquals(AUDIO, Arrays.copyOfRange(tagged, expected.length, tagged.length));
    }

    @Test
    public void writesTitleAndArtistFramesOnly() throws IOException {
        // when
        final String tag = new String(Id3Tags.tag(TAGS), StandardCharsets.ISO_8859_1);

        // then
        Assert.assertTrue(tag.contains("TIT2"));
        Assert.assertTrue(tag.contains("TPE1"));
        Assert.assertFalse(tag.contains("TALB"));
    }

    private static byte[] tagged(File source) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        Id3Tags.write(source, out, TAGS);
        return out.toByteArray();
    }

}
