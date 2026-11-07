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
package app.hxreborn.extension.terabox;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import app.hxreborn.extension.BuildConfig;
import app.hxreborn.extension.terabox.AppliedPatches.AppliedPatch;

final class PatchesSettingsDialog {

    private static final String RESET_DESCRIPTION = "Restores every switch to its default.";

    private static final int APP_BAR_HEIGHT_DP = 64;

    private static final int APP_BAR_START_PADDING_DP = 4;

    private static final int BACK_TOUCH_TARGET_DP = 48;

    private static final int BACK_ICON_INSET_DP = 12;

    private static final int TITLE_START_PADDING_DP = 12;

    private static final int CONTENT_BOTTOM_PADDING_DP = 24;

    private static final int CONFIRMATION_MAX_WIDTH_DP = 360;

    private static final int CONFIRMATION_SIDE_MARGIN_DP = 24;

    private final Activity activity;

    private final SettingsStyle style;

    private final SettingsRows rows;

    private final Dialog dialog;

    private final ScrollView scroll;

    PatchesSettingsDialog(Activity activity) {
        this.activity = activity;
        this.style = new SettingsStyle(activity);
        this.rows = new SettingsRows(activity, this.style);
        this.dialog = new Dialog(activity);
        this.dialog.getContext().getTheme().setTo(activity.getTheme());
        this.dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.scroll = new ScrollView(activity);
    }

    void show() {
        LinearLayout screen = new LinearLayout(this.activity);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setBackgroundColor(this.style.screenBackground);
        screen.setOnApplyWindowInsetsListener(PatchesSettingsDialog::applySystemBarInsets);
        screen.addView(appBar());
        replaceContent();
        screen.addView(this.scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        this.dialog.setContentView(screen);
        Window window = this.dialog.getWindow();
        if (window != null) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            styleWindow(window);
        }
        this.dialog.show();
    }

    private View appBar() {
        LinearLayout bar = new LinearLayout(this.activity);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(this.style.dp(APP_BAR_START_PADDING_DP), 0, this.style.dp(SettingsStyle.SCREEN_INSET_DP), 0);
        bar.setMinimumHeight(this.style.dp(APP_BAR_HEIGHT_DP));
        ImageView back = new ImageView(this.activity);
        back.setImageDrawable(this.style.backIcon(this.style.primaryText));
        back.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        int inset = this.style.dp(BACK_ICON_INSET_DP);
        back.setPadding(inset, inset, inset, inset);
        back.setContentDescription("Back");
        back.setBackground(this.style.ripple(BACK_TOUCH_TARGET_DP / 2));
        back.setOnClickListener((view) -> this.dialog.dismiss());
        bar.addView(back, new LinearLayout.LayoutParams(this.style.dp(BACK_TOUCH_TARGET_DP),
                this.style.dp(BACK_TOUCH_TARGET_DP)));
        TextView title = this.style.text(PatchesMenu.SETTINGS_ROW_TITLE, SettingsStyle.BAR_TITLE_SP,
                this.style.primaryText, this.style.medium);
        title.setPadding(this.style.dp(TITLE_START_PADDING_DP), 0, 0, 0);
        SettingsStyle.markHeading(title);
        bar.addView(title);
        return bar;
    }

    private void replaceContent() {
        this.scroll.removeAllViews();
        this.scroll.addView(content());
    }

    private View content() {
        LinearLayout column = new LinearLayout(this.activity);
        column.setOrientation(LinearLayout.VERTICAL);
        int inset = this.style.dp(SettingsStyle.SCREEN_INSET_DP);
        column.setPadding(inset, 0, inset, this.style.dp(CONTENT_BOTTOM_PADDING_DP));
        LinearLayout home = this.rows.card();
        addSwitch(home, AppliedPatches.hideHomeShortcuts(), AppliedPatches.hideHomeShortcutsName(),
                AppliedPatches.HIDE_HOME_SHORTCUTS, AppliedPatches.hideHomeShortcutsDescription(),
                "hx_terabox_ic_shortcuts", true);
        addSwitch(home, AppliedPatches.removeRecentPanel(), AppliedPatches.removeRecentPanelName(),
                AppliedPatches.REMOVE_RECENT_PANEL, AppliedPatches.removeRecentPanelDescription(),
                "hx_terabox_ic_recent", false);
        addSection(column, "Home", home);
        LinearLayout backup = this.rows.card();
        addSwitch(backup, AppliedPatches.showBackupCount(), AppliedPatches.showBackupCountName(),
                AppliedPatches.SHOW_BACKUP_COUNT, "Shows remaining backup files.", "hx_terabox_ic_backup", true);
        addSection(column, "Backup", backup);
        LinearLayout videos = this.rows.card();
        addSwitch(videos, AppliedPatches.hideVideoRecommendations(), AppliedPatches.hideVideoRecommendationsName(),
                AppliedPatches.HIDE_VIDEO_RECOMMENDATIONS, AppliedPatches.hideVideoRecommendationsDescription(),
                "hx_terabox_ic_video", false);
        addSwitch(videos, AppliedPatches.unlockPremium(), AppliedPatches.unlockPremiumName(),
                AppliedPatches.UNLOCK_PREMIUM, AppliedPatches.unlockPremiumDescription(), "hx_terabox_ic_premium",
                false);
        addSection(column, "Videos", videos);
        LinearLayout ads = this.rows.card();
        addSwitch(ads, AppliedPatches.hideAds(), AppliedPatches.hideAdsName(), AppliedPatches.HIDE_ADS,
                AppliedPatches.hideAdsDescription(), "hx_terabox_ic_ads", false);
        addSwitch(ads, AppliedPatches.hidePromotions(), AppliedPatches.hidePromotionsName(),
                AppliedPatches.HIDE_PROMOTIONS, AppliedPatches.hidePromotionsDescription(), "hx_terabox_ic_promotions",
                false);
        addSection(column, "Ads", ads);
        LinearLayout about = this.rows.card();
        this.rows.actionRow(about, "hx_terabox_ic_reset", "Reset to defaults", RESET_DESCRIPTION, this::confirmReset);
        this.rows.valueRow(about, "hx_terabox_ic_info", "App version", appVersion());
        this.rows.valueRow(about, "hx_terabox_ic_patches", "Patch bundle", BuildConfig.BUNDLE_VERSION);
        addSection(column, "About", about);
        LinearLayout applied = this.rows.card();
        for (AppliedPatch patch : AppliedPatches.applied()) {
            this.rows.expandableRow(applied, patch.name, patch.description);
        }
        addSection(column, "Applied patches", applied);
        return column;
    }

