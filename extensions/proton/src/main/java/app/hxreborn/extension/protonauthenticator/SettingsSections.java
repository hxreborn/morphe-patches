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
package app.hxreborn.extension.protonauthenticator;

import java.util.Collections;
import java.util.List;

import app.hxreborn.extension.proton.PatchSettings;

@SuppressWarnings("unused")
public final class SettingsSections {

    public static final String SPREAD_THE_WORD = "hide_spread_the_word";

    public static final String DISCOVER_PROTON = "hide_discover_proton";

    public static final String PASS_BANNER = "hide_pass_banner";

    private SettingsSections() {

    }

    public static boolean isPatched() {
        return false;
    }

    public static boolean isDiscoverProtonHidden() {
        return isHidden(DISCOVER_PROTON);
    }

    public static boolean isPassBannerHidden() {
        return isHidden(PASS_BANNER);
    }

    public static List<?> spreadTheWordRows(List<?> rows) {
        return isHidden(SPREAD_THE_WORD) ? Collections.emptyList() : rows;
    }

    private static boolean isHidden(String key) {
        return isPatched() && PatchSettings.isEnabled(key, false);
    }

}
