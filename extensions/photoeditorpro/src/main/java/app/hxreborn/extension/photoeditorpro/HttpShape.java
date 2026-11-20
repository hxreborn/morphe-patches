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
package app.hxreborn.extension.photoeditorpro;

import java.io.Closeable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

final class HttpShape {

    private HttpShape() {
    }

    static Method interceptMethod(Object interceptor) {
        for (Class<?> type : interceptor.getClass().getInterfaces()) {
            for (Method method : type.getMethods()) {
                if (Modifier.isAbstract(method.getModifiers()) && method.getParameterTypes().length == 1
                        && method.getReturnType() != void.class) {
                    return method;
                }
            }
        }
        throw new IllegalStateException("No intercept method on " + interceptor.getClass().getName());
    }

    static Class<?> requestType(Object chain, Class<?> responseType) {
        Class<?> found = null;
        for (Method method : chain.getClass().getMethods()) {
            if (method.getParameterTypes().length == 1 && method.getReturnType() == responseType) {
                if (found != null && found != method.getParameterTypes()[0]) {
                    return null;
                }
                found = method.getParameterTypes()[0];
            }
        }
        return found;
    }

    static Object valueOfType(Object owner, Class<?> type) {
        if (owner == null || type == null) {
            return null;
        }
        for (Field field : instanceFields(owner.getClass())) {
            Object value = read(field, owner);
            if (type.isInstance(value)) {
                return value;
            }
        }
        return null;
    }

    static String method(Object request) {
        Object value = firstOfType(request, String.class);
        return (value != null) ? (String) value : "";
    }

    static String url(Object request) {
        Object url = urlValue(request);
        return (url != null) ? url.toString() : "";
    }

    static List<String[]> headers(Object owner) {
        List<String[]> pairs = new ArrayList<>();
        if (owner == null) {
            return pairs;
        }
        for (Field field : instanceFields(owner.getClass())) {
            if (!Iterable.class.isAssignableFrom(field.getType())) {
                continue;
            }
            Object headers = read(field, owner);
            String[] namesAndValues = (String[]) firstOfType(headers, String[].class);
            if (namesAndValues == null) {
                continue;
            }
            for (int i = 0; i + 1 < namesAndValues.length; i += 2) {
                pairs.add(new String[] { namesAndValues[i], namesAndValues[i + 1] });
            }
            return pairs;
        }
        return pairs;
    }

    static List<String[]> formFields(Object request) {
        List<String[]> pairs = new ArrayList<>();
        if (request == null) {
            return pairs;
        }
        Object url = urlValue(request);
        for (Field field : instanceFields(request.getClass())) {
            Object body = read(field, request);
            if (body == null || body == url || !isConcreteSubclassOfAbstract(body.getClass())) {
                continue;
            }
            List<List<?>> lists = new ArrayList<>();
            for (Field bodyField : instanceFields(body.getClass())) {
                if (List.class.isAssignableFrom(bodyField.getType())) {
                    lists.add((List<?>) read(bodyField, body));
                }
            }
            if (lists.size() != 2 || lists.get(0) == null || lists.get(1) == null) {
                continue;
            }
            List<?> names = lists.get(0);
            List<?> values = lists.get(1);
            for (int i = 0; i < Math.min(names.size(), values.size()); i++) {
                pairs.add(new String[] { formDecode(names.get(i)), formDecode(values.get(i)) });
            }
            return pairs;
        }
        return pairs;
    }

    static int statusCode(Object response, int fallback) {
        Object value = firstOfType(response, int.class);
        return (value != null) ? (Integer) value : fallback;
    }

    static String statusMessage(Object response) {
        Object value = firstOfType(response, String.class);
        return (value != null) ? (String) value : "";
    }

    static String protocol(Object response) {
        if (response == null) {
            return "";
        }
        for (Field field : instanceFields(response.getClass())) {
            if (field.getType().isEnum()) {
                Object value = read(field, response);
                return (value != null) ? value.toString() : "";
            }
        }
        return "";
    }

