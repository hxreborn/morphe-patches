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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("unused")
public final class AppliedPatches {

    static final String CLONE_APP = "Clone app";

    static final String FIX_GOOGLE_LOGIN = "Fix Google login";

    static final String HIDE_ADS = "Hide ads";

    static final String HIDE_CONSENT_FORM = "Hide consent form";

    static final String HIDE_HOME_SHORTCUTS = "Hide home shortcuts";

    static final String HIDE_PROMOTIONS = "Hide promotions";

    static final String HIDE_VIDEO_RECOMMENDATIONS = "Hide video recommendations";

    static final String REMOVE_RECENT_PANEL = "Remove recent panel";

    static final String UNLOCK_PREMIUM = "Unlock Premium Plus";

    private static final List<String> NAMES = Collections
        .unmodifiableList(Arrays.asList(CLONE_APP, FIX_GOOGLE_LOGIN, HIDE_ADS, HIDE_CONSENT_FORM, HIDE_HOME_SHORTCUTS,
                HIDE_PROMOTIONS, HIDE_VIDEO_RECOMMENDATIONS, REMOVE_RECENT_PANEL, UNLOCK_PREMIUM));

    private AppliedPatches() {
    }

    static List<String> names() {
        List<String> names = new ArrayList<>();
        for (String name : NAMES) {
            if (description(name) != null) {
                names.add(name);
            }
        }
        return names;
    }

    static String description(String name) {
        switch (name) {
            case CLONE_APP:
                return cloneAppDescription();
            case FIX_GOOGLE_LOGIN:
                return fixGoogleLoginDescription();
            case HIDE_ADS:
                return hideAdsDescription();
            case HIDE_CONSENT_FORM:
                return hideConsentFormDescription();
            case HIDE_HOME_SHORTCUTS:
                return hideHomeShortcutsDescription();
            case HIDE_PROMOTIONS:
                return hidePromotionsDescription();
            case HIDE_VIDEO_RECOMMENDATIONS:
                return hideVideoRecommendationsDescription();
            case REMOVE_RECENT_PANEL:
                return removeRecentPanelDescription();
            case UNLOCK_PREMIUM:
                return unlockPremiumDescription();
            default:
                return null;
        }
    }

    public static String cloneAppDescription() {
        return null;
    }

    public static String fixGoogleLoginDescription() {
        return null;
    }

    static boolean hideAds() {
        return hideAdsDescription() != null;
    }

    public static String hideAdsDescription() {
        return null;
    }

    public static String hideConsentFormDescription() {
        return null;
    }

    static boolean hideHomeShortcuts() {
        return hideHomeShortcutsDescription() != null;
    }

    public static String hideHomeShortcutsDescription() {
        return null;
    }

    static boolean hidePromotions() {
        return hidePromotionsDescription() != null;
    }

    public static String hidePromotionsDescription() {
        return null;
    }

    static boolean hideVideoRecommendations() {
        return hideVideoRecommendationsDescription() != null;
    }

    public static String hideVideoRecommendationsDescription() {
        return null;
    }

    static boolean removeRecentPanel() {
        return removeRecentPanelDescription() != null;
    }

    public static String removeRecentPanelDescription() {
        return null;
    }

    static boolean unlockPremium() {
        return unlockPremiumDescription() != null;
    }

    public static String unlockPremiumDescription() {
        return null;
    }

}
