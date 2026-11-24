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
package app.hxreborn.extension.outlook.settings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
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

import app.hxreborn.extension.outlook.AppliedPatches;
import app.hxreborn.extension.outlook.BuildConfig;
import app.hxreborn.extension.shared.patchesmenu.PatchesMenuText;
import app.hxreborn.extension.shared.patchesmenu.PatchesSettingsDialogBase;

@SuppressWarnings({ "deprecation", "NullableProblems" })
final class PatchesSettingsDialog extends PatchesSettingsDialogBase implements View.OnAttachStateChangeListener {

    private static final int APP_BAR_HEIGHT_DP = 64;

    private static final int APP_BAR_START_PADDING_DP = 4;

    private static final int BACK_TOUCH_TARGET_DP = 48;

    private static final int BACK_ICON_INSET_DP = 12;

    private static final int TITLE_START_PADDING_DP = 12;

    private static final int CONTENT_BOTTOM_PADDING_DP = 24;

    private static final String KEEP_MESSAGES_UNREAD_SUMMARY = "Opening a message, or flagging or moving it from "
            + "the reading pane, leaves it unread.";

    private static final String REMOVE_INBOX_ADS_SUMMARY = "Removes sponsored messages from Focused and Other.";

    private static final String OPEN_EMAIL_TEMPLATES_SUMMARY = "Opens .oft template attachments as new drafts, "
            + "without Microsoft 365.";

    private static final String SELECT_MULTIPLE_SEARCH_RESULTS_SUMMARY = "Long-press a result in the Mail tab of "
            + "search to select several, then mark, move or delete them together.";

    private static final String HIDE_COPILOT_SUMMARY = "Hides the Copilot tab, inbox card, Get a Summary, Help me "
            + "write and Schedule with Copilot.";

    private static final String ADD_UNREAD_SHORTCUT_SUMMARY = "Long-press the Outlook icon, then tap Unread.";

    private final Activity activity;

    private final SettingsStyle style;

    private final SettingsRows rows;

    private final Dialog dialog;

    private final ScrollView scroll;

    PatchesSettingsDialog(Activity activity) {
        this(activity, new SettingsStyle(activity));
    }

    private PatchesSettingsDialog(Activity activity, SettingsStyle style) {
        super(activity, new Dialog(activity, android.R.style.Theme_DeviceDefault_NoActionBar));
        this.activity = activity;
        this.style = style;
        this.rows = new SettingsRows(activity, style);
        this.dialog = dialog();
        this.scroll = new ScrollView(activity);
        this.dialog.setOwnerActivity(activity);
        this.dialog.setOnDismissListener(
                (dismissed) -> this.activity.getWindow().getDecorView().removeOnAttachStateChangeListener(this));
    }

