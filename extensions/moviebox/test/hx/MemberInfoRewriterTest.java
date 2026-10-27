/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package hx;

import org.assertj.core.api.Assertions;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public final class MemberInfoRewriterTest {

    @Test
    public void extendsAMembershipNestedInTheResponse() throws JSONException {
        // given
        String body = "{\"data\":{\"member\":{\"daysLeft\":3,\"memberType\":\"FREE\",\"expiryDate\":\"2026-01-01\"}}}";

        // when
        String rewritten = MemberInfoRewriter.rewrite(body);

        // then
        JSONObject member = new JSONObject(rewritten).getJSONObject("data").getJSONObject("member");
        Assertions.assertThat(member.getInt("daysLeft")).isEqualTo(9999);
        Assertions.assertThat(member.getString("expiryDate")).isEqualTo("2099-12-31");
        Assertions.assertThat(member.getString("memberType")).isEqualTo("FREE");
    }

    @Test
    public void extendsMembershipsInsideArrays() throws JSONException {
        // given
        String body = "{\"data\":[{\"daysLeft\":1,\"memberType\":\"A\",\"expiryDate\":\"x\"}]}";

        // when
        String rewritten = MemberInfoRewriter.rewrite(body);

        // then
        JSONObject member = new JSONObject(rewritten).getJSONArray("data").getJSONObject(0);
        Assertions.assertThat(member.getInt("daysLeft")).isEqualTo(9999);
    }

    @Test
    public void leavesAnObjectMissingAnyMembershipFieldAlone() {
        // given
        String body = "{\"data\":{\"daysLeft\":3,\"memberType\":\"FREE\"}}";

        // when
        String rewritten = MemberInfoRewriter.rewrite(body);

        // then
        Assertions.assertThat(rewritten).isSameAs(body);
    }

    @Test
    public void leavesAResponseThatIsNotJsonAlone() {
        // when
        String rewritten = MemberInfoRewriter.rewrite("nope");

        // then
        Assertions.assertThat(rewritten).isEqualTo("nope");
    }

}
