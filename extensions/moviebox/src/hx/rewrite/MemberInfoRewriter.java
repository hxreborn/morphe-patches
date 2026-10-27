/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * See LICENSE for GPLv3.
 * Additional term under GPLv3 Section 7(b): When conveying this material
 * or modified versions, preserve this notice and credit hxreborn for this
 * material in any Appropriate Legal Notices displayed by works containing it.
 */
package hx.rewrite;

import android.util.Log;
import hx.util.Logs;
import org.json.JSONException;
import org.json.JSONObject;

final class MemberInfoRewriter {

    private static final int DAYS_LEFT = 9999;

    private static final String EXPIRY_DATE = "2099-12-31";

    private MemberInfoRewriter() {
    }

    static String rewrite(String body) {
        try {
            JSONObject root = new JSONObject(body);
            return JsonTree.rewriteObjects(root, MemberInfoRewriter::extendMembership) ? root.toString() : body;
        } catch (JSONException ex) {
            Log.w(Logs.TAG, "member info not rewritten: " + ex.getMessage());
            return body;
        }
    }

    private static boolean extendMembership(JSONObject node) throws JSONException {
        if (!node.has("daysLeft") || !node.has("memberType") || !node.has("expiryDate")) {
            return false;
        }
        node.put("daysLeft", DAYS_LEFT);
        node.put("expiryDate", EXPIRY_DATE);
        return true;
    }

}
