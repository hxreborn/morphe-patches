/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.netanalyzer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

final class StaticStates {

    private static final String VALUE_GETTER = "getValue";

    private StaticStates() {
    }

    static Object value(Object state) throws ReflectiveOperationException {
        if (state == null) {
            return null;
        }
        for (Method method : state.getClass().getMethods()) {
            if (VALUE_GETTER.equals(method.getName()) && method.getParameterCount() == 0) {
                method.setAccessible(true);
                return method.invoke(state);
            }
        }
        return null;
    }

    static List<Object> values(Class<?> owner) throws ReflectiveOperationException {
        final List<Object> values = new ArrayList<>();
        for (Field field : owner.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) || field.getType().isPrimitive()) {
                continue;
            }
            field.setAccessible(true);
            final Object value = value(field.get(null));
            if (value != null) {
                values.add(value);
            }
        }
        return values;
    }

    static List<?> listOf(Class<?> owner, Class<?> elementType) throws ReflectiveOperationException {
        for (Object value : values(owner)) {
            if (value instanceof List && !((List<?>) value).isEmpty()
                    && elementType.isInstance(((List<?>) value).get(0))) {
                return (List<?>) value;
            }
        }
        return new ArrayList<>();
    }

}
