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
package app.hxreborn.extension.narababy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.util.Log;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@SuppressWarnings("unused")
public final class SubscriptionCards {

    private static final String TAG = "NaraSubscriptionCards";

    private static final long MAX_RESPONSE_BYTES = 1048576L;

    private static final String DYNAMIC_PAGE_PATH_SUFFIX = "/dynamicPage";

    private static final String ACTIVITY_CARD_PREFIX = "activity-card/";

    private static final String SUBSCRIPTION_ROUTE = "/account/subscription";

    private static final String NAVIGATE_ACTION = "navigate";

    private static final String PAGES_KEY = "pages";

    private static final String CARD_KEY = "card";

    private static final String ACTION_TYPE_KEY = "type";

    private static final String ACTION_ROUTE_KEY = "route";

    private SubscriptionCards() {

    }

    public static Response removeSubscriptionCards(Response response) {
        if (response.body() == null || !response.request().url().encodedPath().endsWith(DYNAMIC_PAGE_PATH_SUFFIX)) {
            return response;
        }
        try {
            ResponseBody preview = response.peekBody(MAX_RESPONSE_BYTES + 1);
            if (preview.contentLength() > MAX_RESPONSE_BYTES) {
                return response;
            }
            String body = preview.string();
            String filtered = removeSubscriptionCards(body);
            if (body.equals(filtered)) {
                return response;
            }
            ResponseBody replacement = ResponseBody.create(response.body().contentType(), filtered);
            return response.newBuilder().removeHeader("Content-Length").body(replacement).build();
        } catch (IOException ex) {
            Log.e(TAG, "Could not read activity cards", ex);
            return response;
        }
    }

    static String removeSubscriptionCards(String body) {
        if (!body.contains(ACTIVITY_CARD_PREFIX) || !body.contains(SUBSCRIPTION_ROUTE)) {
            return body;
        }
        try {
            JSONObject response = new JSONObject(body);
            JSONObject pages = response.optJSONObject(PAGES_KEY);
            if (pages == null) {
                return body;
            }
            List<String> removed = new ArrayList<>();
            Iterator<String> keys = pages.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                JSONObject page = pages.optJSONObject(key);
                if (key.startsWith(ACTIVITY_CARD_PREFIX) && page != null
                        && containsSubscriptionAction(page.optJSONObject(CARD_KEY))) {
                    removed.add(key);
                }
            }
            for (String key : removed) {
                pages.remove(key);
            }
            return removed.isEmpty() ? body : response.toString();
        } catch (JSONException ex) {
            Log.e(TAG, "Could not remove subscription cards");
            return body;
        }
    }

    private static boolean containsSubscriptionAction(Object value) throws JSONException {
        if (value instanceof JSONObject) {
            JSONObject object = (JSONObject) value;
            if (NAVIGATE_ACTION.equals(object.optString(ACTION_TYPE_KEY))
                    && SUBSCRIPTION_ROUTE.equals(object.optString(ACTION_ROUTE_KEY))) {
                return true;
            }
            Iterator<String> keys = object.keys();
            while (keys.hasNext()) {
                if (containsSubscriptionAction(object.get(keys.next()))) {
                    return true;
                }
            }
        } else if (value instanceof JSONArray) {
            JSONArray array = (JSONArray) value;
            for (int i = 0; i < array.length(); i++) {
                if (containsSubscriptionAction(array.get(i))) {
                    return true;
                }
            }
        }
        return false;
    }

}
