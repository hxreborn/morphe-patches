/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.protonvpn;

import android.view.View;

import app.hxreborn.extension.proton.UpsellingVisibility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SuppressWarnings("unused")
public final class Promotions {

    private static final int TYPE_ONE_TIME_POPUP = 1;
    private static final int TYPE_HOME_SCREEN_BANNER = 2;
    private static final int TYPE_HOME_PROMINENT_BANNER = 3;
    private static final int TYPE_ONE_TIME_IAP_POPUP = 5;
    private static final int TYPE_BUILTIN_UPSELL_ONBOARDING = 6;
    private static final int TYPE_BUILTIN_UPSELL_PADLOCK = 7;
    private static final int TYPE_INTERNAL_ONE_TIME_IAP_POPUP = 1_000_000;
    private static final Set<Integer> PROMO_NOTIFICATION_TYPES = new HashSet<>(Arrays.asList(
            TYPE_ONE_TIME_POPUP,
            TYPE_HOME_SCREEN_BANNER,
            TYPE_HOME_PROMINENT_BANNER,
            TYPE_ONE_TIME_IAP_POPUP,
            TYPE_BUILTIN_UPSELL_ONBOARDING,
            TYPE_BUILTIN_UPSELL_PADLOCK,
            TYPE_INTERNAL_ONE_TIME_IAP_POPUP));
    private static final String SERVER_GROUP_BANNER =
            "com.protonvpn.android.redesign.countries.ui.ServerGroupUiItem$Banner";

    private Promotions() {}

    public static List<?> withoutPromoNotifications(List<?> notifications) {
        if (!UpsellingVisibility.isHidden()) return notifications;
        List<Object> filtered = new ArrayList<>(notifications.size());
        for (Object notification : notifications) {
            if (!PROMO_NOTIFICATION_TYPES.contains(notificationType(notification))) filtered.add(notification);
        }
        return filtered;
    }

    public static List<?> withoutUpgradeBanners(List<?> items) {
        if (!UpsellingVisibility.isHidden()) return items;
        List<Object> filtered = new ArrayList<>(items.size());
        for (Object item : items) {
            if (!SERVER_GROUP_BANNER.equals(item.getClass().getName())) filtered.add(item);
        }
        return filtered;
    }

    public static void hideUpgradeView(View view) {
        if (UpsellingVisibility.isHidden()) view.setVisibility(View.GONE);
    }

    private static int notificationType(Object notification) {
        try {
            return (Integer) notification.getClass().getMethod("getType").invoke(notification);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
