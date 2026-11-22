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

    private static String metadata(String key, boolean description) {
        return null;
    }

    static boolean isApplied(String key) {
        return name(key) != null;
    }

    static String name(String key) {
        return metadata(key, false);
    }

    static String description(String key) {
        return metadata(key, true);
    }

    static List<AppliedPatch> applied() {
        List<AppliedPatch> applied = new ArrayList<>();
        add(applied, unlockPremium(), unlockPremiumName(), unlockPremiumDescription());
        add(applied, unlockCustomizations(), unlockCustomizationsName(), unlockCustomizationsDescription());
        Collections.sort(applied, (first, second) -> first.name.compareToIgnoreCase(second.name));
        return applied;
    }

    private static void add(List<AppliedPatch> applied, boolean patchApplied, String name, String description) {
        if (patchApplied) {
            applied.add(new AppliedPatch(name, description));
        }
    }

    public static boolean unlockPremium() {
        return isApplied("unlockPremium");
    }

    public static String unlockPremiumName() {
        return name("unlockPremium");
    }

    public static String unlockPremiumDescription() {
        return description("unlockPremium");
    }

    public static boolean unlockCustomizations() {
        return isApplied("unlockCustomizations");
    }

    public static String unlockCustomizationsName() {
        return name("unlockCustomizations");
    }

    public static String unlockCustomizationsDescription() {
        return description("unlockCustomizations");
    }

    static final class AppliedPatch {

        final String name;

        final String description;

        AppliedPatch(String name, String description) {
            this.name = name;
            this.description = description;
        }

    }

}
