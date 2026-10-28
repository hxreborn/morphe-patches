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
package app.hxreborn.extension.shared;

import java.nio.charset.StandardCharsets;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;
import org.json.JSONObject;

@SuppressWarnings("unused")
public final class AppCheckTokenSeed {

    private static final String TAG = "AppCheckTokenSeed";

    private static final String STORE_PREFIX = "com.google.firebase.appcheck.store.";

    private static final String TOKEN_KEY = "com.google.firebase.appcheck.APP_CHECK_TOKEN";

    private static final String TOKEN_TYPE_KEY = "com.google.firebase.appcheck.TOKEN_TYPE";

    private static final String DEFAULT_TOKEN_TYPE = "DEFAULT_APP_CHECK_TOKEN";

    private static final String DEFAULT_APP_NAME = "[DEFAULT]";

    private static final int PERSISTENCE_KEY_FLAGS = Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP;

    private AppCheckTokenSeed() {
    }

    public static void seed(Context context) {
        try {
            String token = token();
            long[] validityMillis = validityMillis(token);
            long now = System.currentTimeMillis();
            if (validityMillis[1] <= now) {
                return;
            }
            SharedPreferences store = store(context);
            if (store == null || storedExpiryMillis(store) > now) {
                return;
            }
            String value = new JSONObject().put("token", token)
                .put("receivedAt", validityMillis[0])
                .put("expiresIn", validityMillis[1] - validityMillis[0])
                .toString();
            store.edit().putString(TOKEN_KEY, value).putString(TOKEN_TYPE_KEY, DEFAULT_TOKEN_TYPE).commit();
        } catch (Exception ex) {
            Log.w(TAG, "App Check token not seeded", ex);
        }
    }

    private static String token() {
        return "<app-check-token>";
    }

    private static SharedPreferences store(Context context) {
        int appIdResource = context.getResources().getIdentifier("google_app_id", "string", context.getPackageName());
        if (appIdResource == 0) {
            return null;
        }
        String persistenceKey = encode(DEFAULT_APP_NAME) + "+" + encode(context.getString(appIdResource));
        return context.getSharedPreferences(STORE_PREFIX + persistenceKey, Context.MODE_PRIVATE);
    }

    private static String encode(String value) {
        return Base64.encodeToString(value.getBytes(StandardCharsets.UTF_8), PERSISTENCE_KEY_FLAGS);
    }

    private static long storedExpiryMillis(SharedPreferences store) {
        try {
            JSONObject stored = new JSONObject(store.getString(TOKEN_KEY, "{}"));
            return stored.getLong("receivedAt") + stored.getLong("expiresIn");
        } catch (Exception ex) {
            return 0;
        }
    }

    private static long[] validityMillis(String token) throws Exception {
        String payload = token.split("\\.")[1];
        JSONObject claims = new JSONObject(
                new String(Base64.decode(payload, Base64.URL_SAFE | Base64.NO_PADDING), StandardCharsets.UTF_8));
        return new long[] { claims.getLong("iat") * 1000, claims.getLong("exp") * 1000 };
    }

}
