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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import android.util.Log;

final class TemplateNodes {

    private static final String NODE_CLASS = "com.ahe.android.hybridengine.widget.AHEWidgetNode";

    private static final String HOME_IMAGE_NODE_CLASS = "com.aliexpress.android.aehome.ahe.ahenode."
            + "HomeAHEAEImageViewWidgetNode";

    private static final int MAX_ANCESTORS = 8;

    private static volatile Accessors accessors;

    private static volatile boolean unavailable;

    private TemplateNodes() {
    }

    static boolean keepsLightColors(Object node) {
        Accessors resolved = accessors(node);
        if (resolved == null) {
            return false;
        }
        try {
            return resolved.keepsLightColors(node);
        } catch (IllegalAccessException | InvocationTargetException ex) {
            return failed(ex);
        }
    }

    static boolean isBackdropImage(Object node) {
        Accessors resolved = accessors(node);
        if (resolved == null || !resolved.homeImageNode.isInstance(node)) {
            return false;
        }
        try {
            Object frame = resolved.parent.invoke(node);
            if (frame == null) {
                return false;
            }
            return darkensToNight(resolved.backgroundColor(frame));
        } catch (IllegalAccessException | InvocationTargetException ex) {
            return failed(ex);
        }
    }

    static boolean hasLightSurface(Object node) {
        Accessors resolved = accessors(node);
        if (resolved == null) {
            return false;
        }
        try {
            return darkensToNight(resolved.backgroundColor(node));
        } catch (IllegalAccessException | InvocationTargetException ex) {
            return failed(ex);
        }
    }

    static boolean drawnOnNightSurface(Object node) {
        Accessors resolved = accessors(node);
        if (resolved == null) {
            return false;
        }
        try {
            Object current = node;
            for (int depth = 0; current != null && depth <= MAX_ANCESTORS; depth++) {
                int surface = resolved.backgroundColor(current);
                if (DarkColors.hasSurface(surface)) {
                    return !resolved.keepsLightColors(current) && darkensToNight(surface);
                }
                current = resolved.parent.invoke(current);
            }
        } catch (IllegalAccessException | InvocationTargetException ex) {
            return failed(ex);
        }
        return false;
    }

    private static boolean darkensToNight(int surface) {
        return DarkColors.hasSurface(surface) && DarkColors.night(DarkColors.BACKGROUND_TYPE, surface) != surface;
    }

    private static boolean failed(ReflectiveOperationException ex) {
        Log.w(PatchSettings.LOG_TAG, "Template tree read failed", ex);
        unavailable = true;
        return false;
    }

    private static Accessors accessors(Object node) {
        if (accessors == null && !unavailable) {
            try {
                ClassLoader loader = node.getClass().getClassLoader();
                Class<?> nodeClass = Class.forName(NODE_CLASS, false, loader);
                accessors = new Accessors(nodeClass.getMethod("getParentWidget"), nodeClass.getMethod("getChildren"),
                        nodeClass.getMethod("getBackGroundColor"), Class.forName(HOME_IMAGE_NODE_CLASS, false, loader));
            } catch (ReflectiveOperationException ex) {
                Log.w(PatchSettings.LOG_TAG, "Template node lookup failed", ex);
                unavailable = true;
            }
        }
        return accessors;
    }

    private static final class Accessors {

        final Method parent;

        final Method children;

        final Method backgroundColor;

        final Class<?> homeImageNode;

        Accessors(Method parent, Method children, Method backgroundColor, Class<?> homeImageNode) {
            this.parent = parent;
            this.children = children;
            this.backgroundColor = backgroundColor;
            this.homeImageNode = homeImageNode;
        }

        int backgroundColor(Object node) throws IllegalAccessException, InvocationTargetException {
            return (Integer) this.backgroundColor.invoke(node);
        }

        boolean keepsLightColors(Object node) throws IllegalAccessException, InvocationTargetException {
            return !hasImageChild(node) && hasImageGrandchild(node);
        }

        private boolean hasImageChild(Object node) throws IllegalAccessException, InvocationTargetException {
            for (Object child : childrenOf(node)) {
                if (this.homeImageNode.isInstance(child)) {
                    return true;
                }
            }
            return false;
        }

        private boolean hasImageGrandchild(Object node) throws IllegalAccessException, InvocationTargetException {
            for (Object child : childrenOf(node)) {
                if (hasImageChild(child)) {
                    return true;
                }
            }
            return false;
        }

        private List<?> childrenOf(Object node) throws IllegalAccessException, InvocationTargetException {
            Object nodes = this.children.invoke(node);
            return (nodes instanceof List<?>) ? (List<?>) nodes : Collections.emptyList();
        }

    }

}
