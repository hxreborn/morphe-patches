/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package hx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class RangeHeaderTest {

    @Test
    public void readsAnOpenEndedRangeToTheLastByte() {
        // given
        RangeHeader header = RangeHeader.parse("bytes=5-");

        // when
        ByteRange range = header.within(100);

        // then
        assertEquals(5, range.start);
        assertEquals(99, range.end);
    }

    @Test
    public void readsAClosedRange() {
        // given
        RangeHeader header = RangeHeader.parse("bytes=10-19");

        // when
        ByteRange range = header.within(100);

        // then
        assertEquals(10, range.start);
        assertEquals(19, range.end);
        assertEquals(10, range.size());
    }

    @Test
    public void clipsTheEndToTheLength() {
        // given
        RangeHeader header = RangeHeader.parse("bytes=90-500");

        // when
        ByteRange range = header.within(100);

        // then
        assertEquals(99, range.end);
    }

    @Test
    public void marksARangeBeyondTheLengthAsUnsatisfiable() {
        // given
        RangeHeader header = RangeHeader.parse("bytes=200-");

        // when
        ByteRange range = header.within(100);

        // then
        assertFalse(range.isSatisfiable());
    }

    @Test
    public void marksAnEmptyFileAsUnsatisfiable() {
        // given
        RangeHeader header = RangeHeader.parse("bytes=0-");

        // when
        ByteRange range = header.within(0);

        // then
        assertFalse(range.isSatisfiable());
    }

    @Test
    public void remembersWhereTheRangeStarts() {
        // when
        RangeHeader header = RangeHeader.parse("bytes=1234-");

        // then
        assertEquals(1234, header.first);
    }

    @Test
    public void ignoresSuffixRanges() {
        // when
        RangeHeader header = RangeHeader.parse("bytes=-500");

        // then
        assertNull(header);
    }

    @Test
    public void ignoresMultipleRanges() {
        // when
        RangeHeader header = RangeHeader.parse("bytes=0-1,5-6");

        // then
        assertNull(header);
    }

    @Test
    public void ignoresAMissingHeader() {
        // when
        RangeHeader header = RangeHeader.parse(null);

        // then
        assertNull(header);
        assertTrue(new ByteRange(0, 9).isSatisfiable());
    }

}
