/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package hx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

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
        assertEquals(9999, member.getInt("daysLeft"));
        assertEquals("2099-12-31", member.getString("expiryDate"));
        assertEquals("FREE", member.getString("memberType"));
    }

    @Test
    public void extendsMembershipsInsideArrays() throws JSONException {
        // given
        String body = "{\"data\":[{\"daysLeft\":1,\"memberType\":\"A\",\"expiryDate\":\"x\"}]}";

        // when
        String rewritten = MemberInfoRewriter.rewrite(body);

        // then
        JSONObject member = new JSONObject(rewritten).getJSONArray("data").getJSONObject(0);
        assertEquals(9999, member.getInt("daysLeft"));
    }

    @Test
    public void leavesAnObjectMissingAnyMembershipFieldAlone() {
        // given
        String body = "{\"data\":{\"daysLeft\":3,\"memberType\":\"FREE\"}}";

        // when
        String rewritten = MemberInfoRewriter.rewrite(body);

        // then
        assertSame(body, rewritten);
    }

    @Test
    public void leavesAResponseThatIsNotJsonAlone() {
        // when
        String rewritten = MemberInfoRewriter.rewrite("nope");

        // then
        assertEquals("nope", rewritten);
    }

}
