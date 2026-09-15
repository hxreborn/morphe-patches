/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.protonmail;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import java.util.List;

@SuppressLint("SetTextI18n")
@SuppressWarnings("unused")
public final class PatchesSettingsActivity extends Activity {

    private static final int FALLBACK_SCREEN_BACKGROUND = 0xFF1C1B24;
    private static final int FALLBACK_CARD_BACKGROUND = 0xFF292733;
    private static final int FALLBACK_TEXT_NORM = 0xFFEDEDEE;
    private static final int FALLBACK_TEXT_WEAK = 0xFFA9A9AF;
    private static final int FALLBACK_ICON_DISABLED = 0xFF5B5966;

    private static final int SCREEN_INSET_DP = 16;
    private static final int SCREEN_TOP_PADDING_DP = 0;
    private static final int SCREEN_BOTTOM_PADDING_DP = 24;

    private static final int APP_BAR_START_PADDING_DP = 4;
    private static final int APP_BAR_VERTICAL_PADDING_DP = 8;
    private static final int APP_BAR_TITLE_SP = 22;
    private static final int APP_BAR_TITLE_START_DP = 4;
    private static final int BACK_TOUCH_TARGET_DP = 48;
    private static final int BACK_ICON_INSET_DP = 12;

    private static final int CARD_RADIUS_DP = 13;
    private static final int CARD_GAP_DP = 14;
    private static final int CARD_PADDING_DP = 20;

    private static final int ROW_MIN_HEIGHT_DP = 56;
    private static final int ROW_TITLE_SP = 16;
    private static final int ROW_SUMMARY_SP = 14;
    private static final int CHEVRON_SIZE_DP = 24;
    private static final int CHEVRON_START_PADDING_DP = 16;

    private static final int FIELD_VERTICAL_PADDING_DP = 4;
    private static final int FIELD_LABEL_WIDTH_DP = 120;
    private static final int SECTION_LABEL_INSET_DP = 4;
    private static final int SECTION_LABEL_TOP_DP = 16;
    private static final int SECTION_LABEL_BOTTOM_DP = 8;
    private static final int FIELD_TEXT_SP = 14;

    private int screenBackgroundColor;
    private int cardBackgroundColor;
    private int textNormColor;
    private int textWeakColor;
    private int iconDisabledColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        screenBackgroundColor = AmoledTheme.isEnabled()
                ? Color.BLACK
                : protonColor("proton_background_norm", FALLBACK_SCREEN_BACKGROUND);
        cardBackgroundColor =
                protonColor("proton_background_secondary", FALLBACK_CARD_BACKGROUND);
        textNormColor = protonColor("proton_text_norm", FALLBACK_TEXT_NORM);
        textWeakColor = protonColor("proton_text_weak", FALLBACK_TEXT_WEAK);
        iconDisabledColor = protonColor("proton_icon_disabled", FALLBACK_ICON_DISABLED);

