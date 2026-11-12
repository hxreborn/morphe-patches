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
package app.hxreborn.extension.idlefish;

import android.net.Uri;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;

@SuppressWarnings("unused")
public final class ShareLink {

    private static final String TAG = "hxXianyu";

    private static final String PUBLIC_ITEM_URL_PREFIX = "https://h5.m.goofish.com/item?id=";

    private static final String SHARE_URL_KEY = "url";

    private static final String ITEM_DETAIL_SCHEME = "fleamarket";

    private static final String ITEM_DETAIL_HOST = "awesome_detail";

    private static final String ITEM_ID_PARAMETER = "itemId";

    private ShareLink() {
    }

    public static String publicItemUrl(String shareInfo) {
        if (shareInfo == null) {
            return null;
        }
        Object deepLink;
        try {
            deepLink = new JSONObject(shareInfo).opt(SHARE_URL_KEY);
        } catch (JSONException ex) {
            Log.w(TAG, "Share info is not a JSON object");
            return null;
        }
        if (!(deepLink instanceof String)) {
            return null;
        }
        Uri link = Uri.parse((String) deepLink);
        if (link.isOpaque() || !ITEM_DETAIL_SCHEME.equals(link.getScheme()) || !ITEM_DETAIL_HOST.equals(link.getHost())
                || (link.getPath() != null && !link.getPath().isEmpty())) {
            return null;
        }
        String itemId = link.getQueryParameter(ITEM_ID_PARAMETER);
        if (itemId == null || itemId.isEmpty() || !isAsciiDigits(itemId)) {
            return null;
        }
        return PUBLIC_ITEM_URL_PREFIX + itemId;
    }

    private static boolean isAsciiDigits(String value) {
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character < '0' || character > '9') {
                return false;
            }
        }
        return true;
    }

}
