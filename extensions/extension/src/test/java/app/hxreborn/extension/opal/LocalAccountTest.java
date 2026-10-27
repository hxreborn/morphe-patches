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
package app.hxreborn.extension.opal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class LocalAccountTest {

    private static final String USER_KEY = "com.google.firebase.auth.FIREBASE_USER";

    private static final String USER_ID = "local-user";

    private SharedPreferences store;

    @Before
    public void createStore() {
        this.store = RuntimeEnvironment.getApplication().getSharedPreferences("firebase", Context.MODE_PRIVATE);
    }

    private JSONObject seededUser() throws Exception {
        LocalAccount.seedFirebaseUser(this.store, USER_ID);
        return new JSONObject(this.store.getString(USER_KEY, null));
    }

    private static JSONObject tokenClaims(JSONObject user) throws Exception {
        final String token = new JSONObject(user.getString("cachedTokenState")).getString("access_token");
        final String payload = token.split("\\.")[1];
        final byte[] decoded = Base64.decode(payload, Base64.URL_SAFE);
        return new JSONObject(new String(decoded, StandardCharsets.UTF_8));
    }

    @Test
    public void storesAnAnonymousFirebaseUser() throws Exception {
        // when
        final JSONObject user = seededUser();

        // then
        assertTrue(user.getBoolean("anonymous"));
        assertEquals("[DEFAULT]", user.getString("applicationName"));
        assertEquals("com.google.firebase.auth.internal.DefaultFirebaseUser", user.getString("type"));
    }

    @Test
    public void keepsAnExistingUser() {
        // given
        this.store.edit().putString(USER_KEY, "signed-in").commit();

        // when
        LocalAccount.seedFirebaseUser(this.store, USER_ID);

        // then
        assertEquals("signed-in", this.store.getString(USER_KEY, null));
    }

    @Test
    public void seedsNothingElse() {
        // when
        LocalAccount.seedFirebaseUser(this.store, USER_ID);

        // then
        assertEquals(1, this.store.getAll().size());
    }

    @Test
    public void listsTheUserIdAsTheOnlyProvider() throws Exception {
        // when
        final JSONObject user = seededUser();

        // then
        assertEquals(1, user.getJSONArray("userInfos").length());
        final JSONObject provider = new JSONObject(user.getJSONArray("userInfos").getString(0));
        assertEquals(USER_ID, provider.getString("userId"));
        assertFalse(provider.getBoolean("isEmailVerified"));
    }

    @Test
    public void issuesATokenForTheUser() throws Exception {
        // when
        final JSONObject claims = tokenClaims(seededUser());

        // then
        assertEquals(USER_ID, claims.getString("user_id"));
        assertEquals(USER_ID, claims.getString("sub"));
        assertEquals("opal-fa413", claims.getString("aud"));
        assertEquals("https://securetoken.google.com/opal-fa413", claims.getString("iss"));
        assertEquals("anonymous", claims.getJSONObject("firebase").getString("sign_in_provider"));
    }

    @Test
    public void issuesATokenThatOutlivesTheSession() throws Exception {
        // when
        final JSONObject claims = tokenClaims(seededUser());

        // then
        final long tenYears = 10L * 365 * 24 * 60 * 60;
        assertEquals(tenYears, claims.getLong("exp") - claims.getLong("iat"));
    }

    @Test
    public void sealsTheTokenWithAPlaceholderSignature() throws Exception {
        // when
        final JSONObject user = seededUser();

        // then
        final String token = new JSONObject(user.getString("cachedTokenState")).getString("access_token");
        final String[] parts = token.split("\\.");
        assertEquals(3, parts.length);
        assertEquals("local", parts[2]);
        final byte[] header = Base64.decode(parts[0], Base64.URL_SAFE);
        assertEquals("none", new JSONObject(new String(header, StandardCharsets.UTF_8)).getString("alg"));
    }

    @Test
    public void stampsTheSignInTimeInMilliseconds() throws Exception {
        // when
        final JSONObject user = seededUser();

        // then
        final JSONObject metadata = user.getJSONObject("userMetadata");
        assertEquals(metadata.getLong("creationTimestamp"), metadata.getLong("lastSignInTimestamp"));
        assertEquals(metadata.getLong("creationTimestamp") / 1000, tokenClaims(user).getLong("auth_time"));
    }

}
