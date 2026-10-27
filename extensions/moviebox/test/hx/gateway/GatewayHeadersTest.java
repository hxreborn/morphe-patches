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
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Random;

import android.content.pm.PackageManager;
import org.assertj.core.api.Assertions;
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
    public void sendsTheTagInTheBuildHeader() throws IOException, PackageManager.NameNotFoundException {
        // given
        GatewayHeaders gateway = new GatewayHeaders(clientInfo(), stockCertificateTag());

        // when
        Map<String, String> headers = gateway.create(SIGNING_KEY, "GET", PATH, null, "token-1");

        // then
        Assertions.assertThat(headers.get(BuildTag.HEADER))
            .isEqualTo("170000000012300045.1a8f8b991f95799fba169a8a7f889ebc");
    }

    @Test
    public void sendsTheSameTagInsideTheClientInfo()
            throws IOException, JSONException, PackageManager.NameNotFoundException {
        // given
        GatewayHeaders gateway = new GatewayHeaders(clientInfo(), stockCertificateTag());

        // when
        Map<String, String> headers = gateway.create(SIGNING_KEY, "GET", PATH, null, "token-1");

        // then
        JSONObject info = new JSONObject(headers.get("x-client-info"));
        Assertions.assertThat(info.getString(BuildTag.HEADER)).isEqualTo(headers.get(BuildTag.HEADER));
    }

    @Test
    public void sendsThePackageIdentityTheCdnLookupNeeds()
            throws IOException, JSONException, PackageManager.NameNotFoundException {
        // given
        GatewayHeaders gateway = new GatewayHeaders(clientInfo(), stockCertificateTag());

        // when
        Map<String, String> headers = gateway.create(SIGNING_KEY, "GET", PATH, null, "token-1");

        // then
        JSONObject info = new JSONObject(headers.get("x-client-info"));
        Assertions.assertThat(info.getString("package_name"))
            .isEqualTo(RuntimeEnvironment.getApplication().getPackageName());
        Assertions.assertThat(info.get("version_code") instanceof Number).isTrue();
        Assertions.assertThat(info.getString("gaid").matches("[0-9a-f-]{36}")).isTrue();
        Assertions.assertThat(info.has("device_id")).isTrue();
        Assertions.assertThat(info.has("os_version")).isTrue();
    }

    @Test
    public void signsTheRequestAndAuthorizesWithTheVisitorToken()
            throws IOException, PackageManager.NameNotFoundException {
        // given
        GatewayHeaders gateway = new GatewayHeaders(clientInfo(), stockCertificateTag());

        // when
        Map<String, String> headers = gateway.create(SIGNING_KEY, "GET", PATH, null, "token-1");

        // then
        Assertions.assertThat(headers.get("x-tr-signature").matches("\\d+\\|2\\|[A-Za-z0-9+/=]+")).isTrue();
        Assertions.assertThat(headers.get("x-client-token").matches("\\d+,[0-9a-f]{32}")).isTrue();
        Assertions.assertThat(headers.get("Authorization")).isEqualTo("Bearer token-1");
        Assertions.assertThat(headers.get("x-forwarded-for")).isEqualTo("unknown");
        Assertions.assertThat(headers.get("Accept")).isEqualTo("application/json");
        Assertions.assertThat(headers.get("Content-Type")).isEqualTo("application/json");
    }

    @Test
    public void omitsTheAuthorizationBeforeTheVisitorLogin() throws IOException, PackageManager.NameNotFoundException {
        // given
        GatewayHeaders gateway = new GatewayHeaders(clientInfo(), stockCertificateTag());

        // when
        Map<String, String> headers = gateway.create(SIGNING_KEY, "POST", "/wefeed-mobile-bff/user-api/visitor-login",
                "{}", null);

        // then
        Assertions.assertThat(headers.containsKey("Authorization")).isFalse();
    }

    @Test
    public void sendsNoTagWithoutACertificate()
            throws IOException, JSONException, PackageManager.NameNotFoundException {
        // given
        GatewayHeaders gateway = new GatewayHeaders(clientInfo(), null);

        // when
        Map<String, String> headers = gateway.create(SIGNING_KEY, "GET", PATH, null, "token-1");

        // then
        Assertions.assertThat(headers.containsKey(BuildTag.HEADER)).isFalse();
        Assertions.assertThat(new JSONObject(headers.get("x-client-info")).has(BuildTag.HEADER)).isFalse();
    }

    private static String clientInfo() throws PackageManager.NameNotFoundException {
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
