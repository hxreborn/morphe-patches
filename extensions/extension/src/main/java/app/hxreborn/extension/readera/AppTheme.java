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
package app.hxreborn.extension.readera;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.util.Log;

@SuppressWarnings("unused")
public final class AppTheme {

    private static final String THEME = "theme";

    private static final String STOCK = "stock";

    private static final String AMOLED = "amoled";

    private static final float UNSET_FONT_SCALE = 0f;

    private static boolean amoledApplied;

    private AppTheme() {
    }

    public static Context applyToApplication(Context base) {
        amoledApplied = amoledSelected(base);
        return wrap(base);
    }

    public static Context wrap(Context base) {
        if (!amoledApplied) {
            return base;
        }
        Configuration override = new Configuration();
        override.fontScale = UNSET_FONT_SCALE;
        override.uiMode = Configuration.UI_MODE_NIGHT_YES;
        override.navigation = Configuration.NAVIGATION_WHEEL;
        return base.createConfigurationContext(override);
    }

    static boolean amoledApplied() {
        return amoledApplied;
    }

    static boolean amoledSelected(Context context) {
        return AMOLED.equals(PatchSettings.preferences(context).getString(THEME, STOCK));
    }

    static boolean saveAmoledSelected(Context context, boolean amoled) {
        SharedPreferences preferences = PatchSettings.preferences(context);
        String previous = preferences.getString(THEME, STOCK);
        if (preferences.edit().putString(THEME, amoled ? AMOLED : STOCK).commit()) {
            return true;
        }
        boolean restored = preferences.edit().putString(THEME, previous).commit();
        Log.w(PatchSettings.LOG_TAG,
                restored ? "Could not save the app theme" : "Could not save or restore the app theme");
        return false;
    }

}
