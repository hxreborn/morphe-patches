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
import android.graphics.Color;

public final class HostStyle {

    public static final int INSET_DP = 16;

    public static final int DIALOG_INSET_DP = 24;

    public static final int GAP_DP = 12;

    public static final int CARD_RADIUS_DP = 16;

    public static final int TOUCH_DP = 48;

    public static final int PILL_WIDTH_DP = 80;

    public static final int PILL_HEIGHT_DP = 36;

    public static final int PILL_GAP_DP = 12;

    public static final float POWER_TILE_HEIGHT_RATIO = 64f / 300f;

    public static final int VOLUME_ROW_DP = 96;

    public static final int TITLE_SP = 24;

    public static final int CARD_TITLE_SP = 20;

    public static final int PRESET_HEIGHT_DP = 80;

    public static final int BODY_SP = 17;

    public static final int CAPTION_SP = 14;

    public static final int TAB_SP = 24;

    public static final int GRAPH_HEIGHT_DP = 360;

    public static final float DISABLED_OPACITY = .38f;

    private static final int DARK_PAGE = 0xff111111;

    private static final int DARK_CARD = 0xff24242a;

    private static final int DARK_INSET = 0xff121216;

    private static final int DARK_TRACK = 0xff2f2f36;

    private static final int DARK_TEXT = 0xffd1d6dc;

    private static final int DARK_CAPTION = 0xffbec6cb;

    private static final int DARK_FILL = 0xff4a4a54;

    private static final int LIGHT_PAGE = 0xfff6f6f6;

    private static final int LIGHT_INSET = 0xfff8f8f8;

    private static final int LIGHT_TEXT = 0xff1c1c1e;

    private static final int LIGHT_CAPTION = 0xff525252;

    private static final int LIGHT_FILL = 0xffd7d7d7;

    private static final int PILL = 0xffe9e9e9;

    private static final int PILL_INK = 0xff525252;

    public final boolean dark;

    public final int background;

    public final int surface;

    public final int inactiveCard;

    public final int barTrack;

    public final int barFill;

    public final int text;

    public final int secondaryText;

    public final int accent;

    public final int selectedSurface;

    public final int ripple;

    public final int dialogTheme;

    public final int pill = PILL;

    public final int pillInk = PILL_INK;

    public HostStyle(Context context) {
        SettingsStyle settings = new SettingsStyle(context);
        EqualizerStyle controls = new EqualizerStyle(context);
        this.dark = settings.dark;
        if (settings.screenBackground == Color.BLACK) {
            this.background = Color.BLACK;
        } else {
            this.background = this.dark ? DARK_PAGE : LIGHT_PAGE;
        }
        this.surface = this.dark ? DARK_CARD : Color.WHITE;
        this.inactiveCard = this.dark ? DARK_INSET : LIGHT_INSET;
        this.barTrack = this.dark ? DARK_TRACK : PILL;
        this.barFill = this.dark ? DARK_FILL : LIGHT_FILL;
        this.text = this.dark ? DARK_TEXT : LIGHT_TEXT;
        this.secondaryText = this.dark ? DARK_CAPTION : LIGHT_CAPTION;
        this.accent = controls.buttonText;
        this.selectedSurface = AccentColor.surface(context, this.dark);
        this.ripple = controls.ripple;
        this.dialogTheme = controls.dialogTheme;
    }

}
