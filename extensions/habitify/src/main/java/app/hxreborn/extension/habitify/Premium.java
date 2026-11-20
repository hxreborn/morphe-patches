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
package app.hxreborn.extension.habitify;

import java.util.HashMap;
import java.util.Map;

import android.net.Uri;

@SuppressWarnings("unused")
public final class Premium {

    private static final String USER_PATH_PREFIX = "/users/";

    private static final String USER_ROOT_PATH = "";

    private static final String SUBSCRIPTION_PATH = "/subscription";

    private static final String ANDROID_PREMIUM_PATH = "/premiumStatusAndroid";

    private static final String ANDROID_PREMIUM_KEY = "premiumStatusAndroid";

    private static final String SUBSCRIPTION_KEY = "subscription";

    private static final String PRODUCT_ID_KEY = "productId";

    private static final String EXPIRES_AT_KEY = "expiresAt";

    private static final String PRO_PRODUCT = "PRO";

    private Premium() {
    }

    static String currentUserId() {
        return null;
    }

    public static Object rewriteNode(Object reference, Object indexedNode) {
        return indexedNode;
    }

    public static String userRelativePath(String url, String userId) {
        if (userId == null || userId.isEmpty()) {
            return null;
        }
        String path = Uri.parse(url).getPath();
        String userPath = USER_PATH_PREFIX + userId;
        if (userPath.equals(path)) {
            return USER_ROOT_PATH;
        }
        if ((userPath + SUBSCRIPTION_PATH).equals(path)) {
            return SUBSCRIPTION_PATH;
        }
        if ((userPath + ANDROID_PREMIUM_PATH).equals(path)) {
            return ANDROID_PREMIUM_PATH;
        }
        return null;
    }

    public static Object unlockValue(String path, Object value) {
        if (path == null) {
            return value;
        }
        if (ANDROID_PREMIUM_PATH.equals(path)) {
            return 1L;
        }
        if (SUBSCRIPTION_PATH.equals(path)) {
            if (isLifetimePro(value)) {
                return value;
            }
            Map<String, Object> subscription = copyMap(value);
            grantLifetimePro(subscription);
            return subscription;
        }
        if (!(value instanceof Map<?, ?>)) {
            return value;
        }
        Map<?, ?> user = (Map<?, ?>) value;
        if (Long.valueOf(1L).equals(user.get(ANDROID_PREMIUM_KEY)) && isLifetimePro(user.get(SUBSCRIPTION_KEY))) {
            return value;
        }
        Map<String, Object> result = copyMap(value);
        result.put(ANDROID_PREMIUM_KEY, 1L);
        Map<String, Object> subscription = copyMap(result.get(SUBSCRIPTION_KEY));
        grantLifetimePro(subscription);
        result.put(SUBSCRIPTION_KEY, subscription);
        return result;
    }

    private static void grantLifetimePro(Map<String, Object> subscription) {
        subscription.put(PRODUCT_ID_KEY, PRO_PRODUCT);
        subscription.remove(EXPIRES_AT_KEY);
    }

    private static boolean isLifetimePro(Object value) {
        if (!(value instanceof Map<?, ?>)) {
            return false;
        }
        Map<?, ?> subscription = (Map<?, ?>) value;
        return PRO_PRODUCT.equals(subscription.get(PRODUCT_ID_KEY)) && !subscription.containsKey(EXPIRES_AT_KEY);
    }

    private static Map<String, Object> copyMap(Object value) {
        Map<String, Object> result = new HashMap<>();
        if (value instanceof Map<?, ?>) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                if (entry.getKey() instanceof String) {
                    result.put((String) entry.getKey(), entry.getValue());
                }
            }
        }
        return result;
    }

}
