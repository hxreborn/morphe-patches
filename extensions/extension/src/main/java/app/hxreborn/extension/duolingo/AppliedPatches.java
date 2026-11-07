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
package app.hxreborn.extension.duolingo;

@SuppressWarnings("unused")
public final class AppliedPatches {

    static final String UNLOCK_PREMIUM = "Unlock premium";

    static final String DISABLE_DYNAMIC_APP_ICON = "Disable dynamic app icon";

    static final String ENABLE_DEBUG_MODE = "Enable debug mode";

    static final String UNLIMITED_RAMPUP_TIME = "Unlimited RampUp time";

    private AppliedPatches() {
    }

    public static boolean unlockPremium() {
        return false;
    }

    public static boolean disableDynamicAppIcon() {
        return false;
    }

    public static boolean enableDebugMode() {
        return false;
    }

    public static boolean unlimitedRampUpTime() {
        return false;
    }

    public static String unlockPremiumDescription() {
        return null;
    }

    public static String unlockPremiumName() {
        return null;
    }

    public static String disableDynamicAppIconName() {
        return null;
    }

    public static String disableDynamicAppIconDescription() {
        return null;
    }

    public static String enableDebugModeDescription() {
        return null;
    }

    public static String enableDebugModeName() {
        return null;
    }

    public static String unlimitedRampUpTimeName() {
        return null;
    }

    public static String unlimitedRampUpTimeDescription() {
        return null;
    }

}
