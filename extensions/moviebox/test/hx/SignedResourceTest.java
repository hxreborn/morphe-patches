/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package hx;

import java.nio.charset.StandardCharsets;

import android.net.Uri;
import android.util.Base64;
import org.assertj.core.api.Assertions;
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
        Assertions.assertThat(resource.manifestUrl).isEqualTo("https://sacdn.hakunaymatata.com/abc/index.mpd");
        Assertions.assertThat(resource.cookie).isEqualTo(cookie);
        Assertions.assertThat(resource.isEdgeCache()).isFalse();
    }

    @Test
    public void readsTheManifestFromAnEdgeCacheCookie() {
        // given
        String cookie = edgeCacheCookie("https://sbcdn5.example.com/abc");

        // when
        SignedResource resource = SignedResource.fromCookie(cookie);

        // then
        Assertions.assertThat(resource.manifestUrl).isEqualTo("https://sbcdn5.example.com/abc/index.mpd");
        Assertions.assertThat(resource.isEdgeCache()).isTrue();
    }

    @Test
    public void keepsTheSlashOfAnEdgeCachePrefix() {
        // when
        SignedResource resource = SignedResource.fromCookie(edgeCacheCookie("https://sbcdn5.example.com/abc/"));

        // then
        Assertions.assertThat(resource.manifestUrl).isEqualTo("https://sbcdn5.example.com/abc/index.mpd");
    }

    @Test
    public void refusesAPolicyOutsideHttps() {
        // when
        SignedResource resource = SignedResource.fromCookie(cloudFrontCookie("http://sacdn.example.com/abc/*"));

        // then
        Assertions.assertThat(resource).isNull();
    }

    @Test
    public void refusesAPolicyWithoutAWildcardResource() {
        // when
        SignedResource resource = SignedResource.fromCookie(cloudFrontCookie("https://sacdn.example.com/abc"));

        // then
        Assertions.assertThat(resource).isNull();
    }

    @Test
    public void refusesAnEdgeCachePrefixOutsideHttps() {
        // when
        SignedResource resource = SignedResource.fromCookie(edgeCacheCookie("http://sbcdn5.example.com/abc"));

        // then
        Assertions.assertThat(resource).isNull();
    }

    @Test
    public void refusesACookieWithoutAPolicy() {
        // when
        SignedResource resource = SignedResource.fromCookie("session=1");

        // then
        Assertions.assertThat(resource).isNull();
    }

    @Test
    public void refusesAPolicyThatIsNotBase64Json() {
        // when
        SignedResource resource = SignedResource.fromCookie("CloudFront-Policy=@@@;");

        // then
        Assertions.assertThat(resource).isNull();
    }

    @Test
    public void takesTheFirstStreamThatCarriesASignedCookie() {
        // given
        String body = playInfo("", cloudFrontCookie("https://sacdn.example.com/one/*"),
                cloudFrontCookie("https://sacdn.example.com/two/*"));

        // when
        SignedResource resource = SignedResource.fromPlayInfo(body);

        // then
        Assertions.assertThat(resource.manifestUrl).isEqualTo("https://sacdn.example.com/one/index.mpd");
    }

    @Test
    public void givesNoResourceForPlayInfoThatIsNotJson() {
        // when
        SignedResource resource = SignedResource.fromPlayInfo("<html>");

        // then
        Assertions.assertThat(resource).isNull();
    }

    @Test
    public void recognizesEdgeCachePlayInfo() {
        // when
        boolean edge = SignedResource.isEdgeCachePlayInfo(playInfo(edgeCacheCookie("https://sbcdn5.example.com/a")));

        // then
        Assertions.assertThat(edge).isTrue();
    }

    @Test
    public void recognizesCloudFrontPlayInfoAsNotEdgeCache() {
        // when
        boolean edge = SignedResource.isEdgeCachePlayInfo(playInfo(cloudFrontCookie("https://sacdn.example.com/a/*")));

        // then
        Assertions.assertThat(edge).isFalse();
    }

    @Test
    public void keysAPlayInfoRequestBySubjectSeasonAndEpisode() {
        // when
        String key = SignedResource.key(Uri.parse("https://h/play-info/v2?subjectId=77&se=2&ep=5&isVip=true"));

        // then
        Assertions.assertThat(key).isEqualTo("77/2/5");
    }

    @Test
    public void givesNoKeyWhenTheRequestLacksAnEpisode() {
        // when
        String key = SignedResource.key(Uri.parse("https://h/play-info/v2?subjectId=77&se=2"));

        // then
        Assertions.assertThat(key).isNull();
    }

    @Test
    public void givesNoKeyWhenTheSeasonIsNotANumber() {
        // when
        String key = SignedResource.key(Uri.parse("https://h/play-info/v2?subjectId=77&se=x&ep=5"));

        // then
        Assertions.assertThat(key).isNull();
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
