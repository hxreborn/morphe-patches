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
package app.hxreborn.extension.shared;

import android.app.Notification;
import android.app.Service;
import android.os.Build;
import android.util.Log;

@SuppressWarnings("unused")
public final class ForegroundService {

    private static final String TAG = "ForegroundService";

    private ForegroundService() {

    }

    public static boolean start(Service service, int id, Notification notification, int type) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                service.startForeground(id, notification, type);
            } else {
                service.startForeground(id, notification);
            }
            return true;
        } catch (RuntimeException ex) {
            Log.w(TAG, "startForeground failed for notification " + id, ex);
            service.stopSelf();
            return false;
        }
    }

}
