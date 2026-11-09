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

@SuppressWarnings("unused")
public final class FloatingNavigationBar {

    public static final String KEY = "floating_navigation_bar";

    private static final long DARK_SURFACE = 0xff2b2b3800000000L;

    private static final long LIGHT_SURFACE = 0xffffffff00000000L;

    private static final float BAR_HEIGHT_DP = 56f;

    private static final long COLOR_ENCODING_MASK = 0xffffffffL;

    private FloatingNavigationBar() {
    }

    public static boolean isPatched() {
        return false;
    }

    public static boolean isEnabled() {
        return isPatched() && PatchSettings.isEnabled(KEY, false);
    }

    public static Object modifier(Object original, Object composer) {
        return isEnabled() ? NavigationBarBackdrop.bar(floatingModifier(outerInsets(original, composer))) : original;
    }

    public static long background(long original) {
        if (!isEnabled() || (original & COLOR_ENCODING_MASK) != 0) {
            return original;
        }
        return NavigationBarBackdrop.background((AmoledTheme.isDark(original)) ? DARK_SURFACE : LIGHT_SURFACE);
    }

    static int surfaceColor(boolean dark) {
        return (int) ((dark ? DARK_SURFACE : LIGHT_SURFACE) >>> Integer.SIZE);
    }

    public static float elevation(float original) {
        return isEnabled() ? 0f : original;
    }

    public static float height(float original) {
        return isEnabled() ? BAR_HEIGHT_DP : original;
    }

    public static float itemBottomPadding(float original) {
        return isEnabled() ? 0f : original;
    }

    private static Object outerInsets(Object original, Object composer) {
        return original;
    }

    private static Object floatingModifier(Object original) {
        return original;
    }

}
