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
package app.hxreborn.extension.terabox;

import android.view.View;

@SuppressWarnings("unused")
public final class RecentPanel {

    private static final String REMOVED_TAG = "hx_terabox_recent_removed";

    private RecentPanel() {
    }

    static int panelTabsId() {
        return 0;
    }

    static int panelContentId() {
        return 0;
    }

    public static void updateRecentPanel(View root) {
        if (root == null) {
            return;
        }
        boolean removed = PatchSettings.isRecentPanelRemoved();
        updateView(root, panelTabsId(), removed);
        updateView(root, panelContentId(), removed);
    }

    private static void updateView(View root, int identifier, boolean removed) {
        View panel = root.findViewById(identifier);
        if (panel == null) {
            return;
        }
        if (removed && panel.getVisibility() != View.GONE) {
            panel.setTag(identifier, REMOVED_TAG);
            panel.setVisibility(View.GONE);
        } else if (!removed && REMOVED_TAG.equals(panel.getTag(identifier))) {
            panel.setTag(identifier, null);
            panel.setVisibility(View.VISIBLE);
        }
    }

}
