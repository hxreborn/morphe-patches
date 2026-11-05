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
package app.hxreborn.extension.ultimateguitar;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import android.net.Uri;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

@SuppressWarnings("unused")
public final class ApiResponseRewriter {

    private static final String LOG_TAG = "hxUltimateGuitar";

    private static final String SERVICE_ACCESS = "service_access";

    private static final byte[] SERVICE_ACCESS_KEY = ("\"" + SERVICE_ACCESS + "\"").getBytes(StandardCharsets.UTF_8);

    private static final String SERVICE_NAME = "service_name";

    private static final List<String> GRANTED_SERVICES = Collections
        .unmodifiableList(Arrays.asList("android_ug_pro_account", "android_ug_mobile_account", "android_ug_edu_courses",
                "android_ug_sing", "android_ug_practice", "android_ug_ebook"));

    private static final long ACCESS_END_EPOCH_SECONDS = 4102444800L;

    private static final String SERVICE_ACCESS_PATH = "/api/v1/purchase/service/access";

    private static final String EXPERIMENTS_PATH = "/api/v1/ab/test";

    private static final String ACTIVE_EXPERIMENTS = "experiments";

    private static final String CLOSED_EXPERIMENTS = "closed_experiments";

    private static final String WEB_PRINT_EXPERIMENT = "8078";

    private static final String SING_EXPERIMENT = "8000";

    private static final String PDF_UPSELL_EXPERIMENT = "7061";

    private static final int CONTROL_VARIATION = 1;

    private static final int ENABLED_VARIATION = 2;

    private static final int HTTP_OK = 200;

    private static final int ENABLED_FLAG = 1;

    private static final int DISABLED_FLAG = 0;

    private static final ThreadLocal<String> RESPONSE_PATH = new ThreadLocal<>();

    private ApiResponseRewriter() {
    }

    public static int rewriteStatus(String url, int statusCode) {
        String path = (url != null) ? Uri.parse(url).getPath() : null;
        RESPONSE_PATH.set(path);
        return SERVICE_ACCESS_PATH.equals(path) ? HTTP_OK : statusCode;
    }

    public static byte[] rewriteBody(byte[] body) {
        String path = takeResponsePath();
        if (SERVICE_ACCESS_PATH.equals(path)) {
            return replaceServiceAccessBody((body != null) ? new String(body, StandardCharsets.UTF_8) : "")
                .getBytes(StandardCharsets.UTF_8);
        }
        if (EXPERIMENTS_PATH.equals(path) && body != null) {
            return assignExperiments(new String(body, StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8);
        }
        if (body == null || indexOf(body, SERVICE_ACCESS_KEY) < 0) {
            return body;
        }
        return grantInJson(new String(body, StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8);
    }

    public static String rewriteBody(String body) {
        String path = takeResponsePath();
        if (SERVICE_ACCESS_PATH.equals(path)) {
            return replaceServiceAccessBody((body != null) ? body : "");
        }
        if (EXPERIMENTS_PATH.equals(path) && body != null) {
            return assignExperiments(body);
        }
        return grantInJson(body);
    }

    private static String takeResponsePath() {
        try {
            return RESPONSE_PATH.get();
        } finally {
            RESPONSE_PATH.remove();
        }
    }

    private static String assignExperiments(String body) {
        try {
            JSONObject root = new JSONObject(body);
            JSONObject active = root.optJSONObject(ACTIVE_EXPERIMENTS);
            if (active == null) {
                active = new JSONObject();
                root.put(ACTIVE_EXPERIMENTS, active);
            }
            assignVariations(active);
            JSONObject closed = root.optJSONObject(CLOSED_EXPERIMENTS);
            if (closed != null) {
                assignVariations(closed);
            }
            return root.toString();
        } catch (JSONException ex) {
            Log.w(LOG_TAG, "Cannot parse the experiment assignments", ex);
            return body;
        }
    }

    private static void assignVariations(JSONObject experiments) throws JSONException {
        experiments.put(WEB_PRINT_EXPERIMENT, CONTROL_VARIATION)
            .put(PDF_UPSELL_EXPERIMENT, CONTROL_VARIATION)
            .put(SING_EXPERIMENT, ENABLED_VARIATION);
    }

    private static String grantInJson(String body) {
        if (body == null || !body.contains(SERVICE_ACCESS)) {
            return body;
        }
        try {
            Object root = new JSONTokener(body).nextValue();
            return grantInNode(root) ? root.toString() : body;
        } catch (JSONException ex) {
            Log.w(LOG_TAG, "Cannot parse a response that lists service access", ex);
            return body;
        }
    }

    private static String replaceServiceAccessBody(String body) {
        try {
            return withGrantedServices(parseServerServices(body)).toString();
        } catch (JSONException ex) {
            Log.e(LOG_TAG, "Cannot build the service access list", ex);
            return body;
        }
    }

    private static JSONArray parseServerServices(String body) {
        try {
            Object root = new JSONTokener(body).nextValue();
            return (root instanceof JSONArray) ? (JSONArray) root : new JSONArray();
        } catch (JSONException ex) {
            Log.w(LOG_TAG, "Cannot parse the service access list", ex);
            return new JSONArray();
        }
    }

    private static boolean grantInNode(Object node) throws JSONException {
        boolean granted = false;
        if (node instanceof JSONObject) {
            JSONObject object = (JSONObject) node;
            Iterator<String> keys = object.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                Object value = object.get(key);
                if (SERVICE_ACCESS.equals(key) && value instanceof JSONArray) {
                    object.put(key, withGrantedServices((JSONArray) value));
                    granted = true;
                } else {
                    granted |= grantInNode(value);
                }
            }
        } else if (node instanceof JSONArray) {
            JSONArray array = (JSONArray) node;
            for (int i = 0; i < array.length(); i++) {
                granted |= grantInNode(array.get(i));
            }
        }
        return granted;
    }

    private static JSONArray withGrantedServices(JSONArray services) throws JSONException {
        JSONArray merged = new JSONArray();
        for (int i = 0; i < services.length(); i++) {
            JSONObject service = services.optJSONObject(i);
            if (service == null || !GRANTED_SERVICES.contains(service.optString(SERVICE_NAME))) {
                merged.put(services.get(i));
            }
        }
        for (String name : GRANTED_SERVICES) {
            merged.put(lifetimeAccess(name));
        }
        return merged;
    }

    private static JSONObject lifetimeAccess(String serviceName) throws JSONException {
        return new JSONObject().put(SERVICE_NAME, serviceName)
            .put("date_access", ACCESS_END_EPOCH_SECONDS)
            .put("lifetime", ENABLED_FLAG)
            .put("trial", DISABLED_FLAG)
            .put("auto_renew", DISABLED_FLAG)
            .put("payment_state", ENABLED_FLAG)
            .put("product_id", JSONObject.NULL)
            .put("product_group", JSONObject.NULL)
            .put("product_code", JSONObject.NULL);
    }

    private static int indexOf(byte[] haystack, byte[] needle) {
        for (int start = 0; start <= haystack.length - needle.length; start++) {
            int matched = 0;
            while (matched < needle.length && haystack[start + matched] == needle[matched]) {
                matched++;
            }
            if (matched == needle.length) {
                return start;
            }
        }
        return -1;
    }

}
