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
package app.hxreborn.extension.netanalyzer;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import android.util.Log;

import app.hxreborn.extension.shared.ProxyHandler;

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

    private static final class ClickHandler extends ProxyHandler {

        private final Runnable action;

        ClickHandler(Runnable action) {
            this.action = action;
        }

        @Override
        protected String describe() {
            return "hxNetAnalyzerClick";
        }

        @Override
        protected Object handle(Method method, Object[] args) {
            this.action.run();
            return null;
        }

    }

}
