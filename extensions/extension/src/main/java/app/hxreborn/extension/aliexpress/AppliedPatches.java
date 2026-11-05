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

    static final String REMOVE_PROMOTIONAL_POPUPS = "Remove promotional popups";

    static final String REMOVE_SPONSORED_ITEMS = "Remove sponsored items";

    static final String SKIP_SPLASH_PROMOTIONS = "Skip splash promotions";

    static final List<String> SWITCH_KEYS = Collections
        .unmodifiableList(Arrays.asList(REMOVE_SPONSORED_ITEMS, DISABLE_ANALYTICS, DISABLE_UPDATE_PROMPTS,
                ENABLE_IMAGE_SAVING, REMOVE_PROMOTIONAL_POPUPS, SKIP_SPLASH_PROMOTIONS));

    private AppliedPatches() {
    }

    static List<String> identifiers() {
        List<String> identifiers = new ArrayList<>();
        if (disableAnalytics()) {
            identifiers.add("disableAnalytics");
        }
        if (disableUpdatePrompts()) {
            identifiers.add("disableUpdatePrompts");
        }
        if (enableImageSaving()) {
            identifiers.add("enableImageSaving");
        }
        if (darkTheme()) {
            identifiers.add("darkTheme");
        }
        if (gmsCoreSupport()) {
            identifiers.add("gmsCoreSupport");
        }
        if (removePromotionalPopups()) {
            identifiers.add("removePromotionalPopups");
        }
        if (removeSponsoredItems()) {
            identifiers.add("removeSponsoredItems");
        }
        if (skipSplashPromotions()) {
            identifiers.add("skipSplashPromotions");
        }
        if (spoofSignature()) {
            identifiers.add("spoofSignature");
        }
        Collections.sort(identifiers, (first, second) -> name(first).compareToIgnoreCase(name(second)));
        return identifiers;
    }

    static String name(String identifier) {
        switch (identifier) {
            case "disableAnalytics":
                return disableAnalyticsName();
            case "disableUpdatePrompts":
                return disableUpdatePromptsName();
            case "enableImageSaving":
                return enableImageSavingName();
            case "darkTheme":
                return darkThemeName();
            case "gmsCoreSupport":
                return gmsCoreSupportName();
            case "removePromotionalPopups":
                return removePromotionalPopupsName();
            case "removeSponsoredItems":
                return removeSponsoredItemsName();
            case "skipSplashPromotions":
                return skipSplashPromotionsName();
            case "spoofSignature":
                return spoofSignatureName();
            default:
                return null;
        }
    }

    static String description(String identifier) {
        switch (identifier) {
            case "disableAnalytics":
                return disableAnalyticsDescription();
            case "disableUpdatePrompts":
                return disableUpdatePromptsDescription();
            case "enableImageSaving":
                return enableImageSavingDescription();
            case "darkTheme":
                return darkThemeDescription();
            case "gmsCoreSupport":
                return gmsCoreSupportDescription();
            case "removePromotionalPopups":
                return removePromotionalPopupsDescription();
            case "removeSponsoredItems":
                return removeSponsoredItemsDescription();
            case "skipSplashPromotions":
                return skipSplashPromotionsDescription();
            case "spoofSignature":
                return spoofSignatureDescription();
            default:
                return null;
        }
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

    public static boolean disableUpdatePrompts() {
        return false;
    }

    public static String disableUpdatePromptsName() {
        return null;
    }

    public static String disableUpdatePromptsDescription() {
        return null;
    }

    public static boolean enableImageSaving() {
        return false;
    }

    public static String enableImageSavingName() {
        return null;
    }

    public static String enableImageSavingDescription() {
        return null;
    }

    public static boolean darkTheme() {
        return false;
    }

    public static String darkThemeName() {
        return null;
    }

    public static String darkThemeDescription() {
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

    public static boolean removePromotionalPopups() {
        return false;
    }

    public static String removePromotionalPopupsName() {
        return null;
    }

    public static String removePromotionalPopupsDescription() {
        return null;
    }

    public static boolean removeSponsoredItems() {
        return false;
    }

    public static String removeSponsoredItemsName() {
        return null;
    }

    public static String removeSponsoredItemsDescription() {
        return null;
    }

    public static boolean skipSplashPromotions() {
        return false;
    }

    public static String skipSplashPromotionsName() {
        return null;
    }

    public static String skipSplashPromotionsDescription() {
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

}
