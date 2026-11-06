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
            names.add(accentColorName());
        }
        if (amoledDarkTheme()) {
            names.add(amoledDarkThemeName());
        }
        if (hideUpgradePromotions()) {
            names.add(hideUpgradePromotionsName());
        }
        if (MaterialSwitches.isPatched()) {
            names.add(materialSwitchesName());
        }
        if (LoadingIndicator.isPatched()) {
            names.add(loadingIndicatorName());
        }
        if (LayoutComponents.isPatched() || SidebarEntries.isPatched() || SettingsSections.isPatched()) {
            names.add(hideLayoutComponentsName());
        }
        if (NavigationBarAutoHide.isPatched()) {
            names.add(hideNavigationBarOnScrollName());
        }
        if (hidePromotionalMessages()) {
            names.add(hidePromotionalMessagesName());
        }
        if (removeSentFromSignature()) {
            names.add(removeSentFromSignatureName());
        }
        if (removeFreeAccountsLimit()) {
            names.add(removeFreeAccountsLimitName());
        }
        if (scheduledDeletion()) {
            names.add(scheduledDeletionName());
        }
        if (unlockCustomTimePicker()) {
            names.add(unlockCustomTimePickerName());
        }
        if (swipeToSwitchAccounts()) {
            names.add(swipeToSwitchAccountsName());
        }
        if (removeServerChangeDelay()) {
            names.add(removeServerChangeDelayName());
        }
        if (unlockSplitTunneling()) {
            names.add(unlockSplitTunnelingName());
        }
        if (unlockLanConnections()) {
            names.add(unlockLanConnectionsName());
        }
        if (unlockCustomDns()) {
            names.add(unlockCustomDnsName());
        }
        if (unlockNetShield()) {
            names.add(unlockNetShieldName());
        }
        if (unlockConnectionPreferences()) {
            names.add(unlockConnectionPreferencesName());
        }
        if (unlockProfiles()) {
            names.add(unlockProfilesName());
        }
        if (showFreeServerLocations()) {
            names.add(showFreeServerLocationsName());
        }
        if (disableTelemetry()) {
            names.add(disableTelemetryName());
        }
        return names;
    }

    public static String accentColorName() {
        return null;
    }

    public static String amoledDarkThemeDescription() {
        return null;
    }

    public static String amoledDarkThemeName() {
        return null;
    }

    public static String hideUpgradePromotionsDescription() {
        return null;
    }

    public static String hideUpgradePromotionsName() {
        return null;
    }

    public static String materialSwitchesDescription() {
        return null;
    }

    public static String materialSwitchesName() {
        return null;
    }

    public static String loadingIndicatorDescription() {
        return null;
    }

    public static String loadingIndicatorName() {
        return null;
    }

    public static String hideLayoutComponentsName() {
        return null;
    }

    public static String hideNavigationBarOnScrollName() {
        return null;
    }

    public static String hideNavigationBarOnScrollDescription() {
        return null;
    }

    public static String hidePromotionalMessagesName() {
        return null;
    }

    public static String removeSentFromSignatureName() {
        return null;
    }

    public static String removeFreeAccountsLimitName() {
        return null;
    }

    public static String scheduledDeletionName() {
        return null;
    }

    public static String unlockCustomTimePickerName() {
        return null;
    }

    public static String swipeToSwitchAccountsName() {
        return null;
    }

    public static String removeServerChangeDelayName() {
        return null;
    }

    public static String unlockSplitTunnelingName() {
        return null;
    }

    public static String unlockLanConnectionsName() {
        return null;
    }

    public static String unlockCustomDnsName() {
        return null;
    }

    public static String unlockNetShieldName() {
        return null;
    }

    public static String unlockConnectionPreferencesName() {
        return null;
    }

    public static String unlockProfilesName() {
        return null;
    }

    public static String showFreeServerLocationsName() {
        return null;
    }

    public static String disableTelemetryName() {
        return null;
    }

}
