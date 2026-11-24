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
package app.hxreborn.extension.outlook.theme;

import java.util.Locale;
import java.util.regex.Pattern;

final class HexColor {

    static final int INVALID = 0;

    private static final Pattern RGB_HEX = Pattern.compile("#?[0-9a-fA-F]{6}");

    private static final String PREFIX = "#";

    private static final int HEX_RADIX = 16;

    private static final int OPAQUE = 0xFF000000;

    private static final int RGB_MASK = 0x00FFFFFF;

    private HexColor() {
    }

    static int parse(String text) {
        String trimmed = text.trim();
        if (!RGB_HEX.matcher(trimmed).matches()) {
            return INVALID;
        }
        String digits = trimmed.startsWith(PREFIX) ? trimmed.substring(PREFIX.length()) : trimmed;
        return OPAQUE | Integer.parseInt(digits, HEX_RADIX);
    }

    static String format(int color) {
        return String.format(Locale.ROOT, "#%06X", color & RGB_MASK);
    }

}
