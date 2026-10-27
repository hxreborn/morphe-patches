/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package hx;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class PieceTest {

    private static final int PREFIX = 100;

    private static final int ID_IN_PAYLOAD = 40;

    @Test
    public void measuresThePayloadWithoutTheIndexPrefix() {
        // when
        Piece piece = piece(2);

        // then
        assertEquals(1000, piece.length);
        assertEquals(PREFIX, piece.prefixBytes);
        assertEquals(5000, piece.offset);
    }

    @Test
    public void writesTheTrackIdOverTheTrackHeaderField() {
        // given
        byte[] bytes = new byte[100];

        // when
        piece(0x01020304).patchTrackId(bytes, 0);

        // then
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, java.util.Arrays.copyOfRange(bytes, 40, 44));
        assertEquals(0, bytes[39]);
        assertEquals(0, bytes[44]);
    }

    @Test
    public void writesTheTailOfTheIdWhenTheReadStartsInsideIt() {
        // given
        byte[] bytes = new byte[10];

        // when
        piece(0x01020304).patchTrackId(bytes, ID_IN_PAYLOAD + 2);

        // then
        assertArrayEquals(new byte[] { 3, 4, 0 }, java.util.Arrays.copyOfRange(bytes, 0, 3));
    }

    @Test
    public void writesTheHeadOfTheIdWhenTheReadEndsInsideIt() {
        // given
        byte[] bytes = new byte[ID_IN_PAYLOAD + 2];

        // when
        piece(0x01020304).patchTrackId(bytes, 0);

        // then
        assertArrayEquals(new byte[] { 1, 2 }, java.util.Arrays.copyOfRange(bytes, ID_IN_PAYLOAD, ID_IN_PAYLOAD + 2));
    }

    @Test
    public void leavesAReadThatMissesTheIdAlone() {
        // given
        byte[] bytes = new byte[10];

        // when
        piece(0x01020304).patchTrackId(bytes, 500);

        // then
        assertArrayEquals(new byte[10], bytes);
    }

    private static Piece piece(int trackId) {
        Segment segment = new Segment("https://cdn.example.com/s.m4s", 0, 1000);
        segment.byteLength = 1100;
        segment.prefixBytes = PREFIX;
        segment.trackIdOffset = PREFIX + ID_IN_PAYLOAD;
        return new Piece(5000, segment, trackId);
    }

}
