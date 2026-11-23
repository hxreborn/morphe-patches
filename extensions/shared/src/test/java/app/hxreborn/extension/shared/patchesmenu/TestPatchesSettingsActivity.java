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

import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.LinearLayout;

public final class TestPatchesSettingsActivity extends PatchesSettingsActivityBase {

    boolean enabled = true;

    boolean optionalPatch = true;

    boolean resetSaves = true;

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
    protected void initializeSettings() {
    }

    @Override
    protected SettingsStyle createStyle() {
        return new SettingsStyle(this, false, 0, android.R.color.black, android.R.color.black, android.R.color.black,
                android.R.color.black) {
            @Override
            protected int backIconResource() {
                return 0;
            }

            @Override
            public Drawable hostIcon(int identifier, int tint) {
                return new ColorDrawable(tint);
            }
        };
    }

    @Override
    protected SettingsRows createRows(SettingsStyle style) {
        return new SettingsRows(this, style) {
            @Override
            protected void attachSwitch(LinearLayout row, boolean checked, int accent, SwitchChangeListener listener,
                    LinearLayout.LayoutParams params) {
                row.addView(new View(TestPatchesSettingsActivity.this), params);
                row.setOnClickListener((view) -> listener.onChanged(!checked));
            }
        };
    }

    private boolean isEnabled() {
        return this.enabled;
    }

    private void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    protected boolean resetToDefaults() {
        this.enabled = true;
        return this.resetSaves;
    }

    private int themeIconId() {
        return 0;
    }

    private int resetIconId() {
        return 0;
    }

    private int infoIconId() {
        return 0;
    }

    private int patchesIconId() {
        return 0;
    }

    @Override
    protected String logTag() {
        return "test";
    }

    private String bundleVersion() {
        return "1.0";
    }

    private String settingTitle() {
        return "Test setting";
    }

    private String settingDescription() {
        return "Test description";
    }

    private String primaryPatchTitle() {
        return "First patch";
    }

    private String optionalPatchTitle() {
        return "Second patch";
    }

    private String optionalPatchDescription() {
        return "Optional description";
    }

    @Override
    protected String restartMessage() {
        return "Restart test app.";
    }

    private boolean optionalPatchApplied() {
        return this.optionalPatch;
    }

}
