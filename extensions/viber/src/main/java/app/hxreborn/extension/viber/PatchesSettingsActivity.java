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
package app.hxreborn.extension.viber;

import java.util.List;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;

import app.hxreborn.extension.shared.patchesmenu.PatchesMenuText;
import app.hxreborn.extension.shared.patchesmenu.PatchesSettingsActivityBase;
import app.hxreborn.extension.viber.AppliedPatches.AppliedPatch;

@SuppressLint("SetTextI18n")
@SuppressWarnings("unused")
public final class PatchesSettingsActivity extends PatchesSettingsActivityBase {

    private static final int CONTENT_BOTTOM_PADDING_DP = 24;

    private SettingsStyle style;

    private SettingsRows rows;

    private BusinessChatSelection businessChatSelection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(createScreen());
    }

    @Override
    protected int settingsTheme() {
        return SettingsStyle.theme();
    }

    @Override
    protected boolean showScreenOnCreate() {
        return false;
    }

    @Override
    protected void initializeSettings() {
        PatchSettings.initialize(this);
        this.businessChatSelection = new BusinessChatSelection(this, this::confirmRestart);
    }

    @Override
    protected SettingsStyle createStyle() {
        this.style = new SettingsStyle(this);
        return this.style;
    }

    @Override
    protected SettingsRows createRows(app.hxreborn.extension.shared.patchesmenu.SettingsStyle style) {
        this.rows = new SettingsRows(this, (SettingsStyle) style);
        return this.rows;
    }

    @Override
    protected boolean resetToDefaults() {
        return PatchSettings.resetToDefaults();
    }

    @Override
    protected String restartMessage() {
        return "Viber must restart to apply the change.";
    }

    @Override
    protected String logTag() {
        return PatchSettings.LOG_TAG;
    }

    @SuppressLint("InlinedApi")
    @SuppressWarnings("deprecation")
    @Override
    protected void styleSystemBars(Window window) {
        window.setStatusBarColor(this.style.screenBackground);
        window.setNavigationBarColor(this.style.screenBackground);
        window.getDecorView()
            .setSystemUiVisibility(this.style.dark ? 0
                    : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected WindowInsets applySystemBarInsets(View view, WindowInsets insets) {
        view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
        return insets.consumeSystemWindowInsets();
    }

    @Override
    protected View createContent() {
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(this.style.dp(SettingsStyle.SCREEN_INSET_DP), 0, this.style.dp(SettingsStyle.SCREEN_INSET_DP),
                this.style.dp(CONTENT_BOTTOM_PADDING_DP));

        String section = null;
        LinearLayout card = null;
        for (PatchSetting setting : PatchSetting.values()) {
            if (!setting.applied()) {
                continue;
            }
            if (!setting.section.equals(section)) {
                if (card != null) {
                    addSection(column, section, card);
                }
                section = setting.section;
                card = this.rows.card();
            }
            addSetting(card, setting);
        }
        if (card != null) {
            addSection(column, section, card);
        }
        LinearLayout about = this.rows.card();
        this.rows.actionRow(about, "hx_viber_ic_reset", "Reset to defaults", PatchesMenuText.RESET_DESCRIPTION,
                this::confirmReset);
        this.rows.valueRow(about, "hx_viber_ic_info", "App version", appVersion());
        this.rows.valueRow(about, "hx_viber_ic_patches", "Patch bundle", BuildConfig.BUNDLE_VERSION);
        addSection(column, "About", about);
        LinearLayout applied = this.rows.card();
        for (AppliedPatch patch : AppliedPatches.applied()) {
            this.rows.plainRow(applied, patch.name, patch.description);
        }
        addSection(column, "Applied patches", applied);
        return column;
    }

    private void addSetting(LinearLayout card, PatchSetting setting) {
        if (setting.choice) {
            this.rows.actionRow(card, setting.icon(), setting.title(), setting.description(),
                    () -> showStartFolderSelection(setting));
            return;
        }
        this.rows.switchRow(card, setting, PatchSettings.isEnabled(setting.key), (button, enabled) -> {
            PatchSettings.setEnabled(setting.key, enabled);
            if (setting.restart) {
                confirmRestart();
            }
        });
        if (setting == PatchSetting.FILTER_BUSINESS_CHATS) {
            this.rows.actionRow(card, "hx_viber_ic_select_business_chats", "Business chats",
                    "Choose chats to filter after reading.", this.businessChatSelection::show);
        }
    }

    @SuppressWarnings("DataFlowIssue")
    private void showStartFolderSelection(PatchSetting setting) {
        List<Object> choices = StartFolder.folders();
        if (choices.isEmpty()) {
            showDialog(new AlertDialog.Builder(this, dialogTheme()).setTitle(setting.title())
                .setMessage("Open Chats to load folders, then try again.")
                .setPositiveButton("OK", null));
            return;
        }
        String[] names = new String[choices.size()];
        for (int index = 0; index < choices.size(); index++) {
            names[index] = StartFolder.folderTitle(choices.get(index), getResources());
        }
        showDialog(new AlertDialog.Builder(this, dialogTheme()).setTitle(setting.title())
            .setSingleChoiceItems(names, StartFolder.savedFolderIndexOrAll(), (dialog, index) -> {
                String folderId = StartFolder.folderId(choices.get(index));
                boolean changed = !folderId.equals(PatchSettings.startFolder());
                PatchSettings.setStartFolder(folderId);
                dialog.dismiss();
                if (changed && setting.restart) {
                    confirmRestart();
                }
            })
            .setNegativeButton("Cancel", null));
    }

    private void addSection(LinearLayout column, String title, LinearLayout card) {
        if (card.getChildCount() == 0) {
            return;
        }
        column.addView(this.rows.sectionHeader(title));
        column.addView(card);
    }

    @Override
    protected AlertDialog showDialog(AlertDialog.Builder builder) {
        AlertDialog dialog = builder.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setAllCaps(false);
        Button negative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        if (negative != null) {
            negative.setAllCaps(false);
        }
        return dialog;
    }

}
