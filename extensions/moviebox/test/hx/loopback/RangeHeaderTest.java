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
package hx.loopback;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public final class RangeHeaderTest {

    @Test
    public void readsAnOpenEndedRangeToTheLastByte() {
        // given
        RangeHeader header = RangeHeader.parse("bytes=5-");

        // when
        ByteRange range = header.within(100);

        // then
        Assertions.assertThat(range.start).isEqualTo(5);
        Assertions.assertThat(range.end).isEqualTo(99);
    }

    @Test
    public void readsAClosedRange() {
        // given
        RangeHeader header = RangeHeader.parse("bytes=10-19");

        // when
        ByteRange range = header.within(100);

        // then
        Assertions.assertThat(range.start).isEqualTo(10);
        Assertions.assertThat(range.end).isEqualTo(19);
        Assertions.assertThat(range.size()).isEqualTo(10);
    }

    @Test
    public void clipsTheEndToTheLength() {
        // given
        RangeHeader header = RangeHeader.parse("bytes=90-500");

        // when
        ByteRange range = header.within(100);

        // then
        Assertions.assertThat(range.end).isEqualTo(99);
    }

    @Test
    public void marksARangeBeyondTheLengthAsUnsatisfiable() {
        // given
        RangeHeader header = RangeHeader.parse("bytes=200-");

        // when
        ByteRange range = header.within(100);

        // then
        Assertions.assertThat(range.isSatisfiable()).isFalse();
    }

    @Test
    public void marksAnEmptyFileAsUnsatisfiable() {
        // given
        RangeHeader header = RangeHeader.parse("bytes=0-");

        // when
        ByteRange range = header.within(0);

        // then
        Assertions.assertThat(range.isSatisfiable()).isFalse();
    }

    @Test
    public void remembersWhereTheRangeStarts() {
        // when
        RangeHeader header = RangeHeader.parse("bytes=1234-");

        // then
        Assertions.assertThat(header.first).isEqualTo(1234);
    }

    @Test
    public void ignoresSuffixRanges() {
        // when
        RangeHeader header = RangeHeader.parse("bytes=-500");

        // then
        Assertions.assertThat(header).isNull();
    }

    @Test
    public void ignoresMultipleRanges() {
        // when
        RangeHeader header = RangeHeader.parse("bytes=0-1,5-6");

        // then
        Assertions.assertThat(header).isNull();
    }

    @Test
    public void ignoresAMissingHeader() {
        // when
        RangeHeader header = RangeHeader.parse(null);

        // then
        Assertions.assertThat(header).isNull();
        Assertions.assertThat(new ByteRange(0, 9).isSatisfiable()).isTrue();
    }

}
