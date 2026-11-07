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
package app.hxreborn.extension.duolingo;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@Implements(value = SettingsStyle.class, isInAndroidSdk = false)
public final class FakeHostResources {

    @Implementation
    public static int snowColor() {
        return android.R.color.white;
    }

    @Implementation
    public static int polarColor() {
        return android.R.color.darker_gray;
    }

    @Implementation
    public static int swanColor() {
        return android.R.color.tertiary_text_light;
    }

    @Implementation
    public static int eelColor() {
        return android.R.color.black;
    }

    @Implementation
    public static int wolfColor() {
        return android.R.color.secondary_text_light;
    }

    @Implementation
    public static int macawColor() {
        return android.R.color.holo_blue_light;
    }

    @Implementation
    public static int whaleColor() {
        return android.R.color.holo_blue_dark;
    }

    @Implementation
    public static int boldFont() {
        return 0;
    }

    @Implementation
    public static int regularFont() {
        return 0;
    }

    @Implementation
    public static int backIconDrawable() {
        return android.R.drawable.ic_menu_revert;
    }

}
