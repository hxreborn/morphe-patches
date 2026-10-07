/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.protonmail;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import app.hxreborn.extension.proton.PatchedBuild;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
public final class WebContentBackgroundTest {

    @Before
    public void useApplicationContext() {
        PatchedBuild.useApplicationContext();
    }

    private static byte[] ascii(String text) {
        return text.getBytes(StandardCharsets.US_ASCII);
    }

    private static InputStream replacing(byte[] content) {
        return WebContentBackground.replaceBackground(new ByteArrayInputStream(content));
    }

    private static String replaced(String text) throws IOException {
        return new String(readAll(replacing(ascii(text))), StandardCharsets.US_ASCII);
    }

    private static byte[] readAll(InputStream input) throws IOException {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        final byte[] buffer = new byte[7];
        for (int count; (count = input.read(buffer, 0, buffer.length)) != -1;) {
            output.write(buffer, 0, count);
        }
        return output.toByteArray();
    }

    private static final class CountingInput extends InputStream {

        private final InputStream delegate;

        int reads;

        boolean closed;

        CountingInput(byte[] content) {
            this.delegate = new ByteArrayInputStream(content);
        }

        @Override
        public int read() throws IOException {
            this.reads++;
            return this.delegate.read();
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            this.reads++;
            return this.delegate.read(buffer, offset, length);
        }

        @Override
        public void close() {
            this.closed = true;
        }

    }

    @Test
    public void unpatchedBuildReturnsTheSameHtml() {
        PatchedBuild.setAmoledEnabled(true);

        final String html = "<body style=\"background:#191927\"></body>";
        assertSame(html, WebContentBackground.replaceBackground(html));
        assertNull(WebContentBackground.replaceBackground((String) null));
    }