    private void addSwitch(LinearLayout card, boolean applied, String title, String key, String description,
            String icon, boolean restartRequired) {
        if (!applied) {
            return;
        }
        this.rows.switchRow(card, icon, title, description, PatchSettings.isEnabled(key), (button, enabled) -> {
            PatchSettings.setEnabled(key, enabled);
            if (restartRequired) {
                confirmRestart();
            }
        });
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
            String version = this.activity.getPackageManager()
                .getPackageInfo(this.activity.getPackageName(), 0).versionName;
            return (version != null) ? version : "";
        } catch (PackageManager.NameNotFoundException ex) {
            Log.w(PatchSettings.LOG_TAG, "Package info lookup failed", ex);
            return "";
        }
    }

    private void confirmReset() {
        AlertDialog confirmation = new AlertDialog.Builder(this.activity).setTitle("Reset to defaults?")
            .setMessage(RESET_DESCRIPTION)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Reset", (dialog, which) -> {
                PatchSettings.resetToDefaults();
                replaceContent();
                confirmRestart();
            })
            .create();
        showConfirmation(confirmation);
    }

    private void confirmRestart() {
        AlertDialog confirmation = new AlertDialog.Builder(this.activity).setTitle("Restart required")
            .setMessage("TeraBox must restart to apply the change.")
            .setNegativeButton("Later", null)
            .setPositiveButton("Restart", (dialog, which) -> restartApp())
            .create();
        showConfirmation(confirmation);
    }

    private void showConfirmation(AlertDialog confirmation) {
        confirmation.show();
        confirmation.getButton(AlertDialog.BUTTON_POSITIVE).setAllCaps(false);
        confirmation.getButton(AlertDialog.BUTTON_NEGATIVE).setAllCaps(false);
        Window window = confirmation.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(this.style.rounded(this.style.cardSurface, SettingsStyle.CARD_RADIUS_DP));
            int available = this.activity.getResources().getDisplayMetrics().widthPixels
                    - 2 * this.style.dp(CONFIRMATION_SIDE_MARGIN_DP);
            window.setLayout(Math.min(this.style.dp(CONFIRMATION_MAX_WIDTH_DP), available),
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            styleConfirmationText(window.getDecorView());
        }
    }

    private void styleConfirmationText(View view) {
        if (view instanceof TextView) {
            ((TextView) view).setTextColor(this.style.primaryText);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int index = 0; index < group.getChildCount(); index++) {
                styleConfirmationText(group.getChildAt(index));
            }
        }
    }

    private void restartApp() {
        Intent launch = this.activity.getPackageManager().getLaunchIntentForPackage(this.activity.getPackageName());
        if (launch == null) {
            Log.w(PatchSettings.LOG_TAG, "Launch intent missing");
            return;
        }
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        this.activity.startActivity(launch);
        this.activity.finishAffinity();
        Runtime.getRuntime().exit(0);
    }

    @SuppressWarnings("deprecation")
    private void styleWindow(Window window) {
        window.setBackgroundDrawable(new ColorDrawable(this.style.screenBackground));
        window.setStatusBarColor(this.style.screenBackground);
        window.setNavigationBarColor(this.style.screenBackground);
        int flags = this.activity.getWindow().getDecorView().getSystemUiVisibility();
        window.getDecorView().setSystemUiVisibility(flags);
    }

    @SuppressWarnings("deprecation")
    private static WindowInsets applySystemBarInsets(View view, WindowInsets insets) {
        view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
        return insets.consumeSystemWindowInsets();
    }

}
