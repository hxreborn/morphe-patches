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

    public static boolean disableAnalytics() {
        return false;
    }

    public static String disableAnalyticsName() {
        return null;
    }

    public static String disableAnalyticsDescription() {
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

    static List<String> identifiers() {
        List<String> identifiers = new ArrayList<>();
        if (hideAds()) {
            identifiers.add("hideAds");
        }
        if (hideMoreItems()) {
            identifiers.add("hideMoreItems");
        }
        if (hideNavigationButtons()) {
            identifiers.add("hideNavigationButtons");
        }
        if (disableAnalytics()) {
            identifiers.add("disableAnalytics");
        }
        if (spoofSignature()) {
            identifiers.add("spoofSignature");
        }
        if (gmsCoreSupport()) {
            identifiers.add("gmsCoreSupport");
        }
        Collections.sort(identifiers, (first, second) -> name(first).compareToIgnoreCase(name(second)));
        return identifiers;
    }

    static String name(String identifier) {
        switch (identifier) {
            case "hideAds":
                return hideAdsName();
            case "hideMoreItems":
                return hideMoreItemsName();
            case "hideNavigationButtons":
                return hideNavigationButtonsName();
            case "disableAnalytics":
                return disableAnalyticsName();
            case "spoofSignature":
                return spoofSignatureName();
            case "gmsCoreSupport":
                return gmsCoreSupportName();
            default:
                return null;
        }
    }

    static String description(String identifier) {
        switch (identifier) {
            case "hideAds":
                return hideAdsDescription();
            case "hideMoreItems":
                return hideMoreItemsDescription();
            case "hideNavigationButtons":
                return hideNavigationButtonsDescription();
            case "disableAnalytics":
                return disableAnalyticsDescription();
            case "spoofSignature":
                return spoofSignatureDescription();
            case "gmsCoreSupport":
                return gmsCoreSupportDescription();
            default:
                return "";
        }
    }

}
