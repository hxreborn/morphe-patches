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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import hx.util.Reflect;

final class ClientInterceptors {

    private static final String INTERCEPTOR = "okhttp3.Interceptor";

    private ClientInterceptors() {
    }

    static Object proxy(ClassLoader loader, InvocationHandler handler) throws ClassNotFoundException {
        Class<?> interceptorClass = Class.forName(INTERCEPTOR, false, loader);
        return Proxy.newProxyInstance(loader, new Class<?>[] { interceptorClass }, handler);
    }

    static Object addFirst(Object client, Object interceptor) throws ReflectiveOperationException {
        Object builder = Reflect.call(client, "newBuilder");
        insert(Reflect.call(builder, "interceptors"), 0, interceptor);
        return Reflect.call(builder, "build");
    }

    static Object addBeforeTerminal(Object client, Object interceptor) throws ReflectiveOperationException {
        Object builder = Reflect.call(client, "newBuilder");
        Object interceptors = Reflect.call(builder, "interceptors");
        int terminal = (Integer) Reflect.call(interceptors, "size") - 1;
        insert(interceptors, Math.max(terminal, 0), interceptor);
        return Reflect.call(builder, "build");
    }

    private static void insert(Object interceptors, int index, Object interceptor) throws ReflectiveOperationException {
        Reflect.call(interceptors, "add", new Class<?>[] { int.class, Object.class }, index, interceptor);
    }

}
