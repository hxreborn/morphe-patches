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
package app.hxreborn.extension.aliexpress;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("unused")
public final class AppliedPatches {

    static final String DISABLE_ANALYTICS = "Disable analytics";

    static final String DISABLE_UPDATE_PROMPTS = "Disable update prompts";

    static final String ENABLE_IMAGE_SAVING = "Enable image saving";

    static final String FIX_DARK_MODE = "Fix dark mode";

    static final String GMSCORE_SUPPORT = "GmsCore support";

    static final String REMOVE_PROMOTIONAL_POPUPS = "Remove promotional popups";

    static final String REMOVE_SPONSORED_ITEMS = "Remove sponsored items";

    static final String SKIP_SPLASH_PROMOTIONS = "Skip splash promotions";

    static final String SPOOF_SIGNATURE = "Spoof signature";

    static final List<String> SWITCH_KEYS = Collections
        .unmodifiableList(Arrays.asList(REMOVE_SPONSORED_ITEMS, DISABLE_ANALYTICS, DISABLE_UPDATE_PROMPTS,
                ENABLE_IMAGE_SAVING, REMOVE_PROMOTIONAL_POPUPS, SKIP_SPLASH_PROMOTIONS));

    private static final List<String> NAMES = Collections.unmodifiableList(Arrays.asList(DISABLE_ANALYTICS,
            DISABLE_UPDATE_PROMPTS, ENABLE_IMAGE_SAVING, FIX_DARK_MODE, GMSCORE_SUPPORT, REMOVE_PROMOTIONAL_POPUPS,
            REMOVE_SPONSORED_ITEMS, SKIP_SPLASH_PROMOTIONS, SPOOF_SIGNATURE));

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
            case DISABLE_ANALYTICS:
                return disableAnalyticsDescription();
            case DISABLE_UPDATE_PROMPTS:
                return disableUpdatePromptsDescription();
            case ENABLE_IMAGE_SAVING:
                return enableImageSavingDescription();
            case FIX_DARK_MODE:
                return darkThemeDescription();
            case GMSCORE_SUPPORT:
                return gmsCoreSupportDescription();
            case REMOVE_PROMOTIONAL_POPUPS:
                return removePromotionalPopupsDescription();
            case REMOVE_SPONSORED_ITEMS:
                return removeSponsoredItemsDescription();
            case SKIP_SPLASH_PROMOTIONS:
                return skipSplashPromotionsDescription();
            case SPOOF_SIGNATURE:
                return spoofSignatureDescription();
            default:
                return null;
        }
    }

    static boolean disableAnalytics() {
        return disableAnalyticsDescription() != null;
    }

    public static String disableAnalyticsDescription() {
        return null;
    }

    static boolean disableUpdatePrompts() {
        return disableUpdatePromptsDescription() != null;
    }

    public static String disableUpdatePromptsDescription() {
        return null;
    }

    static boolean enableImageSaving() {
        return enableImageSavingDescription() != null;
    }

    public static String enableImageSavingDescription() {
        return null;
    }

    static boolean darkTheme() {
        return darkThemeDescription() != null;
    }

    public static String darkThemeDescription() {
        return null;
    }

    public static String gmsCoreSupportDescription() {
        return null;
    }

    static boolean removePromotionalPopups() {
        return removePromotionalPopupsDescription() != null;
    }

    public static String removePromotionalPopupsDescription() {
        return null;
    }

    static boolean removeSponsoredItems() {
        return removeSponsoredItemsDescription() != null;
    }

    public static String removeSponsoredItemsDescription() {
        return null;
    }

    static boolean skipSplashPromotions() {
        return skipSplashPromotionsDescription() != null;
    }

    public static String skipSplashPromotionsDescription() {
        return null;
    }

    public static String spoofSignatureDescription() {
        return null;
    }

}
