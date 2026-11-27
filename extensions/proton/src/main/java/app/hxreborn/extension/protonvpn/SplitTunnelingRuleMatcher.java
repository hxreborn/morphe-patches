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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class SplitTunnelingRuleMatcher {

    private final Set<SplitTunnelingCategory> categories;

    private final List<String> keywords;

    SplitTunnelingRuleMatcher(Set<SplitTunnelingCategory> categories, List<String> keywords) {
        this.categories = categories;
        this.keywords = keywords;
    }

    List<SplitTunnelingApp> select(List<SplitTunnelingApp> apps) {
        final List<SplitTunnelingApp> selected = new ArrayList<>();
        for (SplitTunnelingApp app : apps) {
            if (matches(app)) {
                selected.add(app);
            }
        }
        return selected;
    }

    boolean matches(SplitTunnelingApp app) {
        for (SplitTunnelingCategory category : this.categories) {
            if (category.contains(app)) {
                return true;
            }
        }

        final String name = app.label.toLowerCase(Locale.ROOT);
        for (String keyword : this.keywords) {
            if (startsAWord(name, keyword)) {
                return true;
            }
        }
        return false;
    }

    private static boolean startsAWord(String name, String keyword) {
        int index = name.indexOf(keyword);
        while (index >= 0) {
            if (index == 0 || !Character.isLetterOrDigit(name.charAt(index - 1))) {
                return true;
            }
            index = name.indexOf(keyword, index + 1);
        }
        return false;
    }

}
