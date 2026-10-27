/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package hx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;

import android.net.Uri;
import android.util.Base64;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public final class SignedResourceTest {

    @Test
    public void readsTheManifestFromACloudFrontPolicy() {
        // given
        String cookie = cloudFrontCookie("https://sacdn.hakunaymatata.com/abc/*");

        // when
        SignedResource resource = SignedResource.fromCookie(cookie);

        // then
        assertEquals("https://sacdn.hakunaymatata.com/abc/index.mpd", resource.manifestUrl);
        assertEquals(cookie, resource.cookie);
        assertFalse(resource.isEdgeCache());
    }

    @Test
    public void readsTheManifestFromAnEdgeCacheCookie() {
        // given
        String cookie = edgeCacheCookie("https://sbcdn5.example.com/abc");

        // when
        SignedResource resource = SignedResource.fromCookie(cookie);

        // then
        assertEquals("https://sbcdn5.example.com/abc/index.mpd", resource.manifestUrl);
        assertTrue(resource.isEdgeCache());
    }

    @Test
    public void keepsTheSlashOfAnEdgeCachePrefix() {
        // when
        SignedResource resource = SignedResource.fromCookie(edgeCacheCookie("https://sbcdn5.example.com/abc/"));

        // then
        assertEquals("https://sbcdn5.example.com/abc/index.mpd", resource.manifestUrl);
    }

    @Test
    public void refusesAPolicyOutsideHttps() {
        // when
        SignedResource resource = SignedResource.fromCookie(cloudFrontCookie("http://sacdn.example.com/abc/*"));

        // then
        assertNull(resource);
    }

    @Test
    public void refusesAPolicyWithoutAWildcardResource() {
        // when
        SignedResource resource = SignedResource.fromCookie(cloudFrontCookie("https://sacdn.example.com/abc"));

        // then
        assertNull(resource);
    }

    @Test
    public void refusesAnEdgeCachePrefixOutsideHttps() {
        // when
        SignedResource resource = SignedResource.fromCookie(edgeCacheCookie("http://sbcdn5.example.com/abc"));

        // then
        assertNull(resource);
    }

    @Test
    public void refusesACookieWithoutAPolicy() {
        // when
        SignedResource resource = SignedResource.fromCookie("session=1");

        // then
        assertNull(resource);
    }

    @Test
    public void refusesAPolicyThatIsNotBase64Json() {
        // when
        SignedResource resource = SignedResource.fromCookie("CloudFront-Policy=@@@;");

        // then
        assertNull(resource);
    }

    @Test
    public void takesTheFirstStreamThatCarriesASignedCookie() {
        // given
        String body = playInfo("", cloudFrontCookie("https://sacdn.example.com/one/*"),
                cloudFrontCookie("https://sacdn.example.com/two/*"));

        // when
        SignedResource resource = SignedResource.fromPlayInfo(body);

        // then
        assertEquals("https://sacdn.example.com/one/index.mpd", resource.manifestUrl);
    }

    @Test
    public void givesNoResourceForPlayInfoThatIsNotJson() {
        // when
        SignedResource resource = SignedResource.fromPlayInfo("<html>");

        // then
        assertNull(resource);
    }

    @Test
    public void recognizesEdgeCachePlayInfo() {
        // when
        boolean edge = SignedResource.isEdgeCachePlayInfo(playInfo(edgeCacheCookie("https://sbcdn5.example.com/a")));
        boolean cloudFront = SignedResource
            .isEdgeCachePlayInfo(playInfo(cloudFrontCookie("https://sacdn.example.com/a/*")));

        // then
        assertTrue(edge);
        assertFalse(cloudFront);
    }

    @Test
    public void keysAPlayInfoRequestBySubjectSeasonAndEpisode() {
        // when
        String key = SignedResource.key(Uri.parse("https://h/play-info/v2?subjectId=77&se=2&ep=5&isVip=true"));

        // then
        assertEquals("77/2/5", key);
    }

    @Test
    public void givesNoKeyWhenTheRequestLacksAnEpisode() {
        // when
        String key = SignedResource.key(Uri.parse("https://h/play-info/v2?subjectId=77&se=2"));

        // then
        assertNull(key);
    }

    @Test
    public void givesNoKeyWhenTheSeasonIsNotANumber() {
        // when
        String key = SignedResource.key(Uri.parse("https://h/play-info/v2?subjectId=77&se=x&ep=5"));

        // then
        assertNull(key);
    }

    static String cloudFrontCookie(String resource) {
        String json = "{\"Statement\":[{\"Resource\":\"" + resource + "\"}]}";
        String policy = Base64.encodeToString(json.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP)
            .replace('+', '-')
            .replace('=', '_')
            .replace('/', '~');
        return "CloudFront-Policy=" + policy + ";CloudFront-Signature=sig;CloudFront-Key-Pair-Id=key";
    }

    static String edgeCacheCookie(String prefix) {
        String encoded = Base64.encodeToString(prefix.getBytes(StandardCharsets.UTF_8),
                Base64.URL_SAFE | Base64.NO_WRAP);
        return "Edge-Cache-Cookie=urlprefix=" + encoded + ":expires=1:hmac=abc;path=/";
    }

    static String playInfo(String... cookies) {
        StringBuilder streams = new StringBuilder();
        for (String cookie : cookies) {
            if (streams.length() > 0) {
                streams.append(',');
            }
            streams.append("{\"url\":\"https://origin.example.com/f.mp4\",\"signCookie\":\"")
                .append(cookie)
                .append("\"}");
        }
        return "{\"data\":{\"streams\":[" + streams + "]}}";
    }

}
