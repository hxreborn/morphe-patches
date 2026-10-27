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

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;

@SuppressWarnings("unused")
public final class SmartNotification {

    private static final String SPEED_METER_SERVICE = "com.internet.speed.meter.lite.SpeedMeterService";

    private static ConnectivityManager.NetworkCallback offlineWatcher;

    private SmartNotification() {

    }

    public static boolean stopWhenOffline(Service service) {
        if (PremiumSettings.notificationPreference(service) != PremiumSettings.SHOW_WHEN_CONNECTED) {
            stopWatchingOffline(service);
            return false;
        }
        if (!isConnected(service)) {
            stopService(service);
            return true;
        }
        watchForOffline(service);
        return false;
    }

    private static void stopService(Service service) {
        stopWatchingOffline(service);
        watchForNetwork(service);
        service.stopSelf();
    }

    @SuppressLint("MissingPermission")
    private static synchronized void watchForOffline(Service service) {
        if (offlineWatcher != null || Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return;
        }
        offlineWatcher = new ConnectivityManager.NetworkCallback() {

            @Override
            public void onLost(Network network) {
                if (!isConnected(service)) {
                    stopService(service);
                }
            }

        };
        connectivity(service).registerDefaultNetworkCallback(offlineWatcher);
    }

    private static synchronized void stopWatchingOffline(Context context) {
        if (offlineWatcher != null) {
            connectivity(context).unregisterNetworkCallback(offlineWatcher);
            offlineWatcher = null;
        }
    }

    static void applyPreference(Context context) {
        if (PremiumSettings.notificationPreference(context) == PremiumSettings.SHOW_WHEN_CONNECTED
                && !isConnected(context)) {
            context.stopService(serviceIntent(context));
            watchForNetwork(context);
        } else {
            startService(context);
        }
    }

    static void onNetworkAvailable(Context context) {
        connectivity(context).unregisterNetworkCallback(networkIntent(context));
        startService(context);
    }

    private static void startService(Context context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent(context));
            } else {
                context.startService(serviceIntent(context));
            }
        } catch (IllegalStateException | SecurityException ex) {
            watchForNetwork(context);
        }
    }

    @SuppressLint("MissingPermission")
    private static boolean isConnected(Context context) {
        return connectivity(context).getActiveNetwork() != null;
    }

    @SuppressLint("MissingPermission")
    private static void watchForNetwork(Context context) {
        ConnectivityManager connectivity = connectivity(context);
        PendingIntent networkIntent = networkIntent(context);
        connectivity.unregisterNetworkCallback(networkIntent);
        connectivity.registerNetworkCallback(
                new NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build(),
                networkIntent);
    }

    private static ConnectivityManager connectivity(Context context) {
        return context.getSystemService(ConnectivityManager.class);
    }

    private static Intent serviceIntent(Context context) {
        return new Intent().setClassName(context, SPEED_METER_SERVICE);
    }

    private static PendingIntent networkIntent(Context context) {
        int mutability = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) ? PendingIntent.FLAG_MUTABLE : 0;
        return PendingIntent.getBroadcast(context, 0, new Intent(context, NetworkAvailableReceiver.class),
                PendingIntent.FLAG_UPDATE_CURRENT | mutability);
    }

}
