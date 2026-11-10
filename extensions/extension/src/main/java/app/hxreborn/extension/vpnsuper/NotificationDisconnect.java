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

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

@SuppressWarnings("unused")
public final class NotificationDisconnect {

    private NotificationDisconnect() {
    }

    public static PendingIntent disconnectAction(Context context, int requestCode, Intent openAppIntent, int flags) {
        if (!PatchSettings.isDisconnectFromNotificationEnabled(context)) {
            return PendingIntent.getActivity(context, requestCode, openAppIntent, flags);
        }
        Intent disconnectBroadcast = new Intent(context, NotificationDisconnectReceiver.class);
        return PendingIntent.getBroadcast(context, requestCode, disconnectBroadcast, flags);
    }

    static void disconnect(Context context) {
        markUserDisconnected();
        Intent stopConnection = new Intent(context, connectionService()).setAction(stopConnectionAction());
        try {
            context.startService(stopConnection);
        } catch (IllegalStateException ex) {
            Log.e(PatchSettings.LOG_TAG, "Cannot start the VPN service to stop the connection", ex);
        }
    }

    static void markUserDisconnected() {
    }

    static Class<?> connectionService() {
        return null;
    }

    static String stopConnectionAction() {
        return null;
    }

}
