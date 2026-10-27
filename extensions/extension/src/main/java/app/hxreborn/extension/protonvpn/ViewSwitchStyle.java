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
package app.hxreborn.extension.protonvpn;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;

import app.hxreborn.extension.proton.AccentColor;
import app.hxreborn.extension.proton.PatchesTheme;
import app.hxreborn.extension.proton.SwitchStyle;

@SuppressWarnings("unused")
public final class ViewSwitchStyle {

    private ViewSwitchStyle() {

    }

    public static Drawable thumb(View view) {
        final Context context = view.getContext();
        return SwitchStyle.thumb(context, AccentColor.getAccentColor(PatchesTheme.isAppNightMode(context)));
    }

    public static Drawable track(View view) {
        final Context context = view.getContext();
        return SwitchStyle.track(context, AccentColor.getAccentColor(PatchesTheme.isAppNightMode(context)));
    }

}