    @SuppressWarnings("DataFlowIssue")
    void show() {
        View screen = createScreen(appBar(), this.scroll);
        refreshContent(this.scroll);
        screen.setBackgroundColor(this.style.screenBackground);
        screen.setOnApplyWindowInsetsListener(PatchesSettingsDialog::applySystemBarInsets);
        this.dialog.setContentView(screen);
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
        back.setImageDrawable(this.style.backIcon(this.style.primaryText));
        back.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        int inset = this.style.dp(BACK_ICON_INSET_DP);
        back.setPadding(inset, inset, inset, inset);
        back.setContentDescription("Back");
        back.setBackground(this.style.ripple(BACK_TOUCH_TARGET_DP / 2));
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

    @Override
    protected View createContent() {
        LinearLayout column = new LinearLayout(this.activity);
        column.setOrientation(LinearLayout.VERTICAL);
        int inset = this.style.dp(SettingsStyle.SCREEN_INSET_DP);
        column.setPadding(inset, 0, inset, this.style.dp(CONTENT_BOTTOM_PADDING_DP));

        LinearLayout mail = this.rows.card();
        if (AppliedPatches.isApplied(AppliedPatches.KEEP_MESSAGES_UNREAD)) {
            this.rows.switchRow(mail, SettingsStyle.unreadIconId(),
                    AppliedPatches.name(AppliedPatches.KEEP_MESSAGES_UNREAD), KEEP_MESSAGES_UNREAD_SUMMARY,
                    PatchSettings.isEnabled(PatchSettings.KEEP_MESSAGES_UNREAD),
                    (enabled) -> PatchSettings.setEnabled(PatchSettings.KEEP_MESSAGES_UNREAD, enabled));
        }
        if (AppliedPatches.isApplied(AppliedPatches.REMOVE_INBOX_ADS)) {
            this.rows.switchRow(mail, SettingsStyle.adsIconId(), AppliedPatches.name(AppliedPatches.REMOVE_INBOX_ADS),
                    REMOVE_INBOX_ADS_SUMMARY, PatchSettings.isEnabled(PatchSettings.REMOVE_INBOX_ADS), (enabled) -> {
                        PatchSettings.setEnabled(PatchSettings.REMOVE_INBOX_ADS, enabled);
                        confirmRestart();
                    });
        }
        if (AppliedPatches.isApplied(AppliedPatches.OPEN_EMAIL_TEMPLATES)) {
            this.rows.switchRow(mail, SettingsStyle.templatesIconId(),
                    AppliedPatches.name(AppliedPatches.OPEN_EMAIL_TEMPLATES), OPEN_EMAIL_TEMPLATES_SUMMARY,
                    PatchSettings.isEnabled(PatchSettings.OPEN_EMAIL_TEMPLATES),
                    (enabled) -> PatchSettings.setEnabled(PatchSettings.OPEN_EMAIL_TEMPLATES, enabled));
        }
        addSection(column, "Mail", mail);

        LinearLayout search = this.rows.card();
        if (AppliedPatches.isApplied(AppliedPatches.SELECT_MULTIPLE_SEARCH_RESULTS)) {
            this.rows.switchRow(search, SettingsStyle.searchIconId(),
                    AppliedPatches.name(AppliedPatches.SELECT_MULTIPLE_SEARCH_RESULTS),
                    SELECT_MULTIPLE_SEARCH_RESULTS_SUMMARY,
                    PatchSettings.isEnabled(PatchSettings.SELECT_MULTIPLE_SEARCH_RESULTS),
                    (enabled) -> PatchSettings.setEnabled(PatchSettings.SELECT_MULTIPLE_SEARCH_RESULTS, enabled));
        }
        addSection(column, "Search", search);

        LinearLayout app = this.rows.card();
        if (AppliedPatches.isApplied(AppliedPatches.HIDE_COPILOT)) {
            this.rows.switchRow(app, SettingsStyle.copilotIconId(), AppliedPatches.name(AppliedPatches.HIDE_COPILOT),
                    HIDE_COPILOT_SUMMARY, PatchSettings.isEnabled(PatchSettings.HIDE_COPILOT), (enabled) -> {
                        PatchSettings.setEnabled(PatchSettings.HIDE_COPILOT, enabled);
                        confirmRestart();
                    });
        }
        if (AppliedPatches.isApplied(AppliedPatches.ADD_UNREAD_SHORTCUT)) {
            this.rows.switchRow(app, SettingsStyle.shortcutIconId(),
                    AppliedPatches.name(AppliedPatches.ADD_UNREAD_SHORTCUT), ADD_UNREAD_SHORTCUT_SUMMARY,
                    PatchSettings.isEnabled(PatchSettings.ADD_UNREAD_SHORTCUT), (enabled) -> {
                        PatchSettings.setEnabled(PatchSettings.ADD_UNREAD_SHORTCUT, enabled);
                        PatchSettings.updateUnreadShortcut(this.activity);
                    });
        }
        addSection(column, "App", app);

        LinearLayout about = this.rows.card();
        this.rows.actionRow(about, SettingsStyle.resetIconId(), "Reset to defaults", PatchesMenuText.RESET_DESCRIPTION,
                this::confirmReset);
        this.rows.valueRow(about, SettingsStyle.infoIconId(), "App version", appVersion());
        this.rows.valueRow(about, SettingsStyle.patchesIconId(), "Patch bundle", BuildConfig.BUNDLE_VERSION);
        addSection(column, "About", about);

        LinearLayout applied = this.rows.card();
        for (String key : appliedKeys()) {
            this.rows.patchRow(applied, AppliedPatches.name(key), AppliedPatches.description(key));
        }
        addSection(column, "Applied patches", applied);
        return column;
    }

    private static List<String> appliedKeys() {
        List<String> keys = new ArrayList<>();
        for (String key : AppliedPatches.keys()) {
            if (AppliedPatches.isApplied(key)) {
                keys.add(key);
            }
        }
        Collections.sort(keys,
                (first, second) -> AppliedPatches.name(first).compareToIgnoreCase(AppliedPatches.name(second)));
        return keys;
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
            return "";
        }
    }

    private void confirmReset() {
        AlertDialog confirmation = new AlertDialog.Builder(this.activity).setTitle("Reset to defaults?")
            .setMessage(PatchesMenuText.RESET_DESCRIPTION)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Reset", (dialog, which) -> {
                boolean saved = PatchSettings.resetToDefaults();
                PatchSettings.updateUnreadShortcut(this.activity);
                refreshContent(this.scroll);
                if (saved) {
                    confirmRestart();
                }
            })
            .create();
        showConfirmation(confirmation);
    }

    private void confirmRestart() {
        AlertDialog confirmation = new AlertDialog.Builder(this.activity).setTitle("Restart required")
            .setMessage("Outlook must restart to apply the change.")
            .setNegativeButton("Later", null)
            .setPositiveButton("Restart", (dialog, which) -> restartApp())
            .create();
        showConfirmation(confirmation);
    }

    private void showConfirmation(AlertDialog confirmation) {
        confirmation.show();
        confirmation.getButton(AlertDialog.BUTTON_POSITIVE).setAllCaps(false);
        confirmation.getButton(AlertDialog.BUTTON_NEGATIVE).setAllCaps(false);
    }

    private void restartApp() {
        Intent launch = this.activity.getPackageManager().getLaunchIntentForPackage(this.activity.getPackageName());
        if (launch == null) {
            Log.w(PatchSettings.LOG_TAG, "Launch intent lookup failed for " + this.activity.getPackageName());
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
