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
package app.hxreborn.extension.notesnook;

import java.nio.charset.StandardCharsets;

import org.json.JSONException;
import org.json.JSONObject;

public final class ProUnlock {

    private static final int PLAN_BELIEVER = 3;

    private static final int STATUS_ACTIVE = 0;

    private static final int MAX_BODY_BYTES = 64 * 1024;

    private ProUnlock() {

    }

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
        } catch (JSONException ex) {
            return body;
        }
    }

}
