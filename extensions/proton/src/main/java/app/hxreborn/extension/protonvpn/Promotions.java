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
package app.hxreborn.extension.protonvpn;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.view.View;

import app.hxreborn.extension.proton.UpsellingVisibility;

@SuppressWarnings("unused")
public final class Promotions {

    private static final int TYPE_ONE_TIME_POPUP = 1;

    private static final int TYPE_HOME_SCREEN_BANNER = 2;

    private static final int TYPE_HOME_PROMINENT_BANNER = 3;

    private static final int TYPE_ONE_TIME_IAP_POPUP = 5;

    private static final int TYPE_BUILTIN_UPSELL_ONBOARDING = 6;

    private static final int TYPE_BUILTIN_UPSELL_PADLOCK = 7;

    private static final int TYPE_INTERNAL_ONE_TIME_IAP_POPUP = 1_000_000;

    private static final String UPGRADE_SOURCE_EXTRA = "Upgrade Type";

    private static final String ONBOARDING_UPGRADE_SOURCE = "ONBOARDING";

    private static final Set<Integer> PROMO_NOTIFICATION_TYPES = new HashSet<>(Arrays.asList(TYPE_ONE_TIME_POPUP,
            TYPE_HOME_SCREEN_BANNER, TYPE_HOME_PROMINENT_BANNER, TYPE_ONE_TIME_IAP_POPUP,
            TYPE_BUILTIN_UPSELL_ONBOARDING, TYPE_BUILTIN_UPSELL_PADLOCK, TYPE_INTERNAL_ONE_TIME_IAP_POPUP));

    private Promotions() {

    }

    public static List<?> withoutPromoNotifications(List<?> notifications) {
        if (!UpsellingVisibility.isHidden()) {
            return notifications;
        }
        List<Object> filtered = new ArrayList<>(notifications.size());
        for (Object notification : notifications) {
            if (!PROMO_NOTIFICATION_TYPES.contains(notificationType(notification))) {
                filtered.add(notification);
            }
        }
        return filtered;
    }

    public static List<?> withoutUpgradeBanners(List<?> items) {
        if (!UpsellingVisibility.isHidden()) {
            return items;
        }
        List<Object> filtered = new ArrayList<>(items.size());
        for (Object item : items) {
            if (!Members.serverGroupBannerClass().equals(item.getClass().getName())) {
                filtered.add(item);
            }
        }
        return filtered;
    }

    public static boolean showsUpsell(boolean freeUser) {
        return freeUser && !UpsellingVisibility.isHidden();
    }

    public static void hideUpgradeView(View view) {
        if (UpsellingVisibility.isHidden()) {
            view.setVisibility(View.GONE);
        }
    }

    public static boolean hidesOnboardingUpgrade(Activity activity) {
        if (!UpsellingVisibility.isHidden()) {
            return false;
        }
        Serializable source = activity.getIntent().getSerializableExtra(UPGRADE_SOURCE_EXTRA);
        return (source instanceof Enum) && ONBOARDING_UPGRADE_SOURCE.equals(((Enum<?>) source).name());
    }

    private static int notificationType(Object notification) {
        return (Integer) Reflection.read(notification, Members.apiNotificationType());
    }

}
