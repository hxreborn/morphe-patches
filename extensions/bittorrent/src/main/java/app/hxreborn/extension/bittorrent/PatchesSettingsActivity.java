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
package app.hxreborn.extension.bittorrent;

import app.hxreborn.extension.shared.torrentmenu.PatchesSettingsActivityBase;

@SuppressWarnings("unused")
public final class PatchesSettingsActivity extends PatchesSettingsActivityBase {

    @Override
    protected void initializeSettings() {
        PatchSettings.initialize(this);
    }

    @Override
    protected app.hxreborn.extension.shared.torrentmenu.SettingsStyle createStyle() {
        return new SettingsStyle(this);
    }

    @Override
    protected app.hxreborn.extension.shared.torrentmenu.SettingsRows createRows(
            app.hxreborn.extension.shared.torrentmenu.SettingsStyle style) {
        return new SettingsRows(this, (SettingsStyle) style);
    }

    @Override
    protected boolean isEnabled() {
        return PatchSettings.isEnabled();
    }

    @Override
    protected void setEnabled(boolean enabled) {
        PatchSettings.setEnabled(enabled);
    }

    @Override
    protected void resetToDefaults() {
        PatchSettings.resetToDefaults();
    }

    @Override
    protected int themeIconId() {
        return SettingsStyle.themeIconId();
    }

    @Override
    protected int resetIconId() {
        return SettingsStyle.resetIconId();
    }

    @Override
    protected int infoIconId() {
        return SettingsStyle.infoIconId();
    }

    @Override
    protected int patchesIconId() {
        return SettingsStyle.patchesIconId();
    }

    @Override
    protected String logTag() {
        return PatchSettings.LOG_TAG;
    }

    @Override
    protected String bundleVersion() {
        return BuildConfig.BUNDLE_VERSION;
    }

    @Override
    protected String settingTitle() {
        return "Prefer AMOLED in dark mode";
    }

    @Override
    protected String primaryPatchTitle() {
        return "AMOLED dark theme";
    }

    @Override
    protected String optionalPatchTitle() {
        return "Unlock premium";
    }

    @Override
    protected String settingDescription() {
        return "Makes dialogs and cards pure black.";
    }

    @Override
    protected String optionalPatchDescription() {
        return "Unlocks Pro settings and removes ads.";
    }

    @Override
    protected String restartMessage() {
        return "BitTorrent must restart to apply the change.";
    }

    @Override
    protected boolean optionalPatchApplied() {
        return hasPremiumPatch();
    }

    private static boolean hasPremiumPatch() {
        return false;
    }

}
