/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
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
