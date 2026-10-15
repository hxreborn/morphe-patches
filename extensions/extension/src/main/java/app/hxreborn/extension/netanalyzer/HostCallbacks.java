/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.netanalyzer;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import android.util.Log;

final class HostCallbacks {

    private static final String TAG = "hxNetAnalyzer";

    private HostCallbacks() {
    }

    static void invoke(Object callback) {
        try {
            for (Class<?> callbackType : callback.getClass().getInterfaces()) {
                for (Method method : callbackType.getMethods()) {
                    if (method.getParameterCount() == 0) {
                        method.invoke(callback);
                        return;
                    }
                }
            }
        } catch (ReflectiveOperationException ex) {
            Log.e(TAG, "Cannot run the original callback", ex);
        }
    }

    static Object create(Class<?> onClickType, Runnable action) {
        return create(new Class<?>[] { onClickType }, action);
    }

    static Object create(Class<?>[] interfaces, Runnable action) {
        return Proxy.newProxyInstance(HostCallbacks.class.getClassLoader(), interfaces, new ClickHandler(action));
    }

    private static final class ClickHandler implements InvocationHandler {

        private final Runnable action;

        ClickHandler(Runnable action) {
            this.action = action;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            final String name = method.getName();
            if ("toString".equals(name)) {
                return "hxNetAnalyzerClick";
            }
            if ("hashCode".equals(name)) {
                return System.identityHashCode(proxy);
            }
            if ("equals".equals(name)) {
                return proxy == args[0];
            }
            this.action.run();
            return null;
        }

    }

}
