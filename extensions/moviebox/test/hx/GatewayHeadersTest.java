/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package hx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Random;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public final class GatewayHeadersTest {

    private static final byte[] SIGNING_KEY = "test-signing-key".getBytes(StandardCharsets.UTF_8);

    private static final String PATH = "/wefeed-mobile-bff/subject-api/play-info/v2?subjectId=1&se=0&ep=0";

    @Test
    public void sendsTheTagInTheBuildHeader() throws Exception {
        // given
        GatewayHeaders gateway = new GatewayHeaders(clientInfo(), stockCertificateTag());

        // when
        Map<String, String> headers = gateway.create(SIGNING_KEY, "GET", PATH, null, "token-1");

        // then
        assertEquals("170000000012300045.1a8f8b991f95799fba169a8a7f889ebc", headers.get(BuildTag.HEADER));
    }

    @Test
    public void sendsTheSameTagInsideTheClientInfo() throws Exception {
        // given
        GatewayHeaders gateway = new GatewayHeaders(clientInfo(), stockCertificateTag());

        // when
        Map<String, String> headers = gateway.create(SIGNING_KEY, "GET", PATH, null, "token-1");

        // then
        JSONObject info = new JSONObject(headers.get("x-client-info"));
        assertEquals(headers.get(BuildTag.HEADER), info.getString(BuildTag.HEADER));
    }

    @Test
    public void sendsThePackageIdentityTheCdnLookupNeeds() throws Exception {
        // given
        GatewayHeaders gateway = new GatewayHeaders(clientInfo(), stockCertificateTag());

        // when
        Map<String, String> headers = gateway.create(SIGNING_KEY, "GET", PATH, null, "token-1");

        // then
        JSONObject info = new JSONObject(headers.get("x-client-info"));
        assertEquals(RuntimeEnvironment.getApplication().getPackageName(), info.getString("package_name"));
        assertTrue(info.get("version_code") instanceof Number);
        assertTrue(info.getString("gaid").matches("[0-9a-f-]{36}"));
        assertTrue(info.has("device_id"));
        assertTrue(info.has("os_version"));
    }

    @Test
    public void signsTheRequestAndAuthorizesWithTheVisitorToken() throws Exception {
        // given
        GatewayHeaders gateway = new GatewayHeaders(clientInfo(), stockCertificateTag());

        // when
        Map<String, String> headers = gateway.create(SIGNING_KEY, "GET", PATH, null, "token-1");

        // then
        assertTrue(headers.get("x-tr-signature").matches("\\d+\\|2\\|[A-Za-z0-9+/=]+"));
        assertTrue(headers.get("x-client-token").matches("\\d+,[0-9a-f]{32}"));
        assertEquals("Bearer token-1", headers.get("Authorization"));
        assertEquals("unknown", headers.get("x-forwarded-for"));
        assertEquals("application/json", headers.get("Accept"));
        assertEquals("application/json", headers.get("Content-Type"));
    }

    @Test
    public void omitsTheAuthorizationBeforeTheVisitorLogin() throws Exception {
        // given
        GatewayHeaders gateway = new GatewayHeaders(clientInfo(), stockCertificateTag());

        // when
        Map<String, String> headers = gateway.create(SIGNING_KEY, "POST", "/wefeed-mobile-bff/user-api/visitor-login",
                "{}", null);

        // then
        assertFalse(headers.containsKey("Authorization"));
    }

    @Test
    public void sendsNoTagWithoutACertificate() throws Exception {
        // given
        GatewayHeaders gateway = new GatewayHeaders(clientInfo(), null);

        // when
        Map<String, String> headers = gateway.create(SIGNING_KEY, "GET", PATH, null, "token-1");

        // then
        assertFalse(headers.containsKey(BuildTag.HEADER));
        assertFalse(new JSONObject(headers.get("x-client-info")).has(BuildTag.HEADER));
    }

    private static String clientInfo() throws JSONException, IOException, Exception {
        return ClientInfo.create(RuntimeEnvironment.getApplication());
    }

    private static BuildTag stockCertificateTag() {
        Random fixed = new Random() {
            @Override
            public int nextInt(int bound) {
                return 45;
            }
        };
        return BuildTag.fromCertificate("stock-certificate".getBytes(StandardCharsets.UTF_8), () -> 1700000000123L,
                fixed);
    }

}
