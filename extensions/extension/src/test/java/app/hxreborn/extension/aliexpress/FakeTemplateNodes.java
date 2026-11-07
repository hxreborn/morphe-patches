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

import java.util.List;

import com.ahe.android.hybridengine.widget.AHEWidgetNode;
import com.aliexpress.android.aehome.ahe.ahenode.HomeAHEAEImageViewWidgetNode;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@Implements(value = TemplateNodes.class, isInAndroidSdk = false)
public final class FakeTemplateNodes {

    @Implementation
    public static boolean isHomeImageNode(Object node) {
        return node instanceof HomeAHEAEImageViewWidgetNode;
    }

    @Implementation
    public static Object parentOf(Object node) {
        return ((AHEWidgetNode) node).getParentWidget();
    }

    @Implementation
    public static List<?> childrenOf(Object node) {
        return ((AHEWidgetNode) node).getChildren();
    }

    @Implementation
    public static int backgroundColorOf(Object node) {
        return ((AHEWidgetNode) node).getBackGroundColor();
    }

}
