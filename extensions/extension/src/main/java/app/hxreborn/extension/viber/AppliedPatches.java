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

    public static String hideAdsDescription() {
        return null;
    }

    public static String hideMoreItemsDescription() {
        return null;
    }

    public static String hideNavigationButtonsDescription() {
        return null;
    }

    public static String disableAnalyticsDescription() {
        return null;
    }

    public static String spoofSignatureDescription() {
        return null;
    }

    public static String gmsCoreSupportDescription() {
        return null;
    }

    static List<String> names() {
        List<String> names = new ArrayList<>();
        if (hideAdsDescription() != null) {
            names.add("Hide ads");
        }
        if (hideMoreItemsDescription() != null) {
            names.add("Hide More screen items");
        }
        if (hideNavigationButtonsDescription() != null) {
            names.add("Hide navigation buttons");
        }
        if (disableAnalyticsDescription() != null) {
            names.add("Disable analytics");
        }
        if (spoofSignatureDescription() != null) {
            names.add("Spoof signature");
        }
        if (gmsCoreSupportDescription() != null) {
            names.add("GmsCore support");
        }
        Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    static String description(String name) {
        switch (name) {
            case "Hide ads":
                return hideAdsDescription();
            case "Hide More screen items":
                return hideMoreItemsDescription();
            case "Hide navigation buttons":
                return hideNavigationButtonsDescription();
            case "Disable analytics":
                return disableAnalyticsDescription();
            case "Spoof signature":
                return spoofSignatureDescription();
            case "GmsCore support":
                return gmsCoreSupportDescription();
            default:
                return "";
        }
    }

}
