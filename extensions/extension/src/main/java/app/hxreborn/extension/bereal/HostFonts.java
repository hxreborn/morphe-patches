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
package app.hxreborn.extension.bereal;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Build;
import android.util.Log;

final class HostFonts {

    private static final String TAG = "HostFonts";

    final Typeface regular;

    final Typeface medium;

    final Typeface semibold;

    HostFonts(Context context) {
        this.regular = load(context, interRegular(), Typeface.DEFAULT);
        this.medium = load(context, interMedium(), Typeface.DEFAULT);
        this.semibold = load(context, interSemibold(), Typeface.DEFAULT_BOLD);
    }

    static int interRegular() {
        return 0;
    }

    static int interMedium() {
        return 0;
    }

    static int interSemibold() {
        return 0;
    }

    private static Typeface load(Context context, int id, Typeface fallback) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return fallback;
        }
        try {
            return context.getResources().getFont(id);
        } catch (Resources.NotFoundException ex) {
            Log.e(TAG, "Host font not loaded: " + id, ex);
            return fallback;
        }
    }

}
