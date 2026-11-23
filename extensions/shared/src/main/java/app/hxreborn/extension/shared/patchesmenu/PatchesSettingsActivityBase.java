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
package app.hxreborn.extension.shared.patchesmenu;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.drawable.ColorDrawable;
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
import android.widget.Toast;

@SuppressLint("SetTextI18n")
@SuppressWarnings("unused")
public abstract class PatchesSettingsActivityBase extends Activity {

    private static final int APP_BAR_HEIGHT_DP = 64;

    private static final int APP_BAR_START_PADDING_DP = 4;

    private static final int BACK_TOUCH_TARGET_DP = 48;

    private static final int BACK_ICON_INSET_DP = 12;

    private static final int TITLE_START_PADDING_DP = 12;

    private SettingsStyle style;

    private SettingsRows rows;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        int theme = settingsTheme();
        if (theme != 0) {
            setTheme(theme);
        }
        super.onCreate(savedInstanceState);
        initializeSettings();
        this.style = createStyle();
        this.rows = createRows(this.style);
        if (showScreenOnCreate()) {
            setContentView(createScreen());
        }
        styleSystemBars(getWindow());
    }

    @SuppressWarnings({ "deprecation", "DataFlowIssue" })
    @SuppressLint("InlinedApi")
    protected void styleSystemBars(Window window) {
        window.setStatusBarColor(this.style.screenBackground);
        window.setNavigationBarColor((Build.VERSION.SDK_INT < Build.VERSION_CODES.O && !this.style.dark) ? Color.BLACK
                : this.style.screenBackground);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            int lightBars = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
            window.getInsetsController().setSystemBarsAppearance(this.style.dark ? 0 : lightBars, lightBars);
            return;
        }
        window.getDecorView()
            .setSystemUiVisibility(this.style.dark ? 0
                    : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    }

    protected View createScreen() {
        LinearLayout screen = MenuLayout.createColumn(this);
        screen.setBackgroundColor(this.style.screenBackground);
        screen.setOnApplyWindowInsetsListener(this::applySystemBarInsets);
        screen.addView(appBar());

        ScrollView scroll = new ScrollView(this);
        scroll.addView(createContent(),
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        MenuLayout.addWeightedContent(screen, scroll);
        return screen;
    }

    @SuppressWarnings("deprecation")
    protected WindowInsets applySystemBarInsets(View view, WindowInsets insets) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Insets bars = insets.getInsets(
                    WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout() | WindowInsets.Type.ime());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsets.CONSUMED;
        }
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

        TextView title = this.style.text("hxreborn patches", SettingsStyle.BAR_TITLE_SP, this.style.primaryText,
                this.style.medium);
        title.setPadding(this.style.dp(TITLE_START_PADDING_DP), 0, 0, 0);
        SettingsStyle.markHeading(title);
        bar.addView(title);
        return bar;
    }

    protected final SettingsStyle style() {
        return this.style;
    }

    protected final SettingsRows rows() {
        return this.rows;
    }

    protected final void refreshContent() {
        setContentView(createScreen());
    }

    protected boolean showScreenOnCreate() {
        return true;
    }

    protected int settingsTheme() {
        return 0;
    }

    protected abstract View createContent();

    protected final String appVersion() {
        try {
            String version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            return (version != null) ? version : "";
        } catch (PackageManager.NameNotFoundException ex) {
            Log.w(logTag(), "Package info lookup failed for " + getPackageName(), ex);
            return "unknown";
        }
    }

    protected final void confirmReset() {
        showDialog(new AlertDialog.Builder(this, dialogTheme()).setTitle("Reset to defaults?")
            .setMessage(PatchesMenuText.RESET_DESCRIPTION)
            .setPositiveButton("Reset", (dialog, which) -> {
                boolean saved = resetToDefaults();
                refreshContent();
                if (!saved) {
                    Toast.makeText(this, "Couldn't reset. Try again.", Toast.LENGTH_SHORT).show();
                    return;
                }
                confirmRestart();
            })
            .setNegativeButton("Cancel", null));
    }

    public final int dialogTheme() {
        return this.style.dark ? android.R.style.Theme_DeviceDefault_Dialog_Alert
                : android.R.style.Theme_DeviceDefault_Light_Dialog_Alert;
    }

    protected final void confirmRestart() {
        showDialog(new AlertDialog.Builder(this, dialogTheme()).setTitle("Restart required")
            .setMessage(restartMessage())
            .setPositiveButton("Restart", (dialog, which) -> restartApp())
            .setNegativeButton("Later", null));
    }

    protected AlertDialog showDialog(AlertDialog.Builder builder) {
        AlertDialog dialog = builder.show();
        styleDialog(dialog);
        return dialog;
    }

    @SuppressWarnings("DataFlowIssue")
    private void styleDialog(AlertDialog dialog) {
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(this.style.screenBackground));
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(this.style.accent);
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(this.style.accent);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setAllCaps(false);
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setAllCaps(false);
    }

    protected abstract void initializeSettings();

    protected abstract SettingsStyle createStyle();

    protected abstract SettingsRows createRows(SettingsStyle style);

    protected abstract boolean resetToDefaults();

    protected abstract String logTag();

    protected abstract String restartMessage();

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
