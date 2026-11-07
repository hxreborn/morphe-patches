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
package app.hxreborn.extension.terabox;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@Implements(value = SettingsStyle.class, isInAndroidSdk = false)
public final class FakeHostResources {

    public static final int PAGE_BACKGROUND = android.R.color.black;

    public static final int CARD_BACKGROUND = android.R.color.darker_gray;

    public static final int DIVIDER = android.R.color.holo_blue_light;

    public static final int PRIMARY_TEXT = android.R.color.white;

    public static final int SECONDARY_TEXT = android.R.color.tertiary_text_dark;

    @Implementation
    public static int pageBackgroundColor() {
        return PAGE_BACKGROUND;
    }

    @Implementation
    public static int cardBackgroundColor() {
        return CARD_BACKGROUND;
    }

    @Implementation
    public static int dividerColor() {
        return DIVIDER;
    }

    @Implementation
    public static int primaryTextColor() {
        return PRIMARY_TEXT;
    }

    @Implementation
    public static int secondaryTextColor() {
        return SECONDARY_TEXT;
    }

    @Implementation
    public static int switchButtonDrawable() {
        return android.R.drawable.ic_dialog_alert;
    }

    @Implementation
    public static int backIconDrawable() {
        return android.R.drawable.ic_menu_revert;
    }

}
