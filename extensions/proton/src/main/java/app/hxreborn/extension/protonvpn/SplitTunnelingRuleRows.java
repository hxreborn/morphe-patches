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

import java.lang.reflect.Proxy;
import java.util.List;

import android.view.View;

import app.hxreborn.extension.proton.SettingsScreen;

@SuppressWarnings("unused")
public final class SplitTunnelingRuleRows {

    private static volatile Object rulesRowOnClickProxy;

    private SplitTunnelingRuleRows() {

    }

    public static void add(SettingsScreen screen, List<View> rows) {
        rows.add(screen.row(SplitTunnelingRuleEditor.SETTINGS_ROW_TITLE, SplitTunnelingRuleEditor.summary(),
                () -> SplitTunnelingRuleEditor.show(screen.activity(), screen::recreateContent)));
    }

    public static Object rulesRowOnClick(Class<?> onClickType) {
        if (rulesRowOnClickProxy == null) {
            rulesRowOnClickProxy = Proxy.newProxyInstance(SplitTunnelingRuleRows.class.getClassLoader(),
                    new Class<?>[] { onClickType }, new SplitTunnelingRulesRowClick());
        }
        return rulesRowOnClickProxy;
    }

    public static String rulesRowSummary() {
        return SplitTunnelingRuleEditor.summary();
    }

    public static String appsSummary(String pickedApps) {
        final int count = SplitTunnelingRuleSettings.matchCount();
        if (!SplitTunnelingRuleSettings.isActive() || count <= 0) {
            return pickedApps;
        }
        return pickedApps + SplitTunnelingRuleEditor.SUMMARY_SEPARATOR + count + " by rules";
    }

}
