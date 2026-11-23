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
package app.hxreborn.extension.utorrent;

import android.view.View;
import android.widget.LinearLayout;

import app.hxreborn.extension.shared.patchesmenu.PatchesMenuText;
import app.hxreborn.extension.shared.patchesmenu.PatchesSettingsActivityBase;

@SuppressWarnings("unused")
public final class PatchesSettingsActivity extends PatchesSettingsActivityBase {

    private static final int CONTENT_BOTTOM_PADDING_DP = 24;

    @Override
    protected View createContent() {
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(style().dp(app.hxreborn.extension.shared.patchesmenu.SettingsStyle.SCREEN_INSET_DP), 0,
                style().dp(app.hxreborn.extension.shared.patchesmenu.SettingsStyle.SCREEN_INSET_DP),
                style().dp(CONTENT_BOTTOM_PADDING_DP));

        LinearLayout appearance = rows().card();
        rows().switchRow(appearance, themeIconId(), settingTitle(), settingDescription(), isEnabled(), (enabled) -> {
            setEnabled(enabled);
            confirmRestart();
        });
        addSection(column, "Appearance", appearance);

        LinearLayout about = rows().card();
        rows().actionRow(about, resetIconId(), "Reset to defaults", PatchesMenuText.RESET_DESCRIPTION,
                this::confirmReset);
        rows().valueRow(about, infoIconId(), "App version", appVersion());
        rows().valueRow(about, patchesIconId(), "Patch bundle", bundleVersion());
        addSection(column, "About", about);

        LinearLayout applied = rows().card();
        rows().patchRow(applied, primaryPatchTitle(), settingDescription());
        if (optionalPatchApplied()) {
            rows().patchRow(applied, optionalPatchTitle(), optionalPatchDescription());
        }
        addSection(column, "Applied patches", applied);
        return column;
    }

    private void addSection(LinearLayout column, String title, LinearLayout card) {
        if (card.getChildCount() == 0) {
            return;
        }
        column.addView(rows().sectionHeader(title));
        column.addView(card);
    }

    @Override
    protected int settingsTheme() {
        return menuTheme();
    }

    static int menuTheme() {
        return 0;
    }

    @Override
    protected void initializeSettings() {
        PatchSettings.initialize(this);
    }

    @Override
    protected app.hxreborn.extension.shared.patchesmenu.SettingsStyle createStyle() {
        return new SettingsStyle(this);
    }

    @Override
    protected app.hxreborn.extension.shared.patchesmenu.SettingsRows createRows(
            app.hxreborn.extension.shared.patchesmenu.SettingsStyle style) {
        return new SettingsRows(this, (SettingsStyle) style);
    }

    private boolean isEnabled() {
        return PatchSettings.isEnabled();
    }

    private void setEnabled(boolean enabled) {
        PatchSettings.setEnabled(enabled);
    }

    @Override
    protected boolean resetToDefaults() {
        return PatchSettings.resetToDefaults();
    }

    private int themeIconId() {
        return SettingsStyle.themeIconId();
    }

    private int resetIconId() {
        return SettingsStyle.resetIconId();
    }

    private int infoIconId() {
        return SettingsStyle.infoIconId();
    }

    private int patchesIconId() {
        return SettingsStyle.patchesIconId();
    }

    @Override
    protected String logTag() {
        return PatchSettings.LOG_TAG;
    }

    private String bundleVersion() {
        return BuildConfig.BUNDLE_VERSION;
    }

    private String settingTitle() {
        return "Prefer AMOLED in dark mode";
    }

    private String primaryPatchTitle() {
        return "AMOLED dark theme";
    }

    private String optionalPatchTitle() {
        return "Unlock premium";
    }

    private String settingDescription() {
        return "Makes dark surfaces pure black.";
    }

    private String optionalPatchDescription() {
        return "Unlocks Pro features and removes ads.";
    }

    @Override
    protected String restartMessage() {
        return "µTorrent must restart to apply the change.";
    }

    private boolean optionalPatchApplied() {
        return hasPremiumPatch();
    }

    private static boolean hasPremiumPatch() {
        return false;
    }

}
