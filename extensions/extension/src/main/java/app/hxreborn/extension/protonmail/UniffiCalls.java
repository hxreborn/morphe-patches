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
package app.hxreborn.extension.protonmail;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import app.hxreborn.extension.shared.ProxyHandler;

public final class UniffiCalls {

    static final String UNIFFI_PACKAGE = "uniffi.mail_uniffi.";

    private static final long SUSPEND_TIMEOUT_SECONDS = 60L;

    private UniffiCalls() {

    }

    static Object ioDispatcher() {
        return null;
    }

    public static Object invokeSuspending(Method method, Object target, Object... args) throws Exception {
        final Object dispatcher = ioDispatcher();
        if (dispatcher == null) {
            throw new IllegalStateException("I/O coroutine dispatcher is unavailable");
        }

        final Class<?>[] parameters = method.getParameterTypes();
        final BlockingContinuation blockingContinuation = new BlockingContinuation(dispatcher);
        final Object continuation = Proxy.newProxyInstance(UniffiCalls.class.getClassLoader(),
                new Class<?>[] { parameters[parameters.length - 1] }, blockingContinuation);

        final Object[] withContinuation = new Object[args.length + 1];
        System.arraycopy(args, 0, withContinuation, 0, args.length);
        withContinuation[args.length] = continuation;

        final Object immediate = method.invoke(target, withContinuation);
        if (!isCoroutineSuspended(immediate)) {
            return immediate;
        }

        if (!blockingContinuation.resumed.await(SUSPEND_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            throw new IllegalStateException(
                    method.getName() + " did not resume within " + SUSPEND_TIMEOUT_SECONDS + "s");
        }
        throwOnFailure(blockingContinuation.result);
        return blockingContinuation.result;
    }

    public static Object okValueOrNull(Object result) {
        return ("Ok".equals(simpleNameOf(result))) ? callNoArg(result, "getV1") : null;
    }

    static Method uniffiMethod(String name) throws Exception {
        return methodNamed(Class.forName(UNIFFI_PACKAGE + "Mail_uniffiKt"), name);
    }

    public static Method methodNamed(Class<?> type, String name) throws NoSuchMethodException {
        for (Method candidate : type.getMethods()) {
            if (candidate.getName().equals(name)) {
                candidate.setAccessible(true);
                return candidate;
            }
        }
        throw new NoSuchMethodException(type.getName() + "." + name);
    }

    public static Object callNoArg(Object target, String name) {
        try {
            return methodNamed(target.getClass(), name).invoke(target);
        } catch (Throwable throwable) {
            throw new IllegalStateException("Failed to invoke " + simpleNameOf(target) + "." + name + "()", throwable);
        }
    }

    public static String simpleNameOf(Object value) {
        return (value != null) ? value.getClass().getSimpleName() : "null";
    }

    private static boolean isCoroutineSuspended(Object value) {
        return value instanceof Enum && "COROUTINE_SUSPENDED".equals(((Enum<?>) value).name());
    }

    private static void throwOnFailure(Object result) throws Exception {
        if (result == null || !"Failure".equals(result.getClass().getSimpleName())) {
            return;
        }

        final Field cause = result.getClass().getDeclaredField("exception");
        cause.setAccessible(true);
        final Object thrown = cause.get(result);

        if (thrown instanceof Exception) {
            throw (Exception) thrown;
        }
        throw new IllegalStateException(String.valueOf(thrown));
    }

    private static final class BlockingContinuation extends ProxyHandler {

        private final CountDownLatch resumed = new CountDownLatch(1);

        private final Object context;

        private volatile Object result;

        BlockingContinuation(Object context) {
            this.context = context;
        }

        @Override
        protected String describe() {
            return "UniffiCalls";
        }

        @Override
        protected Object handle(Method method, Object[] args) {
            final String name = method.getName();
            if ("getContext".equals(name)) {
                return this.context;
            }
            if ("resumeWith".equals(name)) {
                this.result = args[0];
                this.resumed.countDown();
                return null;
            }
            return null;
        }

    }

}
