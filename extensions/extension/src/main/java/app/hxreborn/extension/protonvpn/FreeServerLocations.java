/*
 * Copyright (C) 2026 Rushi Ranpise
 * Copyright (C) 2026 Paresh Maheshwari
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from rushiranpise/morphe-patches:
 * https://github.com/rushiranpise/morphe-patches/commit/81207e12513860720ac4f56c90d582a0c7e008b6
 * Commit 81207e12513860720ac4f56c90d582a0c7e008b6 (2026-09-15),
 * patches/src/main/kotlin/app/template/patches/protonvpn/premium/ProtonVpnPremiumPatch.kt
 */
package app.hxreborn.extension.protonvpn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressWarnings({"unused", "unchecked", "rawtypes"})
public final class FreeServerLocations {

    private static final int FREE_TIER = 0;

    private static volatile boolean freeAccount;

    private FreeServerLocations() {}

    public static void onUserInfoChanged(Object userInfo) {
        Object vpnUser = userInfo == null ? null : call(userInfo, "getVpnUser");
        freeAccount = vpnUser != null && (Boolean) call(vpnUser, "isFreeUser");
    }

    public static void onUserInfoInvalidated() {
        freeAccount = false;
    }

    public static boolean shouldExcludeServer(boolean isFreeServer) {
        return freeAccount != isFreeServer;
    }

    public static Integer tierForAvailabilityCheck(Object item, Integer userTier) {
        if (userTier != null && userTier == FREE_TIER && tierOf(item) == FREE_TIER) return FREE_TIER + 1;
        return userTier;
    }

    public static List<?> itemsVisibleToTier(List<?> items, Integer userTier) {
        if (userTier == null || userTier != FREE_TIER) return items;
        List<Object> filtered = new ArrayList<>(items.size());
        for (Object item : items) {
            if (tierOf(item) == FREE_TIER) filtered.add(item);
        }
        return filtered;
    }

    public static Object resolveSelectedFilter(Object filter) {
        if (!freeAccount || filter == null) return filter;
        return Enum.valueOf((Class) ((Enum<?>) filter).getDeclaringClass(), "All");
    }

    public static List<?> resolveFilterButtons(List<?> buttons) {
        return freeAccount ? Collections.emptyList() : buttons;
    }

    private static int tierOf(Object item) {
        return (Integer) call(item, "getTier");
    }

    private static Object call(Object target, String method) {
        try {
            return target.getClass().getMethod(method).invoke(target);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
