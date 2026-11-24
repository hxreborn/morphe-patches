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
package app.hxreborn.extension.protonpass;

import java.util.ArrayList;
import java.util.List;

import android.view.View;

import app.hxreborn.extension.proton.AppSettings;
import app.hxreborn.extension.proton.SettingsScreen;

@SuppressWarnings("unused")
public final class PassSettings implements AppSettings {

    public static boolean hidePromotionalMessages() {
        return false;
    }

    public static String hidePromotionalMessagesName() {
        return null;
    }

    public static boolean removeFreeAccountsLimit() {
        return false;
    }

    public static String removeFreeAccountsLimitName() {
        return null;
    }

    public static String hideNavigationBarOnScrollName() {
        return null;
    }

    public static String hideNavigationBarOnScrollDescription() {
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
        if (LayoutComponents.isPatched()) {
            rows.add(screen.layoutSwitchRow("Hide Help center", "Hides the feedback, import/export and tutorial links.",
                    LayoutComponents.HELP_CENTER));
            if (LayoutComponents.hasSpreadTheWord()) {
                rows.add(screen.layoutSwitchRow("Hide Spread the word", "Hides Tell a friend and Rate app.",
                        LayoutComponents.SPREAD_THE_WORD));
            }
            rows.add(screen.layoutSwitchRow("Hide About", "Hides Privacy policy and Terms of service.",
                    LayoutComponents.ABOUT));
        }
        if (NavigationBarAutoHide.isPatched()) {
            rows.add(screen.switchRow(hideNavigationBarOnScrollName(), hideNavigationBarOnScrollDescription(),
                    NavigationBarAutoHide.isEnabled(), NavigationBarAutoHide::setEnabled));
        }
    }

    @Override
    public boolean hasLayoutComponents() {
        return LayoutComponents.isPatched();
    }

    @Override
    public List<String> appliedPatchNames() {
        final List<String> names = new ArrayList<>();
        if (NavigationBarAutoHide.isPatched()) {
            names.add(hideNavigationBarOnScrollName());
        }
        if (hidePromotionalMessages()) {
            names.add(hidePromotionalMessagesName());
        }
        if (removeFreeAccountsLimit()) {
            names.add(removeFreeAccountsLimitName());
        }
        return names;
    }

}
