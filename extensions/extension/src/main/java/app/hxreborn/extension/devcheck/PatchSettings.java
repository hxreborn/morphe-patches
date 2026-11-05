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
package app.hxreborn.extension.devcheck;

import android.content.Context;
import android.content.SharedPreferences;

final class PatchSettings {

    static final String LOG_TAG = "hxDevCheck";

    private static final String PREFERENCES_FILE = "hx_devcheck_patches";

    private static final String AMOLED_KEY = "amoled";

    private PatchSettings() {
    }

    static boolean isAmoled(Context context) {
        return preferences(context).getBoolean(AMOLED_KEY, false);
    }

    static void setAmoled(Context context, boolean enabled) {
        preferences(context).edit().putBoolean(AMOLED_KEY, enabled).apply();
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
    }

}
