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
package app.hxreborn.extension.viber;

import java.util.Locale;

enum PatchSetting {

    HIDE_ADS("Ads", "Hide ads", "hideAds", "Removes ads.", false),
    HIDE_MORE_ITEMS("More screen", "Hide More screen items", "hideMoreItems", "Removes selected entries.", false),
    HIDE_NAVIGATION_BUTTONS("Navigation bar", "Hide navigation buttons", "hideNavigationButtons",
            "Removes selected tabs.", true),
    DISABLE_ANALYTICS("Privacy", "Disable analytics", "disableAnalytics", "Stops usage tracking.", true);

    final String section;

    final String key;

    final String identifier;

    final String description;

    final boolean restart;

    PatchSetting(String section, String key, String identifier, String description, boolean restart) {
        this.section = section;
        this.key = key;
        this.identifier = identifier;
        this.description = description;
        this.restart = restart;
    }

    String title() {
        return AppliedPatches.name(this.identifier);
    }

    String icon() {
        return "hx_viber_ic_" + name().toLowerCase(Locale.ROOT);
    }

}