        setContentView(buildScreen());
    }

    @Override
    protected void onResume() {
        super.onResume();
        recreateContent();
    }

    private ScrollView content;

    private View buildScreen() {
        final LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setBackgroundColor(screenBackgroundColor);
        screen.setPadding(0, systemBarHeight("status_bar_height", 24), 0,
                systemBarHeight("navigation_bar_height", 0));
        screen.addView(header());

        content = new ScrollView(this);
        screen.addView(content, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        recreateContent();
        return screen;
    }

    private void recreateContent() {
        if (content == null) return;

        content.removeAllViews();
        final LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(dp(SCREEN_INSET_DP), dp(SCREEN_TOP_PADDING_DP),
                dp(SCREEN_INSET_DP), dp(SCREEN_BOTTOM_PADDING_DP));

        if (AppliedPatches.scheduledDeletion()) {
            column.addView(card(row(ScheduledDeletionEditor.SETTINGS_ROW_TITLE,
                    ScheduledDeletionEditor.summary(this),
                    () -> ScheduledDeletionEditor.show(this, this::recreateContent))));
        }

        if (AmoledTheme.isPatched()) {
            column.addView(card(toggle(AppliedPatches.AMOLED_DARK_THEME,
                    "Pure black backgrounds.",
                    AmoledTheme.isEnabled(),
                    new Toggle() {
                        @Override
                        public void set(boolean enabled) {
                            AmoledTheme.setEnabled(enabled);
                            confirmRestart();
                        }
                    })));
        }

        if (UpsellingVisibility.isPatched()) {
            column.addView(card(toggle(AppliedPatches.HIDE_UPGRADE_PROMOTIONS,
                    "Banners, sidebar offers and top-bar upgrade buttons.",
                    UpsellingVisibility.isHidden(),
                    new Toggle() {
                        @Override
                        public void set(boolean hidden) {
                            UpsellingVisibility.setHidden(hidden);
                        }
                    })));
        }

        column.addView(sectionLabel("App"));
        column.addView(card(appFields()));

        content.addView(column, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private View sectionLabel(String text) {
        final TextView label = new TextView(this);
        label.setText(text);
        label.setTextColor(textWeakColor);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, ROW_SUMMARY_SP);
        label.setPadding(dp(SECTION_LABEL_INSET_DP), dp(SECTION_LABEL_TOP_DP),
                dp(SECTION_LABEL_INSET_DP), dp(SECTION_LABEL_BOTTOM_DP));
        return label;
    }

    private void confirmRestart() {
        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Restart required")
                .setMessage("Proton Mail must restart to apply the theme.")
                .setNegativeButton("Later", null)
                .setPositiveButton("Restart", (dialog, which) -> restartApp())
                .show();
    }

    private void restartApp() {
        final Intent launch = getPackageManager().getLaunchIntentForPackage(getPackageName());
        if (launch == null) return;

        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(launch);
        finishAffinity();
        Runtime.getRuntime().exit(0);
    }

    private interface Toggle {
        void set(boolean enabled);
    }

    private View toggle(String title, String summary, boolean checked, final Toggle onChange) {
        final LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(CARD_PADDING_DP), dp(CARD_PADDING_DP),
                dp(CARD_PADDING_DP), dp(CARD_PADDING_DP));
        row.setMinimumHeight(dp(ROW_MIN_HEIGHT_DP));
        row.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        text.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        final TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextColor(textNormColor);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, ROW_TITLE_SP);
        text.addView(titleView);

        final TextView summaryView = new TextView(this);
        summaryView.setText(summary);
        summaryView.setTextColor(textWeakColor);
        summaryView.setTextSize(TypedValue.COMPLEX_UNIT_SP, ROW_SUMMARY_SP);
        text.addView(summaryView);
        row.addView(text);

        final Switch control = new Switch(this);
        control.setContentDescription(title + ". " + summary);
        control.setChecked(checked);
        control.setMinimumHeight(dp(ROW_MIN_HEIGHT_DP));
        final LinearLayout.LayoutParams controlParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        controlParams.setMarginStart(dp(CHEVRON_START_PADDING_DP));
        control.setLayoutParams(controlParams);
        control.setOnCheckedChangeListener((button, isChecked) -> onChange.set(isChecked));
        row.addView(control);

        row.setOnClickListener(ignored -> control.performClick());
        applyPressedFeedback(row);
        return row;
    }

    private View header() {
        final LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(APP_BAR_START_PADDING_DP), dp(APP_BAR_VERTICAL_PADDING_DP),
                dp(SCREEN_INSET_DP), dp(APP_BAR_VERTICAL_PADDING_DP));

        final ImageView back = new ImageView(this);
        back.setImageDrawable(protonDrawable("ic_proton_arrow_left"));
        back.setColorFilter(textNormColor);
        back.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        back.setPadding(dp(BACK_ICON_INSET_DP), dp(BACK_ICON_INSET_DP),
                dp(BACK_ICON_INSET_DP), dp(BACK_ICON_INSET_DP));
        back.setLayoutParams(new LinearLayout.LayoutParams(dp(BACK_TOUCH_TARGET_DP), dp(BACK_TOUCH_TARGET_DP)));
        back.setClickable(true);
        back.setFocusable(true);
        back.setContentDescription("Back");
        applyPressedFeedback(back);
        back.setOnClickListener(ignored -> finish());
        header.addView(back);

        final TextView title = new TextView(this);
        title.setText(PatchesMenu.SETTINGS_ROW_TITLE);
        title.setTextColor(textNormColor);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, APP_BAR_TITLE_SP);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        title.setPadding(dp(APP_BAR_TITLE_START_DP), 0, 0, 0);
        header.addView(title);
        return header;
    }

    private int protonColor(String name, int fallback) {
        try {
            final int attribute = getResources().getIdentifier(name, "attr", getPackageName());
            if (attribute == 0) return fallback;

            final TypedValue value = new TypedValue();
            if (!getTheme().resolveAttribute(attribute, value, true)) return fallback;
            if (value.type >= TypedValue.TYPE_FIRST_COLOR_INT
                    && value.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                return value.data;
            }
            return value.resourceId == 0 ? fallback : getColor(value.resourceId);
        } catch (Throwable t) {
            return fallback;
        }
    }

    private Drawable protonDrawable(String name) {
        final int identifier = getResources().getIdentifier(name, "drawable", getPackageName());
        return identifier == 0 ? null : getResources().getDrawable(identifier, getTheme());
    }

    private View card(View child) {
        final LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);

        final GradientDrawable background = new GradientDrawable();
        background.setColor(cardBackgroundColor);
        background.setCornerRadius(dp(CARD_RADIUS_DP));
        card.setBackground(background);
        card.addView(child);

        final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(CARD_GAP_DP);
        card.setLayoutParams(params);
        return card;
    }

    private View row(String title, String summary, final Runnable onClick) {
        final LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(CARD_PADDING_DP), dp(CARD_PADDING_DP),
                dp(CARD_PADDING_DP), dp(CARD_PADDING_DP));
        row.setMinimumHeight(dp(ROW_MIN_HEIGHT_DP));
        row.setClickable(true);
        row.setFocusable(true);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        text.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        final TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextColor(textNormColor);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, ROW_TITLE_SP);
        text.addView(titleView);

        if (summary != null) {
            final TextView summaryView = new TextView(this);
            summaryView.setText(summary);
            summaryView.setTextColor(textWeakColor);
            summaryView.setTextSize(TypedValue.COMPLEX_UNIT_SP, ROW_SUMMARY_SP);
            text.addView(summaryView);
        }
        row.addView(text);

        final ImageView chevron = new ImageView(this);
        chevron.setImageDrawable(protonDrawable("ic_proton_chevron_right"));
        chevron.setColorFilter(iconDisabledColor);
        chevron.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        final LinearLayout.LayoutParams chevronParams = new LinearLayout.LayoutParams(
                dp(CHEVRON_SIZE_DP), dp(CHEVRON_SIZE_DP));
        chevronParams.setMarginStart(dp(CHEVRON_START_PADDING_DP));
        chevron.setLayoutParams(chevronParams);
        row.addView(chevron);

        row.setOnClickListener(ignored -> onClick.run());
        applyPressedFeedback(row);
        return row;
    }

    private int systemBarHeight(String name, int fallbackDp) {
        final int identifier = getResources().getIdentifier(name, "dimen", "android");
        return identifier > 0 ? getResources().getDimensionPixelSize(identifier) : dp(fallbackDp);
    }

    private void applyPressedFeedback(View view) {
        final TypedValue value = new TypedValue();
        if (!getTheme().resolveAttribute(
                android.R.attr.selectableItemBackground, value, true)) {
            return;
        }
        view.setForeground(getDrawable(value.resourceId));
    }

    private String appVersion() {
        try {
            final PackageInfo info =
                    getPackageManager().getPackageInfo(getPackageName(), 0);
            return info.versionName + " (" + info.versionCode + ")";
        } catch (Throwable t) {
            return "unknown";
        }
    }

    private LinearLayout fieldColumn() {
        final LinearLayout fields = new LinearLayout(this);
        fields.setOrientation(LinearLayout.VERTICAL);
        fields.setPadding(dp(CARD_PADDING_DP), dp(CARD_PADDING_DP),
                dp(CARD_PADDING_DP), dp(CARD_PADDING_DP));
        fields.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return fields;
    }

    private View appFields() {
        final LinearLayout fields = fieldColumn();
        fields.addView(field("App version", appVersion()));
        fields.addView(field("Patch bundle", PatchesMenu.bundleVersion()));

        final List<String> applied = AppliedPatches.names();
        for (int index = 0; index < applied.size(); index++) {
            fields.addView(field(index == 0 ? "Applied patches" : "", applied.get(index)));
        }
        return fields;
    }

    private View field(String label, String value) {
        final LinearLayout field = new LinearLayout(this);
        field.setOrientation(LinearLayout.HORIZONTAL);
        field.setPadding(0, dp(FIELD_VERTICAL_PADDING_DP), 0, dp(FIELD_VERTICAL_PADDING_DP));
        field.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final TextView labelView = new TextView(this);
        labelView.setText(label);
        labelView.setTextColor(textWeakColor);
        labelView.setTextSize(TypedValue.COMPLEX_UNIT_SP, FIELD_TEXT_SP);
        labelView.setLayoutParams(new LinearLayout.LayoutParams(dp(FIELD_LABEL_WIDTH_DP),
                ViewGroup.LayoutParams.WRAP_CONTENT));
        field.addView(labelView);

        final TextView valueView = new TextView(this);
        valueView.setText(value == null || value.isEmpty() ? "unknown" : value);
        valueView.setTextColor(textNormColor);
        valueView.setTextSize(TypedValue.COMPLEX_UNIT_SP, FIELD_TEXT_SP);
        valueView.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        field.addView(valueView);
        return field;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
