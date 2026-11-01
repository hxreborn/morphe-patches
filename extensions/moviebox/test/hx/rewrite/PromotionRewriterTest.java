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

    private static final String MEMBER_ACTIVITY = "oneroom://com.community.oneroom?type=/member/MemberActivity";

    private static final String WEB = "oneroom://com.community.oneroom?type=/web/web&url=https%3A%2F%2Fexample.com";

    private static final String PROFILE = "oneroom://com.community.oneroom?type=/profile/tv_download_web_link&index=0";

    private static final String ACTIVITY_PAGE = "https://activity.example.com/spa/invite-friends?activityId=1";

    @Test
    public void removesPromotionBannersAndKeepsContentBanners() throws JSONException {
        // given
        String body = feed(banner(MEMBER_ACTIVITY, MOVIE, CATEGORY, WEB));

        // when
        String rewritten = PromotionRewriter.withoutFeedPromotions(body);

        // then
        JSONArray banners = items(rewritten).getJSONObject(0).getJSONObject("banner").getJSONArray("banners");
        Assertions.assertThat(banners.length()).isEqualTo(2);
        Assertions.assertThat(banners.getJSONObject(0).getString("deepLink")).isEqualTo(MOVIE);
        Assertions.assertThat(banners.getJSONObject(1).getString("deepLink")).isEqualTo(CATEGORY);
    }

    @Test
    public void removesABannerSectionWhoseBannersAreAllPromotions() throws JSONException {
        // given
        String body = feed(banner(MEMBER_ACTIVITY, WEB), content("Trending"));

        // when
        String rewritten = PromotionRewriter.withoutFeedPromotions(body);

        // then
        JSONArray items = items(rewritten);
        Assertions.assertThat(items.length()).isEqualTo(1);
        Assertions.assertThat(items.getJSONObject(0).getString("title")).isEqualTo("Trending");
    }

    @Test
    public void removesSingleImageSectionsThatLeaveTheContent() throws JSONException {
        // given
        String body = feed(image(ACTIVITY_PAGE), image(PROFILE), image(CATEGORY));

        // when
        String rewritten = PromotionRewriter.withoutFeedPromotions(body);

        // then
        JSONArray items = items(rewritten);
        Assertions.assertThat(items.length()).isEqualTo(1);
        Assertions.assertThat(items.getJSONObject(0).getJSONObject("page").getString("deepLink")).isEqualTo(CATEGORY);
    }

    @Test
    public void removesPromotionsFromCustomSections() throws JSONException {
        // given
        String body = feed(custom(WEB), custom(MOVIE, WEB));

        // when
        String rewritten = PromotionRewriter.withoutFeedPromotions(body);

        // then
        JSONArray items = items(rewritten);
        Assertions.assertThat(items.length()).isEqualTo(1);
        Assertions.assertThat(items.getJSONObject(0).getJSONObject("customData").getJSONArray("items").length())
            .isEqualTo(1);
    }

    @Test
    public void keepsSectionsWithoutADeepLink() {
        // given
        String body = feed(content("Trending"), banner(""));

        // when
        String rewritten = PromotionRewriter.withoutFeedPromotions(body);

        // then
        Assertions.assertThat(rewritten).isSameAs(body);
    }

    @Test
    public void keepsAFeedWithoutPromotions() {
        // given
        String body = feed(banner(MOVIE, CATEGORY), image(MOVIE), custom(MOVIE));

        // when
        String rewritten = PromotionRewriter.withoutFeedPromotions(body);

        // then
        Assertions.assertThat(rewritten).isSameAs(body);
    }

    @Test
    public void keepsAFeedResponseWithoutItems() {
        // given
        String body = "{\"data\":{}}";

        // when
        String rewritten = PromotionRewriter.withoutFeedPromotions(body);

        // then
        Assertions.assertThat(rewritten).isSameAs(body);
    }

    @Test
    public void keepsAFeedThatIsNotJson() {
        // when
        String rewritten = PromotionRewriter.withoutFeedPromotions("nope");

        // then
        Assertions.assertThat(rewritten).isEqualTo("nope");
    }

    @Test
    public void emptiesTheSkuPromotion() throws JSONException {
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
    public void keepsAnEmptySkuPromotion() {
        // given
        String body = "{\"code\":0,\"data\":null}";

        // when
        String rewritten = PromotionRewriter.withoutSkuPromotion(body);

        // then
        Assertions.assertThat(rewritten).isSameAs(body);
    }

    private static JSONArray items(String body) throws JSONException {
        return new JSONObject(body).getJSONObject("data").getJSONArray("items");
    }

    private static String feed(String... items) {
        return "{\"data\":{\"items\":[" + String.join(",", items) + "]}}";
    }

    private static String banner(String... deepLinks) {
        return "{\"type\":\"BANNER\",\"title\":\"Banner\",\"banner\":{\"banners\":[" + entries(deepLinks) + "]}}";
    }

    private static String image(String deepLink) {
        return "{\"type\":\"SINGLE_IMAGE\",\"banner\":null,\"page\":{\"deepLink\":\"" + deepLink + "\"}}";
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
