/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.shared;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

@SuppressWarnings("unused")
public final class AdMobLoadFailure {
    private static final String TAG = "AdMobLoadFailure";
    private static final String ADS_PACKAGE = "com.google.android.gms.ads.";
    private static final int ERROR_CODE_NO_FILL = 3;

    private AdMobLoadFailure() {}

    public static void report(final Object callback) {
        if (callback == null) {
            return;
        }

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                deliver(callback);
            }
        });
    }

    private static void deliver(Object callback) {
        try {
            ClassLoader loader = callback.getClass().getClassLoader();
            Class<?> loadAdError = Class.forName(ADS_PACKAGE + "LoadAdError", false, loader);
            Object error = loadAdError
                .getConstructor(
                    int.class,
                    String.class,
                    String.class,
                    Class.forName(ADS_PACKAGE + "AdError", false, loader),
                    Class.forName(ADS_PACKAGE + "ResponseInfo", false, loader))
                .newInstance(ERROR_CODE_NO_FILL, "No fill", "com.google.android.gms.ads", null, null);
            sdkType(callback.getClass()).getMethod("onAdFailedToLoad", loadAdError).invoke(callback, error);
        } catch (Throwable throwable) {
            Log.e(TAG, "Could not deliver the load failure", throwable);
        }
    }

    private static Class<?> sdkType(Class<?> type) {
        while (!type.getName().startsWith(ADS_PACKAGE)) {
            type = type.getSuperclass();
        }
        return type;
    }
}
