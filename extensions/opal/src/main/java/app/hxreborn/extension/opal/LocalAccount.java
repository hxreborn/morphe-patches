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

import java.nio.charset.StandardCharsets;

import android.content.SharedPreferences;
import android.util.Base64;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@SuppressWarnings("unused")
public final class LocalAccount {

    private static final String USER_KEY = "com.google.firebase.auth.FIREBASE_USER";

    private static final String PROJECT_ID = "opal-fa413";

    private static final String PLACEHOLDER_SECRET = "local";

    private static final long TOKEN_LIFETIME_SECONDS = 10L * 365 * 24 * 60 * 60;

    private LocalAccount() {
    }

    public static void seedFirebaseUser(SharedPreferences store, String userId) {
        if (store.contains(USER_KEY)) {
            return;
        }

        try {
            store.edit().putString(USER_KEY, buildUser(userId).toString()).commit();
        } catch (JSONException ignored) {
        }
    }

    private static JSONObject buildUser(String userId) throws JSONException {
        long now = System.currentTimeMillis() / 1000;

        JSONObject tokenState = new JSONObject().put("refresh_token", PLACEHOLDER_SECRET)
            .put("access_token", buildToken(userId, now))
            .put("expires_in", TOKEN_LIFETIME_SECONDS)
            .put("token_type", "Bearer")
            .put("issued_at", now * 1000);

        JSONObject provider = new JSONObject().put("userId", userId)
            .put("providerId", "firebase")
            .put("isEmailVerified", false);

        JSONObject metadata = new JSONObject().put("lastSignInTimestamp", now * 1000)
            .put("creationTimestamp", now * 1000);

        return new JSONObject().put("cachedTokenState", tokenState.toString())
            .put("applicationName", "[DEFAULT]")
            .put("type", "com.google.firebase.auth.internal.DefaultFirebaseUser")
            .put("userInfos", new JSONArray().put(provider.toString()))
            .put("anonymous", true)
            .put("version", "2.0")
            .put("userMetadata", metadata);
    }

    private static String buildToken(String userId, long now) throws JSONException {
        JSONObject header = new JSONObject().put("alg", "none").put("typ", "JWT");
        JSONObject claims = new JSONObject().put("iss", "https://securetoken.google.com/" + PROJECT_ID)
            .put("aud", PROJECT_ID)
            .put("auth_time", now)
            .put("user_id", userId)
            .put("sub", userId)
            .put("iat", now)
            .put("exp", now + TOKEN_LIFETIME_SECONDS)
            .put("firebase", new JSONObject().put("identities", new JSONObject()).put("sign_in_provider", "anonymous"));
        return encode(header) + "." + encode(claims) + "." + PLACEHOLDER_SECRET;
    }

    private static String encode(JSONObject json) {
        return Base64.encodeToString(json.toString().getBytes(StandardCharsets.UTF_8),
                Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
    }

}
