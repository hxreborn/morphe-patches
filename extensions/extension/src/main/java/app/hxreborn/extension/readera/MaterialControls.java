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
package app.hxreborn.extension.readera;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.widget.CompoundButton;

@SuppressWarnings("unused")
public final class MaterialControls {

    private static final int CONTROL_MIN_WIDTH_DP = 32;

    private MaterialControls() {
    }

    public static Drawable thumb(CompoundButton button) {
        return drawable(button, "switch_thumb");
    }

    public static Drawable track(CompoundButton button) {
        return drawable(button, "switch_track");
    }

    public static void styleButton(CompoundButton button, boolean radio) {
        button.setButtonDrawable(drawable(button, radio ? "radio" : "check"));
        int minimum = Math.round(CONTROL_MIN_WIDTH_DP * button.getResources().getDisplayMetrics().density);
        button.setMinWidth(Math.max(button.getMinWidth(), minimum));
        if (!radio) {
            button.setMinHeight(Math.max(button.getMinHeight(), minimum));
        }
    }

    private static Drawable drawable(CompoundButton button, String name) {
        Context context = button.getContext();
        int resource = context.getResources()
            .getIdentifier("hx_readera_control_" + name, "drawable", context.getPackageName());
        return context.getDrawable(resource).mutate();
    }

}
