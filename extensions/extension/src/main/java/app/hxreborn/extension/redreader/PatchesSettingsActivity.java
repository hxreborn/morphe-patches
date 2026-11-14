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
package app.hxreborn.extension.redreader;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import app.hxreborn.extension.BuildConfig;

@SuppressWarnings("unused")
public final class PatchesSettingsActivity extends Activity {

    private static final int APP_BAR_HEIGHT_DP = 64;

    private static final int BACK_ICON_INSET_DP = 12;

    private static final int EDITOR_MIN_LINES = 4;

    private static final int EDITOR_MAX_LINES = 12;

    private static final String RESET_DESCRIPTION = "Restores every switch to its default.";

    private SettingsStyle style;

    private SettingsRows rows;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        PatchSettings.initialize(this);
        this.style = new SettingsStyle(this);
        this.rows = new SettingsRows(this, this.style);
        showContent();
    }

    private void showContent() {
        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setBackgroundColor(this.style.background);
        screen.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                    insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets.consumeSystemWindowInsets();
        });
        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setMinimumHeight(this.style.dp(APP_BAR_HEIGHT_DP));
        ImageView back = new ImageView(this);
        back.setImageDrawable(this.style.backIcon());
        int backInset = this.style.dp(BACK_ICON_INSET_DP);
        back.setPadding(backInset, backInset, backInset, backInset);
        back.setContentDescription("Back");
        back.setBackground(this.style.ripple());
        back.setFocusable(true);
        back.setOnClickListener((view) -> finish());
        bar.addView(back, new LinearLayout.LayoutParams(this.style.dp(SettingsStyle.TOUCH_TARGET_DP),
                this.style.dp(SettingsStyle.TOUCH_TARGET_DP)));
        bar.addView(this.style.text("hxreborn patches", SettingsStyle.TITLE_SP, this.style.text));
        screen.addView(bar);
        ScrollView scroll = new ScrollView(this);
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        int inset = this.style.dp(SettingsStyle.INSET_DP);
        column.setPadding(inset, 0, inset, inset);
        this.rows.section(column, "Feed");
        LinearLayout filters = this.rows.card(column);
        this.rows.row(filters, "Title keywords", "Filters posts with matching title words.",
                () -> editRules("Title keywords", PatchSettings.TITLES_KEY));
        this.rows.row(filters, "Subreddit filters", "Filters posts from matching subreddits.",
                () -> editRules("Subreddit filters", PatchSettings.SUBREDDITS_KEY));
        this.rows.row(filters, "Post flair filters", "Filters posts with matching flair text.",
                () -> editRules("Post flair filters", PatchSettings.FLAIRS_KEY));
        this.rows.section(column, "About");
        LinearLayout about = this.rows.card(column);
        this.rows.row(about, "Reset to defaults", RESET_DESCRIPTION, this::confirmReset);
        this.rows.row(about, "App version", appVersion(), null);
        this.rows.row(about, "Patch bundle", BuildConfig.BUNDLE_VERSION, null);
        this.rows.section(column, "Applied patches");
        LinearLayout applied = this.rows.card(column);
        this.rows.row(applied, "Filter posts", "Filters posts by title, subreddit or flair.", null);
        scroll.addView(column);
        screen.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(screen);
    }

    private void editRules(String title, String key) {
        EditText input = new EditText(this);
        input.setText(PatchSettings.value(key));
        input.setTextColor(this.style.text);
        input.setMinLines(EDITOR_MIN_LINES);
        input.setMaxLines(EDITOR_MAX_LINES);
        input.setGravity(Gravity.TOP);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        input.setHint("One rule per line. Use /expression/ for regex.");
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int inset = this.style.dp(SettingsStyle.INSET_DP);
        content.setPadding(inset, 0, inset, 0);
        content.addView(
                this.style.text("Matches ignore case. Subreddit names match exactly. Refresh the feed after saving.",
                        SettingsStyle.SUMMARY_SP, this.style.secondary));
        content.addView(input);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(title)
            .setView(content)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save", null)
            .create();
        dialog.setOnShowListener((shown) -> {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setAllCaps(false);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setAllCaps(false);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener((view) -> {
                String value = input.getText().toString();
                try {
                    PatchSettings.save(ruleText(key, PatchSettings.TITLES_KEY, value),
                            ruleText(key, PatchSettings.SUBREDDITS_KEY, value),
                            ruleText(key, PatchSettings.FLAIRS_KEY, value));
                    dialog.dismiss();
                } catch (IllegalArgumentException ex) {
                    input.setError(ex.getMessage());
                }
            });
        });
        dialog.show();
    }

    private static String ruleText(String editedKey, String key, String editedText) {
        return key.equals(editedKey) ? editedText : PatchSettings.value(key);
    }

    private void confirmReset() {
        showDialog(new AlertDialog.Builder(this).setTitle("Reset to defaults?")
            .setMessage(RESET_DESCRIPTION)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Reset", (dialog, which) -> {
                PatchSettings.resetToDefaults();
                confirmRestart();
            }));
    }

    private void confirmRestart() {
        showDialog(new AlertDialog.Builder(this).setTitle("Restart required")
            .setMessage("RedReader must restart to apply the change.")
            .setNegativeButton("Later", null)
            .setPositiveButton("Restart", (dialog, which) -> {
                Intent launch = getPackageManager().getLaunchIntentForPackage(getPackageName());
                if (launch != null) {
                    startActivity(Intent.makeRestartActivityTask(launch.getComponent()));
                }
                finish();
            }));
    }

    private void showDialog(AlertDialog.Builder builder) {
        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setAllCaps(false);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setAllCaps(false);
    }

    private String appVersion() {
        try {
            String version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            return (version != null) ? version : "";
        } catch (PackageManager.NameNotFoundException ex) {
            Log.w(PatchSettings.LOG_TAG, "App version lookup failed");
            return "";
        }
    }

}
