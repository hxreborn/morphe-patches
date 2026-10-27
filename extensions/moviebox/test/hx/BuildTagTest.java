/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package hx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Random;

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
    public void joinsTheTimestampAndTheMd5OfTheCertificateDigestSaltAndTimestamp() {
        // given
        BuildTag buildTag = BuildTag.fromCertificate(CERTIFICATE, () -> 1700000000123L, fixedRandom(45));

        // when
        String tag = buildTag.next();

        // then
        assertEquals("170000000012300045.1a8f8b991f95799fba169a8a7f889ebc", tag);
    }

    @Test
    public void writesTheRandomPartWithoutPadding() {
        // given
        BuildTag buildTag = BuildTag.fromCertificate(CERTIFICATE, () -> 1700000000007L, fixedRandom(7));

        // when
        String tag = buildTag.next();

        // then
        assertEquals("17000000000070007.d1c4e3525f7a8bf894ffdb6fcda377b2", tag);
    }

    @Test
    public void readsTheClockAndTheRandomOnEveryTag() {
        // given
        long[] now = { 1700000000123L };
        BuildTag buildTag = BuildTag.fromCertificate(CERTIFICATE, () -> now[0]++, fixedRandom(45));

        // when
        String first = buildTag.next();
        String second = buildTag.next();

        // then
        assertTrue(first.startsWith("170000000012300045."));
        assertTrue(second.startsWith("170000000012400045."));
    }

    @Test
    public void drawsATimestampedTagFromTheClock() {
        // given
        BuildTag buildTag = BuildTag.fromCertificate(CERTIFICATE);

        // when
        String tag = buildTag.next();

        // then
        assertTrue(tag, tag.matches("\\d{17,19}\\.[0-9a-f]{32}"));
    }

    @Test
    public void putsTheTagIntoTheClientInfo() throws JSONException {
        // when
        String tagged = BuildTag.addTo("{\"device_id\":\"d1\"}", "stamp.hash");

        // then
        JSONObject info = new JSONObject(tagged);
        assertEquals("d1", info.getString("device_id"));
        assertEquals("stamp.hash", info.getString(BuildTag.HEADER));
    }

    @Test
    public void replacesATagAlreadyInTheClientInfo() throws JSONException {
        // when
        String tagged = BuildTag.addTo("{\"X-Client-Build\":\"old\"}", "new");

        // then
        assertEquals("new", new JSONObject(tagged).getString(BuildTag.HEADER));
    }

    @Test
    public void keepsClientInfoThatIsNotJson() {
        // when
        String tagged = BuildTag.addTo("not json", "stamp.hash");

        // then
        assertEquals("not json", tagged);
    }

    private static Random fixedRandom(int value) {
        return new Random() {
            @Override
            public int nextInt(int bound) {
                return value;
            }
        };
    }

}
