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

package app.hxreborn.extension.aliexpress;

import java.util.Collections;
import java.util.List;

final class TemplateNodes {

    private static final int MAX_ANCESTORS = 8;

    private TemplateNodes() {
    }

    static boolean isHomeImageNode(Object node) {
        return false;
    }

    static Object parentOf(Object node) {
        return null;
    }

    static List<?> childrenOf(Object node) {
        return null;
    }

    static int backgroundColorOf(Object node) {
        return 0;
    }

    static boolean keepsLightColors(Object node) {
        return !hasImageChild(node) && hasImageGrandchild(node);
    }

    static boolean isBackdropImage(Object node) {
        if (!isHomeImageNode(node)) {
            return false;
        }
        Object frame = parentOf(node);
        return frame != null && hasNightColor(backgroundColorOf(frame));
    }

    static boolean hasLightSurface(Object node) {
        return hasNightColor(backgroundColorOf(node));
    }

    static boolean drawnOnNightSurface(Object node) {
        Object current = node;
        for (int depth = 0; current != null && depth <= MAX_ANCESTORS; depth++) {
            int surface = backgroundColorOf(current);
            if (DarkColors.hasSurface(surface)) {
                return !keepsLightColors(current) && hasNightColor(surface);
            }
            current = parentOf(current);
        }
        return false;
    }

    private static boolean hasNightColor(int surface) {
        return DarkColors.hasSurface(surface) && DarkColors.night(DarkColors.BACKGROUND_TYPE, surface) != surface;
    }

    private static boolean hasImageChild(Object node) {
        for (Object child : children(node)) {
            if (isHomeImageNode(child)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasImageGrandchild(Object node) {
        for (Object child : children(node)) {
            if (hasImageChild(child)) {
                return true;
            }
        }
        return false;
    }

    private static List<?> children(Object node) {
        List<?> nodes = childrenOf(node);
        return (nodes != null) ? nodes : Collections.emptyList();
    }

}
