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
package app.hxreborn.extension.photoeditorpro;

import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

@SuppressWarnings("unused")
public final class PushNotifications {

    private static final String PLAY_SERVICES_PACKAGE = "com.google.android.gms";

    private PushNotifications() {
    }

    public static void clearPlayServicesDelegate(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return;
        }
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (PLAY_SERVICES_PACKAGE.equals(manager.getNotificationDelegate())) {
            manager.setNotificationDelegate(null);
        }
    }

}
