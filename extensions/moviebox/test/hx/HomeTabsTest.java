/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package hx;

import java.util.Arrays;
import java.util.HashSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public final class HomeTabsTest {

    private static final String TABS = "{\"data\":{\"bottomTabs\":[{\"subTabs\":[{\"tabCode\":\"home\"},"
            + "{\"tabCode\":\"live\"},{\"tabCode\":\"shorts\"}]},{\"name\":\"no sub tabs\"}]}}";

    @Test
    public void removesTheHiddenSubTabs() throws JSONException {
        // when
        String rewritten = HomeTabs.withoutTabs(TABS, new HashSet<>(Arrays.asList("live", "shorts")));

        // then
        JSONArray subTabs = new JSONObject(rewritten).getJSONObject("data")
            .getJSONArray("bottomTabs")
            .getJSONObject(0)
            .getJSONArray("subTabs");
        Assertions.assertThat(subTabs.length()).isEqualTo(1);
        Assertions.assertThat(subTabs.getJSONObject(0).getString("tabCode")).isEqualTo("home");
    }

    @Test
    public void keepsTheResponseWhenNoTabMatches() {
        // when
        String rewritten = HomeTabs.withoutTabs(TABS, new HashSet<>(Arrays.asList("missing")));

        // then
        Assertions.assertThat(rewritten).isSameAs(TABS);
    }

    @Test
    public void keepsAResponseWithoutBottomTabs() {
        // given
        String body = "{\"data\":{}}";

        // when
        String rewritten = HomeTabs.withoutTabs(body, new HashSet<>(Arrays.asList("live")));

        // then
        Assertions.assertThat(rewritten).isSameAs(body);
    }

    @Test
    public void keepsAResponseThatIsNotJson() {
        // when
        String rewritten = HomeTabs.withoutTabs("nope", new HashSet<>(Arrays.asList("live")));

        // then
        Assertions.assertThat(rewritten).isEqualTo("nope");
    }

}
