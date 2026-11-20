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
package app.hxreborn.extension.liftoff;

import java.nio.charset.StandardCharsets;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import android.content.Context;
import app.morphe.extension.shared.Utils;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = EveryPatchApplied.class)
public final class CustomizationsTest {

    private static final String PROFILE_URL = "https://v2-17-5.api.getgymbros.com/api/trpc/"
            + "user.findUserProfileByAccessToken";

    @Before
    public void useApplicationContext() {
        Utils.setContext(RuntimeEnvironment.getApplication());
        RuntimeEnvironment.getApplication()
            .getSharedPreferences(PatchSettings.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit();
    }

    @After
    public void clearPendingResponse() {
        Customizations.rewriteBody((String) null);
        RuntimeEnvironment.getApplication()
            .getSharedPreferences(PatchSettings.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit();
    }

    @Test
    public void unlocksCustomizationsWithoutChangingBalanceOrEquippedItems() throws JSONException {
        Customizations.onResponse(PROFILE_URL);
        JSONObject profile = profile(new JSONObject(Customizations.rewriteBody(response().toString())));
        JSONObject purchases = profile.getJSONObject("purchases");

        Assert.assertTrue(contains(purchases.getJSONArray("purchasedThemes"), "theme_neonRider"));
        Assert.assertTrue(contains(purchases.getJSONArray("purchasedProfileBorders"), "border_steelShade"));
        Assert.assertTrue(contains(purchases.getJSONArray("purchasedProfileBanners"), "banner_constellation"));
        Assert.assertTrue(contains(purchases.getJSONArray("purchasedThemes"), "theme_dark"));
        Assert.assertEquals(3, profile.getInt("eggs"));
        Assert.assertEquals("border_defaultColor", profile.getString("equippedProfileBorder"));
        Assert.assertEquals("title_default", purchases.getJSONArray("collectedTitles").getString(0));
    }

    @Test
    public void rewritesOnlySelfProfileInBatchedResponses() throws JSONException {
        String url = "https://v2-17-5.api.getgymbros.com/api/trpc/"
                + "user.byUserIdOrUsernameForProfile,user.findUserProfileByAccessToken?batch=1";
        JSONArray batch = new JSONArray().put(response()).put(response());
        Customizations.onResponse(url);
        JSONArray rewritten = new JSONArray(Customizations.rewriteBody(batch.toString()));

        Assert.assertEquals(1,
                profile(rewritten.getJSONObject(0)).getJSONObject("purchases")
                    .getJSONArray("purchasedThemes")
                    .length());
        Assert.assertTrue(
                contains(profile(rewritten.getJSONObject(1)).getJSONObject("purchases").getJSONArray("purchasedThemes"),
                        "theme_neonRider"));
    }

    @Test
    public void rewritesBlobBytes() throws JSONException {
        byte[] body = response().toString().getBytes(StandardCharsets.UTF_8);
        Customizations.onResponse(PROFILE_URL);
        byte[] rewritten = Customizations.rewriteBody(body);
        JSONObject profile = profile(new JSONObject(new String(rewritten, StandardCharsets.UTF_8)));

        Assert.assertTrue(
                contains(profile.getJSONObject("purchases").getJSONArray("purchasedThemes"), "theme_neonRider"));
        Assert.assertTrue(rewritten.length > body.length);
    }

    @Test
    public void consumesResponseScopeOnce() throws JSONException {
        String body = response().toString();
        Customizations.onResponse(PROFILE_URL);
        Customizations.rewriteBody(body);

        Assert.assertSame(body, Customizations.rewriteBody(body));
    }

    @Test
    public void preservesForeignHostsAndOtherProfiles() throws JSONException {
        String body = response().toString();
        Customizations.onResponse("https://example.com/api/trpc/user.findUserProfileByAccessToken");
        Assert.assertSame(body, Customizations.rewriteBody(body));

        Customizations.onResponse("https://v2-17-5.api.getgymbros.com/api/trpc/user.byUserIdOrUsernameForProfile");
        Assert.assertSame(body, Customizations.rewriteBody(body));
    }

    @Test
    public void clearsResponseScopeForUnrelatedResponses() throws JSONException {
        String body = response().toString();
        Customizations.onResponse(PROFILE_URL);
        Customizations.onResponse("https://v2-17-5.api.getgymbros.com/api/trpc/user.getUserData");

        Assert.assertSame(body, Customizations.rewriteBody(body));
    }

    @Test
    public void preservesMalformedBodiesAndApiErrors() {
        Customizations.onResponse(PROFILE_URL);
        Assert.assertEquals("{", Customizations.rewriteBody("{"));

        String error = "{\"error\":{\"json\":{\"message\":\"Session expired\"}}}";
        Customizations.onResponse(PROFILE_URL);
        Assert.assertSame(error, Customizations.rewriteBody(error));
    }

    @Test
    public void keepsOwnershipListsUniqueAcrossRepeatedRewrites() throws JSONException {
        Customizations.onResponse(PROFILE_URL);
        String first = Customizations.rewriteBody(response().toString());
        Customizations.onResponse(PROFILE_URL);
        String second = Customizations.rewriteBody(first);

        Assert.assertSame(first, second);
    }

    @Test
    public void preservesOwnershipWhenSwitchIsOff() throws JSONException {
        PatchSettings.setEnabled(AppliedPatches.UNLOCK_CUSTOMIZATIONS, false);
        String body = response().toString();
        Customizations.onResponse(PROFILE_URL);

        Assert.assertSame(body, Customizations.rewriteBody(body));
    }

    private static JSONObject response() throws JSONException {
        JSONObject purchases = new JSONObject().put("purchasedThemes", new JSONArray().put("theme_dark"))
            .put("purchasedProfileBorders", new JSONArray().put("border_defaultColor"))
            .put("purchasedProfileBanners", new JSONArray().put("banner_defaultColor"))
            .put("collectedTitles", new JSONArray().put("title_default"));
        JSONObject profile = new JSONObject().put("eggs", 3)
            .put("equippedProfileBorder", "border_defaultColor")
            .put("purchases", purchases);
        return new JSONObject().put("result", new JSONObject().put("data", new JSONObject().put("json", profile)));
    }

    private static JSONObject profile(JSONObject response) throws JSONException {
        return response.getJSONObject("result").getJSONObject("data").getJSONObject("json");
    }

    private static boolean contains(JSONArray values, String expected) {
        for (int index = 0; index < values.length(); index++) {
            if (expected.equals(values.optString(index))) {
                return true;
            }
        }
        return false;
    }

}
