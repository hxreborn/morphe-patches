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
package app.hxreborn.extension.ultimea;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
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

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            styleSystemBars(getWindow());
        }
    }

    @SuppressWarnings("deprecation")
    private void styleSystemBars(Window window) {
        window.setStatusBarColor(this.style.screenBackground);
        window.setNavigationBarColor(this.style.screenBackground);
        window.getDecorView()
            .setSystemUiVisibility(this.style.dark ? 0
                    : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                int lightBars = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                        | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                controller.setSystemBarsAppearance(this.style.dark ? 0 : lightBars, lightBars);
            }
        }
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

        LinearLayout app = this.rows.card();
        if (AppliedPatches.removeDistractions()) {
            this.rows.switchRow(app, "hx_ultimea_ic_distractions", AppliedPatches.removeDistractionsName(),
                    "Removes social and support entries.", PatchSettings.removeDistractions(), (button, enabled) -> {
                        PatchSettings.setEnabled(PatchSettings.REMOVE_DISTRACTIONS, enabled);
                        confirmRestart();
                    });
        }
        if (AppliedPatches.removePopups()) {
            this.rows.switchRow(app, "hx_ultimea_ic_popups", AppliedPatches.removePopupsName(),
                    "Removes promotions and ratings.", PatchSettings.removePopups(), (button, enabled) -> {
                        PatchSettings.setEnabled(PatchSettings.REMOVE_POPUPS, enabled);
                        confirmRestart();
                    });
        }
        if (AppliedPatches.stopFirmwareReminders()) {
            this.rows.switchRow(app, "hx_ultimea_ic_firmware", AppliedPatches.stopFirmwareRemindersName(),
                    "Stops automatic update reminders.", PatchSettings.stopFirmwareReminders(), (button, enabled) -> {
                        PatchSettings.setEnabled(PatchSettings.STOP_FIRMWARE_REMINDERS, enabled);
                        confirmRestart();
                    });
        }
        addSection(column, "App", app);

        LinearLayout about = this.rows.card();
        this.rows.actionRow(about, "hx_ultimea_ic_reset", "Reset to defaults", RESET_DESCRIPTION, this::confirmReset);
        this.rows.valueRow(about, "hx_ultimea_ic_info", "App version", appVersion());
        this.rows.valueRow(about, "hx_ultimea_ic_patches", "Patch bundle", BuildConfig.BUNDLE_VERSION);
        addSection(column, "About", about);

        LinearLayout applied = this.rows.card();
        List<String> names = new ArrayList<>();
        if (AppliedPatches.disableTracking()) {
            names.add(AppliedPatches.disableTrackingName());
        }
        if (AppliedPatches.removeDistractions()) {
            names.add(AppliedPatches.removeDistractionsName());
        }
        if (AppliedPatches.removePopups()) {
            names.add(AppliedPatches.removePopupsName());
        }
        if (AppliedPatches.stopFirmwareReminders()) {
            names.add(AppliedPatches.stopFirmwareRemindersName());
        }
        if (AppliedPatches.xupmix()) {
            names.add(AppliedPatches.xupmixName());
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
            String version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            return (version != null) ? version : "";
        } catch (PackageManager.NameNotFoundException ex) {
            Log.w(PatchSettings.LOG_TAG, "Package info lookup failed for " + getPackageName(), ex);
            return "unknown";
        }
    }

    private void confirmReset() {
        AlertDialog dialog = new AlertDialog.Builder(this, dialogTheme()).setTitle("Reset to defaults?")
            .setMessage(RESET_DESCRIPTION)
            .setPositiveButton("Reset", (confirmation, which) -> {
                PatchSettings.resetToDefaults();
                setContentView(screen());
                confirmRestart();
            })
            .setNegativeButton("Cancel", null)
            .show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setAllCaps(false);
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setAllCaps(false);
    }

    private int dialogTheme() {
        return this.style.dark ? android.R.style.Theme_DeviceDefault_Dialog_Alert
                : android.R.style.Theme_DeviceDefault_Light_Dialog_Alert;
    }

    private void confirmRestart() {
        CharSequence appName = getApplicationInfo().loadLabel(getPackageManager());
        AlertDialog dialog = new AlertDialog.Builder(this, dialogTheme()).setTitle("Restart required")
            .setMessage(appName + " must restart to apply the change.")
            .setPositiveButton("Restart", (confirmation, which) -> restartApp())
            .setNegativeButton("Later", null)
            .show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setAllCaps(false);
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setAllCaps(false);
    }

    private void restartApp() {
        Intent launch = getPackageManager().getLaunchIntentForPackage(getPackageName());
        if (launch == null) {
            return;
        }
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(launch);
        Runtime.getRuntime().exit(0);
    }

}
