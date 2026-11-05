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

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.TextView;

import app.hxreborn.extension.BuildConfig;

final class PatchesSettingsDialog {

    private static final String RESET_DESCRIPTION = "Restores every switch to its default.";

    private static final String UNKNOWN_VERSION = "unknown";

    private static final String SUBSCRIPTION_TITLE = "Subscription";

    private static final String SUBSCRIPTION_MESSAGE = "Picks the tier to unlock.";

    private static final int APP_BAR_HEIGHT_DP = 64;

    private static final int APP_BAR_START_PADDING_DP = 4;

    private static final int BACK_ICON_INSET_DP = 12;

    private static final int TITLE_START_PADDING_DP = 12;

    private static final int DIALOG_WIDTH_DP = 320;

    private static final int DIALOG_PADDING_DP = 24;

    private static final int DIALOG_GAP_DP = 8;

    private static final int OPTION_PADDING_DP = 16;

    private final Activity activity;

    private final SettingsStyle style;

    private final SettingsRows rows;

    private final Dialog dialog;

    private final ScrollView scroll;

    PatchesSettingsDialog(Activity activity) {
        this.activity = activity;
        this.style = new SettingsStyle(activity);
        this.rows = new SettingsRows(this.style);
        this.dialog = new Dialog(activity, android.R.style.Theme_DeviceDefault_NoActionBar);
        this.scroll = new ScrollView(activity);
    }

    void show() {
        LinearLayout root = new LinearLayout(this.activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(this.style.snow);
        root.setOnApplyWindowInsetsListener(PatchesSettingsDialog::applySystemBarInsets);
        root.addView(appBar());

        replaceContent();
        root.addView(this.scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        this.dialog.setContentView(root);
        styleWindow(this.dialog.getWindow());
        this.dialog.show();
    }

    private View appBar() {
        LinearLayout bar = new LinearLayout(this.activity);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(this.style.dp(APP_BAR_START_PADDING_DP), 0, this.style.dp(SettingsStyle.SCREEN_INSET_DP), 0);
        bar.setMinimumHeight(this.style.dp(APP_BAR_HEIGHT_DP));

        ImageView back = new ImageView(this.activity);
        back.setImageDrawable(this.style.hostIcon("ic_arrow_back_black_24dp", this.style.eel));
        back.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        int inset = this.style.dp(BACK_ICON_INSET_DP);
        back.setPadding(inset, inset, inset, inset);
        back.setContentDescription("Back");
        back.setBackground(this.style.pressedFeedback());
        back.setOnClickListener((view) -> this.dialog.dismiss());
        int touchTarget = this.style.dp(SettingsStyle.MIN_TOUCH_TARGET_DP);
        bar.addView(back, new LinearLayout.LayoutParams(touchTarget, touchTarget));

        TextView title = this.style.boldText(PatchesMenu.SETTINGS_ROW_TITLE, SettingsStyle.BAR_TITLE_SP,
                this.style.eel);
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

        LinearLayout learning = this.rows.card();
        if (AppliedPatches.unlockPremium()) {
            PatchSettings.Subscription subscription = PatchSettings.subscription();
            this.rows.switchRow(learning, "hx_duolingo_ic_premium", AppliedPatches.UNLOCK_PREMIUM,
                    AppliedPatches.unlockPremiumName(), unlockPremiumDescription(subscription), this::confirmRestart);
            this.rows.actionRow(learning, "hx_duolingo_ic_premium", SUBSCRIPTION_TITLE, subscription.description,
                    this::chooseSubscription);
        }
        if (AppliedPatches.unlimitedRampUpTime()) {
            this.rows.switchRow(learning, "hx_duolingo_ic_rampup", AppliedPatches.UNLIMITED_RAMPUP_TIME,
                    AppliedPatches.unlimitedRampUpTimeName(), AppliedPatches.unlimitedRampUpTimeDescription(),
                    this::confirmRestart);
        }
        addSection(column, "Learning", learning);

        LinearLayout app = this.rows.card();
        if (AppliedPatches.disableDynamicAppIcon()) {
            this.rows.switchRow(app, "hx_duolingo_ic_dynamic_icon", AppliedPatches.DISABLE_DYNAMIC_APP_ICON,
                    AppliedPatches.disableDynamicAppIconName(), AppliedPatches.disableDynamicAppIconDescription(),
                    this::confirmRestart);
        }
        if (AppliedPatches.enableDebugMode()) {
            this.rows.switchRow(app, "hx_duolingo_ic_debug", AppliedPatches.ENABLE_DEBUG_MODE,
                    AppliedPatches.enableDebugModeName(), "Shows the debug menu and debug button.",
                    this::confirmRestart);
        }
        addSection(column, "App", app);

        LinearLayout about = this.rows.card();
        this.rows.actionRow(about, "hx_duolingo_ic_reset", "Reset to defaults", RESET_DESCRIPTION, this::confirmReset);
        this.rows.valueRow(about, "hx_duolingo_ic_info", "App version", appVersion());
        this.rows.valueRow(about, "hx_duolingo_ic_patches", "Patch bundle", BuildConfig.BUNDLE_VERSION);
        addSection(column, "About", about);
        return column;
    }

    static String unlockPremiumDescription(PatchSettings.Subscription subscription) {
        return "Unlocks " + subscription.label + " and keeps energy full.";
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
            return this.activity.getPackageManager().getPackageInfo(this.activity.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException ex) {
            Log.w(PatchSettings.LOG_TAG, "Package info lookup failed for " + this.activity.getPackageName(), ex);
            return UNKNOWN_VERSION;
        }
    }

    private void confirmReset() {
        showConfirmation("Reset to defaults?", RESET_DESCRIPTION, "Reset", "Cancel", () -> {
            PatchSettings.resetToDefaults();
            replaceContent();
            confirmRestart();
        });
    }

    private void confirmRestart() {
        CharSequence appName = this.activity.getApplicationInfo().loadLabel(this.activity.getPackageManager());
        showConfirmation("Restart required", appName + " must restart to apply the change.", "Restart", "Later",
                this::restartApp);
    }

    private void chooseSubscription() {
        Dialog chooser = new Dialog(this.activity);
        LinearLayout panel = dialogPanel(SUBSCRIPTION_TITLE, SUBSCRIPTION_MESSAGE);
        PatchSettings.Subscription current = PatchSettings.subscription();
        for (PatchSettings.Subscription subscription : PatchSettings.Subscription.values()) {
            panel.addView(subscriptionOption(subscription, subscription == current, () -> {
                chooser.dismiss();
                if (subscription != current) {
                    PatchSettings.setSubscription(subscription);
                    replaceContent();
                    confirmRestart();
                }
            }), fullWidth(this.style.dp(DIALOG_GAP_DP)));
        }
        panel.addView(this.style.textButton("Cancel", (view) -> chooser.dismiss()),
                fullWidth(this.style.dp(DIALOG_GAP_DP)));
        showDialog(chooser, panel);
    }

    private View subscriptionOption(PatchSettings.Subscription subscription, boolean selected, Runnable onChosen) {
        LinearLayout option = new LinearLayout(this.activity);
        option.setOrientation(LinearLayout.VERTICAL);
        int padding = this.style.dp(OPTION_PADDING_DP);
        option.setPadding(padding, padding, padding, padding);
        option.setBackground(this.style.outlined(this.style.snow, selected ? this.style.macaw : this.style.swan,
                SettingsStyle.CARD_RADIUS_DP));
        option.addView(this.style.boldText(subscription.label, SettingsStyle.ROW_TITLE_SP,
                selected ? this.style.macaw : this.style.eel));
        option.addView(this.style.regularText(subscription.description, SettingsStyle.SUMMARY_SP, this.style.wolf));
        option.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override
            public void onInitializeAccessibilityNodeInfo(View view, AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(view, info);
                info.setClassName(RadioButton.class.getName());
                info.setCheckable(true);
                info.setChecked(selected);
            }
        });
        option.setOnClickListener((view) -> onChosen.run());
        return option;
    }

