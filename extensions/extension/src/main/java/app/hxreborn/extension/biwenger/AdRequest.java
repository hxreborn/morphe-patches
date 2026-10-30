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
package app.hxreborn.extension.biwenger;

import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;

@SuppressWarnings("unused")
public final class AdRequest {

    private static final String TAG = "hxBiwenger";

    private static final String ADMOB_EVENT = "AdMob";

    private static final String AD_MANAGER_EVENT = "AdManager";

    private AdRequest() {
    }

    public static boolean matches(String message) {
        try {
            String event = new JSONObject(message).getString("event");
            return ADMOB_EVENT.equals(event) || AD_MANAGER_EVENT.equals(event);
        } catch (JSONException ex) {
            Log.w(TAG, "Bridge message has no readable event");
            return false;
        }
    }

}
