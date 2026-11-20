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
package app.hxreborn.extension.proton;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public final class AppliedPatches {

    private AppliedPatches() {

    }

    public static boolean accentColor() {
        return AccentColor.isPatched();
    }

    public static boolean amoledDarkTheme() {
        return AmoledTheme.isPatched();
    }

    public static boolean hideUpgradePromotions() {
        return false;
    }

    public static boolean loadingIndicator() {
        return false;
    }

    public static boolean floatingNavigationBar() {
        return false;
    }

    static List<String> names() {
        final List<String> names = new ArrayList<>();
        if (accentColor()) {
            names.add(accentColorName());
        }
        if (amoledDarkTheme()) {
            names.add(amoledDarkThemeName());
        }
        if (hideUpgradePromotions()) {
            names.add(hideUpgradePromotionsName());
        }
        if (MaterialSwitches.isPatched()) {
            names.add(materialSwitchesName());
        }
        if (loadingIndicator()) {
            names.add(loadingIndicatorName());
        }
        final AppSettings appSettings = AppSettingsBinding.current();
        if (appSettings.hasLayoutComponents()) {
            names.add(hideLayoutComponentsName());
        }
        if (floatingNavigationBar()) {
            names.add(floatingNavigationBarName());
        }
        names.addAll(appSettings.appliedPatchNames());
        return names;
    }

    public static String accentColorName() {
        return null;
    }

    public static String amoledDarkThemeDescription() {
        return null;
    }

    public static String amoledDarkThemeName() {
        return null;
    }

    public static String hideUpgradePromotionsDescription() {
        return null;
    }

    public static String hideUpgradePromotionsName() {
        return null;
    }

    public static String materialSwitchesDescription() {
        return null;
    }

    public static String materialSwitchesName() {
        return null;
    }

    public static String loadingIndicatorDescription() {
        return null;
    }

    public static String loadingIndicatorName() {
        return null;
    }

    public static String hideLayoutComponentsName() {
        return null;
    }

    public static String floatingNavigationBarName() {
        return null;
    }

}
