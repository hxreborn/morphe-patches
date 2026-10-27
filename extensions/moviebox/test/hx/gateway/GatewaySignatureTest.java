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
package hx.gateway;

import java.io.IOException;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public final class GatewaySignatureTest {

    private static final byte[] KEY = "test-signing-key".getBytes();

    private static final long TIMESTAMP = 1700000000123L;

    @Test
    public void signsAGetWithTheCanonicalRequest() throws IOException {
        // when
        String signature = GatewaySignature.create(KEY, "GET",
                "/wefeed-mobile-bff/subject-api/play-info/v2?subjectId=1&se=0&ep=0&isVip=true", null, TIMESTAMP);

        // then
        Assertions.assertThat(signature).isEqualTo("1700000000123|2|qVn2zS7MkEJJwI3+YDWFIg==");
    }

    @Test
    public void signsAPostWithTheBodyLengthAndDigest() throws IOException {
        // when
        String signature = GatewaySignature.create(KEY, "POST", "/wefeed-mobile-bff/user-api/visitor-login", "{}",
                TIMESTAMP);

        // then
        Assertions.assertThat(signature).isEqualTo("1700000000123|2|coQhNaSHLjcMsQubBCarlA==");
    }

    @Test
    public void signsTheQueryInKeyOrderWithDecodedValues() throws IOException {
        // when
        String signature = GatewaySignature.create(KEY, "GET", "/p?b=2&a=1&c=x%20y", null, TIMESTAMP);

        // then
        Assertions.assertThat(signature).isEqualTo("1700000000123|2|B5d5OopG3St/B051VCcRQQ==");
    }

    @Test
    public void keepsDuplicateKeysInTheirOriginalOrder() throws IOException {
        // when
        String signature = GatewaySignature.create(KEY, "GET", "/p?k=2&j=0&k=1", null, TIMESTAMP);

        // then
        Assertions.assertThat(signature).isEqualTo("1700000000123|2|klLhPDlWusNi0OfPnmaTkQ==");
    }

    @Test
    public void signsAPathWithoutAQueryAsIs() throws IOException {
        // when
        String signature = GatewaySignature.create(KEY, "GET", "/a/b", null, TIMESTAMP);

        // then
        Assertions.assertThat(signature).isEqualTo("1700000000123|2|sMKJ7cxxZDoJmFsWvNH8XA==");
    }

    @Test
    public void pairsTheTimestampWithTheMd5OfItsReversal() throws IOException {
        // when
        String token = GatewaySignature.clientToken(TIMESTAMP);

        // then
        Assertions.assertThat(token).isEqualTo("1700000000123,8bb2b37cedae787f581503a05e847ec5");
        Assertions.assertThat(token.startsWith(TIMESTAMP + ",")).isTrue();
    }

}
