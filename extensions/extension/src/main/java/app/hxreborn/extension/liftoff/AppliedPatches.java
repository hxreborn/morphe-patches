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
import java.util.Collections;
import java.util.List;

@SuppressWarnings("unused")
public final class AppliedPatches {

    static final String UNLOCK_CUSTOMIZATIONS = "Unlock customizations";

    static final String UNLOCK_PREMIUM = "Unlock premium";

    private AppliedPatches() {
    }

    static List<String> identifiers() {
        List<String> identifiers = new ArrayList<>();
        if (unlockPremium()) {
            identifiers.add("unlockPremium");
        }
        if (unlockCustomizations()) {
            identifiers.add("unlockCustomizations");
        }
        Collections.sort(identifiers, (first, second) -> name(first).compareToIgnoreCase(name(second)));
        return identifiers;
    }

    static String name(String identifier) {
        switch (identifier) {
            case "unlockCustomizations":
                return unlockCustomizationsName();
            case "unlockPremium":
                return unlockPremiumName();
            default:
                return null;
        }
    }

    static String description(String identifier) {
        switch (identifier) {
            case "unlockCustomizations":
                return unlockCustomizationsDescription();
            case "unlockPremium":
                return unlockPremiumDescription();
            default:
                return null;
        }
    }

    public static boolean unlockPremium() {
        return false;
    }

    public static String unlockPremiumName() {
        return null;
    }

    public static String unlockPremiumDescription() {
        return null;
    }

    public static boolean unlockCustomizations() {
        return false;
    }

    public static String unlockCustomizationsName() {
        return null;
    }

    public static String unlockCustomizationsDescription() {
        return null;
    }

}
