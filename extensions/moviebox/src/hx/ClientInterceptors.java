/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * See LICENSE for GPLv3.
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