    static long[] timestamps(Object response) {
        List<Long> values = new ArrayList<>();
        if (response != null) {
            for (Field field : instanceFields(response.getClass())) {
                if (field.getType() == long.class) {
                    values.add((Long) read(field, response));
                }
            }
        }
        long[] result = new long[values.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = values.get(i);
        }
        return result;
    }

    static Object responseBody(Object response) {
        if (response == null) {
            return null;
        }
        for (Field field : instanceFields(response.getClass())) {
            Class<?> type = field.getType();
            if (Closeable.class.isAssignableFrom(type) && Modifier.isAbstract(type.getModifiers())
                    && !type.isInterface() && type != response.getClass()) {
                return read(field, response);
            }
        }
        return null;
    }

    static long contentLength(Object body) {
        Method method = abstractMethod(body, true);
        Object value = (method != null) ? invoke(method, body) : null;
        return (value instanceof Long) ? (Long) value : -1;
    }

    static String contentType(Object body) {
        Method method = abstractMethod(body, false);
        Object value = (method != null) ? invoke(method, body) : null;
        return (value != null) ? value.toString() : "";
    }

    static String peekBody(Object response, long maxBytes) {
        try {
            Object peeked = response.getClass().getMethod("peekBody", long.class).invoke(response, maxBytes);
            return (peeked != null) ? String.valueOf(peeked.getClass().getMethod("string").invoke(peeked)) : "";
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return "";
        }
    }

    static Object invokeRethrowing(Method method, Object target, Object argument) {
        try {
            return method.invoke(target, argument);
        } catch (InvocationTargetException ex) {
            throw HttpShape.<RuntimeException>rethrow(ex.getCause());
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @SuppressWarnings("unchecked")
    static <T extends Throwable> T rethrow(Throwable cause) throws T {
        throw (T) cause;
    }

    private static Object urlValue(Object request) {
        if (request == null) {
            return null;
        }
        for (Field field : instanceFields(request.getClass())) {
            if (field.getType() == String.class) {
                continue;
            }
            Object value = read(field, request);
            if (value != null && value.toString().startsWith("http")) {
                return value;
            }
        }
        return null;
    }

    private static Method abstractMethod(Object body, boolean returnsLong) {
        if (body == null) {
            return null;
        }
        for (Class<?> type = body.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            for (Method method : type.getDeclaredMethods()) {
                Class<?> returnType = method.getReturnType();
                if (!Modifier.isAbstract(method.getModifiers()) || method.getParameterTypes().length != 0) {
                    continue;
                }
                boolean matches = (returnsLong) ? returnType == long.class
                        : !returnType.isPrimitive() && !returnType.isInterface();
                if (matches) {
                    method.setAccessible(true);
                    return method;
                }
            }
        }
        return null;
    }

    private static Object invoke(Method method, Object target) {
        try {
            return method.invoke(target);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return null;
        }
    }

    private static boolean isConcreteSubclassOfAbstract(Class<?> type) {
        Class<?> parent = type.getSuperclass();
        return parent != null && parent != Object.class && Modifier.isAbstract(parent.getModifiers())
                && !Modifier.isAbstract(type.getModifiers());
    }

    private static String formDecode(Object value) {
        String text = String.valueOf(value);
        try {
            return URLDecoder.decode(text, "UTF-8");
        } catch (Exception ex) {
            return text;
        }
    }

    private static Object firstOfType(Object owner, Class<?> type) {
        if (owner == null) {
            return null;
        }
        for (Field field : instanceFields(owner.getClass())) {
            if (field.getType() == type) {
                return read(field, owner);
            }
        }
        return null;
    }

    private static List<Field> instanceFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers())) {
                    fields.add(field);
                }
            }
        }
        return fields;
    }

    private static Object read(Field field, Object owner) {
        try {
            field.setAccessible(true);
            return field.get(owner);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return null;
        }
    }

}
