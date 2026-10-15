/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
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

    static Object get(Field field, Object owner) {
        try {
            return field.get(owner);
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException(ex);
        }
    }

}