    @Test
    public void unpatchedBuildReturnsTheSameStream() {
        PatchedBuild.setAmoledEnabled(true);

        final InputStream input = new ByteArrayInputStream(ascii("#191927"));
        assertSame(input, WebContentBackground.replaceBackground(input));
        assertNull(WebContentBackground.replaceBackground((InputStream) null));
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void disabledThemeReturnsTheInputsUnchanged() {
        PatchedBuild.setAmoledEnabled(false);

        final String html = "<body style=\"background:#191927\"></body>";
        final InputStream input = new ByteArrayInputStream(ascii("#191927"));
        assertSame(html, WebContentBackground.replaceBackground(html));
        assertSame(input, WebContentBackground.replaceBackground(input));
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void htmlHasTheProtonDarkBackgroundReplacedWithBlack() {
        assertEquals("<body style=\"background:#000000\"></body>",
                WebContentBackground.replaceBackground("<body style=\"background:#191927\"></body>"));
        assertEquals("a{background:#000000}b{background:#000000}",
                WebContentBackground.replaceBackground("a{background:#191927}b{background:#191927}"));
        assertEquals("#000000ff", WebContentBackground.replaceBackground("#191927ff"));
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void htmlWithoutTheProtonDarkBackgroundIsKept() {
        for (String html : new String[] { "", "#000000", "#191928", "#181927", "#19192", "191927", "#FFFFFF" }) {
            assertEquals(html, WebContentBackground.replaceBackground(html));
        }
        assertNull(WebContentBackground.replaceBackground((String) null));
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void missingStreamStaysMissing() {
        assertNull(WebContentBackground.replaceBackground((InputStream) null));
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void patchedBuildWrapsTheStream() {
        // given
        final InputStream input = new ByteArrayInputStream(ascii("#191927"));

        // when
        final InputStream wrapped = WebContentBackground.replaceBackground(input);

        // then
        assertNotSame(input, wrapped);
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void replacesTheProtonDarkBackgroundWithBlack() throws IOException {
        assertEquals("body{background:#000000}", replaced("body{background:#191927}"));
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void replacesEveryOccurrence() throws IOException {
        assertEquals("a{background:#000000}b{background:#000000}c{border:#000000}",
                replaced("a{background:#191927}b{background:#191927}c{border:#191927}"));
        assertEquals("#000000#000000#000000", replaced("#191927#191927#191927"));
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void replacesAMatchAtEitherEnd() throws IOException {
        assertEquals("#000000", replaced("#191927"));
        assertEquals("#000000 x #000000", replaced("#191927 x #191927"));
        assertEquals("xx#000000", replaced("xx#191927"));
        assertEquals("#000000xx", replaced("#191927xx"));
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void leavesNonMatchingColorsAlone() throws IOException {
        for (String text : new String[] { "", "#000000", "#191928", "#181927", "#19192", "191927", " #19 1927",
                "#FFFFFF" }) {
            assertEquals(text, replaced(text));
        }
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void keepsTheAlphaDigitsOfAnEightDigitColor() throws IOException {
        assertEquals("#000000ff", replaced("#191927ff"));
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void preservesNonAsciiBytes() throws IOException {
        // given
        final byte[] content = new byte[300];
        for (int index = 0; index < content.length; index++) {
            content[index] = (byte) (index * 7);
        }
        final byte[] expected = content.clone();
        System.arraycopy(ascii("#191927"), 0, content, 100, 7);
        System.arraycopy(ascii("#000000"), 0, expected, 100, 7);

        // when
        final byte[] actual = readAll(replacing(content));

        // then
        assertArrayEquals(expected, actual);
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void handlesAMatchStraddlingTheInternalReadBuffer() throws IOException {
        // given
        final byte[] content = new byte[3 * 8192];
        Arrays.fill(content, (byte) 'x');
        final byte[] expected = content.clone();
        for (int offset : new int[] { 8190, 16381, 0, content.length - 7 }) {
            System.arraycopy(ascii("#191927"), 0, content, offset, 7);
            System.arraycopy(ascii("#000000"), 0, expected, offset, 7);
        }

        // when
        final byte[] actual = readAll(replacing(content));

        // then
        assertArrayEquals(expected, actual);
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void singleByteReadsReturnUnsignedValuesThenEndOfStream() throws IOException {
        final InputStream stream = replacing(new byte[] { 'a', (byte) 0xFF, (byte) 0x80 });

        assertEquals('a', stream.read());
        assertEquals(0xFF, stream.read());
        assertEquals(0x80, stream.read());
        assertEquals(-1, stream.read());
        assertEquals(-1, stream.read());
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void emptySourceIsAnEmptyStream() throws IOException {
        final InputStream stream = replacing(new byte[0]);

        assertEquals(0, stream.available());
        assertEquals(-1, stream.read());
        assertEquals(-1, stream.read(new byte[4], 0, 4));
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void streamPositionFollowsTheReplacedContent() throws IOException {
        final InputStream stream = replacing(ascii("ab#191927cd"));

        assertEquals(11, stream.available());
        assertEquals(2, stream.skip(2));
        assertEquals('#', stream.read());
        assertEquals('0', stream.read());
        assertEquals(7, stream.available());
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void sourceIsNotTouchedUntilTheFirstRead() throws IOException {
        final CountingInput source = new CountingInput(ascii("#191927"));
        final InputStream stream = WebContentBackground.replaceBackground(source);

        assertEquals(0, source.reads);
        assertEquals('#', stream.read());
        assertTrue(source.reads > 0);
        final int readsAfterFirst = source.reads;
        stream.read();
        stream.read(new byte[3], 0, 3);
        assertEquals(readsAfterFirst, source.reads);
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void closingClosesTheSourceWithoutReadingIt() throws IOException {
        // given
        final CountingInput source = new CountingInput(ascii("#191927"));
        final InputStream stream = WebContentBackground.replaceBackground(source);

        // when
        stream.close();

        // then
        assertTrue(source.closed);
        assertEquals(0, source.reads);
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void sourceFailureSurfacesOnFirstRead() {
        // given
        final IOException failure = new IOException("reset");
        final InputStream broken = new InputStream() {

            @Override
            public int read() throws IOException {
                throw failure;
            }

        };

        // when
        try {
            WebContentBackground.replaceBackground(broken).read();
            fail("expected the source failure");
        } catch (IOException thrown) {
            assertSame(failure, thrown);
        }
    }

}
