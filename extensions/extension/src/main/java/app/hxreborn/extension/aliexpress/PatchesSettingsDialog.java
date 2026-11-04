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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.text.TextUtils;
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

final class PatchesSettingsDialog {

    private static final String RESET_DESCRIPTION = "Restores every switch to its default.";

    private static final int APP_BAR_HEIGHT_DP = 64;

    private static final int APP_BAR_START_PADDING_DP = 4;

    private static final int BACK_TOUCH_TARGET_DP = 48;

    private static final int BACK_ICON_INSET_DP = 12;

    private static final int TITLE_START_PADDING_DP = 12;

    private final Activity activity;

    private final SettingsStyle style;

    private final SettingsRows rows;

    private final Dialog dialog;

    private final ScrollView scroll;

    PatchesSettingsDialog(Activity activity, TextView nativeTitle) {
        this.activity = activity;
        this.style = new SettingsStyle(activity, nativeTitle);
        this.rows = new SettingsRows(activity, this.style);
        this.dialog = new Dialog(activity, android.R.style.Theme_DeviceDefault_NoActionBar);
        this.dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.scroll = new ScrollView(activity);
    }

    void show() {
        LinearLayout root = new LinearLayout(this.activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(this.style.screenBackground);
        root.setOnApplyWindowInsetsListener(PatchesSettingsDialog::applySystemBarInsets);
        root.addView(appBar());
        replaceContent();
        root.addView(this.scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        this.dialog.setContentView(root);
        this.dialog.show();
        Window window = this.dialog.getWindow();
        if (window == null) {
            return;
        }
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        window.setBackgroundDrawable(new ColorDrawable(this.style.screenBackground));
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(this.style.screenBackground);
        window.setNavigationBarColor(this.style.screenBackground);
        window.getDecorView().setSystemUiVisibility(systemBarIconFlags());
    }

    private int systemBarIconFlags() {
        if (!this.style.lightScreen) {
            return View.SYSTEM_UI_FLAG_VISIBLE;
        }
        int flags = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }
        return flags;
    }

    private View appBar() {
        LinearLayout bar = new LinearLayout(this.activity);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(this.style.dp(APP_BAR_START_PADDING_DP), 0, this.style.dp(SettingsStyle.SCREEN_INSET_DP), 0);
        bar.setMinimumHeight(this.style.dp(APP_BAR_HEIGHT_DP));
        ImageView back = new ImageView(this.activity);
        back.setImageDrawable(this.style.hostIcon("hx_aliexpress_ic_back", this.style.primaryText));
        back.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        int inset = this.style.dp(BACK_ICON_INSET_DP);
        back.setPadding(inset, inset, inset, inset);
        back.setContentDescription("Back");
        back.setBackground(this.style.ripple(0));
        back.setOnClickListener((view) -> this.dialog.dismiss());
        int target = this.style.dp(BACK_TOUCH_TARGET_DP);
        bar.addView(back, new LinearLayout.LayoutParams(target, target));
        TextView title = this.style.text(PatchesMenu.SETTINGS_ROW_TITLE, SettingsStyle.BAR_TITLE_SP,
                this.style.primaryText, this.style.medium);
        title.setPadding(this.style.dp(TITLE_START_PADDING_DP), 0, 0, 0);
        SettingsStyle.markHeading(title);
        title.setMaxLines(1);
        title.setEllipsize(TextUtils.TruncateAt.END);
        bar.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
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
        column.setPadding(inset, 0, inset, inset);
        LinearLayout browsing = this.rows.card();
        if (AppliedPatches.removeSponsoredItems()) {
            addSwitch(browsing, "hx_aliexpress_ic_sponsored", AppliedPatches.REMOVE_SPONSORED_ITEMS);
        }
        if (AppliedPatches.removePromotionalPopups()) {
            addSwitch(browsing, "hx_aliexpress_ic_promotions", AppliedPatches.REMOVE_PROMOTIONAL_POPUPS);
        }
        if (AppliedPatches.enableImageSaving()) {
            addSwitch(browsing, "hx_aliexpress_ic_images", AppliedPatches.ENABLE_IMAGE_SAVING);
        }
        addSection(column, "Browsing", browsing);
        LinearLayout launch = this.rows.card();
        if (AppliedPatches.skipSplashPromotions()) {
            addSwitch(launch, "hx_aliexpress_ic_splash", AppliedPatches.SKIP_SPLASH_PROMOTIONS);
        }
        if (AppliedPatches.disableUpdatePrompts()) {
            addSwitch(launch, "hx_aliexpress_ic_updates", AppliedPatches.DISABLE_UPDATE_PROMPTS);
        }
        addSection(column, "Launch", launch);
        LinearLayout privacy = this.rows.card();
        if (AppliedPatches.disableAnalytics()) {
            addSwitch(privacy, "hx_aliexpress_ic_analytics", AppliedPatches.DISABLE_ANALYTICS);
        }
        addSection(column, "Privacy", privacy);
        LinearLayout about = this.rows.card();
        this.rows.actionRow(about, "hx_aliexpress_ic_reset", "Reset to defaults", RESET_DESCRIPTION,
                this::confirmReset);
        this.rows.valueRow(about, "hx_aliexpress_ic_info", "App version", appVersion());
        this.rows.valueRow(about, "hx_aliexpress_ic_bundle", "Patch bundle", BuildConfig.BUNDLE_VERSION);
        column.addView(this.rows.sectionHeader("About"));
        column.addView(about);
        LinearLayout applied = this.rows.card();
        for (String name : AppliedPatches.names()) {
            this.rows.expandableRow(applied, name, AppliedPatches.description(name));
        }
        addSection(column, "Applied patches", applied);
        return column;
    }

    private void addSwitch(LinearLayout card, String icon, String key) {
        this.rows.switchRow(card, icon, key, AppliedPatches.description(key), PatchSettings.isEnabled(key),
                (button, checked) -> PatchSettings.setEnabled(key, checked));
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
            })
            .show();
        styleButtons(confirmation);
    }

    private void styleButtons(AlertDialog dialog) {
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setAllCaps(false);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setAllCaps(false);
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(this.style.accent);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(this.style.accent);
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTypeface(this.style.medium);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTypeface(this.style.medium);
    }

    private static WindowInsets applySystemBarInsets(View view, WindowInsets insets) {
        view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
        return insets.consumeSystemWindowInsets();
    }

}
