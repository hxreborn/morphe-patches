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
package hx.platform;

import java.io.FileNotFoundException;
import java.io.IOException;

import android.content.Context;
import android.util.Log;
import hx.util.Logs;

public final class PremiumMarker {

    private static final String MARKER_ASSET = "hx_unlock_premium";

    private PremiumMarker() {
    }

    public static boolean read() {
        try {
            Context context = InstalledApp.context();
            if (context == null) {
                Log.e(Logs.TAG, "no application context, " + MARKER_ASSET + " not read");
                return false;
            }
            context.getAssets().open(MARKER_ASSET).close();
            return true;
        } catch (FileNotFoundException ex) {
            return false;
        } catch (IOException | ReflectiveOperationException ex) {
            Log.e(Logs.TAG, "cannot read " + MARKER_ASSET, ex);
            return false;
        }
    }

}
