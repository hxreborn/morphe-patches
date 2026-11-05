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
package app.hxreborn.extension.liftoff;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
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

final class PatchesSettingsDialog implements View.OnAttachStateChangeListener {

    private static final String UNKNOWN_VERSION = "unknown";

    private static final int APP_BAR_HEIGHT_DP = 64;

    private static final int BACK_TOUCH_TARGET_DP = 48;

    private static final int APP_BAR_START_PADDING_DP = 4;

    private static final int BACK_ICON_INSET_DP = 12;

    private static final int TITLE_START_PADDING_DP = 12;

    private final Activity activity;

    private final SettingsStyle style;

    private final SettingsRows rows;

    private final Dialog dialog;

    private final ScrollView scroll;

    private Dialog confirmationDialog;

    PatchesSettingsDialog(Activity activity, TextView template) {
        this.activity = activity;
        this.style = new SettingsStyle(activity, template);
        this.rows = new SettingsRows(activity, this.style);
        this.dialog = new Dialog(activity, android.R.style.Theme_DeviceDefault_NoActionBar);
        this.scroll = new ScrollView(activity);
        this.dialog.setOwnerActivity(activity);
        this.dialog.setOnDismissListener((dismissed) -> {
            this.activity.getWindow().getDecorView().removeOnAttachStateChangeListener(this);
            if (this.confirmationDialog != null) {
                this.confirmationDialog.dismiss();
            }
        });
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
        styleWindow(this.dialog.getWindow());
        this.activity.getWindow().getDecorView().addOnAttachStateChangeListener(this);
        this.dialog.show();
        this.dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    private View appBar() {
        LinearLayout bar = new LinearLayout(this.activity);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(this.style.dp(APP_BAR_START_PADDING_DP), 0, this.style.dp(SettingsStyle.SCREEN_INSET_DP), 0);
        bar.setMinimumHeight(this.style.dp(APP_BAR_HEIGHT_DP));

        ImageView back = new ImageView(this.activity);
        back.setImageDrawable(this.style.hostIcon("hx_liftoff_ic_back", this.style.primaryText));
        back.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        int inset = this.style.dp(BACK_ICON_INSET_DP);
        back.setPadding(inset, inset, inset, inset);
        back.setContentDescription("Back");
        back.setBackground(this.style.ripple(0));
        back.setOnClickListener((view) -> this.dialog.dismiss());
        int touchTarget = this.style.dp(BACK_TOUCH_TARGET_DP);
        bar.addView(back, new LinearLayout.LayoutParams(touchTarget, touchTarget));

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
        column.setPadding(inset, 0, inset, inset);

        LinearLayout features = this.rows.card();
        if (AppliedPatches.unlockPremium()) {
            this.rows.switchRow(features, "hx_liftoff_ic_premium", AppliedPatches.UNLOCK_PREMIUM,
                    "Unlocks rank analysis and nutrition tools.",
                    PatchSettings.isEnabled(AppliedPatches.UNLOCK_PREMIUM), (button, enabled) -> {
                        PatchSettings.setEnabled(AppliedPatches.UNLOCK_PREMIUM, enabled);
                        confirmRestart();
                    });
        }
        if (AppliedPatches.unlockCustomizations()) {
            this.rows.switchRow(features, "hx_liftoff_ic_customizations", AppliedPatches.UNLOCK_CUSTOMIZATIONS,
                    "Unlocks themes, profile borders and profile banners.",
                    PatchSettings.isEnabled(AppliedPatches.UNLOCK_CUSTOMIZATIONS), (button, enabled) -> {
                        PatchSettings.setEnabled(AppliedPatches.UNLOCK_CUSTOMIZATIONS, enabled);
                        confirmRestart();
                    });
        }
        addSection(column, "Features", features);

        LinearLayout about = this.rows.card();
        this.rows.actionRow(about, "hx_liftoff_ic_reset", "Reset to defaults", ConfirmationDialog.RESET_DESCRIPTION,
                this::confirmReset);
        this.rows.valueRow(about, "hx_liftoff_ic_info", "App version", appVersion());
        this.rows.valueRow(about, "hx_liftoff_ic_patches", "Patch bundle", BuildConfig.BUNDLE_VERSION);
        addSection(column, "About", about);

        LinearLayout applied = this.rows.card();
        for (String name : AppliedPatches.names()) {
            this.rows.expandableRow(applied, name, AppliedPatches.description(name));
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
            String version = this.activity.getPackageManager()
                .getPackageInfo(this.activity.getPackageName(), 0).versionName;
            return (version != null) ? version : "";
        } catch (PackageManager.NameNotFoundException ex) {
            Log.w(PatchSettings.LOG_TAG, "Package info lookup failed for " + this.activity.getPackageName(), ex);
            return UNKNOWN_VERSION;
        }
    }

    private void confirmReset() {
        observeConfirmation(ConfirmationDialog.showReset(this.activity, this.style, () -> {
            PatchSettings.resetToDefaults();
            replaceContent();
            confirmRestart();
        }));
    }

    private void confirmRestart() {
        observeConfirmation(ConfirmationDialog.showRestart(this.activity, this.style, this::restartApp));
    }

    private void observeConfirmation(Dialog confirmation) {
        this.confirmationDialog = confirmation;
        confirmation.setOnDismissListener((dismissed) -> {
            if (this.confirmationDialog == confirmation) {
                this.confirmationDialog = null;
            }
        });
    }

    private void restartApp() {
        Intent launch = this.activity.getPackageManager().getLaunchIntentForPackage(this.activity.getPackageName());
        if (launch == null) {
            Log.w(PatchSettings.LOG_TAG, "No launch intent for " + this.activity.getPackageName());
            return;
        }
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        this.activity.startActivity(launch);
        this.activity.finishAffinity();
        Runtime.getRuntime().exit(0);
    }

    private void styleWindow(Window window) {
        if (window == null) {
            return;
        }
        window.setBackgroundDrawable(new ColorDrawable(this.style.screenBackground));
        window.setStatusBarColor(this.style.screenBackground);
        window.setNavigationBarColor(this.style.screenBackground);
        if (!this.style.dark) {
            int lightBars = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                lightBars |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
            window.getDecorView().setSystemUiVisibility(lightBars);
        }
    }

    @Override
    public void onViewAttachedToWindow(View view) {
    }

    @Override
    public void onViewDetachedFromWindow(View view) {
        this.dialog.dismiss();
    }

    private static WindowInsets applySystemBarInsets(View view, WindowInsets insets) {
        view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
        return insets.consumeSystemWindowInsets();
    }

}
