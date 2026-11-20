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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Random;

import org.assertj.core.api.Assertions;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public final class BuildTagTest {

    private static final byte[] CERTIFICATE = "stock-certificate".getBytes(StandardCharsets.UTF_8);

    @Test
    public void readsTheCertificatePackagedInTheApp() {
        // when
        byte[] certificate = BuildTag.readCertificate(loaderServing("assets/hx.cer", CERTIFICATE));

        // then
        Assertions.assertThat(certificate).isEqualTo(CERTIFICATE);
    }

    @Test
    public void findsNoCertificateWhenTheAppCarriesNone() {
        // when
        byte[] certificate = BuildTag.readCertificate(loaderServing("assets/other", CERTIFICATE));

        // then
        Assertions.assertThat(certificate).isNull();
    }

    @Test
    public void joinsTheTimestampAndTheMd5OfTheCertificateDigestSaltAndTimestamp() {
        // given
        BuildTag buildTag = BuildTag.fromCertificate(CERTIFICATE, () -> 1700000000123L, fixedRandom(45));

        // when
        String tag = buildTag.next();

        // then
        Assertions.assertThat(tag).isEqualTo("170000000012300045.1a8f8b991f95799fba169a8a7f889ebc");
    }

    @Test
    public void writesTheRandomPartWithoutPadding() {
        // given
        BuildTag buildTag = BuildTag.fromCertificate(CERTIFICATE, () -> 1700000000007L, fixedRandom(7));

        // when
        String tag = buildTag.next();

        // then
        Assertions.assertThat(tag).isEqualTo("17000000000070007.d1c4e3525f7a8bf894ffdb6fcda377b2");
    }

    @Test
    public void readsTheClockAndTheRandomOnEveryTag() {
        // given
        long[] now = { 1700000000123L };
        BuildTag buildTag = BuildTag.fromCertificate(CERTIFICATE, () -> now[0]++, fixedRandom(45));
        String first = buildTag.next();

        // when
        String second = buildTag.next();

        // then
        Assertions.assertThat(first).startsWith("170000000012300045.");
        Assertions.assertThat(second).startsWith("170000000012400045.");
    }

    @Test
    public void drawsATimestampedTagFromTheClock() {
        // given
        BuildTag buildTag = BuildTag.fromCertificate(CERTIFICATE);

        // when
        String tag = buildTag.next();

        // then
        Assertions.assertThat(tag.matches("\\d{17,19}\\.[0-9a-f]{32}")).as(tag).isTrue();
    }

    @Test
    public void putsTheTagIntoTheClientInfo() throws JSONException {
        // when
        String tagged = BuildTag.addTo("{\"device_id\":\"d1\"}", "stamp.hash");

        // then
        JSONObject info = new JSONObject(tagged);
        Assertions.assertThat(info.getString("device_id")).isEqualTo("d1");
        Assertions.assertThat(info.getString(BuildTag.HEADER)).isEqualTo("stamp.hash");
    }

    @Test
    public void replacesATagAlreadyInTheClientInfo() throws JSONException {
        // when
        String tagged = BuildTag.addTo("{\"X-Client-Build\":\"old\"}", "new");

        // then
        Assertions.assertThat(new JSONObject(tagged).getString(BuildTag.HEADER)).isEqualTo("new");
    }

    @Test
    public void keepsClientInfoThatIsNotJson() {
        // when
        String tagged = BuildTag.addTo("not json", "stamp.hash");

        // then
        Assertions.assertThat(tagged).isEqualTo("not json");
    }

    private static Random fixedRandom(int value) {
        return new Random() {
            @Override
            public int nextInt(int bound) {
                return value;
            }
        };
    }

    private static ClassLoader loaderServing(String name, byte[] content) {
        return new ClassLoader(null) {
            @Override
            public InputStream getResourceAsStream(String requested) {
                return name.equals(requested) ? new ByteArrayInputStream(content) : null;
            }
        };
    }

}
