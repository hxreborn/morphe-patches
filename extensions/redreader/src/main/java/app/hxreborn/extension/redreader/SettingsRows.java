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
package app.hxreborn.extension.redreader;

import android.content.Context;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

final class SettingsRows {

    private static final int DIVIDER_ALPHA_MASK = 0x33FFFFFF;

    private static final int DIVIDER_HEIGHT_DP = 1;

    private final Context context;

    private final SettingsStyle style;

    SettingsRows(Context context, SettingsStyle style) {
        this.context = context;
        this.style = style;
    }

    void section(LinearLayout column, String title) {
        TextView heading = this.style.text(title, SettingsStyle.SUMMARY_SP, this.style.secondary);
        heading.setPadding(this.style.dp(SettingsStyle.INSET_DP), this.style.dp(SettingsStyle.INSET_DP), 0,
                this.style.dp(SettingsStyle.INSET_DP));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            heading.setAccessibilityHeading(true);
        }
        column.addView(heading);
    }

    LinearLayout card(LinearLayout column) {
        LinearLayout card = new LinearLayout(this.context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(this.style.surface());
        column.addView(card, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return card;
    }

    void row(LinearLayout card, String title, String description, Runnable action) {
        if (card.getChildCount() > 0) {
            View divider = new View(this.context);
            divider.setBackgroundColor(this.style.secondary & DIVIDER_ALPHA_MASK);
            LinearLayout.LayoutParams separator = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    this.style.dp(DIVIDER_HEIGHT_DP));
            separator.setMargins(this.style.dp(SettingsStyle.INSET_DP), 0, this.style.dp(SettingsStyle.INSET_DP), 0);
            card.addView(divider, separator);
        }
        LinearLayout row = new LinearLayout(this.context);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(this.style.dp(SettingsStyle.INSET_DP), this.style.dp(SettingsStyle.INSET_DP),
                this.style.dp(SettingsStyle.INSET_DP), this.style.dp(SettingsStyle.INSET_DP));
        row.setMinimumHeight(this.style.dp(SettingsStyle.ROW_HEIGHT_DP));
        row.addView(this.style.text(title, SettingsStyle.BODY_SP, this.style.text));
        row.addView(this.style.text(description, SettingsStyle.SUMMARY_SP, this.style.secondary));
        if (action != null) {
            row.setBackground(this.style.ripple());
            row.setFocusable(true);
            row.setOnClickListener((view) -> action.run());
        }
        card.addView(row);
    }

}
