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
package app.hxreborn.extension.protonmail;

import java.util.ArrayList;
import java.util.List;

import android.view.View;

import app.hxreborn.extension.proton.AppSettings;
import app.hxreborn.extension.proton.SettingsScreen;

@SuppressWarnings("unused")
public final class MailSettings implements AppSettings {

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

    @Override
    public void accountRows(SettingsScreen screen, List<View> rows) {
        addSwipeToSwitchAccountsRows(screen, rows);
    }

    public static void addSwipeToSwitchAccountsRows(SettingsScreen screen, List<View> rows) {
    }

    @Override
    public void generalRows(SettingsScreen screen, List<View> rows) {
        addScheduledDeletionRows(screen, rows);
    }

    public static void addScheduledDeletionRows(SettingsScreen screen, List<View> rows) {
    }

    @Override
    public void layoutRows(SettingsScreen screen, List<View> rows) {
        if (SidebarEntries.isPatched()) {
            rows.add(screen.layoutSwitchRow("Hide Subscription", "Hides the plan and billing entry in the sidebar.",
                    SidebarEntries.SUBSCRIPTION));
            rows.add(screen.layoutSwitchRow("Hide Report a problem", "Hides the bug report entry in the sidebar.",
                    SidebarEntries.REPORT_A_PROBLEM));
        }
    }

    @Override
    public boolean hasLayoutComponents() {
        return SidebarEntries.isPatched();
    }

    @Override
    public List<String> appliedPatchNames() {
        final List<String> names = new ArrayList<>();
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
        return names;
    }

}
