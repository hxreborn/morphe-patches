/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.allinonecalculator;

import java.lang.reflect.Method;

@SuppressWarnings("unused")
public final class PairipMethods {
    private PairipMethods() {
    }

    public static Method find(String className, String methodName, String parameterTypes) {
        try {
            Class<?>[] parameters = parseParameters(parameterTypes);
            Method method = Class.forName(className).getDeclaredMethod(methodName, parameters);
            method.setAccessible(true);

            return method;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(className + "." + methodName + " is missing", e);
        }
    }

    private static Class<?>[] parseParameters(String parameterTypes) throws ClassNotFoundException {
        if (parameterTypes.isEmpty()) {
            return new Class<?>[0];
        }

        String[] names = parameterTypes.split(",");
        Class<?>[] parameters = new Class<?>[names.length];
        for (int i = 0; i < names.length; i++) {
            parameters[i] = forName(names[i]);
        }

        return parameters;
    }

    private static Class<?> forName(String name) throws ClassNotFoundException {
        switch (name) {
            case "boolean":
                return boolean.class;
            case "byte":
                return byte.class;
            case "char":
                return char.class;
            case "double":
                return double.class;
            case "float":
                return float.class;
            case "int":
                return int.class;
            case "long":
                return long.class;
            case "short":
                return short.class;
            case "void":
                return void.class;
            default:
                return Class.forName(name);
        }
    }
}
