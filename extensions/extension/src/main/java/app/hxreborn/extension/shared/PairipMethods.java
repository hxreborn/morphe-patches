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
package app.hxreborn.extension.shared;

import java.lang.reflect.Array;
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
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(className + "." + methodName + " is missing", ex);
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
        if (name.endsWith("[]")) {
            return Array.newInstance(forName(name.substring(0, name.length() - 2)), 0).getClass();
        }

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
