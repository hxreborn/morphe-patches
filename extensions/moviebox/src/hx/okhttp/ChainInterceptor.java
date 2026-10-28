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
package hx.okhttp;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import hx.util.Reflect;

public abstract class ChainInterceptor implements InvocationHandler {

    private final String description;

    private final String failureMessage;

    public ChainInterceptor(String description, String failureMessage) {
        this.description = description;
        this.failureMessage = failureMessage;
    }

    @Override
    public final Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String name = method.getName();
        int count = (args != null) ? args.length : 0;
        if (count == 0 && "toString".equals(name)) {
            return this.description;
        }
        if (count == 0 && "hashCode".equals(name)) {
            return System.identityHashCode(proxy);
        }
        if (count == 1 && "equals".equals(name)) {
            return proxy == args[0];
        }
        if (count != 1) {
            return null;
        }
        return interceptAsIoFailure(args[0]);
    }

    protected abstract Object intercept(Object chain) throws Throwable;

    protected static Object request(Object chain) throws ReflectiveOperationException {
        return Reflect.call(chain, "request");
    }

    protected static Object proceed(Object chain, Object request) throws ReflectiveOperationException {
        return Reflect.call(chain, "proceed", new Class<?>[] { request.getClass() }, request);
    }

    private Object interceptAsIoFailure(Object chain) throws IOException {
        try {
            return intercept(chain);
        } catch (InvocationTargetException ex) {
            throw asIoException((ex.getCause() != null) ? ex.getCause() : ex);
        } catch (Throwable ex) {
            throw asIoException(ex);
        }
    }

    private IOException asIoException(Throwable failure) {
        return (failure instanceof IOException) ? (IOException) failure : new IOException(this.failureMessage, failure);
    }

}
