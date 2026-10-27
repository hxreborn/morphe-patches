/*
 * Copyright (C) 2026 hxreborn
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
 */
package hx;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

final class BuildTagInterceptor implements InvocationHandler {
    private static final String INFO_HEADER = "X-Client-Info";

    private final BuildTag buildTag;

    BuildTagInterceptor(BuildTag buildTag) {
        this.buildTag = buildTag;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String name = method.getName();
        if (args == null || args.length == 0) {
            if ("toString".equals(name)) return "MovieBoxBuildTagInterceptor";
            if ("hashCode".equals(name)) return System.identityHashCode(proxy);
        }
        if ("equals".equals(name) && args != null && args.length == 1) return proxy == args[0];
        if (args == null || args.length != 1) return null;

        try {
            return intercept(args[0]);
        } catch (InvocationTargetException wrapped) {
            throw asIoException(wrapped.getCause() != null ? wrapped.getCause() : wrapped);
        } catch (Throwable failure) {
            throw asIoException(failure);
        }
    }

    private static IOException asIoException(Throwable failure) {
        return failure instanceof IOException ? (IOException) failure : new IOException("MovieBox build tag failed", failure);
    }

    private Object intercept(Object chain) throws Throwable {
        Object request = chain.getClass().getMethod("request").invoke(chain);
        Object stamped = withStockCertificateTag(request);
        return chain.getClass().getMethod("proceed", request.getClass()).invoke(chain, stamped);
    }

    private Object withStockCertificateTag(Object request) throws ReflectiveOperationException {
        Method header = request.getClass().getMethod("header", String.class);
        if (header.invoke(request, BuildTag.HEADER) == null) return request;

        String tag = this.buildTag.next();
        Object builder = request.getClass().getMethod("newBuilder").invoke(request);
        Method set = builder.getClass().getMethod("header", String.class, String.class);
        set.invoke(builder, BuildTag.HEADER, tag);
        String info = (String) header.invoke(request, INFO_HEADER);
        if (info != null) set.invoke(builder, INFO_HEADER, BuildTag.addTo(info, tag));
        return builder.getClass().getMethod("build").invoke(builder);
    }
}
