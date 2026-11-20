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

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

final class MaterialMenu {

    private static final int MIN_WIDTH_DP = 196;

    private static final int ROW_HEIGHT_DP = 48;

    private static final int VERTICAL_PADDING_DP = 8;

    private static final int ITEM_PADDING_DP = 12;

    private static final int ELEVATION_DP = 3;

    private static final int ACTION_RADIUS_DP = 4;

    private static final float ACTION_LABEL_SP = 14f;

    private final Context context;

    private final SettingsStyle style;

    MaterialMenu(Context context, SettingsStyle style) {
        this.context = context;
        this.style = style;
    }

    void show(View anchor, Item... items) {
        LinearLayout menu = new LinearLayout(this.context);
        menu.setOrientation(LinearLayout.VERTICAL);
        menu.setPadding(0, this.style.dp(VERTICAL_PADDING_DP), 0, this.style.dp(VERTICAL_PADDING_DP));
        boolean actionMenu = items.length == 1 && !items[0].checkable;
        int surface = this.style.menuSurface;
        int textColor = this.style.primaryText;
        if (actionMenu) {
            TypedArray colors = this.context.obtainStyledAttributes(
                    new int[] { android.R.attr.colorBackgroundFloating, android.R.attr.textColorPrimary });
            surface = colors.getColor(0, surface);
            textColor = colors.getColor(1, textColor);
            colors.recycle();
        }
        menu.setBackground(this.style.rounded(surface, (actionMenu) ? ACTION_RADIUS_DP : SettingsStyle.CARD_RADIUS_DP));
        PopupWindow popup = new PopupWindow(menu, this.style.dp(MIN_WIDTH_DP), ViewGroup.LayoutParams.WRAP_CONTENT,
                true);
        popup.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        popup.setElevation(this.style.dp(ELEVATION_DP));
        popup.setOutsideTouchable(true);
        for (Item item : items) {
            LinearLayout row = new LinearLayout(this.context);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setMinimumHeight(this.style.dp(ROW_HEIGHT_DP));
            row.setPadding(this.style.dp(ITEM_PADDING_DP), 0, this.style.dp(ITEM_PADDING_DP), 0);
            row.setBackground(this.style.ripple(0));
            row.setSelected(item.selected);
            row.setAccessibilityDelegate(new View.AccessibilityDelegate() {
                @Override
                public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
                    super.onInitializeAccessibilityNodeInfo(host, info);
                    info.setCheckable(item.checkable);
                    info.setChecked(item.selected);
                }
            });
            ImageView icon = new ImageView(this.context);
            icon.setImageDrawable(this.style.icon(item.icon, textColor));
            row.addView(icon, new LinearLayout.LayoutParams(this.style.dp(SettingsStyle.ICON_DP),
                    this.style.dp(SettingsStyle.ICON_DP)));
            TextView label = this.style.text(item.label, (actionMenu) ? ACTION_LABEL_SP : SettingsStyle.ROW_TITLE_SP,
                    textColor, (actionMenu) ? this.style.medium : null);
            label.setPadding(this.style.dp(ITEM_PADDING_DP), 0, this.style.dp(ITEM_PADDING_DP), 0);
            row.addView(label, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            if (item.checkable) {
                ImageView check = new ImageView(this.context);
                check.setImageDrawable(this.style.icon("check", this.style.primaryText));
                check.setVisibility(item.selected ? View.VISIBLE : View.INVISIBLE);
                row.addView(check, new LinearLayout.LayoutParams(this.style.dp(SettingsStyle.ICON_DP),
                        this.style.dp(SettingsStyle.ICON_DP)));
            }
            row.setOnClickListener((view) -> {
                popup.dismiss();
                item.action.run();
            });
            menu.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        popup.showAsDropDown(anchor, Math.max(0, anchor.getWidth() - this.style.dp(MIN_WIDTH_DP)), 0);
    }

    static final class Item {

        final String label;

        final String icon;

        final boolean selected;

        final boolean checkable;

        final Runnable action;

        Item(String label, String icon, Runnable action) {
            this.label = label;
            this.icon = icon;
            this.selected = false;
            this.checkable = false;
            this.action = action;
        }

        Item(String label, String icon, boolean selected, Runnable action) {
            this.label = label;
            this.icon = icon;
            this.selected = selected;
            this.checkable = true;
            this.action = action;
        }

    }

}
