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
package app.hxreborn.extension.readera;

import java.util.Map;
import java.util.TreeMap;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
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

    private static final int APP_BAR_HEIGHT_DP = 56;

    private static final int APP_BAR_START_PADDING_DP = 4;

    private static final int BACK_TOUCH_TARGET_DP = 48;

    private static final int BACK_ICON_INSET_DP = 12;

    private static final int TITLE_START_PADDING_DP = 12;

    private static final int CONTENT_BOTTOM_PADDING_DP = 24;

    private static final int DIALOG_THEME = android.R.style.Theme_DeviceDefault_Dialog_Alert;

    private static final String RESET_DESCRIPTION = "Turns footnotes and header skipping back on, and the theme "
            + "and accent back to their defaults.";

    private SettingsStyle style;

    private SettingsRows rows;

    private ScrollView scroll;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(AppTheme.wrap(base));
        AccentColor.applyToActivity(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.style = new SettingsStyle(this);
        this.rows = new SettingsRows(this, this.style);
        styleSystemBars(getWindow());
    }

    @SuppressWarnings("deprecation")
    private void styleSystemBars(Window window) {
        window.setStatusBarColor(this.style.screenBackground);
        window.setNavigationBarColor(this.style.screenBackground);
    }

    private View screen() {
        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setBackgroundColor(this.style.screenBackground);
        screen.setOnApplyWindowInsetsListener(this::applySystemBarInsets);
        screen.addView(appBar());

        this.scroll = new ScrollView(this);
        this.scroll.addView(content(),
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        screen.addView(this.scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
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
        bar.setBackgroundColor(this.style.appBarBackground);
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

        Map<String, String> explanations = new TreeMap<>();
        for (String entry : PatchSettings.appliedPatches().split("\\n")) {
            String[] pair = entry.split("\t", 2);
            if (pair.length == 2) {
                explanations.put(pair[0], pair[1]);
            }
        }
        LinearLayout appearance = this.rows.card();
        if (explanations.containsKey("App theme")) {
            View theme = this.rows.choiceRow(appearance, "theme", "App theme", explanations.get("App theme"),
                    AppTheme.amoledSelected(this) ? "AMOLED ▾" : "Stock ▾");
            theme.setOnClickListener(this::showThemeMenu);
        }
        if (explanations.containsKey(AccentColor.TITLE)) {
            View accent = this.rows.choiceRow(appearance, "palette", AccentColor.TITLE,
                    explanations.get(AccentColor.TITLE), AccentPalette.label(AccentPalette.getPreset(this)) + " ▾");
            accent.setOnClickListener(
                    (view) -> new AccentColorDialog(this, this.style, DIALOG_THEME, this::onAccentSaved).show());
        }
        addSection(column, "Appearance", appearance);

        LinearLayout speech = this.rows.card();
        addMirror(speech, explanations, "footnote", "Footnote speech", PatchSettings.FOOTNOTES);
        addMirror(speech, explanations, "headers", "Skip headers and footers", PatchSettings.SKIP_HEADERS);
        if (explanations.containsKey("Text-to-speech filter")) {
            View omit = this.rows.choiceRow(speech, "omit", SpeechFilterSettings.TITLE,
                    explanations.get("Text-to-speech filter"),
                    Integer.toString(SpeechFilterSettings.count(this)) + " ›");
            omit.setOnClickListener((view) -> SpeechFilterSettings.open(this, this::refresh));
        }
        addSection(column, "Read aloud", speech);

        LinearLayout library = this.rows.card();
        if (explanations.containsKey("Library full screen")) {
            View fullScreen = this.rows.choiceRow(library, "fullscreen", "Library full screen",
                    explanations.get("Library full screen"),
                    PatchSettings.nativeEnabled(PatchSettings.FULL_LIBRARY, false) ? "Always ▾" : "Never ▾");
            fullScreen.setOnClickListener(this::showFullScreenMenu);
        }
        addSection(column, "Library", library);

        LinearLayout about = this.rows.card();
        this.rows.actionRow(about, "reset", "Reset to defaults", RESET_DESCRIPTION, this::confirmReset);
        this.rows.valueRow(about, "info", "App version", appVersion());
        this.rows.valueRow(about, "bundle", "Patch bundle", BuildConfig.BUNDLE_VERSION);
        addSection(column, "About", about);

        LinearLayout applied = this.rows.card();
        for (Map.Entry<String, String> patch : explanations.entrySet()) {
            this.rows.appliedRow(applied, patch.getKey(), patch.getValue());
        }
        addSection(column, "Applied patches", applied);
        return column;
    }

    private void showThemeMenu(View anchor) {
        boolean amoled = AppTheme.amoledSelected(this);
        new MaterialMenu(this, this.style).show(anchor,
                new MaterialMenu.Item("Stock", "stock", !amoled, () -> selectTheme(false)),
                new MaterialMenu.Item("AMOLED", "amoled", amoled, () -> selectTheme(true)));
    }

    private void selectTheme(boolean amoled) {
        if (amoled == AppTheme.amoledSelected(this)) {
            return;
        }
        boolean saved = AppTheme.saveAmoledSelected(this, amoled);
        refresh();
        if (saved) {
            confirmRestart();
        }
    }

    private void onAccentSaved() {
        refresh();
        confirmRestart();
    }

    private void showFullScreenMenu(View anchor) {
        boolean enabled = PatchSettings.nativeEnabled(PatchSettings.FULL_LIBRARY, false);
        new MaterialMenu(this, this.style).show(anchor,
                new MaterialMenu.Item("Never", "fullscreenExit", !enabled, () -> setLibraryFullScreen(false)),
                new MaterialMenu.Item("Always", "fullscreen", enabled, () -> setLibraryFullScreen(true)));
    }

    private void setLibraryFullScreen(boolean enabled) {
        PatchSettings.setNativeEnabled(PatchSettings.FULL_LIBRARY, enabled);
        refresh();
    }

    private void refresh() {
        int position = (this.scroll != null) ? this.scroll.getScrollY() : 0;
        setContentView(screen());
        this.scroll.post(() -> this.scroll.scrollTo(0, position));
    }

    private void addMirror(LinearLayout card, Map<String, String> applied, String icon, String title, String key) {
        if (applied.containsKey(title)) {
            this.rows.switchRow(card, icon, title, applied.get(title), PatchSettings.nativeEnabled(key, true),
                    (button, enabled) -> PatchSettings.setNativeEnabled(key, enabled));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (this.rows != null) {
            refresh();
        }
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
        new AlertDialog.Builder(this, DIALOG_THEME).setTitle("Reset to defaults?")
            .setMessage(RESET_DESCRIPTION)
            .setPositiveButton("Reset", (dialog, which) -> {
                PatchSettings.resetToDefaults();
                setContentView(screen());
                confirmRestart();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void confirmRestart() {
        CharSequence appName = getApplicationInfo().loadLabel(getPackageManager());
        new AlertDialog.Builder(this, DIALOG_THEME).setTitle("Restart required")
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
