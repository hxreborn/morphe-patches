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
package app.hxreborn.extension.internetspeedmeter;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

final class AppResources {

    static final String PREFERENCES = "net";

    static final String MAIN_ACTIVITY = "com.internet.speed.meter.lite.MainActivity";

    private AppResources() {

    }

    static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }

    static int id(Context context, String type, String name) {
        int id = context.getResources().getIdentifier(name, type, context.getPackageName());
        if (id == 0) {
            throw new IllegalStateException("No " + type + "/" + name + " in " + context.getPackageName());
        }
        return id;
    }

    static String string(Context context, String name) {
        return context.getString(id(context, "string", name));
    }

    static String[] array(Context context, String name) {
        return context.getResources().getStringArray(id(context, "array", name));
    }

    static float[] headerHsv(SharedPreferences preferences) {
        return new float[] { preferences.getFloat("global_hue", 215f), preferences.getFloat("header_saturation", 0.7f),
                preferences.getFloat("header_value", 0.93f) };
    }

    static int headerColor(SharedPreferences preferences) {
        return Color.HSVToColor(headerHsv(preferences));
    }

}
