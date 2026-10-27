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
package app.hxreborn.extension.netanalyzer;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

final class FieldLayout {

    private static final Comparator<Field> BY_OBFUSCATED_NAME = new Comparator<Field>() {

        @Override
        public int compare(Field first, Field second) {
            final int byLength = Integer.compare(first.getName().length(), second.getName().length());
            return (byLength != 0) ? byLength : first.getName().compareTo(second.getName());
        }

    };

    private FieldLayout() {
    }

    static Field[] instance(Class<?> type, Class<?>... layout) {
        final List<Field> found = new ArrayList<>();
        for (Field field : type.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) && !field.isSynthetic()) {
                field.setAccessible(true);
                found.add(field);
            }
        }
        Collections.sort(found, BY_OBFUSCATED_NAME);
        if (found.size() != layout.length) {
            throw new IllegalStateException(type.getName() + " has " + found.size() + " instance fields, expected "
                    + layout.length + ": " + Arrays.toString(layout));
        }
        for (int i = 0; i < layout.length; i++) {
            if (!layout[i].isAssignableFrom(found.get(i).getType())) {
                throw new IllegalStateException(type.getName() + " field " + found.get(i).getName() + " is "
                        + found.get(i).getType().getName() + ", expected " + layout[i].getName());
            }
        }
        return found.toArray(new Field[0]);
    }

    static List<Field> instanceFieldsByName(Object object) {
        final List<Field> fields = new ArrayList<>();
        for (Field field : object.getClass().getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) {
                field.setAccessible(true);
                fields.add(field);
            }
        }
        Collections.sort(fields, (left, right) -> left.getName().compareTo(right.getName()));
        return fields;
    }

    static Object get(Field field, Object owner) {
        try {
            return field.get(owner);
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException(ex);
        }
    }

}
