/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
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
