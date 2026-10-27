/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.photoeditorpro;

import java.lang.reflect.Method;

@SuppressWarnings("unused")
public final class AiTraceInterceptor {

    private final Object appInterceptor;

    private final Method interceptMethod;

    public AiTraceInterceptor(Object appInterceptor) {
        this.appInterceptor = appInterceptor;
        this.interceptMethod = HttpShape.interceptMethod(appInterceptor);
    }

    public static Object wrap(Object appInterceptor) {
        return new AiTraceInterceptor(appInterceptor);
    }

    public Object intercept(Object chain) {
        return AiTrace.trace(chain, this.interceptMethod.getReturnType(),
                () -> HttpShape.invokeRethrowing(this.interceptMethod, this.appInterceptor, chain));
    }

}
