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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;
import hx.util.Logs;

public final class Notices {

    private static final Set<String> SHOWN = Collections.synchronizedSet(new HashSet<>());

    private Notices() {
    }

    public static void showOnce(String message) {
        if (SHOWN.add(message)) {
            show(message);
        }
    }

    public static void show(String message) {
        new Handler(Looper.getMainLooper()).post(() -> toast(message));
    }

    private static void toast(String message) {
        try {
            Context application = InstalledApp.context();
            Toast.makeText(application, message, Toast.LENGTH_LONG).show();
        } catch (Throwable ex) {
            Log.e(Logs.TAG, "cannot show \"" + message + "\"", ex);
        }
    }

}
