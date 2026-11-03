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
package app.hxreborn.extension.duolingo;

import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

final class SettingsRows {

    private static final int ROW_MIN_HEIGHT_DP = 64;

    private static final int ROW_VERTICAL_PADDING_DP = 14;

    private static final int ICON_END_MARGIN_DP = 16;

    private static final int SWITCH_START_MARGIN_DP = 16;

    private static final int DESCRIPTION_TOP_PADDING_DP = 2;

    private static final int DIVIDER_HEIGHT_PX = 1;

    private static final int SECTION_START_DP = 4;

    private static final int SECTION_TOP_DP = 24;

    private static final int SECTION_BOTTOM_DP = 8;

    private static final int VALUE_LABEL_WIDTH_DP = 112;

    private final SettingsStyle style;

    SettingsRows(SettingsStyle style) {
        this.style = style;
    }

    TextView sectionHeader(String title) {
        TextView header = this.style.boldText(title, SettingsStyle.SECTION_SP, this.style.wolf);
        header.setPadding(this.style.dp(SECTION_START_DP), this.style.dp(SECTION_TOP_DP), 0,
                this.style.dp(SECTION_BOTTOM_DP));
        SettingsStyle.markHeading(header);
        return header;
    }

    LinearLayout card() {
        LinearLayout card = new LinearLayout(this.style.context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(this.style.rounded(this.style.polar, SettingsStyle.CARD_RADIUS_DP));
        card.setClipToOutline(true);
        return card;
    }

    void switchRow(LinearLayout card, String iconName, String patchName, String description, Runnable onChanged) {
        LinearLayout row = baseRow(card, iconName);
        addLabels(row, patchName, description);

        boolean enabled = PatchSettings.isEnabled(patchName, PatchSettings.SWITCH_DEFAULT);
        SettingsStyle.Toggle toggle = new SettingsStyle.Toggle(this.style, enabled);
        LinearLayout.LayoutParams toggleParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        toggleParams.setMarginStart(this.style.dp(SWITCH_START_MARGIN_DP));
        row.addView(toggle, toggleParams);
        SettingsStyle.Toggle.describeAsSwitch(row, toggle);
        row.setBackground(this.style.pressedFeedback());
        row.setOnClickListener((view) -> {
            toggle.toggle();
            PatchSettings.setEnabled(patchName, toggle.isChecked());
            onChanged.run();
        });
    }

    void actionRow(LinearLayout card, String iconName, String title, String description, Runnable action) {
        LinearLayout row = baseRow(card, iconName);
        addLabels(row, title, description);
        row.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override
            public void onInitializeAccessibilityNodeInfo(View view, AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(view, info);
                info.setClassName(Button.class.getName());
            }
        });
        row.setBackground(this.style.pressedFeedback());
        row.setOnClickListener((view) -> action.run());
    }

    void valueRow(LinearLayout card, String iconName, String label, String value) {
        LinearLayout row = baseRow(card, iconName);
        row.addView(this.style.regularText(label, SettingsStyle.VALUE_SP, this.style.wolf),
                new LinearLayout.LayoutParams(this.style.dp(VALUE_LABEL_WIDTH_DP),
                        ViewGroup.LayoutParams.WRAP_CONTENT));
        row.addView(this.style.regularText(value, SettingsStyle.VALUE_SP, this.style.eel),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row.setContentDescription(label + ", " + value);
    }

    private LinearLayout baseRow(LinearLayout card, String iconName) {
        if (card.getChildCount() > 0) {
            divider(card);
        }
        LinearLayout row = new LinearLayout(this.style.context);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(this.style.dp(ROW_MIN_HEIGHT_DP));
        int inset = this.style.dp(SettingsStyle.SCREEN_INSET_DP);
        int vertical = this.style.dp(ROW_VERTICAL_PADDING_DP);
        row.setPadding(inset, vertical, inset, vertical);

        Drawable icon = this.style.hostIcon(iconName, this.style.eel);
        if (icon != null) {
            ImageView iconView = new ImageView(this.style.context);
            iconView.setImageDrawable(icon);
            iconView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(this.style.dp(SettingsStyle.ICON_DP),
                    this.style.dp(SettingsStyle.ICON_DP));
            iconParams.setMarginEnd(this.style.dp(ICON_END_MARGIN_DP));
            row.addView(iconView, iconParams);
        }
        card.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return row;
    }

    private void addLabels(LinearLayout row, String title, String description) {
        LinearLayout labels = new LinearLayout(this.style.context);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.addView(this.style.boldText(title, SettingsStyle.ROW_TITLE_SP, this.style.eel));
        TextView body = this.style.regularText(description, SettingsStyle.SUMMARY_SP, this.style.wolf);
        body.setPadding(0, this.style.dp(DESCRIPTION_TOP_PADDING_DP), 0, 0);
        labels.addView(body);
        row.addView(labels, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
    }

    private void divider(LinearLayout card) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                DIVIDER_HEIGHT_PX);
        params
            .setMarginStart(this.style.dp(SettingsStyle.SCREEN_INSET_DP + SettingsStyle.ICON_DP + ICON_END_MARGIN_DP));
        card.addView(this.style.divider(), params);
    }

}
