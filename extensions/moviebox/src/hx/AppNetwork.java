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
package hx;

import java.lang.reflect.Field;

import hx.util.Reflect;

final class AppNetwork {

    private static final String OKHTTP_CLIENT = "okhttp3.OkHttpClient";

    private static final String OKHTTP_URL = "okhttp3.HttpUrl";

    private static final String RETROFIT_PACKAGE = "retrofit2.";

    final ClassLoader loader;

    final Object client;

    final String apiBase;

    private final Object instance;

    private final Field clientField;

    private final Object retrofit;

    private final Field callFactoryField;

    private AppNetwork(ClassLoader loader, Object instance, Field clientField, Object retrofit, Field callFactoryField,
            String apiBase) throws IllegalAccessException {
        this.loader = loader;
        this.instance = instance;
        this.clientField = clientField;
        this.client = clientField.get(instance);
        this.retrofit = retrofit;
        this.callFactoryField = callFactoryField;
        this.apiBase = apiBase;
    }

    static AppNetwork locate(Class<?> manager) throws ReflectiveOperationException {
        Object instance = singleton(manager);
        if (instance == null) {
            throw new IllegalStateException("no network manager singleton");
        }
        ClassLoader loader = manager.getClassLoader();
        Class<?> clientClass = Class.forName(OKHTTP_CLIENT, false, loader);
        Field clientField = holderOf(manager, clientClass);
        if (clientField == null) {
            throw new IllegalStateException("no client field on network manager");
        }
        if (!clientClass.isInstance(clientField.get(instance))) {
            throw new IllegalStateException("network manager field is not an OkHttpClient");
        }
        Field retrofitField = fieldInPackage(manager, RETROFIT_PACKAGE);
        Object retrofit = (retrofitField != null) ? retrofitField.get(instance) : null;
        if (retrofit == null) {
            throw new IllegalStateException("no Retrofit instance on network manager");
        }
        Field callFactory = holderOf(retrofit.getClass(), clientClass);
        if (callFactory == null) {
            throw new IllegalStateException("no call factory field on Retrofit");
        }
        Field baseUrl = fieldInPackage(retrofit.getClass(), OKHTTP_URL);
        if (baseUrl == null) {
            throw new IllegalStateException("no base URL field on Retrofit");
        }
        return new AppNetwork(loader, instance, clientField, retrofit, callFactory,
                String.valueOf(baseUrl.get(retrofit)));
    }

    void useClient(Object replacement) throws IllegalAccessException {
        this.clientField.set(this.instance, replacement);
        try {
            this.callFactoryField.set(this.retrofit, replacement);
        } catch (Exception ex) {
            this.clientField.set(this.instance, this.client);
            throw ex;
        }
    }

    private static Object singleton(Class<?> manager) throws ReflectiveOperationException {
        for (Field field : manager.getDeclaredFields()) {
            if ("kotlin.Lazy".equals(field.getType().getName())) {
                field.setAccessible(true);
                Object lazy = field.get(null);
                return (lazy != null) ? Reflect.call(lazy, "getValue") : null;
            }
        }
        return null;
    }

    private static Field holderOf(Class<?> owner, Class<?> clientClass) {
        for (Class<?> type = owner; type != null && type != Object.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (field.getType() != Object.class && field.getType().isAssignableFrom(clientClass)) {
                    field.setAccessible(true);
                    return field;
                }
            }
        }
        return null;
    }

    private static Field fieldInPackage(Class<?> owner, String prefix) {
        for (Class<?> type = owner; type != null && type != Object.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (field.getType().getName().startsWith(prefix)) {
                    field.setAccessible(true);
                    return field;
                }
            }
        }
        return null;
    }

}
