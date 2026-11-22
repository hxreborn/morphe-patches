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

    public static boolean unlockPremium() {
        return isApplied("unlockPremium");
    }

    public static boolean disableDynamicAppIcon() {
        return isApplied("disableDynamicAppIcon");
    }

    public static boolean enableDebugMode() {
        return isApplied("enableDebugMode");
    }

    public static boolean unlimitedRampUpTime() {
        return isApplied("unlimitedRampUpTime");
    }

    public static String unlockPremiumDescription() {
        return description("unlockPremium");
    }

    public static String unlockPremiumName() {
        return name("unlockPremium");
    }

    public static String disableDynamicAppIconName() {
        return name("disableDynamicAppIcon");
    }

    public static String disableDynamicAppIconDescription() {
        return description("disableDynamicAppIcon");
    }

    public static String enableDebugModeDescription() {
        return description("enableDebugMode");
    }

    public static String enableDebugModeName() {
        return name("enableDebugMode");
    }

    public static String unlimitedRampUpTimeName() {
        return name("unlimitedRampUpTime");
    }

    public static String unlimitedRampUpTimeDescription() {
        return description("unlimitedRampUpTime");
    }

}
