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

import java.util.ArrayList;
import java.util.List;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import app.hxreborn.extension.shared.Dimensions;

@TargetApi(Build.VERSION_CODES.Q)
final class DropdownMenu {

    private static final int MIN_WIDTH_DP = 112;

    private static final int VERTICAL_PADDING_DP = 8;

    private static final int CORNER_DP = 4;

    private static final int ELEVATION_DP = 3;

    private static final int ITEM_HEIGHT_DP = 48;

    private static final int ITEM_HORIZONTAL_DP = 12;

    private static final int ICON_DP = 24;

    private static final float PRESSED_ALPHA = 0.1f;

    private final Context context;

    private final HostPalette palette;

    private final List<Item> items = new ArrayList<>();

    DropdownMenu(Context context) {
        this.context = context;
        this.palette = HostPalette.of(context);
    }

    DropdownMenu add(String title, String icon, Runnable action) {
        this.items.add(new Item(title, icon, false, action));
        return this;
    }

    DropdownMenu addSubmenu(String title, String icon, Runnable open) {
        this.items.add(new Item(title, icon, true, open));
        return this;
    }

    void show(View anchor) {
        final LinearLayout column = new LinearLayout(this.context);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(0, dp(VERTICAL_PADDING_DP), 0, dp(VERTICAL_PADDING_DP));
        column.setMinimumWidth(dp(MIN_WIDTH_DP));
        final PopupWindow popup = new PopupWindow(column, ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, true);
        for (Item item : this.items) {
            column.addView(row(item, popup),
                    new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(ITEM_HEIGHT_DP)));
        }
        final GradientDrawable surface = new GradientDrawable();
        surface.setColor(this.palette.surfaceContainer);
        surface.setCornerRadius(dp(CORNER_DP));
        popup.setBackgroundDrawable(surface);
        popup.setElevation(dp(ELEVATION_DP));
        popup.showAsDropDown(anchor, 0, 0, Gravity.END);
    }

    private LinearLayout row(Item item, PopupWindow popup) {
        final LinearLayout row = new LinearLayout(this.context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPaddingRelative(dp(ITEM_HORIZONTAL_DP), 0, dp(ITEM_HORIZONTAL_DP), 0);
        row.setBackground(new RippleDrawable(
                ColorStateList.valueOf(HostScreenActivity.withAlpha(this.palette.keyText, PRESSED_ALPHA)), null,
                new ColorDrawable(this.palette.keyText)));
        if (item.icon != null) {
            row.addView(icon(item.icon), iconParams(false));
        }
        final TextView title = new TextView(this.context);
        title.setText(item.title);
        title.setTextColor(this.palette.keyText);
        title.setSingleLine(true);
        HostScreenActivity.TITLE_SMALL.applyTo(title);
        row.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (item.submenu) {
            row.addView(icon(MaterialIcons.CHEVRON), iconParams(true));
        }
        row.setOnClickListener((view) -> {
            popup.dismiss();
            item.action.run();
        });
        return row;
    }

    private ImageView icon(String pathData) {
        final ImageView icon = new ImageView(this.context);
        icon.setImageDrawable(new VectorIcon(pathData, this.palette.onSurfaceVariant));
        return icon;
    }

    private LinearLayout.LayoutParams iconParams(boolean trailing) {
        final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(ICON_DP), dp(ICON_DP));
        if (trailing) {
            params.setMarginStart(dp(ITEM_HORIZONTAL_DP));
        } else {
            params.setMarginEnd(dp(ITEM_HORIZONTAL_DP));
        }
        return params;
    }

    private int dp(int value) {
        return Dimensions.dp(this.context, value);
    }

    private static final class Item {

        final String title;

        final String icon;

        final boolean submenu;

        final Runnable action;

        Item(String title, String icon, boolean submenu, Runnable action) {
            this.title = title;
            this.icon = icon;
            this.submenu = submenu;
            this.action = action;
        }

    }

}
