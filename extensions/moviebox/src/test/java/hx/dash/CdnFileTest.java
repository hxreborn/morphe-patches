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
package hx.dash;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import hx.testing.LocalCdn;
import hx.testing.Mp4Fixtures;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public final class CdnFileTest {

    private static final String COOKIE = "fresh=1";

    private static final String SEGMENT = "/chunk-stream0-00001.m4s";

    private static final byte[] BODY = Mp4Fixtures.bytes(300_000, 7);

    private static final CdnCookies FIXED_COOKIES = new CdnCookies() {
        @Override
        public String current() {
            return COOKIE;
        }

        @Override
        public String refresh(String rejected) {
            return COOKIE;
        }
    };

    private LocalCdn cdn;

    @Before
    public void startCdn() throws IOException {
        this.cdn = new LocalCdn(COOKIE);
        this.cdn.put(SEGMENT, BODY);
    }

    @After
    public void stopCdn() {
        this.cdn.close();
    }

    @Test
    public void writesTheWholeFileInOrder() throws IOException {
        // when
        CdnFile file = CdnFile.open(this.cdn.url(SEGMENT), 0, FIXED_COOKIES);
        byte[] written = write(file, file.length - 1);

        // then
        Assertions.assertThat(file.length).isEqualTo(BODY.length);
        Assertions.assertThat(written).isEqualTo(BODY);
    }

    @Test
    public void writesFromTheRequestedStart() throws IOException {
        // when
        CdnFile file = CdnFile.open(this.cdn.url(SEGMENT), 100_000, FIXED_COOKIES);
        byte[] written = write(file, 199_999);

        // then
        Assertions.assertThat(written).isEqualTo(Arrays.copyOfRange(BODY, 100_000, 200_000));
    }

    @Test
    public void readsChunksInParallel() throws IOException {
        // given
        this.cdn.delayRanges(50);

        // when
        CdnFile file = CdnFile.open(this.cdn.url(SEGMENT), 0, FIXED_COOKIES);
        write(file, file.length - 1);

        // then
        Assertions.assertThat(this.cdn.peakConcurrentRanges()).isGreaterThan(1);
    }

    private static byte[] write(CdnFile file, long end) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        file.write(out, end);
        return out.toByteArray();
    }

}
