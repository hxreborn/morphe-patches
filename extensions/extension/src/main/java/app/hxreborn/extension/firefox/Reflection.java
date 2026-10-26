/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.firefox;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import android.app.Application;

final class Reflection {

    private static final Map<String, Field> FIELDS = new ConcurrentHashMap<>();

    private static final Map<String, Method> METHODS = new ConcurrentHashMap<>();

    private Reflection() {

    }

    static Application application() throws ReflectiveOperationException {
        return (Application) Class.forName("android.app.ActivityThread").getMethod("currentApplication").invoke(null);
    }

    static Object read(Object target, String name) throws ReflectiveOperationException {
        Class<?> type = target.getClass();
        String key = type.getName() + '#' + name;
        Field field = FIELDS.get(key);
        if (field == null) {
            for (Class<?> current = type; current != null && field == null; current = current.getSuperclass()) {
                try {
                    field = current.getDeclaredField(name);
                } catch (NoSuchFieldException ignored) {
                }
            }
            if (field == null) {
                throw new NoSuchFieldException(name);
            }
            field.setAccessible(true);
            FIELDS.put(key, field);
        }
        return field.get(target);
    }

    static Object call(Object target, String name) throws ReflectiveOperationException {
        Method method = find(target.getClass(), name);
        if (method == null) {
            throw new NoSuchMethodException(name);
        }
        return method.invoke(target);
    }

    static Method find(Class<?> type, String name) {
        String key = type.getName() + '#' + name;
        Method cached = METHODS.get(key);
        if (cached != null) {
            return cached;
        }
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.getName().equals(name) && method.getParameterTypes().length == 0) {
                    method.setAccessible(true);
                    METHODS.put(key, method);
                    return method;
                }
            }
        }
        return null;
    }

}
