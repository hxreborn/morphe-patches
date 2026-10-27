/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.bereal;

import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import app.morphe.extension.shared.settings.BooleanSetting;

final class SettingsRows {

    private final Context context;

    private final SettingsStyle style;

    SettingsRows(Context context, SettingsStyle style) {
        this.context = context;
        this.style = style;
    }

    TextView sectionHeader(String title) {
        final TextView header = this.style.text(title, SettingsStyle.SECTION_SP, SettingsStyle.SECONDARY_TEXT,
                this.style.fonts.medium);
        header.setAllCaps(true);
        header.setLetterSpacing(0.08f);
        header.setPadding(this.style.dp(4), this.style.dp(20), 0, this.style.dp(8));
        SettingsStyle.markHeading(header);
        return header;
    }

    LinearLayout card() {
        final LinearLayout card = new LinearLayout(this.context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(this.style.rounded(SettingsStyle.CARD_SURFACE, SettingsStyle.CARD_RADIUS_DP));
        card.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
        card.setClipToOutline(true);
        return card;
    }

    void divider(LinearLayout card) {
        final View divider = new View(this.context);
        divider.setBackgroundColor(SettingsStyle.DIVIDER);
        final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1);
        params.leftMargin = this.style.dp(SettingsStyle.SCREEN_INSET_DP);
        card.addView(divider, params);
    }

    Row switchRow(LinearLayout card, String iconName, String title, String description, String chip,
            BooleanSetting setting, Runnable onChanged) {
        final HostToggle toggle = displayToggle(setting.get());
        final Row row = baseRow(card, iconName, title, description, chip, chip, toggle);
        toggle.setOnCheckedChangeListener((button, checked) -> {
            setting.save(checked);
            onChanged.run();
            row.view.sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED);
        });
        row.view.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override
            public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setClassName(Switch.class.getName());
                info.setCheckable(true);
                info.setChecked(toggle.isChecked());
            }
        });
        pressable(row.view, toggle::toggle);
        return row;
    }

    Row alwaysOnRow(LinearLayout card, String iconName, String title, String description) {
        final HostToggle toggle = displayToggle(true);
        toggle.setEnabled(false);
        final Row row = baseRow(card, iconName, title, description, "always on", null, toggle);
        row.view.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override
            public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setClassName(Switch.class.getName());
                info.setCheckable(true);
                info.setChecked(true);
                info.setEnabled(false);
            }
        });
        return row;
    }

    Row actionRow(LinearLayout card, String iconName, String title, String description, Runnable action) {
        final Row row = baseRow(card, iconName, title, description, null, null, null);
        row.view.setAccessibilityDelegate(buttonDelegate());
        pressable(row.view, action);
        return row;
    }

    Row textRow(LinearLayout card, String iconName, String title, String description) {
        return baseRow(card, iconName, title, description, null, null, null);
    }

    Row valueRow(LinearLayout card, String iconName, String title, String value) {
        final Row row = baseRow(card, iconName, title, "", null, null, null);
        row.setValue(value);
        return row;
    }

    private Row baseRow(LinearLayout card, String iconName, String title, String description, String spokenState,
            String chip, View trailing) {
        if (card.getChildCount() > 0) {
            divider(card);
        }
        final LinearLayout row = new LinearLayout(this.context);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(this.style.dp(56));
        row.setPadding(this.style.dp(SettingsStyle.SCREEN_INSET_DP), this.style.dp(14),
                this.style.dp(SettingsStyle.SCREEN_INSET_DP), this.style.dp(14));

        final ImageView icon = this.style.hostIconView(iconName);
        if (icon != null) {
            final LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
                    this.style.dp(SettingsStyle.ICON_DP), this.style.dp(SettingsStyle.ICON_DP));
            iconParams.rightMargin = this.style.dp(16);
            row.addView(icon, iconParams);
        }

        final LinearLayout labels = new LinearLayout(this.context);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.addView(this.style.text(title, SettingsStyle.ROW_TITLE_SP, SettingsStyle.PRIMARY_TEXT,
                this.style.fonts.medium));
        if (chip != null) {
            final LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            chipParams.topMargin = this.style.dp(4);
            labels.addView(this.style.chip(chip), chipParams);
        }
        final TextView value = this.style.text("", SettingsStyle.BODY_SP, SettingsStyle.PRIMARY_TEXT,
                this.style.fonts.regular);
        value.setPadding(0, this.style.dp(2), 0, 0);
        value.setSingleLine(true);
        value.setEllipsize(TextUtils.TruncateAt.END);
        value.setVisibility(View.GONE);
        labels.addView(value);
        if (!description.isEmpty()) {
            final TextView body = this.style.text(description, SettingsStyle.BODY_SP, SettingsStyle.SECONDARY_TEXT,
                    this.style.fonts.regular);
            body.setPadding(0, this.style.dp(2), 0, 0);
            labels.addView(body);
        }
        row.addView(labels, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (trailing != null) {
            row.addView(trailing);
        }
        card.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return new Row(row, value, title, description, spokenState);
    }

    private HostToggle displayToggle(boolean checked) {
        final HostToggle toggle = new HostToggle(this.context);
        toggle.setChecked(checked);
        toggle.setPadding(this.style.dp(16), 0, 0, 0);
        toggle.setClickable(false);
        toggle.setFocusable(false);
        toggle.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        return toggle;
    }

    private void pressable(View row, Runnable action) {
        row.setBackground(this.style.ripple(Color.TRANSPARENT, 0));
        row.setClickable(true);
        row.setOnClickListener((view) -> action.run());
    }

    private static View.AccessibilityDelegate buttonDelegate() {
        return new View.AccessibilityDelegate() {
            @Override
            public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setClassName(Button.class.getName());
            }
        };
    }

    static final class Row {

        final LinearLayout view;

        private final TextView valueView;

        private final String title;

        private final String description;

        private final String spokenState;

        private Row(LinearLayout view, TextView valueView, String title, String description, String spokenState) {
            this.view = view;
            this.valueView = valueView;
            this.title = title;
            this.description = description;
            this.spokenState = spokenState;
            describe();
        }

        void setValue(String value) {
            this.valueView.setText(value);
            this.valueView.setVisibility(value.isEmpty() ? View.GONE : View.VISIBLE);
            describe();
        }

        private void describe() {
            final StringBuilder spoken = new StringBuilder(this.title);
            if (this.valueView.getVisibility() == View.VISIBLE) {
                spoken.append(", ").append(this.valueView.getText());
            }
            if (this.spokenState != null) {
                spoken.append(", ").append(this.spokenState);
            }
            if (!this.description.isEmpty()) {
                spoken.append(", ").append(this.description);
            }
            this.view.setContentDescription(spoken);
        }

    }

}
