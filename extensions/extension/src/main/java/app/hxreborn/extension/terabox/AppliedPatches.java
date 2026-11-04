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

@SuppressWarnings("unused")
public final class AppliedPatches {

    static final String HIDE_ADS = "Hide ads";

    static final String HIDE_PROMOTIONS = "Hide promotions";

    static final String HIDE_HOME_SHORTCUTS = "Hide home shortcuts";

    static final String HIDE_VIDEO_RECOMMENDATIONS = "Hide video recommendations";

    static final String UNLOCK_PREMIUM = "Unlock Premium Plus";

    static final String REMOVE_RECENT_PANEL = "Remove recent panel";

    private AppliedPatches() {
    }

    public static boolean hideAds() {
        return false;
    }

    public static boolean hidePromotions() {
        return false;
    }

    public static boolean hideHomeShortcuts() {
        return false;
    }

    public static boolean hideVideoRecommendations() {
        return false;
    }

    public static boolean unlockPremium() {
        return false;
    }

    public static boolean removeRecentPanel() {
        return false;
    }

}
