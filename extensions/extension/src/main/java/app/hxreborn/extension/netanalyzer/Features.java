/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.netanalyzer;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

@SuppressWarnings("unused")
public final class Features {

    private static final String TAG = "hxNetAnalyzer";

    private static Object menuClick;

    private Features() {
    }

    public static void attach(Activity activity) {
        HostActivity.attach(activity);
    }

    public static synchronized Object onMenuClick(Class<?> onClickType, String entries) {
        if (menuClick == null) {
            menuClick = HostCallbacks.create(onClickType, () -> HostActivity.open(PatchesMenuActivity.class.getName(),
                    new Intent().putExtra(PatchesMenuActivity.EXTRA_ENTRIES, entries)));
        }
        return menuClick;
    }

    public static Object onHotspotClick(Class<?> onClickType, Object row) {
        return HostCallbacks.create(onClickType, () -> {
            try {
                WifiDetailsActivity.show(Hotspot.read(Hotspot.modelOf(row)));
            } catch (ReflectiveOperationException ex) {
                Log.e(TAG, "Cannot read the selected Wi-Fi network", ex);
            }
        });
    }

}
