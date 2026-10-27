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
package app.hxreborn.extension.proton;

import java.util.ArrayList;
import java.util.List;

import app.hxreborn.extension.protonauthenticator.SettingsSections;
import app.hxreborn.extension.protonmail.SidebarEntries;
import app.hxreborn.extension.protonpass.LayoutComponents;
import app.hxreborn.extension.protonpass.NavigationBarAutoHide;

@SuppressWarnings("unused")
public final class AppliedPatches {

    static final String SCHEDULED_DELETION = "Scheduled Trash and Spam deletion";
    static final String AMOLED_DARK_THEME = "AMOLED dark theme";
    static final String ACCENT_COLOR = "Custom accent color";
    static final String HIDE_UPGRADE_PROMOTIONS = "Hide upgrade promotions";
    static final String MATERIAL_SWITCHES = "Material 3 switches";
    static final String LOADING_INDICATOR = "M3E loading indicator";
    static final String HIDE_NAVIGATION_BAR_ON_SCROLL = "Hide navigation bar on scroll";

    static final String SWIPE_TO_SWITCH_ACCOUNTS = "Swipe to switch accounts";

    private AppliedPatches() {

    }

    public static boolean accentColor() {
        return AccentColor.isPatched();
    }

    public static boolean amoledDarkTheme() {
        return AmoledTheme.isPatched();
    }

    public static boolean hideUpgradePromotions() {
        return UpsellingVisibility.isPatched();
    }

    public static boolean hidePromotionalMessages() {
        return false;
    }

    public static boolean removeSentFromSignature() {
        return false;
    }

    public static boolean removeFreeAccountsLimit() {
        return false;
    }

    public static boolean scheduledDeletion() {
        return false;
    }

    public static boolean unlockCustomTimePicker() {
        return false;
    }

    public static boolean swipeToSwitchAccounts() {
        return false;
    }

    public static boolean removeServerChangeDelay() {
        return false;
    }

    public static boolean unlockSplitTunneling() {
        return false;
    }

    public static boolean unlockLanConnections() {
        return false;
    }

    public static boolean unlockCustomDns() {
        return false;
    }

    public static boolean unlockNetShield() {
        return false;
    }

    public static boolean unlockConnectionPreferences() {
        return false;
    }

    public static boolean unlockProfiles() {
        return false;
    }

    public static boolean showFreeServerLocations() {
        return false;
    }

    public static boolean disableTelemetry() {
        return false;
    }

    static List<String> names() {
        final List<String> names = new ArrayList<>();
        if (accentColor()) {
            names.add(ACCENT_COLOR);
        }
        if (amoledDarkTheme()) {
            names.add(AMOLED_DARK_THEME);
        }
        if (hideUpgradePromotions()) {
            names.add(HIDE_UPGRADE_PROMOTIONS);
        }
        if (MaterialSwitches.isPatched()) {
            names.add(MATERIAL_SWITCHES);
        }
        if (LoadingIndicator.isPatched()) {
            names.add(LOADING_INDICATOR);
        }
        if (LayoutComponents.isPatched() || SidebarEntries.isPatched() || SettingsSections.isPatched()) {
            names.add("Hide layout components");
        }
        if (NavigationBarAutoHide.isPatched()) {
            names.add(HIDE_NAVIGATION_BAR_ON_SCROLL);
        }
        if (hidePromotionalMessages()) {
            names.add("Hide promotional messages");
        }
        if (removeSentFromSignature()) {
            names.add("Remove 'Sent from' signature");
        }
        if (removeFreeAccountsLimit()) {
            names.add("Remove free accounts limit");
        }
        if (scheduledDeletion()) {
            names.add(SCHEDULED_DELETION);
        }
        if (unlockCustomTimePicker()) {
            names.add("Unlock custom time picker");
        }
        if (swipeToSwitchAccounts()) {
            names.add(SWIPE_TO_SWITCH_ACCOUNTS);
        }
        if (removeServerChangeDelay()) {
            names.add("Remove server change delay");
        }
        if (unlockSplitTunneling()) {
            names.add("Unlock split tunneling");
        }
        if (unlockLanConnections()) {
            names.add("Unlock LAN connections");
        }
        if (unlockCustomDns()) {
            names.add("Unlock custom DNS");
        }
        if (unlockNetShield()) {
            names.add("Unlock NetShield");
        }
        if (unlockConnectionPreferences()) {
            names.add("Unlock connection preferences");
        }
        if (unlockProfiles()) {
            names.add("Unlock profiles");
        }
        if (showFreeServerLocations()) {
            names.add("Show free server locations");
        }
        if (disableTelemetry()) {
            names.add("Disable telemetry");
        }
        return names;
    }

}
