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
package app.hxreborn.extension.protonauthenticator;

import java.util.Collections;
import java.util.List;

import android.view.View;

import app.hxreborn.extension.proton.AppSettings;
import app.hxreborn.extension.proton.SettingsScreen;

@SuppressWarnings("unused")
public final class AuthenticatorSettings implements AppSettings {

    @Override
    public void accountRows(SettingsScreen screen, List<View> rows) {
    }

    @Override
    public void generalRows(SettingsScreen screen, List<View> rows) {
    }

    @Override
    public void layoutRows(SettingsScreen screen, List<View> rows) {
        if (SettingsSections.isPatched()) {
            rows.add(screen.layoutSwitchRow("Hide Spread the word", "Hides Tell a friend and Rate app.",
                    SettingsSections.SPREAD_THE_WORD));
            rows.add(screen.layoutSwitchRow("Hide Discover Proton",
                    "Hides the Proton Mail, Pass, VPN, Drive and Calendar cards.", SettingsSections.DISCOVER_PROTON));
            rows.add(screen.layoutSwitchRow("Hide Proton Pass banner",
                    "Hides the promotion card at the top of Settings.", SettingsSections.PASS_BANNER));
        }
    }

    @Override
    public boolean hasLayoutComponents() {
        return SettingsSections.isPatched();
    }

    @Override
    public List<String> appliedPatchNames() {
        return Collections.emptyList();
    }

}
