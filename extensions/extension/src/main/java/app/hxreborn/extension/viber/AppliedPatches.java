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
package app.hxreborn.extension.viber;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("unused")
public final class AppliedPatches {

    private AppliedPatches() {
    }

    public static boolean disableAiFeatures() {
        return false;
    }

    public static String disableAiFeaturesName() {
        return null;
    }

    public static String disableAiFeaturesDescription() {
        return null;
    }

    public static boolean removeChatPayments() {
        return false;
    }

    public static String removeChatPaymentsName() {
        return null;
    }

    public static String removeChatPaymentsDescription() {
        return null;
    }

    public static boolean hideAds() {
        return false;
    }

    public static String hideAdsName() {
        return null;
    }

    public static String hideAdsDescription() {
        return null;
    }

    public static boolean hideMoreItems() {
        return false;
    }

    public static String hideMoreItemsName() {
        return null;
    }

    public static String hideMoreItemsDescription() {
        return null;
    }

    public static boolean hideNavigationButtons() {
        return false;
    }

    public static String hideNavigationButtonsName() {
        return null;
    }

    public static String hideNavigationButtonsDescription() {
        return null;
    }

    public static boolean unfiledChats() {
        return false;
    }

    public static String unfiledChatsName() {
        return null;
    }

    public static String unfiledChatsDescription() {
        return null;
    }

    public static boolean raiseFolderLimit() {
        return false;
    }

    public static String raiseFolderLimitName() {
        return null;
    }

    public static String raiseFolderLimitDescription() {
        return null;
    }

    public static boolean filterBusinessChats() {
        return false;
    }

    public static String filterBusinessChatsName() {
        return null;
    }

    public static String filterBusinessChatsDescription() {
        return null;
    }

    public static boolean disableAnalytics() {
        return false;
    }

    public static String disableAnalyticsName() {
        return null;
    }

    public static String disableAnalyticsDescription() {
        return null;
    }

    public static boolean removeOnlineStatusLimit() {
        return false;
    }

    public static String removeOnlineStatusLimitName() {
        return null;
    }

    public static String removeOnlineStatusLimitDescription() {
        return null;
    }

    public static boolean hidePromotions() {
        return false;
    }

    public static String hidePromotionsName() {
        return null;
    }

    public static String hidePromotionsDescription() {
        return null;
    }

    public static boolean disableUpdatePrompts() {
        return false;
    }

    public static String disableUpdatePromptsName() {
        return null;
    }

    public static String disableUpdatePromptsDescription() {
        return null;
    }

    public static boolean spoofSignature() {
        return false;
    }

    public static String spoofSignatureName() {
        return null;
    }

    public static String spoofSignatureDescription() {
        return null;
    }

    public static boolean gmsCoreSupport() {
        return false;
    }

    public static String gmsCoreSupportName() {
        return null;
    }

    public static String gmsCoreSupportDescription() {
        return null;
    }

    public static boolean unlockViberPlus() {
        return false;
    }

    public static String unlockViberPlusName() {
        return null;
    }

    public static String unlockViberPlusDescription() {
        return null;
    }

    static List<AppliedPatch> applied() {
        List<AppliedPatch> applied = new ArrayList<>();
        add(applied, disableAiFeatures(), disableAiFeaturesName(), disableAiFeaturesDescription());
        add(applied, removeChatPayments(), removeChatPaymentsName(), removeChatPaymentsDescription());
        add(applied, hideAds(), hideAdsName(), hideAdsDescription());
        add(applied, hidePromotions(), hidePromotionsName(), hidePromotionsDescription());
        add(applied, hideMoreItems(), hideMoreItemsName(), hideMoreItemsDescription());
        add(applied, hideNavigationButtons(), hideNavigationButtonsName(), hideNavigationButtonsDescription());
        add(applied, unfiledChats(), unfiledChatsName(), unfiledChatsDescription());
        add(applied, raiseFolderLimit(), raiseFolderLimitName(), raiseFolderLimitDescription());
        add(applied, filterBusinessChats(), filterBusinessChatsName(), filterBusinessChatsDescription());
        add(applied, removeOnlineStatusLimit(), removeOnlineStatusLimitName(), removeOnlineStatusLimitDescription());
        add(applied, disableAnalytics(), disableAnalyticsName(), disableAnalyticsDescription());
        add(applied, disableUpdatePrompts(), disableUpdatePromptsName(), disableUpdatePromptsDescription());
        add(applied, spoofSignature(), spoofSignatureName(), spoofSignatureDescription());
        add(applied, gmsCoreSupport(), gmsCoreSupportName(), gmsCoreSupportDescription());
        add(applied, unlockViberPlus(), unlockViberPlusName(), unlockViberPlusDescription());
        Collections.sort(applied, (first, second) -> first.name.compareToIgnoreCase(second.name));
        return applied;
    }

    private static void add(List<AppliedPatch> applied, boolean patchApplied, String name, String description) {
        if (patchApplied) {
            applied.add(new AppliedPatch(name, description));
        }
    }

    static final class AppliedPatch {

        final String name;

        final String description;

        AppliedPatch(String name, String description) {
            this.name = name;
            this.description = description;
        }

    }

}
