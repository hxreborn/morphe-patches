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
package hx.source;

import android.net.Uri;
import hx.testing.PlayInfoFixtures;
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
        String cookie = PlayInfoFixtures.cloudFrontCookie("https://sacdn.hakunaymatata.com/abc/*");

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
        String cookie = PlayInfoFixtures.edgeCacheCookie("https://sbcdn5.example.com/abc");

        // when
        SignedResource resource = SignedResource.fromCookie(cookie);

        // then
        Assertions.assertThat(resource.manifestUrl).isEqualTo("https://sbcdn5.example.com/abc/index.mpd");
        Assertions.assertThat(resource.isEdgeCache()).isTrue();
    }

    @Test
    public void keepsTheSlashOfAnEdgeCachePrefix() {
        // when
        SignedResource resource = SignedResource
            .fromCookie(PlayInfoFixtures.edgeCacheCookie("https://sbcdn5.example.com/abc/"));

        // then
        Assertions.assertThat(resource.manifestUrl).isEqualTo("https://sbcdn5.example.com/abc/index.mpd");
    }

    @Test
    public void refusesAPolicyOutsideHttps() {
        // when
        SignedResource resource = SignedResource
            .fromCookie(PlayInfoFixtures.cloudFrontCookie("http://sacdn.example.com/abc/*"));

        // then
        Assertions.assertThat(resource).isNull();
    }

    @Test
    public void refusesAPolicyWithoutAWildcardResource() {
        // when
        SignedResource resource = SignedResource
            .fromCookie(PlayInfoFixtures.cloudFrontCookie("https://sacdn.example.com/abc"));

        // then
        Assertions.assertThat(resource).isNull();
    }

    @Test
    public void refusesAnEdgeCachePrefixOutsideHttps() {
        // when
        SignedResource resource = SignedResource
            .fromCookie(PlayInfoFixtures.edgeCacheCookie("http://sbcdn5.example.com/abc"));

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
        String body = PlayInfoFixtures.playInfo("",
                PlayInfoFixtures.cloudFrontCookie("https://sacdn.example.com/one/*"),
                PlayInfoFixtures.cloudFrontCookie("https://sacdn.example.com/two/*"));

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
        boolean edge = SignedResource.isEdgeCachePlayInfo(
                PlayInfoFixtures.playInfo(PlayInfoFixtures.edgeCacheCookie("https://sbcdn5.example.com/a")));

        // then
        Assertions.assertThat(edge).isTrue();
    }

    @Test
    public void recognizesCloudFrontPlayInfoAsNotEdgeCache() {
        // when
        boolean edge = SignedResource.isEdgeCachePlayInfo(
                PlayInfoFixtures.playInfo(PlayInfoFixtures.cloudFrontCookie("https://sacdn.example.com/a/*")));

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

}
