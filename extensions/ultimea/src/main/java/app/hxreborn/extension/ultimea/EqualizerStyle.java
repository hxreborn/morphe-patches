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
package app.hxreborn.extension.ultimea;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.widget.EditText;

public final class EqualizerStyle {

    private static final int RIPPLE_ALPHA = 0x22000000;

    private static final int RGB_MASK = 0x00ffffff;

    public final int background;

    public final int text;

    public final int buttonText;

    public final int dialogTheme;

    public final int ripple;

    public EqualizerStyle(Context context) {
        SettingsStyle style = new SettingsStyle(context);
        this.background = style.screenBackground;
        this.text = style.primaryText;
        this.buttonText = AccentColor.primary(context, style.dark);
        this.ripple = RIPPLE_ALPHA | (this.buttonText & RGB_MASK);
        this.dialogTheme = style.dark ? android.R.style.Theme_DeviceDefault_Dialog_Alert
                : android.R.style.Theme_DeviceDefault_Light_Dialog_Alert;
    }

    public void tint(AlertDialog dialog) {
        for (int button : new int[] { AlertDialog.BUTTON_POSITIVE, AlertDialog.BUTTON_NEGATIVE,
                AlertDialog.BUTTON_NEUTRAL }) {
            if (dialog.getButton(button) != null) {
                dialog.getButton(button).setTextColor(this.buttonText);
            }
        }
    }

    public void tint(EditText input) {
        input.setBackgroundTintList(ColorStateList.valueOf(this.buttonText));
    }

}
