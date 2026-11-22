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
package app.hxreborn.extension.shared.torrentmenu;

import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.LinearLayout;

public final class TestPatchesSettingsActivity extends PatchesSettingsActivityBase {

    boolean enabled = true;

    boolean optionalPatch = true;

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

    @Override
    protected boolean isEnabled() {
        return this.enabled;
    }

    @Override
    protected void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    protected void resetToDefaults() {
        this.enabled = true;
    }

    @Override
    protected int themeIconId() {
        return 0;
    }

    @Override
    protected int resetIconId() {
        return 0;
    }

    @Override
    protected int infoIconId() {
        return 0;
    }

    @Override
    protected int patchesIconId() {
        return 0;
    }

    @Override
    protected String logTag() {
        return "test";
    }

    @Override
    protected String bundleVersion() {
        return "1.0";
    }

    @Override
    protected String settingTitle() {
        return "Test setting";
    }

    @Override
    protected String settingDescription() {
        return "Test description";
    }

    @Override
    protected String primaryPatchTitle() {
        return "First patch";
    }

    @Override
    protected String optionalPatchTitle() {
        return "Second patch";
    }

    @Override
    protected String optionalPatchDescription() {
        return "Optional description";
    }

    @Override
    protected String restartMessage() {
        return "Restart test app.";
    }

    @Override
    protected boolean optionalPatchApplied() {
        return this.optionalPatch;
    }

}
