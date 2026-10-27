/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package hx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

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
        assertEquals("1700000000123|2|qVn2zS7MkEJJwI3+YDWFIg==", signature);
    }

    @Test
    public void signsAPostWithTheBodyLengthAndDigest() throws IOException {
        // when
        String signature = GatewaySignature.create(KEY, "POST", "/wefeed-mobile-bff/user-api/visitor-login", "{}",
                TIMESTAMP);

        // then
        assertEquals("1700000000123|2|coQhNaSHLjcMsQubBCarlA==", signature);
    }

    @Test
    public void signsTheQueryInKeyOrderWithDecodedValues() throws IOException {
        // when
        String signature = GatewaySignature.create(KEY, "GET", "/p?b=2&a=1&c=x%20y", null, TIMESTAMP);

        // then
        assertEquals("1700000000123|2|B5d5OopG3St/B051VCcRQQ==", signature);
    }

    @Test
    public void keepsDuplicateKeysInTheirOriginalOrder() throws IOException {
        // when
        String signature = GatewaySignature.create(KEY, "GET", "/p?k=2&j=0&k=1", null, TIMESTAMP);

        // then
        assertEquals("1700000000123|2|klLhPDlWusNi0OfPnmaTkQ==", signature);
    }

    @Test
    public void signsAPathWithoutAQueryAsIs() throws IOException {
        // when
        String signature = GatewaySignature.create(KEY, "GET", "/a/b", null, TIMESTAMP);

        // then
        assertEquals("1700000000123|2|sMKJ7cxxZDoJmFsWvNH8XA==", signature);
    }

    @Test
    public void pairsTheTimestampWithTheMd5OfItsReversal() throws Exception {
        // when
        String token = GatewaySignature.clientToken(TIMESTAMP);

        // then
        assertEquals("1700000000123,8bb2b37cedae787f581503a05e847ec5", token);
        assertTrue(token.startsWith(TIMESTAMP + ","));
    }

}
