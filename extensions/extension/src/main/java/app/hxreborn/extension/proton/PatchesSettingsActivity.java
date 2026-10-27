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
package app.hxreborn.extension.proton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
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

import app.hxreborn.extension.protonauthenticator.SettingsSections;
import app.hxreborn.extension.protonmail.ScheduledDeletionEditor;
import app.hxreborn.extension.protonmail.SidebarEntries;
import app.hxreborn.extension.protonmail.accountswipe.AccountAvatarSwipe;
import app.hxreborn.extension.protonmail.accountswipe.AvatarSwitchStyleDialog;
import app.hxreborn.extension.protonpass.LayoutComponents;
import app.hxreborn.extension.protonpass.NavigationBarAutoHide;

@SuppressLint("SetTextI18n")
@SuppressWarnings("unused")
public final class PatchesSettingsActivity extends Activity {

    private static final int SCREEN_INSET_DP = 16;

    private static final int SCREEN_TOP_PADDING_DP = 0;

    private static final int SCREEN_BOTTOM_PADDING_DP = 24;

    private static final int APP_BAR_START_PADDING_DP = 4;

    private static final int APP_BAR_VERTICAL_PADDING_DP = 8;

    private static final int APP_BAR_TITLE_SP = 22;

    private static final int APP_BAR_TITLE_START_DP = 4;

    private static final int BACK_TOUCH_TARGET_DP = 48;

    private static final int BACK_ICON_INSET_DP = 12;

    private static final int CARD_RADIUS_DP = 16;

    private static final int CARD_PADDING_DP = 16;

    private static final int DIVIDER_DP = 1;

    private static final int ROW_MIN_HEIGHT_DP = 56;

    private static final int ROW_TITLE_SP = 16;

    private static final int ROW_SUMMARY_SP = 14;

    private static final int CHEVRON_SIZE_DP = 24;

    private static final int ROW_CONTROL_START_MARGIN_DP = 16;

    private static final int SWATCH_SIZE_DP = 26;

    private static final int FIELD_VERTICAL_PADDING_DP = 4;

    private static final int FIELD_LABEL_WIDTH_DP = 120;

    private static final int SECTION_LABEL_INSET_DP = 4;

    private static final int SECTION_LABEL_TOP_DP = 20;

    private static final int SECTION_LABEL_BOTTOM_DP = 12;

    private static final int SECTION_LABEL_SP = 16;

    private static final int FIELD_TEXT_SP = 14;

    private int screenBackgroundColor;

    private int cardBackgroundColor;

    private int textNormColor;

    private int textWeakColor;

    private int iconDisabledColor;

