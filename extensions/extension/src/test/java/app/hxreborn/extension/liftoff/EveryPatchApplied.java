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

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@Implements(AppliedPatches.class)
public final class EveryPatchApplied {

    static final String PREMIUM_DESCRIPTION = "Unlocks rank analysis, bodygraph and nutrition tools. Requires an account.";

    static final String CUSTOMIZATIONS_DESCRIPTION = "Unlocks every theme, border and banner.";

    @Implementation
    protected static boolean unlockPremium() {
        return true;
    }

    @Implementation
    protected static boolean unlockCustomizations() {
        return true;
    }

    @Implementation
    protected static String unlockPremiumName() {
        return "Unlock premium";
    }

    @Implementation
    protected static String unlockPremiumDescription() {
        return PREMIUM_DESCRIPTION;
    }

    @Implementation
    protected static String unlockCustomizationsName() {
        return "Unlock customizations";
    }

    @Implementation
    protected static String unlockCustomizationsDescription() {
        return CUSTOMIZATIONS_DESCRIPTION;
    }

}
