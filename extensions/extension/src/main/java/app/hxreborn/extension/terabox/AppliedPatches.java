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
package app.hxreborn.extension.terabox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("unused")
public final class AppliedPatches {

    static final String HIDE_ADS = "Hide ads";

    static final String HIDE_HOME_SHORTCUTS = "Hide home shortcuts";

    static final String HIDE_PROMOTIONS = "Hide promotions";

    static final String HIDE_VIDEO_RECOMMENDATIONS = "Hide video recommendations";

    static final String REMOVE_RECENT_PANEL = "Remove recent panel";

    static final String SHOW_BACKUP_COUNT = "Show backup count";

    static final String UNLOCK_PREMIUM = "Unlock Premium Plus";

    private AppliedPatches() {
    }

    static List<AppliedPatch> applied() {
        List<AppliedPatch> applied = new ArrayList<>();
        add(applied, cloneApp(), cloneAppName(), cloneAppDescription());
        add(applied, fixGoogleLogin(), fixGoogleLoginName(), fixGoogleLoginDescription());
        add(applied, fixTransfersOverVpn(), fixTransfersOverVpnName(), fixTransfersOverVpnDescription());
        add(applied, hideAds(), hideAdsName(), hideAdsDescription());
        add(applied, hideConsentForm(), hideConsentFormName(), hideConsentFormDescription());
        add(applied, hideHomeShortcuts(), hideHomeShortcutsName(), hideHomeShortcutsDescription());
        add(applied, hidePromotions(), hidePromotionsName(), hidePromotionsDescription());
        add(applied, hideVideoRecommendations(), hideVideoRecommendationsName(), hideVideoRecommendationsDescription());
        add(applied, removeRecentPanel(), removeRecentPanelName(), removeRecentPanelDescription());
        add(applied, showBackupCount(), showBackupCountName(), showBackupCountDescription());
        add(applied, unlockPremium(), unlockPremiumName(), unlockPremiumDescription());
        Collections.sort(applied, (first, second) -> first.name.compareToIgnoreCase(second.name));
        return applied;
    }

    private static void add(List<AppliedPatch> applied, boolean patchApplied, String name, String description) {
        if (patchApplied) {
            applied.add(new AppliedPatch(name, description));
        }
    }

    public static boolean cloneApp() {
        return false;
    }

    public static String cloneAppName() {
        return null;
    }

    public static String cloneAppDescription() {
        return null;
    }

    public static boolean fixGoogleLogin() {
        return false;
    }

    public static String fixGoogleLoginName() {
        return null;
    }

    public static String fixGoogleLoginDescription() {
        return null;
    }

    public static boolean fixTransfersOverVpn() {
        return false;
    }

    public static String fixTransfersOverVpnName() {
        return null;
    }

    public static String fixTransfersOverVpnDescription() {
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

    public static boolean hideConsentForm() {
        return false;
    }

    public static String hideConsentFormName() {
        return null;
    }

    public static String hideConsentFormDescription() {
        return null;
    }

    public static boolean hideHomeShortcuts() {
        return false;
    }

    public static String hideHomeShortcutsName() {
        return null;
    }

    public static String hideHomeShortcutsDescription() {
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

    public static boolean hideVideoRecommendations() {
        return false;
    }

    public static String hideVideoRecommendationsName() {
        return null;
    }

    public static String hideVideoRecommendationsDescription() {
        return null;
    }

    public static boolean removeRecentPanel() {
        return false;
    }

    public static String removeRecentPanelName() {
        return null;
    }

    public static String removeRecentPanelDescription() {
        return null;
    }

    public static boolean showBackupCount() {
        return false;
    }

    public static String showBackupCountName() {
        return null;
    }

    public static String showBackupCountDescription() {
        return null;
    }

    public static boolean unlockPremium() {
        return false;
    }

    public static String unlockPremiumName() {
        return null;
    }

    public static String unlockPremiumDescription() {
        return null;
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
