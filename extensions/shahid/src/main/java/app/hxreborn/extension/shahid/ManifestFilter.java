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
package app.hxreborn.extension.shahid;

import java.util.regex.Pattern;

@SuppressWarnings("unused")
public final class ManifestFilter {

    private static final Pattern VIDEO_HEIGHT_RANGE = Pattern.compile("video_height:[0-9]+-[0-9]+;?");

    private static final Pattern TRAILING_SEPARATOR = Pattern.compile(";(?=&|$)");

    private ManifestFilter() {

    }

    public static String removeHeightLimit(String url) {
        if (url == null) {
            return null;
        }
        String unlimited = VIDEO_HEIGHT_RANGE.matcher(url).replaceAll("");
        return TRAILING_SEPARATOR.matcher(unlimited).replaceAll("");
    }

}