    private ScrollView content;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(PatchesTheme.withNightMode(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final int backgroundNormColor = PatchesTheme.resolveColorAttribute(this, PatchesTheme.BACKGROUND_NORM);
        this.screenBackgroundColor = (AmoledTheme.isEnabled() && PatchesTheme.isDark(backgroundNormColor)) ? Color.BLACK
                : backgroundNormColor;
        this.cardBackgroundColor = PatchesTheme.resolveColorAttribute(this, PatchesTheme.BACKGROUND_SECONDARY);
        this.textNormColor = PatchesTheme.resolveColorAttribute(this, PatchesTheme.TEXT_NORM);
        this.textWeakColor = PatchesTheme.resolveColorAttribute(this, PatchesTheme.TEXT_WEAK);
        this.iconDisabledColor = PatchesTheme.resolveColorAttribute(this, PatchesTheme.ICON_DISABLED);

        setContentView(buildScreen());
    }

    @Override
    protected void onResume() {
        super.onResume();
        recreateContent();
    }

    private View buildScreen() {
        final LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setBackgroundColor(this.screenBackgroundColor);
        screen.setPadding(0, systemBarHeight("status_bar_height", 24), 0, systemBarHeight("navigation_bar_height", 0));
        screen.addView(header());

        this.content = new ScrollView(this);
        screen.addView(this.content, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        recreateContent();
        return screen;
    }

    private void recreateContent() {
        if (this.content == null) {
            return;
        }

        this.content.removeAllViews();
        final LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(dp(SCREEN_INSET_DP), dp(SCREEN_TOP_PADDING_DP), dp(SCREEN_INSET_DP),
                dp(SCREEN_BOTTOM_PADDING_DP));

        final List<View> accountRows = new ArrayList<>();
        if (AppliedPatches.swipeToSwitchAccounts()) {
            accountRows.add(row(AvatarSwitchStyleDialog.TITLE, AccountAvatarSwipe.getSwitchStyle().getLabel(),
                    () -> AvatarSwitchStyleDialog.show(this, this::recreateContent)));
            accountRows.add(switchRow("Vibrate on switch", "Tick when the account changes.",
                    AccountAvatarSwipe.isHapticFeedbackEnabled(), new CheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(boolean enabled) {
                            AccountAvatarSwipe.setHapticFeedbackEnabled(enabled);
                        }
                    }));
        }
        addSection(column, AppliedPatches.SWIPE_TO_SWITCH_ACCOUNTS, accountRows);

        final List<View> generalRows = new ArrayList<>();
        if (AppliedPatches.scheduledDeletion()) {
            generalRows.add(row(ScheduledDeletionEditor.SETTINGS_ROW_TITLE, ScheduledDeletionEditor.summary(this),
                    () -> ScheduledDeletionEditor.show(this, this::recreateContent)));
        }
        if (UpsellingVisibility.isPatched()) {
            generalRows.add(switchRow(AppliedPatches.HIDE_UPGRADE_PROMOTIONS, "Upgrade buttons, banners and offers.",
                    UpsellingVisibility.isHidden(), new CheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(boolean hidden) {
                            UpsellingVisibility.setHidden(hidden);
                            confirmRestart();
                        }
                    }));
        }
        addSection(column, "General", generalRows);

        final List<View> layoutRows = new ArrayList<>();
        if (LayoutComponents.isPatched()) {
            layoutRows.add(layoutSwitchRow("Hide Help center", "Feedback, import/export and tutorial links.",
                    LayoutComponents.HELP_CENTER));
            if (LayoutComponents.hasSpreadTheWord()) {
                layoutRows.add(layoutSwitchRow("Hide Spread the word", "Tell a friend and Rate app.",
                        LayoutComponents.SPREAD_THE_WORD));
            }
            layoutRows
                .add(layoutSwitchRow("Hide About", "Privacy policy and Terms of service.", LayoutComponents.ABOUT));
        }
        if (NavigationBarAutoHide.isPatched()) {
            layoutRows.add(switchRow(AppliedPatches.HIDE_NAVIGATION_BAR_ON_SCROLL,
                    "Hides on scroll down and shows on scroll up.", NavigationBarAutoHide.isEnabled(),
                    new CheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(boolean enabled) {
                            NavigationBarAutoHide.setEnabled(enabled);
                        }
                    }));
        }
        if (SidebarEntries.isPatched()) {
            layoutRows.add(layoutSwitchRow("Hide Subscription", "Plan and billing entry in the sidebar.",
                    SidebarEntries.SUBSCRIPTION));
            layoutRows.add(layoutSwitchRow("Hide Report a problem", "Bug report entry in the sidebar.",
                    SidebarEntries.REPORT_A_PROBLEM));
        }
        if (SettingsSections.isPatched()) {
            layoutRows.add(layoutSwitchRow("Hide Spread the word", "Tell a friend and Rate app.",
                    SettingsSections.SPREAD_THE_WORD));
            layoutRows.add(layoutSwitchRow("Hide Discover Proton", "Proton Mail, Pass, VPN, Drive and Calendar cards.",
                    SettingsSections.DISCOVER_PROTON));
            layoutRows.add(layoutSwitchRow("Hide Proton Pass banner", "Promotion card at the top of Settings.",
                    SettingsSections.PASS_BANNER));
        }
        addSection(column, "Layout", layoutRows);

