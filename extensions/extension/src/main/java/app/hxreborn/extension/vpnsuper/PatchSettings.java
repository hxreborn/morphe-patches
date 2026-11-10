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
package app.hxreborn.extension.vpnsuper;

import android.content.Context;
import android.content.SharedPreferences;

final class PatchSettings {

    static final String LOG_TAG = "hxVpnSuper";

    static final String DISCONNECT_FROM_NOTIFICATION = "disconnect_from_notification";

    private static final String PREFERENCES_NAME = "hx_vpnsuper_patches";

    private PatchSettings() {
    }

    static boolean isDisconnectFromNotificationEnabled(Context context) {
        return AppliedPatches.disconnectFromNotification() && isEnabled(context, DISCONNECT_FROM_NOTIFICATION);
    }

    static boolean isEnabled(Context context, String key) {
        return preferences(context).getBoolean(key, true);
    }

    static void setEnabled(Context context, String key, boolean enabled) {
        preferences(context).edit().putBoolean(key, enabled).commit();
    }

    static void resetToDefaults(Context context) {
        preferences(context).edit().clear().commit();
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

}
