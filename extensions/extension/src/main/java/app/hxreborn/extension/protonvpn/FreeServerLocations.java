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
    private static final String STANDARD_PROFILE_TYPE = "Standard";
    private static final String UNAVAILABLE_PLAN = "UNAVAILABLE_PLAN";

    private FreeServerLocations() {}

    public static boolean shouldExcludeServer(boolean isFreeServer) {
        return FreeAccount.isSignedIn() != isFreeServer;
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
        if (!FreeAccount.isSignedIn() || filter == null) return filter;
        return Enum.valueOf((Class) ((Enum<?>) filter).getDeclaringClass(), "All");
    }

    public static List<?> resolveFilterButtons(List<?> buttons) {
        return FreeAccount.isSignedIn() ? Collections.emptyList() : buttons;
    }

    public static List<?> countriesForAccount(List<?> countries) {
        if (!FreeAccount.isSignedIn()) return countries;
        List<Object> filtered = new ArrayList<>(countries.size());
        for (Object country : countries) {
            if (hasFreeServer(country)) filtered.add(country);
        }
        return filtered;
    }

    public static List<?> profileTypesForAccount(List<?> types) {
        if (!FreeAccount.isSignedIn()) return types;
        List<Object> filtered = new ArrayList<>(1);
        for (Object type : types) {
            if (STANDARD_PROFILE_TYPE.equals(((Enum<?>) type).name())) filtered.add(type);
        }
        return filtered;
    }

    public static List<?> serversForAccount(List<?> servers) {
        if (!FreeAccount.isSignedIn()) return servers;
        List<Object> filtered = new ArrayList<>(servers.size());
        for (Object server : servers) {
            if (isFreeServer(server)) filtered.add(server);
        }
        return filtered;
    }

    public static List<?> profilesForAccount(List<?> profiles) {
        if (!FreeAccount.isSignedIn()) return profiles;
        List<Object> filtered = new ArrayList<>(profiles.size());
        for (Object profile : profiles) {
            if (!UNAVAILABLE_PLAN.equals(((Enum<?>) Reflection.call(profile, "getAvailability")).name())) {
                filtered.add(profile);
            }
        }
        return filtered;
    }

    private static boolean isFreeServer(Object server) {
        return (Boolean) Reflection.call(server, "isFreeServer");
    }

    private static boolean hasFreeServer(Object country) {
        for (Object server : (List<?>) Reflection.call(country, "getServerList")) {
            if (isFreeServer(server)) return true;
        }
        return false;
    }

    private static int tierOf(Object item) {
        return (Integer) Reflection.call(item, "getTier");
    }
}
