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
package app.hxreborn.extension.bittorrent;

import android.content.Context;

final class SettingsStyle extends app.hxreborn.extension.shared.torrentmenu.SettingsStyle {

    SettingsStyle(Context context) {
        this(context, PatchSettings.isDark(context));
    }

    private SettingsStyle(Context context, boolean dark) {
        super(context, dark, dark ? darkBackgroundId() : 0, dark ? darkDividerId() : lightDividerId(),
                dark ? darkTitleId() : lightTitleId(), dark ? darkLabelId() : lightLabelId(),
                dark ? darkAccentId() : lightAccentId());
    }

    @Override
    protected int backIconResource() {
        return backIconId();
    }

    static int themeIconId() {
        return 0;
    }

    static int resetIconId() {
        return 0;
    }

    static int infoIconId() {
        return 0;
    }

    static int patchesIconId() {
        return 0;
    }

    static int backIconId() {
        return 0;
    }

    static int darkBackgroundId() {
        return 0;
    }

    static int darkDividerId() {
        return 0;
    }

    static int lightDividerId() {
        return 0;
    }

    static int darkTitleId() {
        return 0;
    }

    static int lightTitleId() {
        return 0;
    }

    static int darkLabelId() {
        return 0;
    }

    static int lightLabelId() {
        return 0;
    }

    static int darkAccentId() {
        return 0;
    }

    static int lightAccentId() {
        return 0;
    }

}
