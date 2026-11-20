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
package app.hxreborn.extension.netanalyzer;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

final class HistoryRows {

    private static final int STAR_BUTTON_DP = 34;

    private static final int STAR_GAP_DP = 8;

    private static final int ROW_START_DP = 16;

    private static final int ROW_VERTICAL_DP = 4;

    private static final int ROW_END_DP = 8;

    private static final int TITLE_END_DP = 4;

    private static final int CHEVRON_DP = 24;

    private HistoryRows() {
    }

    static LinearLayout row(HostScreenActivity screen, boolean starred, View.OnClickListener onToggle, View content) {
        final LinearLayout row = new LinearLayout(screen);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(screen.rowBackground());
        row.setPaddingRelative(screen.dp(ROW_START_DP), screen.dp(ROW_VERTICAL_DP), screen.dp(ROW_END_DP),
                screen.dp(ROW_VERTICAL_DP));
        row.addView(new StarButton(screen, screen.palette, starred, onToggle),
                new LinearLayout.LayoutParams(screen.dp(STAR_BUTTON_DP), screen.dp(STAR_BUTTON_DP)));
        final LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        contentParams.setMarginStart(screen.dp(STAR_GAP_DP));
        row.addView(content, contentParams);
        final ImageView more = new ImageView(screen);
        more.setImageDrawable(new VectorIcon(MaterialIcons.CHEVRON, screen.palette.rowSubsubtitle));
        more.setContentDescription("More");
        row.addView(more, new LinearLayout.LayoutParams(screen.dp(CHEVRON_DP), screen.dp(CHEVRON_DP)));
        return row;
    }

    static LinearLayout titleLine(HostScreenActivity screen, String title, String date) {
        final LinearLayout line = new LinearLayout(screen);
        line.setOrientation(LinearLayout.HORIZONTAL);
        line.setGravity(Gravity.CENTER_VERTICAL);
        final TextView titleView = HostScreenActivity
            .singleLine(screen.text(title, HostScreenActivity.BODY_MEDIUM, screen.palette.valueText));
        titleView.setPaddingRelative(0, titleView.getPaddingTop(), screen.dp(TITLE_END_DP),
                titleView.getPaddingBottom());
        line.addView(titleView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        final TextView dateView = HostScreenActivity
            .singleLine(screen.text(date, HostScreenActivity.BODY_MEDIUM, screen.palette.rowSubsubtitle));
        dateView.setGravity(Gravity.END);
        line.addView(dateView);
        return line;
    }

}
