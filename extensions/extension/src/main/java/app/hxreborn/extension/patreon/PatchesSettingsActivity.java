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
package app.hxreborn.extension.patreon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import app.hxreborn.extension.BuildConfig;

@SuppressLint("SetTextI18n")
@SuppressWarnings("unused")
public final class PatchesSettingsActivity extends Activity {

    private static final int APP_BAR_HEIGHT_DP = 64;

    private static final int APP_BAR_START_PADDING_DP = 4;

    private static final int BACK_TOUCH_TARGET_DP = 48;

    private static final int BACK_ICON_INSET_DP = 12;

    private static final int TITLE_START_PADDING_DP = 12;

    private static final int CONTENT_BOTTOM_PADDING_DP = 24;

    private static final String RESET_DESCRIPTION = "Restores every switch to its default.";

    private SettingsStyle style;

    private SettingsRows rows;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.style = new SettingsStyle(this);
        this.rows = new SettingsRows(this, this.style);
        styleSystemBars(getWindow());
        setContentView(screen());
    }

    @SuppressWarnings("deprecation")
    private void styleSystemBars(Window window) {
        window.setStatusBarColor(this.style.screenBackground);
        window.setNavigationBarColor(this.style.screenBackground);
        window.getDecorView()
            .setSystemUiVisibility(this.style.dark ? 0
                    : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    }

    private View screen() {
        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setBackgroundColor(this.style.screenBackground);
        screen.setOnApplyWindowInsetsListener(this::applySystemBarInsets);
        screen.addView(appBar());

        ScrollView scroll = new ScrollView(this);
        scroll.addView(content(),
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        screen.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        return screen;
    }

    @SuppressWarnings("deprecation")
    private WindowInsets applySystemBarInsets(View view, WindowInsets insets) {
        view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
        return insets.consumeSystemWindowInsets();
    }

    private View appBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(this.style.dp(APP_BAR_START_PADDING_DP), 0, this.style.dp(SettingsStyle.SCREEN_INSET_DP), 0);
        bar.setMinimumHeight(this.style.dp(APP_BAR_HEIGHT_DP));

        ImageView back = new ImageView(this);
        back.setImageDrawable(this.style.backIcon(this.style.primaryText));
        back.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        int inset = this.style.dp(BACK_ICON_INSET_DP);
        back.setPadding(inset, inset, inset, inset);
        back.setContentDescription("Back");
        back.setBackground(this.style.ripple(BACK_TOUCH_TARGET_DP / 2));
        back.setOnClickListener((view) -> finish());
        bar.addView(back, new LinearLayout.LayoutParams(this.style.dp(BACK_TOUCH_TARGET_DP),
                this.style.dp(BACK_TOUCH_TARGET_DP)));

        TextView title = this.style.text(PatchesMenu.SETTINGS_ROW_TITLE, SettingsStyle.BAR_TITLE_SP,
                this.style.primaryText, this.style.medium);
        title.setPadding(this.style.dp(TITLE_START_PADDING_DP), 0, 0, 0);
        SettingsStyle.markHeading(title);
        bar.addView(title);
        return bar;
    }

    private View content() {
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(this.style.dp(SettingsStyle.SCREEN_INSET_DP), 0, this.style.dp(SettingsStyle.SCREEN_INSET_DP),
                this.style.dp(CONTENT_BOTTOM_PADDING_DP));

        LinearLayout feed = this.rows.card();
        if (HomeFeedFilter.isPatched()) {
            this.rows.switchRow(feed, "hx_patreon_ic_recommended", HomeFeedFilter.patchName(),
                    HomeFeedFilter.patchDescription(), HomeFeedFilter.isEnabled(), (button, enabled) -> {
                        PatchSettings.setEnabled(HomeFeedFilter.HIDE_RECOMMENDED_CREATORS, enabled);
                        confirmRestart();
                    });
        }
        if (HomeTab.isPatched()) {
            this.rows.switchRow(feed, "hx_patreon_ic_home_tab", HomeTab.patchName(),
                    "Removes the Home tab and starts on Memberships.", HomeTab.isHidden(), (button, hidden) -> {
                        PatchSettings.setEnabled(HomeTab.HIDE_HOME_TAB, hidden);
                        confirmRestart();
                    });
        }
        if (QuipsAndAutoplay.isPatched()) {
            this.rows.switchRow(feed, "hx_patreon_ic_quips", QuipsAndAutoplay.patchName(),
                    QuipsAndAutoplay.patchDescription(), QuipsAndAutoplay.isDisabled(), (button, disabled) -> {
                        PatchSettings.setEnabled(QuipsAndAutoplay.DISABLE_QUIPS_AND_AUTOPLAY, disabled);
                        confirmRestart();
                    });
        }
        addSection(column, "Feed", feed);

        LinearLayout search = this.rows.card();
        if (SearchSuggestions.isPatched()) {
            this.rows.switchRow(search, "hx_patreon_ic_search", SearchSuggestions.patchName(),
                    SearchSuggestions.patchDescription(), SearchSuggestions.isEnabled(), (button, enabled) -> {
                        PatchSettings.setEnabled(SearchSuggestions.HIDE_SEARCH_SUGGESTIONS, enabled);
                        confirmRestart();
                    });
        }
        addSection(column, "Search", search);

        LinearLayout navigation = this.rows.card();
        if (NavigationButtons.isPatched()) {
            for (int index = 0; index < NavigationButtons.TABS.length; index++) {
                String tab = NavigationButtons.TABS[index];
                String label = NavigationButtons.TAB_LABELS[index];
                this.rows.switchRow(navigation, NavigationButtons.TAB_ICONS[index], "Hide " + label,
                        "Removes " + label + " from the bottom bar.", NavigationButtons.isHiddenByUser(tab),
                        (button, hidden) -> {
                            NavigationButtons.setHiddenByUser(tab, hidden);
                            confirmRestart();
                        });
            }
        }
        addSection(column, "Navigation bar", navigation);

        LinearLayout app = this.rows.card();
        if (UpgradePromotions.isPatched()) {
            this.rows.switchRow(app, "hx_patreon_ic_promotions", UpgradePromotions.patchName(),
                    UpgradePromotions.patchDescription(), UpgradePromotions.isHidden(), (button, hidden) -> {
                        PatchSettings.setEnabled(UpgradePromotions.HIDE_UPGRADE_PROMOTIONS, hidden);
                        confirmRestart();
                    });
        }
        if (AppPrompts.isPatched()) {
            this.rows.switchRow(app, "hx_patreon_ic_prompts", AppPrompts.patchName(), AppPrompts.patchDescription(),
                    AppPrompts.isRemoved(), (button, removed) -> {
                        PatchSettings.setEnabled(AppPrompts.REMOVE_APP_PROMPTS, removed);
                        confirmRestart();
                    });
        }
        if (Tracking.isPatched()) {
            this.rows.switchRow(app, "hx_patreon_ic_tracking", Tracking.patchName(), Tracking.patchDescription(),
                    Tracking.isDisabled(), (button, disabled) -> {
                        PatchSettings.setEnabled(Tracking.DISABLE_TRACKING, disabled);
                        confirmRestart();
                    });
        }
        addSection(column, "App", app);

        LinearLayout about = this.rows.card();
        this.rows.actionRow(about, "hx_patreon_ic_reset", "Reset to defaults", RESET_DESCRIPTION, this::confirmReset);
        this.rows.valueRow(about, "hx_patreon_ic_info", "App version", appVersion());
        this.rows.valueRow(about, "hx_patreon_ic_patches", "Patch bundle", BuildConfig.BUNDLE_VERSION);
        addSection(column, "About", about);

        LinearLayout applied = this.rows.card();
        List<String> names = new ArrayList<>();
        if (QuipsAndAutoplay.isPatched()) {
            names.add(QuipsAndAutoplay.patchName());
        }
        if (Tracking.isPatched()) {
            names.add(Tracking.patchName());
        }
        if (HomeTab.isPatched()) {
            names.add(HomeTab.patchName());
        }
        if (NavigationButtons.isPatched()) {
            names.add(NavigationButtons.patchName());
        }
        if (HomeFeedFilter.isPatched()) {
            names.add(HomeFeedFilter.patchName());
        }
        if (SearchSuggestions.isPatched()) {
            names.add(SearchSuggestions.patchName());
        }
        if (UpgradePromotions.isPatched()) {
            names.add(UpgradePromotions.patchName());
        }
        if (AppPrompts.isPatched()) {
            names.add(AppPrompts.patchName());
        }
        Collections.sort(names);
        for (String name : names) {
            this.rows.textRow(applied, name);
        }
        addSection(column, "Applied patches", applied);
        return column;
    }

    private void addSection(LinearLayout column, String title, LinearLayout card) {
        if (card.getChildCount() == 0) {
            return;
        }
        column.addView(this.rows.sectionHeader(title));
        column.addView(card);
    }

    private String appVersion() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException ex) {
            Log.w(PatchSettings.LOG_TAG, "Package info lookup failed for " + getPackageName(), ex);
            return "unknown";
        }
    }

    private void confirmReset() {
        new AlertDialog.Builder(this, dialogTheme()).setTitle("Reset to defaults?")
            .setMessage(RESET_DESCRIPTION)
            .setPositiveButton("Reset", (dialog, which) -> {
                PatchSettings.resetToDefaults();
                setContentView(screen());
                confirmRestart();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private int dialogTheme() {
        return this.style.dark ? android.R.style.Theme_DeviceDefault_Dialog_Alert
                : android.R.style.Theme_DeviceDefault_Light_Dialog_Alert;
    }

    private void confirmRestart() {
        CharSequence appName = getApplicationInfo().loadLabel(getPackageManager());
        new AlertDialog.Builder(this, dialogTheme()).setTitle("Restart required")
            .setMessage(appName + " must restart to apply the change.")
            .setPositiveButton("Restart", (dialog, which) -> restartApp())
            .setNegativeButton("Later", null)
            .show();
    }

    private void restartApp() {
        Intent launch = getPackageManager().getLaunchIntentForPackage(getPackageName());
        if (launch == null) {
            return;
        }
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(launch);
        finishAffinity();
        Runtime.getRuntime().exit(0);
    }

}
