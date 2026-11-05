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
package app.hxreborn.extension.liftoff;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("unused")
public final class AppliedPatches {

    static final String UNLOCK_CUSTOMIZATIONS = "Unlock customizations";

    static final String UNLOCK_PREMIUM = "Unlock premium";

    private static final List<String> NAMES = Collections
        .unmodifiableList(Arrays.asList(UNLOCK_CUSTOMIZATIONS, UNLOCK_PREMIUM));

    private AppliedPatches() {
    }

    static List<String> names() {
        List<String> names = new ArrayList<>();
        for (String name : NAMES) {
            if (description(name) != null) {
                names.add(name);
            }
        }
        return names;
    }

    static String description(String name) {
        switch (name) {
            case UNLOCK_CUSTOMIZATIONS:
                return unlockCustomizationsDescription();
            case UNLOCK_PREMIUM:
                return unlockPremiumDescription();
            default:
                return null;
        }
    }

    public static boolean unlockPremium() {
        return unlockPremiumDescription() != null;
    }

    public static String unlockPremiumDescription() {
        return null;
    }

    public static boolean unlockCustomizations() {
        return unlockCustomizationsDescription() != null;
    }

    public static String unlockCustomizationsDescription() {
        return null;
    }

}
