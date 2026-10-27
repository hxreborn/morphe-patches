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
package app.hxreborn.extension.readera;

import android.view.Menu;
import android.view.MenuItem;

@SuppressWarnings("unused")
public final class LibraryViewMenu {

    private LibraryViewMenu() {

    }

    public static void show(Menu menu, int filterItemId, int currentModeItemId) {
        MenuItem filterItem = menu.findItem(filterItemId);
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            if (!item.hasSubMenu()) {
                continue;
            }
            MenuItem currentModeItem = item.getSubMenu().findItem(currentModeItemId);
            if (currentModeItem == null) {
                continue;
            }
            item.setVisible(filterItem != null && filterItem.isVisible());
            currentModeItem.setChecked(true);
            return;
        }
    }

}
