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

import java.util.List;

import android.app.Activity;
import android.app.Dialog;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;

import app.hxreborn.extension.proton.AccentColor;
import app.hxreborn.extension.proton.PatchesDialog;
import app.hxreborn.extension.proton.PatchesTheme;

final class SplitTunnelingAppList {

    private static final int BODY_TEXT_SP = 14;

    private SplitTunnelingAppList() {

    }

    static void show(Activity activity, List<SplitTunnelingApp> apps) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        final int accentColor = AccentColor
            .getAccentColor(PatchesTheme.isDark(PatchesTheme.backgroundSecondary(activity)));

        final ListView list = new ListView(activity);
        list.setDivider(null);
        list.setAdapter(new SplitTunnelingAppAdapter(activity, apps));

        final LinearLayout surface = PatchesDialog.createContentLayout(activity);
        surface.addView(PatchesDialog.createTitle(activity, "Matching apps"));
        surface.addView(list, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        final Dialog dialog = PatchesDialog.createDialog(activity, surface);
        final LinearLayout actions = PatchesDialog.createButtonRow(activity);
        actions.addView(PatchesDialog.configureActionButton(activity,
                PatchesDialog.createTextView(activity, "Close", BODY_TEXT_SP, accentColor), dialog::dismiss));
        surface.addView(actions, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        dialog.show();
    }

}
