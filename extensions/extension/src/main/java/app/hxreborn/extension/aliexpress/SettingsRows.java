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
package app.hxreborn.extension.aliexpress;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

final class SettingsRows {

    private static final int SECTION_HEADER_START_PADDING_DP = 4;

    private static final int SECTION_HEADER_TOP_PADDING_DP = 24;

    private static final int SECTION_HEADER_BOTTOM_PADDING_DP = 8;

    private static final int DESCRIPTION_TOP_PADDING_DP = 2;

    private static final float EXPANDED_CHEVRON_DEGREES = 180f;

    private static final int ROW_MIN_HEIGHT_DP = 64;

    private static final int ROW_VERTICAL_PADDING_DP = 14;

    private static final int ICON_END_MARGIN_DP = 16;

    private static final int SWITCH_START_MARGIN_DP = 16;

    private static final int FIELD_LABEL_WIDTH_DP = 112;

    private static final int DIVIDER_HEIGHT_PX = 1;

    private final Context context;

    private final SettingsStyle style;

    SettingsRows(Context context, SettingsStyle style) {
        this.context = context;
        this.style = style;
    }

    TextView sectionHeader(String title) {
        TextView header = this.style.text(title, SettingsStyle.SECTION_SP, this.style.secondaryText, this.style.medium);
        header.setPadding(this.style.dp(SECTION_HEADER_START_PADDING_DP), this.style.dp(SECTION_HEADER_TOP_PADDING_DP),
                0, this.style.dp(SECTION_HEADER_BOTTOM_PADDING_DP));
        SettingsStyle.markHeading(header);
        return header;
    }

    LinearLayout card() {
        LinearLayout card = new LinearLayout(this.context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(this.style.rounded(this.style.cardSurface, SettingsStyle.CARD_RADIUS_DP));
        card.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
        card.setClipToOutline(true);
        return card;
    }

    void switchRow(LinearLayout card, String iconName, String title, String description, boolean checked,
            CompoundButton.OnCheckedChangeListener listener) {
        CompoundButton toggle = SettingsStyle.createSwitch(this.context);
        toggle.setChecked(checked);
        toggle.setContentDescription(title);
        toggle.setOnCheckedChangeListener(listener);
        LinearLayout row = baseRow(card, iconName);
        addTitleAndDescription(row, title, description);
        LinearLayout.LayoutParams toggleParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        toggleParams.setMarginStart(this.style.dp(SWITCH_START_MARGIN_DP));
        row.addView(toggle, toggleParams);
        row.setBackground(this.style.ripple(0));
        row.setOnClickListener((view) -> toggle.toggle());
    }

    void actionRow(LinearLayout card, String iconName, String title, String description, Runnable action) {
        LinearLayout row = baseRow(card, iconName);
        addTitleAndDescription(row, title, description);
        row.setBackground(this.style.ripple(0));
        row.setOnClickListener((view) -> action.run());
    }

    void valueRow(LinearLayout card, String iconName, String label, String value) {
        addValue(baseRow(card, iconName), label, value);
    }

    void expandableRow(LinearLayout card, String title, String description) {
        if (card.getChildCount() > 0) {
            divider(card, SettingsStyle.SCREEN_INSET_DP);
        }
        LinearLayout row = new LinearLayout(this.context);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(this.style.dp(ROW_MIN_HEIGHT_DP));
        row.setPadding(this.style.dp(SettingsStyle.SCREEN_INSET_DP), this.style.dp(ROW_VERTICAL_PADDING_DP),
                this.style.dp(SettingsStyle.SCREEN_INSET_DP), this.style.dp(ROW_VERTICAL_PADDING_DP));
        LinearLayout labels = new LinearLayout(this.context);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.addView(this.style.text(title, SettingsStyle.BODY_SP, this.style.primaryText, null));
        TextView details = this.style.text(description, SettingsStyle.BODY_SP, this.style.secondaryText, null);
        details.setPadding(0, this.style.dp(DESCRIPTION_TOP_PADDING_DP), 0, 0);
        details.setVisibility(View.GONE);
        labels.addView(details);
        row.addView(labels, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        ImageView chevron = new ImageView(this.context);
        chevron.setImageDrawable(this.style.hostIcon("hx_aliexpress_ic_expand", this.style.secondaryText));
        chevron.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        LinearLayout.LayoutParams chevronParams = new LinearLayout.LayoutParams(this.style.dp(SettingsStyle.ICON_DP),
                this.style.dp(SettingsStyle.ICON_DP));
        chevronParams.setMarginStart(this.style.dp(SWITCH_START_MARGIN_DP));
        row.addView(chevron, chevronParams);
        row.setBackground(this.style.ripple(0));
        row.setOnClickListener((view) -> {
            boolean expanding = details.getVisibility() == View.GONE;
            details.setVisibility(expanding ? View.VISIBLE : View.GONE);
            chevron.setRotation(expanding ? EXPANDED_CHEVRON_DEGREES : 0f);
        });
        card.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private void addValue(LinearLayout row, String label, String value) {
        TextView labelView = this.style.text(label, SettingsStyle.BODY_SP, this.style.secondaryText, null);
        row.addView(labelView, new LinearLayout.LayoutParams(this.style.dp(FIELD_LABEL_WIDTH_DP),
                ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView valueView = this.style.text(value, SettingsStyle.BODY_SP, this.style.primaryText, null);
        row.addView(valueView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
    }

    private LinearLayout baseRow(LinearLayout card, String iconName) {
        if (card.getChildCount() > 0) {
            divider(card, SettingsStyle.SCREEN_INSET_DP + SettingsStyle.ICON_DP + ICON_END_MARGIN_DP);
        }
        LinearLayout row = new LinearLayout(this.context);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(this.style.dp(ROW_MIN_HEIGHT_DP));
        row.setPadding(this.style.dp(SettingsStyle.SCREEN_INSET_DP), this.style.dp(ROW_VERTICAL_PADDING_DP),
                this.style.dp(SettingsStyle.SCREEN_INSET_DP), this.style.dp(ROW_VERTICAL_PADDING_DP));
        ImageView icon = new ImageView(this.context);
        icon.setImageDrawable(this.style.hostIcon(iconName, this.style.primaryText));
        icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(this.style.dp(SettingsStyle.ICON_DP),
                this.style.dp(SettingsStyle.ICON_DP));
        iconParams.setMarginEnd(this.style.dp(ICON_END_MARGIN_DP));
        row.addView(icon, iconParams);
        card.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return row;
    }

    private void addTitleAndDescription(LinearLayout row, String title, String description) {
        LinearLayout labels = new LinearLayout(this.context);
        labels.setOrientation(LinearLayout.VERTICAL);
        row.addView(labels, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        labels.addView(this.style.text(title, SettingsStyle.ROW_TITLE_SP, this.style.primaryText, this.style.medium));
        TextView body = this.style.text(description, SettingsStyle.BODY_SP, this.style.secondaryText, null);
        body.setPadding(0, this.style.dp(DESCRIPTION_TOP_PADDING_DP), 0, 0);
        labels.addView(body);
    }

    private void divider(LinearLayout card, int startInsetDp) {
        View divider = new View(this.context);
        divider.setBackgroundColor(this.style.divider);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                DIVIDER_HEIGHT_PX);
        params.setMarginStart(this.style.dp(startInsetDp));
        card.addView(divider, params);
    }

}
