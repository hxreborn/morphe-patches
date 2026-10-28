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
package hx.rewrite;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public final class ResourceListRequestTest {

    private static final String HOST = "https://apig.example.com";

    @Test
    public void movesTheV2ListingToV1() {
        // given
        String v2 = HOST + "/wefeed-mobile-bff/subject-api/resource/v2?subjectId=7&page=1&perPage=10&all=0"
                + "&startPosition=1&endPosition=1&pagerMode=0&resolution=0&se=1&epFrom=1&epTo=1&isVip=true";

        // when
        String listing = ResourceListRequest.listingUrl(v2);

        // then
        Assertions.assertThat(listing)
            .isEqualTo(HOST + "/wefeed-mobile-bff/subject-api/resource?subjectId=7&page=1&perPage=10&all=0"
                    + "&startPosition=1&endPosition=1&pagerMode=0&resolution=0&se=1&epFrom=1&epTo=1");
    }

    @Test
    public void keepsTheQueryWhenTheListingHasNoMembershipParameter() {
        // when
        String listing = ResourceListRequest.listingUrl(HOST + "/wefeed-mobile-bff/subject-api/resource/v2?a=1&b=2");

        // then
        Assertions.assertThat(listing).isEqualTo(HOST + "/wefeed-mobile-bff/subject-api/resource?a=1&b=2");
    }

    @Test
    public void leavesTheV1ListingAlone() {
        // when
        String listing = ResourceListRequest.listingUrl(HOST + "/wefeed-mobile-bff/subject-api/resource?a=1");

        // then
        Assertions.assertThat(listing).isNull();
    }

    @Test
    public void leavesPositionLookupsAlone() {
        // when
        String listing = ResourceListRequest
            .listingUrl(HOST + "/wefeed-mobile-bff/subject-api/resource-position/v2?resourceId=1&isVip=true");

        // then
        Assertions.assertThat(listing).isNull();
    }

}
