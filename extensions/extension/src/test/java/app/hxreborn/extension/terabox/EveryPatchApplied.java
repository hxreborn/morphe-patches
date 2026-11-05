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
    public static String cloneAppDescription() {
        return "Installs a second copy.";
    }

    @Implementation
    public static String fixGoogleLoginDescription() {
        return "Restores Google sign-in.";
    }

    @Implementation
    public static String hideAdsDescription() {
        return "Removes ads.";
    }

    @Implementation
    public static String hideConsentFormDescription() {
        return "Hides the consent form.";
    }

    @Implementation
    public static String hideHomeShortcutsDescription() {
        return "Removes shortcuts.";
    }

    @Implementation
    public static String hidePromotionsDescription() {
        return "Removes offers.";
    }

    @Implementation
    public static String hideVideoRecommendationsDescription() {
        return "Removes recommendations.";
    }

    @Implementation
    public static String removeRecentPanelDescription() {
        return "Removes the panel.";
    }

    @Implementation
    public static String unlockPremiumDescription() {
        return "Unlocks Premium.";
    }

}
