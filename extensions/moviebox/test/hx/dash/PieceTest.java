/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package hx.dash;

import hx.mp4.Segment;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public final class PieceTest {

    private static final int PREFIX = 100;

    private static final int ID_IN_PAYLOAD = 40;

    @Test
    public void measuresThePayloadWithoutTheIndexPrefix() {
        // when
        Piece piece = piece(2);

        // then
        Assertions.assertThat(piece.length).isEqualTo(1000);
        Assertions.assertThat(piece.prefixBytes).isEqualTo(PREFIX);
        Assertions.assertThat(piece.offset).isEqualTo(5000);
    }

    @Test
    public void writesTheTrackIdOverTheTrackHeaderField() {
        // given
        byte[] bytes = new byte[100];

        // when
        piece(0x01020304).patchTrackId(bytes, 0);

        // then
        Assertions.assertThat(java.util.Arrays.copyOfRange(bytes, 40, 44)).isEqualTo(new byte[] { 1, 2, 3, 4 });
        Assertions.assertThat(bytes[39]).isEqualTo((byte) 0);
        Assertions.assertThat(bytes[44]).isEqualTo((byte) 0);
    }

    @Test
    public void writesTheTailOfTheIdWhenTheReadStartsInsideIt() {
        // given
        byte[] bytes = new byte[10];

        // when
        piece(0x01020304).patchTrackId(bytes, ID_IN_PAYLOAD + 2);

        // then
        Assertions.assertThat(java.util.Arrays.copyOfRange(bytes, 0, 3)).isEqualTo(new byte[] { 3, 4, 0 });
    }

    @Test
    public void writesTheHeadOfTheIdWhenTheReadEndsInsideIt() {
        // given
        byte[] bytes = new byte[ID_IN_PAYLOAD + 2];

        // when
        piece(0x01020304).patchTrackId(bytes, 0);

        // then
        Assertions.assertThat(java.util.Arrays.copyOfRange(bytes, ID_IN_PAYLOAD, ID_IN_PAYLOAD + 2))
            .isEqualTo(new byte[] { 1, 2 });
    }

    @Test
    public void leavesAReadThatMissesTheIdAlone() {
        // given
        byte[] bytes = new byte[10];

        // when
        piece(0x01020304).patchTrackId(bytes, 500);

        // then
        Assertions.assertThat(bytes).isEqualTo(new byte[10]);
    }

    private static Piece piece(int trackId) {
        Segment segment = new Segment("https://cdn.example.com/s.m4s", 0, 1000);
        segment.byteLength = 1100;
        segment.prefixBytes = PREFIX;
        segment.trackIdOffset = PREFIX + ID_IN_PAYLOAD;
        return new Piece(5000, segment, trackId);
    }

}
