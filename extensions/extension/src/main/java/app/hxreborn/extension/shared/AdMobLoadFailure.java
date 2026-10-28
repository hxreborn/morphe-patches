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

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

@SuppressWarnings("unused")
public final class AdMobLoadFailure {

    private static final String TAG = "AdMobLoadFailure";

    private static final String ADS_PACKAGE = "com.google.android.gms.ads.";

    private static final int ERROR_CODE_NO_FILL = 3;

    private AdMobLoadFailure() {

    }

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
                .getConstructor(int.class, String.class, String.class,
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
