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

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@Implements(AppliedPatches.class)
public final class EveryPatchApplied {

    @Implementation
    public static boolean cloneApp() {
        return true;
    }

    @Implementation
    public static String cloneAppName() {
        return "Clone app";
    }

    @Implementation
    public static String cloneAppDescription() {
        return "Installs a second copy.";
    }

    @Implementation
    public static boolean fixGoogleLogin() {
        return true;
    }

    @Implementation
    public static String fixGoogleLoginName() {
        return "Fix Google login";
    }

    @Implementation
    public static String fixGoogleLoginDescription() {
        return "Restores Google sign-in.";
    }

    @Implementation
    public static boolean fixTransfersOverVpn() {
        return true;
    }

    @Implementation
    public static String fixTransfersOverVpnName() {
        return "Fix transfers over VPN";
    }

    @Implementation
    public static String fixTransfersOverVpnDescription() {
        return "Runs transfers on a VPN.";
    }

    @Implementation
    public static boolean hideAds() {
        return true;
    }

    @Implementation
    public static String hideAdsName() {
        return "Hide ads";
    }

    @Implementation
    public static String hideAdsDescription() {
        return "Removes ads.";
    }

    @Implementation
    public static boolean hideConsentForm() {
        return true;
    }

    @Implementation
    public static String hideConsentFormName() {
        return "Hide consent form";
    }

    @Implementation
    public static String hideConsentFormDescription() {
        return "Hides the consent form.";
    }

    @Implementation
    public static boolean hideHomeShortcuts() {
        return true;
    }

    @Implementation
    public static String hideHomeShortcutsName() {
        return "Hide home shortcuts";
    }

    @Implementation
    public static String hideHomeShortcutsDescription() {
        return "Removes shortcuts.";
    }

    @Implementation
    public static boolean hidePromotions() {
        return true;
    }

    @Implementation
    public static String hidePromotionsName() {
        return "Hide promotions";
    }

    @Implementation
    public static String hidePromotionsDescription() {
        return "Removes offers.";
    }

    @Implementation
    public static boolean hideVideoRecommendations() {
        return true;
    }

    @Implementation
    public static String hideVideoRecommendationsName() {
        return "Hide video recommendations";
    }

    @Implementation
    public static String hideVideoRecommendationsDescription() {
        return "Removes recommendations.";
    }

    @Implementation
    public static boolean removeRecentPanel() {
        return true;
    }

    @Implementation
    public static String removeRecentPanelName() {
        return "Remove recent panel";
    }

    @Implementation
    public static String removeRecentPanelDescription() {
        return "Removes the panel.";
    }

    @Implementation
    public static boolean unlockPremium() {
        return true;
    }

    @Implementation
    public static String unlockPremiumName() {
        return "Unlock Premium Plus";
    }

    @Implementation
    public static String unlockPremiumDescription() {
        return "Unlocks Premium.";
    }

}
