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
package hx.gateway;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;

public final class SharedHeaders {

    private SharedHeaders() {
    }

    @SuppressWarnings("unchecked")
    public static Map<String, String> of(Class<?> store) throws ReflectiveOperationException {
        for (Field field : store.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && Map.class.isAssignableFrom(field.getType())) {
                field.setAccessible(true);
                Object headers = field.get(null);
                if (headers != null) {
                    return (Map<String, String>) headers;
                }
            }
        }
        throw new NoSuchFieldException("no static header map on " + store.getName());
    }

}
