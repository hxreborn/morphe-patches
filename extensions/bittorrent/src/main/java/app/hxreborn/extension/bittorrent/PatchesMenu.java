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
package app.hxreborn.extension.bittorrent;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import app.hxreborn.extension.shared.HostedComponents;

@SuppressWarnings("unused")
public final class PatchesMenu {

    static final String SETTINGS_ROW_TITLE = "hxreborn patches";

    private static final String ROW_TAG = "hx_bittorrent_patches_entry";

    private static final String CHEVRON_TAG = "hx_bittorrent_patches_entry_chevron";

    private PatchesMenu() {
    }

    public static void attach(Activity activity) {
        PatchSettings.initialize(activity);
        View title = activity.getWindow().getDecorView().findViewWithTag(ROW_TAG);
        View chevron = activity.getWindow().getDecorView().findViewWithTag(CHEVRON_TAG);
        if (title == null || chevron == null) {
            Log.w(PatchSettings.LOG_TAG, "Settings entry missing");
            return;
        }
        title.post(() -> copyNativeColor(activity, title, chevron));
        View.OnClickListener open = (view) -> HostedComponents.startActivity(activity,
                new Intent(activity, PatchesSettingsActivity.class));
        title.setOnClickListener(open);
        chevron.setOnClickListener(open);
    }

    private static void copyNativeColor(Activity activity, View title, View chevron) {
        View nativeTitle = activity.findViewById(incomingPortTitleId());
        if (nativeTitle instanceof TextView && title instanceof TextView && chevron instanceof ImageView) {
            int nativeColor = ((TextView) nativeTitle).getCurrentTextColor();
            ((TextView) title).setTextColor(nativeColor);
            ((ImageView) chevron).setColorFilter(nativeColor);
        }
    }

    private static int incomingPortTitleId() {
        return 0;
    }

}
