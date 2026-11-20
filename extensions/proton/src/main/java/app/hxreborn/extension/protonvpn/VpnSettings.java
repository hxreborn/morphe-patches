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
package app.hxreborn.extension.protonvpn;

import java.util.ArrayList;
import java.util.List;

import android.view.View;

import app.hxreborn.extension.proton.AppSettings;
import app.hxreborn.extension.proton.SettingsScreen;

@SuppressWarnings("unused")
public final class VpnSettings implements AppSettings {

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

    @Override
    public void accountRows(SettingsScreen screen, List<View> rows) {
    }

    @Override
    public void generalRows(SettingsScreen screen, List<View> rows) {
    }

    @Override
    public void layoutRows(SettingsScreen screen, List<View> rows) {
    }

    @Override
    public boolean hasLayoutComponents() {
        return false;
    }

    @Override
    public List<String> appliedPatchNames() {
        final List<String> names = new ArrayList<>();
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

}
