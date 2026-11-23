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

    static final String SORT_BY_ORIGINAL_DATE = "Sort by original date";

    static final String UNLOCK_PREMIUM = "Unlock Premium Plus";

    private AppliedPatches() {
    }

    static List<AppliedPatch> applied() {
        List<AppliedPatch> applied = new ArrayList<>();
        add(applied, cloneApp(), cloneAppName(), cloneAppDescription());
        add(applied, fixGoogleLogin(), fixGoogleLoginName(), fixGoogleLoginDescription());
        add(applied, fixTransfersOverVpn(), fixTransfersOverVpnName(), fixTransfersOverVpnDescription());
        add(applied, gmsCoreSupport(), gmsCoreSupportName(), gmsCoreSupportDescription());
        add(applied, hideAds(), hideAdsName(), hideAdsDescription());
        add(applied, hideConsentForm(), hideConsentFormName(), hideConsentFormDescription());
        add(applied, hideHomeShortcuts(), hideHomeShortcutsName(), hideHomeShortcutsDescription());
        add(applied, hidePromotions(), hidePromotionsName(), hidePromotionsDescription());
        add(applied, hideVideoRecommendations(), hideVideoRecommendationsName(), hideVideoRecommendationsDescription());
        add(applied, removeRecentPanel(), removeRecentPanelName(), removeRecentPanelDescription());
        add(applied, showBackupCount(), showBackupCountName(), showBackupCountDescription());
        add(applied, sortByOriginalDate(), sortByOriginalDateName(), sortByOriginalDateDescription());
        add(applied, unlockPremium(), unlockPremiumName(), unlockPremiumDescription());
        Collections.sort(applied, (first, second) -> first.name.compareToIgnoreCase(second.name));
        return applied;
    }

    private static void add(List<AppliedPatch> applied, boolean patchApplied, String name, String description) {
        if (patchApplied) {
            applied.add(new AppliedPatch(name, description));
        }
    }

    private static String metadata(String key, boolean description) {
        return null;
    }

    static boolean isApplied(String key) {
        return name(key) != null;
    }

    static String name(String key) {
        return metadata(key, false);
    }

    static String description(String key) {
        return metadata(key, true);
    }

    public static boolean cloneApp() {
        return isApplied("cloneApp");
    }

    public static String cloneAppName() {
        return name("cloneApp");
    }

    public static String cloneAppDescription() {
        return description("cloneApp");
    }

    public static boolean fixGoogleLogin() {
        return isApplied("fixGoogleLogin");
    }

    public static String fixGoogleLoginName() {
        return name("fixGoogleLogin");
    }

    public static String fixGoogleLoginDescription() {
        return description("fixGoogleLogin");
    }

    public static boolean fixTransfersOverVpn() {
        return isApplied("fixTransfersOverVpn");
    }

    public static String fixTransfersOverVpnName() {
        return name("fixTransfersOverVpn");
    }

    public static String fixTransfersOverVpnDescription() {
        return description("fixTransfersOverVpn");
    }

    public static boolean gmsCoreSupport() {
        return isApplied("gmsCoreSupport");
    }

    public static String gmsCoreSupportName() {
        return name("gmsCoreSupport");
    }

    public static String gmsCoreSupportDescription() {
        return description("gmsCoreSupport");
    }

    public static boolean hideAds() {
        return isApplied("hideAds");
    }

    public static String hideAdsName() {
        return name("hideAds");
    }

    public static String hideAdsDescription() {
        return description("hideAds");
    }

    public static boolean hideConsentForm() {
        return isApplied("hideConsentForm");
    }

    public static String hideConsentFormName() {
        return name("hideConsentForm");
    }

    public static String hideConsentFormDescription() {
        return description("hideConsentForm");
    }

    public static boolean hideHomeShortcuts() {
        return isApplied("hideHomeShortcuts");
    }

    public static String hideHomeShortcutsName() {
        return name("hideHomeShortcuts");
    }

    public static String hideHomeShortcutsDescription() {
        return description("hideHomeShortcuts");
    }

    public static boolean hidePromotions() {
        return isApplied("hidePromotions");
    }

    public static String hidePromotionsName() {
        return name("hidePromotions");
    }

    public static String hidePromotionsDescription() {
        return description("hidePromotions");
    }

    public static boolean hideVideoRecommendations() {
        return isApplied("hideVideoRecommendations");
    }

    public static String hideVideoRecommendationsName() {
        return name("hideVideoRecommendations");
    }

    public static String hideVideoRecommendationsDescription() {
        return description("hideVideoRecommendations");
    }

    public static boolean removeRecentPanel() {
        return isApplied("removeRecentPanel");
    }

    public static String removeRecentPanelName() {
        return name("removeRecentPanel");
    }

    public static String removeRecentPanelDescription() {
        return description("removeRecentPanel");
    }

    public static boolean showBackupCount() {
        return isApplied("showBackupCount");
    }

    public static String showBackupCountName() {
        return name("showBackupCount");
    }

    public static String showBackupCountDescription() {
        return description("showBackupCount");
    }

    public static boolean sortByOriginalDate() {
        return isApplied("sortByOriginalDate");
    }

    public static String sortByOriginalDateName() {
        return name("sortByOriginalDate");
    }

    public static String sortByOriginalDateDescription() {
        return description("sortByOriginalDate");
    }

    public static boolean unlockPremium() {
        return isApplied("unlockPremium");
    }

    public static String unlockPremiumName() {
        return name("unlockPremium");
    }

    public static String unlockPremiumDescription() {
        return description("unlockPremium");
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