    private void showConfirmation(String title, String message, String confirmLabel, String cancelLabel,
            Runnable onConfirm) {
        Dialog confirmation = new Dialog(this.activity);
        LinearLayout panel = dialogPanel(title, message);
        panel.addView(this.style.solidButton(confirmLabel, (view) -> {
            confirmation.dismiss();
            onConfirm.run();
        }), fullWidth(0));
        panel.addView(this.style.textButton(cancelLabel, (view) -> confirmation.dismiss()),
                fullWidth(this.style.dp(DIALOG_GAP_DP)));
        showDialog(confirmation, panel);
    }

    private LinearLayout dialogPanel(String title, String message) {
        LinearLayout panel = new LinearLayout(this.activity);
        panel.setOrientation(LinearLayout.VERTICAL);
        int padding = this.style.dp(DIALOG_PADDING_DP);
        panel.setPadding(padding, padding, padding, this.style.dp(DIALOG_GAP_DP));
        panel.setBackground(this.style.outlined(this.style.snow, this.style.swan, SettingsStyle.CARD_RADIUS_DP));

        panel.addView(this.style.boldText(title, SettingsStyle.TITLE_SP, this.style.eel));
        TextView body = this.style.regularText(message, SettingsStyle.MESSAGE_SP, this.style.wolf);
        body.setPadding(0, this.style.dp(DIALOG_GAP_DP), 0, padding);
        panel.addView(body);
        return panel;
    }

    private void showDialog(Dialog dialog, LinearLayout panel) {
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(panel);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(this.style.dp(DIALOG_WIDTH_DP), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
    }

    private static LinearLayout.LayoutParams fullWidth(int topMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = topMargin;
        return params;
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
        window.setBackgroundDrawable(new ColorDrawable(this.style.snow));
        window.setStatusBarColor(this.style.snow);
        window.setNavigationBarColor(this.style.snow);
        if (!this.style.night) {
            int lightBars = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                lightBars |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
            window.getDecorView().setSystemUiVisibility(lightBars);
        }
    }

    private static WindowInsets applySystemBarInsets(View view, WindowInsets insets) {
        view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
        return insets.consumeSystemWindowInsets();
    }

}
