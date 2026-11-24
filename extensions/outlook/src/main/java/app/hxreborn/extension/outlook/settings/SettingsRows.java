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

import android.content.Context;
import android.content.res.ColorStateList;
import android.widget.LinearLayout;
import android.widget.Switch;

import app.hxreborn.extension.shared.patchesmenu.SwitchChangeListener;

final class SettingsRows extends app.hxreborn.extension.shared.patchesmenu.SettingsRows {

    private static final int SWITCH_TRACK_ALPHA = 0x80;

    private static final float DISABLED_ROW_ALPHA = 0.38f;

    private final Context context;

    private final SettingsStyle style;

    SettingsRows(Context context, SettingsStyle style) {
        super(context, style);
        this.context = context;
        this.style = style;
    }

    @Override
    protected void attachSwitch(LinearLayout row, boolean checked, int accent, SwitchChangeListener listener,
            LinearLayout.LayoutParams params) {
        Switch toggle = new Switch(this.context);
        toggle.setChecked(checked);
        toggle.setThumbTintList(switchColors(accent, this.style.secondaryText));
        toggle.setTrackTintList(switchColors(SettingsStyle.withAlpha(accent, SWITCH_TRACK_ALPHA),
                SettingsStyle.withAlpha(this.style.secondaryText, SWITCH_TRACK_ALPHA)));
        toggle.setOnCheckedChangeListener((button, enabled) -> listener.onChanged(enabled));
        params.width = LinearLayout.LayoutParams.WRAP_CONTENT;
        params.height = LinearLayout.LayoutParams.WRAP_CONTENT;
        row.addView(toggle, params);
        row.setOnClickListener((view) -> toggle.toggle());
    }

    void setRowEnabled(LinearLayout row, boolean enabled) {
        row.setEnabled(enabled);
        row.setAlpha(enabled ? 1f : DISABLED_ROW_ALPHA);
        for (int index = 0; index < row.getChildCount(); index++) {
            row.getChildAt(index).setEnabled(enabled);
        }
    }

    private static ColorStateList switchColors(int checked, int unchecked) {
        return new ColorStateList(new int[][] { { android.R.attr.state_checked }, {} },
                new int[] { checked, unchecked });
    }

}