        final List<View> appearanceRows = new ArrayList<>();
        if (AccentColor.isPatched()) {
            appearanceRows
                .add(row(AppliedPatches.ACCENT_COLOR, AccentColorDialog.getPresetLabel(AccentColor.getPreset()),
                        AccentColor.getAccentColor(isDarkTheme()), () -> AccentColorDialog.show(this, () -> {
                            recreateContent();
                            confirmRestart();
                        })));
        }
        if (AmoledTheme.isPatched()) {
            appearanceRows.add(switchRow(AppliedPatches.AMOLED_DARK_THEME, "Pure black backgrounds.",
                    AmoledTheme.isEnabled(), new CheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(boolean enabled) {
                            AmoledTheme.setEnabled(enabled);
                            recreateContent();
                            confirmRestart();
                        }
                    }));
            if (AmoledTheme.isEnabled()) {
                appearanceRows.add(switchRow("Dim raised surfaces", "Darker bars, cards, buttons and sheets.",
                        AmoledTheme.isDimRaisedSurfacesEnabled(), new CheckedChangeListener() {
                            @Override
                            public void onCheckedChanged(boolean enabled) {
                                AmoledTheme.setDimRaisedSurfacesEnabled(enabled);
                                confirmRestart();
                            }
                        }));
            }
        }
        if (MaterialSwitches.isPatched()) {
            appearanceRows.add(switchRow(AppliedPatches.MATERIAL_SWITCHES, "Check and close icons in switches.",
                    MaterialSwitches.isEnabled(), new CheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(boolean enabled) {
                            MaterialSwitches.setEnabled(enabled);
                            confirmRestart();
                        }
                    }));
        }
        if (LoadingIndicator.isPatched()) {
            appearanceRows.add(switchRow(AppliedPatches.LOADING_INDICATOR, "Shape-morphing pull to refresh indicator.",
                    LoadingIndicator.isEnabled(), new CheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(boolean enabled) {
                            LoadingIndicator.setEnabled(enabled);
                        }
                    }));
        }
        addSection(column, "Appearance", appearanceRows);

        addSection(column, "App", Collections.singletonList(appFields()));

        this.content.addView(column,
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private View sectionLabel(String text) {
        final TextView label = new TextView(this);
        label.setText(text);
        label.setTextColor(this.textWeakColor);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, SECTION_LABEL_SP);
        label.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            label.setAccessibilityHeading(true);
        }
        label.setPadding(dp(SECTION_LABEL_INSET_DP), dp(SECTION_LABEL_TOP_DP), dp(SECTION_LABEL_INSET_DP),
                dp(SECTION_LABEL_BOTTOM_DP));
        return label;
    }

    private void confirmRestart() {
        final CharSequence appName = getApplicationInfo().loadLabel(getPackageManager());
        PatchesDialog.showConfirmation(this, "Restart required", appName + " must restart to apply the change.",
                "Restart", this::restartApp);
    }

    private void restartApp() {
        final Intent launch = getPackageManager().getLaunchIntentForPackage(getPackageName());
        if (launch == null) {
            return;
        }

        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(launch);
        finishAffinity();
        Runtime.getRuntime().exit(0);
    }

    private View layoutSwitchRow(String title, String summary, final String key) {
        return switchRow(title, summary, PatchSettings.isEnabled(key, false), new CheckedChangeListener() {
            @Override
            public void onCheckedChanged(boolean hidden) {
                PatchSettings.setEnabled(key, hidden);
                confirmRestart();
            }
        });
    }

    private View switchRow(String title, String summary, boolean checked, final CheckedChangeListener onChange) {
        final LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(CARD_PADDING_DP), dp(CARD_PADDING_DP), dp(CARD_PADDING_DP), dp(CARD_PADDING_DP));
        row.setMinimumHeight(dp(ROW_MIN_HEIGHT_DP));
        row.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        final LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        text.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        final TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextColor(this.textNormColor);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, ROW_TITLE_SP);
        text.addView(titleView);

        final TextView summaryView = new TextView(this);
        summaryView.setText(summary);
        summaryView.setTextColor(this.textWeakColor);
        summaryView.setTextSize(TypedValue.COMPLEX_UNIT_SP, ROW_SUMMARY_SP);
        text.addView(summaryView);
        row.addView(text);

        final Switch control = new Switch(this);
        control.setContentDescription(title + ". " + summary);
        control.setChecked(checked);
        final LinearLayout.LayoutParams controlParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        controlParams.setMarginStart(dp(ROW_CONTROL_START_MARGIN_DP));
        control.setLayoutParams(controlParams);
        styleSwitch(control);
        control.setOnCheckedChangeListener((button, isChecked) -> onChange.onCheckedChanged(isChecked));
        row.addView(control);

        row.setOnClickListener((ignored) -> control.performClick());
        PatchesTheme.makeClickable(row);
        return row;
    }

    private boolean isDarkTheme() {
        return PatchesTheme.isDark(this.cardBackgroundColor);
    }

    private void styleSwitch(Switch control) {
        SwitchStyle.apply(control, AccentColor.getAccentColor(isDarkTheme()));
    }

    private View header() {
        final LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(APP_BAR_START_PADDING_DP), dp(APP_BAR_VERTICAL_PADDING_DP), dp(SCREEN_INSET_DP),
                dp(APP_BAR_VERTICAL_PADDING_DP));

        final ImageView back = new ImageView(this);
        final Drawable arrow = PatchesTheme.getDrawableByName(this, "ic_proton_arrow_left");
        back.setImageDrawable((arrow != null) ? arrow : PatchesTheme.getDrawableByName(this, "ic_arrow_back"));
        back.setColorFilter(this.textNormColor);
        back.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        back.setPadding(dp(BACK_ICON_INSET_DP), dp(BACK_ICON_INSET_DP), dp(BACK_ICON_INSET_DP), dp(BACK_ICON_INSET_DP));
        back.setLayoutParams(new LinearLayout.LayoutParams(dp(BACK_TOUCH_TARGET_DP), dp(BACK_TOUCH_TARGET_DP)));
        back.setClickable(true);
        back.setFocusable(true);
        back.setContentDescription("Back");
        PatchesTheme.makeClickable(back);
        back.setOnClickListener((ignored) -> finish());
        header.addView(back);

        final TextView title = new TextView(this);
        title.setText(PatchesMenu.SETTINGS_ROW_TITLE);
        title.setTextColor(this.textNormColor);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, APP_BAR_TITLE_SP);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        title.setPadding(dp(APP_BAR_TITLE_START_DP), 0, 0, 0);
        header.addView(title);
        return header;
    }

    private void addSection(LinearLayout column, String title, List<View> rows) {
        if (rows.isEmpty()) {
            return;
        }

        column.addView(sectionLabel(title));
        final LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(PatchesTheme.createRoundedRectangle(this, this.cardBackgroundColor, CARD_RADIUS_DP));
        card.setClipToOutline(true);
        for (int index = 0; index < rows.size(); index++) {
            if (index != 0) {
                final View divider = new View(this);
                divider.setBackgroundColor(this.screenBackgroundColor);
                card.addView(divider,
                        new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(DIVIDER_DP)));
            }
            card.addView(rows.get(index));
        }
        column.addView(card);
    }

    private View row(String title, String summary, final Runnable onClick) {
        return row(title, summary, null, onClick);
    }

    private View row(String title, String summary, Integer swatchColor, final Runnable onClick) {
        final LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(CARD_PADDING_DP), dp(CARD_PADDING_DP), dp(CARD_PADDING_DP), dp(CARD_PADDING_DP));
        row.setMinimumHeight(dp(ROW_MIN_HEIGHT_DP));
        row.setClickable(true);
        row.setFocusable(true);
        row.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        final LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        text.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        final TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextColor(this.textNormColor);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, ROW_TITLE_SP);
        text.addView(titleView);

        if (summary != null) {
            final TextView summaryView = new TextView(this);
            summaryView.setText(summary);
            summaryView.setTextColor(this.textWeakColor);
            summaryView.setTextSize(TypedValue.COMPLEX_UNIT_SP, ROW_SUMMARY_SP);
            text.addView(summaryView);
        }
        row.addView(text);

        if (swatchColor != null) {
            final LinearLayout.LayoutParams swatchParams = new LinearLayout.LayoutParams(dp(SWATCH_SIZE_DP),
                    dp(SWATCH_SIZE_DP));
            swatchParams.setMarginStart(dp(ROW_CONTROL_START_MARGIN_DP));
            final View swatchView = new View(this);
            swatchView.setBackground(PatchesTheme.createCircle(swatchColor));
            row.addView(swatchView, swatchParams);
        }

        final ImageView chevron = new ImageView(this);
        chevron.setImageDrawable(PatchesTheme.getDrawableByName(this, "ic_proton_chevron_right"));
        chevron.setColorFilter(this.iconDisabledColor);
        chevron.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        final LinearLayout.LayoutParams chevronParams = new LinearLayout.LayoutParams(dp(CHEVRON_SIZE_DP),
                dp(CHEVRON_SIZE_DP));
        chevronParams.setMarginStart(dp(ROW_CONTROL_START_MARGIN_DP));
        chevron.setLayoutParams(chevronParams);
        row.addView(chevron);

        row.setOnClickListener((ignored) -> onClick.run());
        PatchesTheme.makeClickable(row);
        return row;
    }

    private int systemBarHeight(String name, int fallbackDp) {
        final int identifier = getResources().getIdentifier(name, "dimen", "android");
        return (identifier > 0) ? getResources().getDimensionPixelSize(identifier) : dp(fallbackDp);
    }

    private String appVersion() {
        try {
            final PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            return packageInfo.versionName + " (" + packageInfo.versionCode + ")";
        } catch (Throwable ex) {
            return "unknown";
        }
    }

    private LinearLayout fieldColumn() {
        final LinearLayout fields = new LinearLayout(this);
        fields.setOrientation(LinearLayout.VERTICAL);
        fields.setPadding(dp(CARD_PADDING_DP), dp(CARD_PADDING_DP), dp(CARD_PADDING_DP), dp(CARD_PADDING_DP));
        fields.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return fields;
    }

    private View appFields() {
        final LinearLayout fields = fieldColumn();
        fields.addView(field("App version", appVersion()));
        fields.addView(field("Patch bundle", PatchesMenu.bundleVersion()));

        final List<String> applied = AppliedPatches.names();
        for (int index = 0; index < applied.size(); index++) {
            fields.addView(field((index != 0) ? "" : "Applied patches", applied.get(index)));
        }
        return fields;
    }

    private View field(String label, String value) {
        final LinearLayout field = new LinearLayout(this);
        field.setOrientation(LinearLayout.HORIZONTAL);
        field.setPadding(0, dp(FIELD_VERTICAL_PADDING_DP), 0, dp(FIELD_VERTICAL_PADDING_DP));
        field.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        final TextView labelView = new TextView(this);
        labelView.setText(label);
        labelView.setTextColor(this.textWeakColor);
        labelView.setTextSize(TypedValue.COMPLEX_UNIT_SP, FIELD_TEXT_SP);
        labelView.setLayoutParams(
                new LinearLayout.LayoutParams(dp(FIELD_LABEL_WIDTH_DP), ViewGroup.LayoutParams.WRAP_CONTENT));
        field.addView(labelView);

        final TextView valueView = new TextView(this);
        valueView.setText((value == null || value.isEmpty()) ? "unknown" : value);
        valueView.setTextColor(this.textNormColor);
        valueView.setTextSize(TypedValue.COMPLEX_UNIT_SP, FIELD_TEXT_SP);
        valueView.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        field.addView(valueView);
        return field;
    }

    private int dp(int value) {
        return PatchesTheme.dpToPx(this, value);
    }

}
