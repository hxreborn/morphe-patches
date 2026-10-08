/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.protonvpn;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import app.hxreborn.extension.proton.MaterialSwitches;

@SuppressWarnings("unused")
public final class SwitchThumbIcon {

    private static volatile Object checkedContent;

    private static volatile Object uncheckedContent;

    private SwitchThumbIcon() {

    }

    public static synchronized Object thumbContent(Class<?> composableLambdaType, boolean checked) {
        if (!MaterialSwitches.isEnabled()) {
            return null;
        }
        if (checked) {
            if (checkedContent == null) {
                checkedContent = newThumbContent(composableLambdaType, true);
            }
            return checkedContent;
        }
        if (uncheckedContent == null) {
            uncheckedContent = newThumbContent(composableLambdaType, false);
        }
        return uncheckedContent;
    }

    static void drawIcon(Object composer, boolean checked) {
    }

    private static Object newThumbContent(Class<?> composableLambdaType, boolean checked) {
        return Proxy.newProxyInstance(SwitchThumbIcon.class.getClassLoader(), new Class<?>[] { composableLambdaType },
                new ThumbContentHandler(checked));
    }

    private static final class ThumbContentHandler implements InvocationHandler {

        private final boolean checked;

        ThumbContentHandler(boolean checked) {
            this.checked = checked;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            final String name = method.getName();
            if ("toString".equals(name)) {
                return (this.checked) ? "SwitchThumbIcon(checked)" : "SwitchThumbIcon(unchecked)";
            }
            if ("hashCode".equals(name)) {
                return System.identityHashCode(proxy);
            }
            if ("equals".equals(name)) {
                return proxy == args[0];
            }

            drawIcon(args[0], this.checked);
            return null;
        }

    }

}
