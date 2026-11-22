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
package app.hxreborn.extension.bereal.header;

import app.hxreborn.extension.bereal.PatchSettings;

@SuppressWarnings("unused")
public final class BeFakeHeader {

    private BeFakeHeader() {
    }

    public static boolean isPatchIncluded() {
        return false;
    }

    public static String patchName() {
        return null;
    }

    public static String patchDescription() {
        return null;
    }

    public static int selectPostingLogo(int stockLogoId) {
        return PatchSettings.CUSTOM_BEFAKE_HEADER.get() ? postingLogoId() : stockLogoId;
    }

    public static int selectTopBarLogo(int stockLogoId) {
        return PatchSettings.CUSTOM_BEFAKE_HEADER.get() ? topBarLogoId() : stockLogoId;
    }

    private static int topBarLogoId() {
        return 0;
    }

    private static int postingLogoId() {
        return 0;
    }

}
