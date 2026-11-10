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

import android.content.Context;

public final class EqualizerStyle {

    private static final int LIGHT_BUTTON_TEXT = 0xff541597;

    public final int background;

    public final int text;

    public final int buttonText;

    public final int dialogTheme;

    public EqualizerStyle(Context context) {
        SettingsStyle style = new SettingsStyle(context);
        this.background = style.screenBackground;
        this.text = style.primaryText;
        this.buttonText = style.dark ? style.primaryText : LIGHT_BUTTON_TEXT;
        this.dialogTheme = style.dark ? android.R.style.Theme_DeviceDefault_Dialog_Alert
                : android.R.style.Theme_DeviceDefault_Light_Dialog_Alert;
    }

}
