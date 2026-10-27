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
package app.hxreborn.extension.netanalyzer;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

@TargetApi(Build.VERSION_CODES.Q)
@SuppressWarnings("unused")
public final class SettingsActivity extends HostScreenActivity {

    private static final String DISABLE_WIFI_THROTTLING_KEY = "DisableWifiThrottlingPreference";

    private static final String ENABLE_6GHZ_KEY = "Enable6GHzWifiPreference";

    private static final String[] THEME_VALUES = { HostPalette.THEME_LIGHT, HostPalette.THEME_DARK,
            HostPalette.THEME_SYSTEM };

    private static final String[] THEME_LABELS = { "Light", "Dark", "System" };

    private SharedPreferences preferences;

    @Override
    String screenTitle() {
        return "Settings";
    }

    @Override
    void buildContent() {
        this.preferences = getSharedPreferences(getPackageName() + HostPalette.PREFERENCES_SUFFIX, MODE_PRIVATE);

        section("Appearance");
        segmentedRow(THEME_LABELS, themeIndex(), (index) -> {
            if (index != themeIndex()) {
                this.preferences.edit().putString(HostPalette.THEME_MODE_KEY, THEME_VALUES[index]).commit();
                restartApp();
            }
        });

        section("Various");
        switchRow("Increase Wi-Fi scan frequency",
                "On Android 9 and newer, Wi-Fi scan frequency is limited to twice per minute. This throttling can "
                        + "be disabled in Android Settings → Developer Options. When disabled, this setting allows "
                        + "Network Analyzer to perform scans more frequently.",
                this.preferences.getBoolean(DISABLE_WIFI_THROTTLING_KEY, false),
                (checked) -> this.preferences.edit().putBoolean(DISABLE_WIFI_THROTTLING_KEY, checked).apply());
        switchRow("Enable 6GHz Wi-Fi scan",
                "Enables scanning in the 6GHz spectrum. Note that both your phone and your router have to support "
                        + "Wi-Fi 6E or Wi-Fi 7 (Wi-Fi 6 doesn't support 6GHz networks).",
                this.preferences.getBoolean(ENABLE_6GHZ_KEY, true),
                (checked) -> this.preferences.edit().putBoolean(ENABLE_6GHZ_KEY, checked).apply());
    }

    private int themeIndex() {
        final String mode = this.preferences.getString(HostPalette.THEME_MODE_KEY, HostPalette.THEME_SYSTEM);
        for (int i = 0; i < THEME_VALUES.length; i++) {
            if (THEME_VALUES[i].equals(mode)) {
                return i;
            }
        }
        return THEME_VALUES.length - 1;
    }

    private void restartApp() {
        final Intent launch = getPackageManager().getLaunchIntentForPackage(getPackageName());
        if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(launch);
        }
        Runtime.getRuntime().exit(0);
    }

}
