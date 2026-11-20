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

import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public final class PromotionRewriterTest {

    private static final String MOVIE = "oneroom://com.community.oneroom?type=/movie/detail&id=1&subject_type=1";

    private static final String CATEGORY = "oneroom://com.community.oneroom?type=/home/category&categoryType=2";

    private static final String MEMBERSHIP = "oneroom://com.community.oneroom?type=/member/MemberActivity";

    private static final String ACTIVITY_BANNER = "oneroom://com.community.oneroom?type=/web/web"
            + "&url=https%3A%2F%2Factivity.aoneroom.com%2Fspa%2Fspaactivity%2Finvite-friends%3FactivityId%3D1";

    private static final String ACTIVITY_PAGE = "https://activity.aoneroom.com/spa/spaactivity/invite-friends";

    private static final String COMMUNITY = "oneroom://com.community.oneroom?type=/web/web&url=https%3A%2F%2Ft.me%2Fc";

    private static final String APP_INSTALL = "oneroom://com.community.oneroom?type=/web/web&externalBrowser=true"
            + "&url=https://movie-box.co/moviebox-tv-apk";

    private static final String MEMBERSHIPS = "oneroom://com.community.oneroom?type=/membership/plans";

    private static final String OPAQUE = "mailto:support@example.com";

    private static final String TV_DOWNLOAD = "oneroom://com.community.oneroom?type=/profile/tv_download_web_link";

    @Test
    public void removesABannerLinkingToTheMembershipPage() throws JSONException {
        // given
        String body = feed(banner(MEMBERSHIP, MOVIE));

        // when
        String rewritten = PromotionRewriter.withoutFeedPromotions(body);

        // then
        Assertions.assertThat(bannerLinks(rewritten)).containsExactly(MOVIE);
    }

    @Test
    public void removesABannerLinkingToTheActivityHost() throws JSONException {
        // given
        String body = feed(banner(ACTIVITY_BANNER, CATEGORY));

        // when
        String rewritten = PromotionRewriter.withoutFeedPromotions(body);

        // then
        Assertions.assertThat(bannerLinks(rewritten)).containsExactly(CATEGORY);
    }

    @Test
    public void removesTheBannerSectionWhenEveryBannerIsAPromotion() throws JSONException {
        // given
        String body = feed(banner(MEMBERSHIP, ACTIVITY_BANNER), content("Trending"));

        // when
        String rewritten = PromotionRewriter.withoutFeedPromotions(body);

        // then
        Assertions.assertThat(titles(rewritten)).containsExactly("Trending");
    }

    @Test
    public void removesABannerWithAWebLink() throws JSONException {
        // given
        String body = feed(banner(ACTIVITY_PAGE, MOVIE));

        // when
        String rewritten = PromotionRewriter.withoutFeedPromotions(body);

        // then
        Assertions.assertThat(bannerLinks(rewritten)).containsExactly(MOVIE);
    }

    @Test
    public void removesBannersLinkingToACommunityOrAnAppInstall() throws JSONException {
        // given
        String body = feed(banner(COMMUNITY, APP_INSTALL, MOVIE));

        // when
        String rewritten = PromotionRewriter.withoutFeedPromotions(body);

        // then
        Assertions.assertThat(bannerLinks(rewritten)).containsExactly(MOVIE);
    }

    @Test
    public void removesASingleImageSectionLinkingToTheActivityHost() throws JSONException {
        // given
        String body = feed(image(ACTIVITY_PAGE, "Get Free Premium"), content("Trending"));

        // when
        String rewritten = PromotionRewriter.withoutFeedPromotions(body);

        // then
        Assertions.assertThat(titles(rewritten)).containsExactly("Trending");
    }

    @Test
    public void keepsBannersLinkingToContent() {
        // given
        String body = feed(banner(MOVIE, CATEGORY));

        // when
        String rewritten = PromotionRewriter.withoutFeedPromotions(body);

        // then
        Assertions.assertThat(rewritten).isSameAs(body);
    }

    @Test
    public void removesASingleImageSectionLinkingToTheTvDownloadPage() throws JSONException {
        // given
        String body = feed(image(TV_DOWNLOAD, "Watch on TV & Web"), content("Trending"));

        // when
        String rewritten = PromotionRewriter.withoutFeedPromotions(body);

        // then
        Assertions.assertThat(titles(rewritten)).containsExactly("Trending");
    }

    @Test
    public void removesPromotionItemsFromACustomSection() throws JSONException {
        // given
        String body = feed(custom(COMMUNITY, CATEGORY));

        // when
        String rewritten = PromotionRewriter.withoutFeedPromotions(body);

        // then
        Assertions.assertThat(customLinks(rewritten)).containsExactly(CATEGORY);
    }

    @Test
    public void removesACustomSectionWhenEveryItemIsAPromotion() throws JSONException {
        // given
        String body = feed(custom(COMMUNITY), content("Trending"));

        // when
        String rewritten = PromotionRewriter.withoutFeedPromotions(body);

        // then
        Assertions.assertThat(titles(rewritten)).containsExactly("Trending");
    }

    @Test
    public void keepsABannerLinkingToAPageNamedLikeTheMembershipPrefix() {
        // given
        String body = feed(banner(MEMBERSHIPS));

        // when
        String rewritten = PromotionRewriter.withoutFeedPromotions(body);

        // then
        Assertions.assertThat(rewritten).isSameAs(body);
    }

    @Test
    public void keepsABannerWithAnOpaqueLink() {
        // given
        String body = feed(banner(OPAQUE));

        // when
        String rewritten = PromotionRewriter.withoutFeedPromotions(body);

        // then
        Assertions.assertThat(rewritten).isSameAs(body);
    }

    @Test
    public void removesPromotionsBesideAnOpaqueLink() throws JSONException {
        // given
        String body = feed(banner(OPAQUE, MEMBERSHIP, MEMBERSHIPS));

        // when
        String rewritten = PromotionRewriter.withoutFeedPromotions(body);

        // then
        Assertions.assertThat(bannerLinks(rewritten)).containsExactly(OPAQUE, MEMBERSHIPS);
    }

    @Test
    public void keepsCustomItemsLinkingToContent() {
        // given
        String body = feed(custom(CATEGORY, MOVIE));

        // when
        String rewritten = PromotionRewriter.withoutFeedPromotions(body);

        // then
        Assertions.assertThat(rewritten).isSameAs(body);
    }

    @Test
    public void keepsContentSections() {
        // given
        String body = feed(content("Trending"));

        // when
        String rewritten = PromotionRewriter.withoutFeedPromotions(body);

        // then
        Assertions.assertThat(rewritten).isSameAs(body);
    }

    @Test
    public void keepsABannerWithoutADeepLink() {
        // given
        String body = feed("{\"type\":\"BANNER\",\"banner\":{\"banners\":[{\"content\":\"x\"}]}}");

        // when
        String rewritten = PromotionRewriter.withoutFeedPromotions(body);

        // then
        Assertions.assertThat(rewritten).isSameAs(body);
    }

    @Test
    public void leavesAFeedThatIsNotJsonUntouched() {
        // when
        String rewritten = PromotionRewriter.withoutFeedPromotions("nope");

        // then
        Assertions.assertThat(rewritten).isEqualTo("nope");
    }

    @Test
    public void leavesAFeedWithoutDataUntouched() {
        // given
        String body = "{\"code\":0}";

        // when
        String rewritten = PromotionRewriter.withoutFeedPromotions(body);

        // then
        Assertions.assertThat(rewritten).isSameAs(body);
    }

    @Test
    public void leavesAFeedWithoutItemsUntouched() {
        // given
        String body = "{\"data\":{}}";

        // when
        String rewritten = PromotionRewriter.withoutFeedPromotions(body);

        // then
        Assertions.assertThat(rewritten).isSameAs(body);
    }

    @Test
    public void leavesTheFeedUntouchedWhenABannerSectionHasNoBanners() {
        // given
        String malformed = "{\"type\":\"BANNER\",\"banner\":{\"banners\":\"none\"}}";
        String body = feed(malformed, banner(MEMBERSHIP));

        // when
        String rewritten = PromotionRewriter.withoutFeedPromotions(body);

        // then
        Assertions.assertThat(rewritten).isSameAs(body);
    }

    @Test
    public void leavesTheFeedUntouchedWhenASingleImageSectionHasNoPage() {
        // given
        String body = feed("{\"type\":\"SINGLE_IMAGE\",\"page\":null}", banner(MEMBERSHIP));

        // when
        String rewritten = PromotionRewriter.withoutFeedPromotions(body);

        // then
        Assertions.assertThat(rewritten).isSameAs(body);
    }

    @Test
    public void leavesTheFeedUntouchedWhenACustomSectionHasNoItems() {
        // given
        String body = feed("{\"type\":\"CUSTOM\",\"customData\":null}", banner(MEMBERSHIP));

        // when
        String rewritten = PromotionRewriter.withoutFeedPromotions(body);

        // then
        Assertions.assertThat(rewritten).isSameAs(body);
    }

    @Test
    public void emptiesThePromotionData() throws JSONException {
        // given
        String body = "{\"code\":0,\"data\":{\"marketingActivityId\":\"1\",\"items\":[{\"sku\":{}}]}}";

        // when
        String rewritten = PromotionRewriter.withoutSkuPromotion(body);

        // then
        JSONObject root = new JSONObject(rewritten);
        Assertions.assertThat(root.isNull("data")).isTrue();
        Assertions.assertThat(root.getInt("code")).isEqualTo(0);
    }

    @Test
    public void keepsPromotionDataThatIsAlreadyEmpty() {
        // given
        String body = "{\"code\":0,\"data\":null}";

        // when
        String rewritten = PromotionRewriter.withoutSkuPromotion(body);

        // then
        Assertions.assertThat(rewritten).isSameAs(body);
    }

    @Test
    public void keepsAPromotionResponseWithoutData() {
        // given
        String body = "{\"code\":0}";

        // when
        String rewritten = PromotionRewriter.withoutSkuPromotion(body);

        // then
        Assertions.assertThat(rewritten).isSameAs(body);
    }

    @Test
    public void keepsAPromotionResponseThatIsNotJson() {
        // when
        String rewritten = PromotionRewriter.withoutSkuPromotion("nope");

        // then
        Assertions.assertThat(rewritten).isEqualTo("nope");
    }

    private static List<String> bannerLinks(String body) throws JSONException {
        JSONArray banners = sections(body).getJSONObject(0).getJSONObject("banner").getJSONArray("banners");
        List<String> links = new ArrayList<>();
        for (int i = 0; i < banners.length(); i++) {
            links.add(banners.getJSONObject(i).getString("deepLink"));
        }
        return links;
    }

    private static List<String> customLinks(String body) throws JSONException {
        JSONArray items = sections(body).getJSONObject(0).getJSONObject("customData").getJSONArray("items");
        List<String> links = new ArrayList<>();
        for (int i = 0; i < items.length(); i++) {
            links.add(items.getJSONObject(i).getString("deepLink"));
        }
        return links;
    }

    private static List<String> titles(String body) throws JSONException {
        JSONArray sections = sections(body);
        List<String> titles = new ArrayList<>();
        for (int i = 0; i < sections.length(); i++) {
            titles.add(sections.getJSONObject(i).optString("title"));
        }
        return titles;
    }

    private static JSONArray sections(String body) throws JSONException {
        return new JSONObject(body).getJSONObject("data").getJSONArray("items");
    }

    private static String feed(String... sections) {
        return "{\"data\":{\"items\":[" + String.join(",", sections) + "]}}";
    }

    private static String banner(String... deepLinks) {
        return "{\"type\":\"BANNER\",\"title\":\"Banner\",\"banner\":{\"banners\":[" + entries(deepLinks) + "]}}";
    }

    private static String image(String deepLink, String title) {
        return "{\"type\":\"SINGLE_IMAGE\",\"title\":\"" + title + "\",\"page\":{\"deepLink\":\"" + deepLink
                + "\"}}";
    }

    private static String custom(String... deepLinks) {
        return "{\"type\":\"CUSTOM\",\"customData\":{\"items\":[" + entries(deepLinks) + "]}}";
    }

    private static String entries(String... deepLinks) {
        StringBuilder entries = new StringBuilder();
        for (String deepLink : deepLinks) {
            entries.append((entries.length() > 0) ? "," : "").append("{\"deepLink\":\"").append(deepLink).append("\"}");
        }
        return entries.toString();
    }

    private static String content(String title) {
        return "{\"type\":\"SUBJECTS_MOVIE\",\"title\":\"" + title + "\",\"subjects\":[{\"subjectId\":\"1\"}]}";
    }

}
