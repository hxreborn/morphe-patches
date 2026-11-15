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

import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

@SuppressWarnings("unused")
public final class AmoledTheme {

    private AmoledTheme() {
    }

    public static void lowPowerBanner(View banner) {
        PatchSettings.initialize(banner.getContext());
        if (!PatchSettings.isDark(banner.getContext())) {
            return;
        }
        boolean amoled = PatchSettings.isEnabled();
        if (amoled) {
            banner.setBackgroundColor(Color.BLACK);
        }
        setBannerText(banner, amoled ? Color.WHITE : Color.BLACK);
    }

    private static void setBannerText(View view, int color) {
        if (view instanceof TextView text) {
            text.setTextColor(color);
        }
        if (view instanceof ViewGroup group) {
            for (int index = 0; index < group.getChildCount(); index++) {
                setBannerText(group.getChildAt(index), color);
            }
        }
    }

    public static int resourceId(int original) {
        return original;
    }

}
