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
package app.hxreborn.extension.utorrent;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

@SuppressWarnings("unused")
public final class PatchesMenu {

    static final String SETTINGS_ROW_TITLE = "hxreborn patches";

    private static final String ROW_TAG = "hx_utorrent_patches_entry";

    private static final int ENTRY_HEIGHT_DP = 48;

    private static final int ENTRY_INSET_DP = 30;

    private static final int CHEVRON_SIZE_DP = 16;

    private static final int ENTRY_TEXT_SP = 14;

    private PatchesMenu() {
    }

    public static void attach(Activity activity) {
        PatchSettings.initialize(activity);
        SettingsStyle style = new SettingsStyle(activity);
        View existing = activity.getWindow().getDecorView().findViewWithTag(ROW_TAG);
        if (existing instanceof LinearLayout row) {
            ((TextView) row.getChildAt(0)).setTextColor(style.secondaryText);
            ((ImageView) row.getChildAt(1))
                .setImageDrawable(style.hostIcon(SettingsStyle.chevronIconId(), style.secondaryText));
            return;
        }
        View anchor = activity.findViewById(downloadFolderRowId());
        TextView nativeLabel = activity.findViewById(downloadFolderLabelId());
        if (anchor == null || nativeLabel == null || !(anchor.getParent() instanceof LinearLayout parent)) {
            Log.w(PatchSettings.LOG_TAG, "Settings entry anchor missing");
            return;
        }
        LinearLayout row = new LinearLayout(activity);
        row.setTag(ROW_TAG);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(style.dp(ENTRY_HEIGHT_DP));
        row.setPadding(style.dp(ENTRY_INSET_DP), 0, style.dp(ENTRY_INSET_DP), 0);
        row.setBackground(style.ripple(0));
        TextView label = style.text(SETTINGS_ROW_TITLE, ENTRY_TEXT_SP, style.secondaryText, nativeLabel.getTypeface());
        row.addView(label, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        ImageView chevron = new ImageView(activity);
        chevron.setImageDrawable(style.hostIcon(SettingsStyle.chevronIconId(), style.secondaryText));
        chevron.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        row.addView(chevron, new LinearLayout.LayoutParams(style.dp(CHEVRON_SIZE_DP), style.dp(CHEVRON_SIZE_DP)));
        row.setOnClickListener((view) -> activity.startActivity(new Intent(activity, PatchesSettingsActivity.class)));
        parent.addView(row, parent.indexOfChild(anchor), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private static int downloadFolderRowId() {
        return 0;
    }

    private static int downloadFolderLabelId() {
        return 0;
    }

}
