/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.notesnook;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

public final class ProUnlock {
    private static final int PLAN_BELIEVER = 3;
    private static final int STATUS_ACTIVE = 0;
    private static final int MAX_BODY_BYTES = 64 * 1024;

    private ProUnlock() {}

    public static byte[] rewriteSubscription(byte[] body) {
        if (body == null || body.length == 0 || body.length > MAX_BODY_BYTES) {
            return body;
        }

        String json = new String(body, StandardCharsets.UTF_8);
        if (!json.contains("\"subscription\"")) {
            return body;
        }

        try {
            JSONObject user = new JSONObject(json);
            JSONObject subscription = user.optJSONObject("subscription");
            if (subscription == null) {
                return body;
            }

            subscription.put("plan", PLAN_BELIEVER);
            subscription.put("status", STATUS_ACTIVE);
            return user.toString().getBytes(StandardCharsets.UTF_8);
        } catch (JSONException e) {
            return body;
        }
    }
}
