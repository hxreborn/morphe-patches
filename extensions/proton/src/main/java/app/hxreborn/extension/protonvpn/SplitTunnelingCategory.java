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

import android.content.pm.ApplicationInfo;

enum SplitTunnelingCategory {

    GAME("game", "Games", ApplicationInfo.CATEGORY_GAME),
    AUDIO("audio", "Music and audio", ApplicationInfo.CATEGORY_AUDIO),
    VIDEO("video", "Video", ApplicationInfo.CATEGORY_VIDEO),
    IMAGE("image", "Photos and images", ApplicationInfo.CATEGORY_IMAGE),
    SOCIAL("social", "Social", ApplicationInfo.CATEGORY_SOCIAL), NEWS("news", "News", ApplicationInfo.CATEGORY_NEWS),
    MAPS("maps", "Maps and navigation", ApplicationInfo.CATEGORY_MAPS),
    PRODUCTIVITY("productivity", "Productivity", ApplicationInfo.CATEGORY_PRODUCTIVITY),
    BROWSER("browser", "Browsers", ApplicationInfo.CATEGORY_UNDEFINED);

    final String id;

    final String label;

    private final int applicationCategory;

    SplitTunnelingCategory(String id, String label, int applicationCategory) {
        this.id = id;
        this.label = label;
        this.applicationCategory = applicationCategory;
    }

    boolean contains(SplitTunnelingApp app) {
        if (this == BROWSER) {
            return app.browser;
        }
        return app.category == this.applicationCategory;
    }

}
