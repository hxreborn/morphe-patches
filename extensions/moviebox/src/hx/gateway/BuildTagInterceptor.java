/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * See LICENSE for GPLv3.
 * Additional term under GPLv3 Section 7(b): When conveying this material
 * or modified versions, preserve this notice and credit hxreborn for this
 * material in any Appropriate Legal Notices displayed by works containing it.
 */
package hx.gateway;

import hx.okhttp.ChainInterceptor;
import hx.util.Reflect;

public final class BuildTagInterceptor extends ChainInterceptor {

    private static final String INFO_HEADER = "X-Client-Info";

    private final BuildTag buildTag;

    public BuildTagInterceptor(BuildTag buildTag) {
        super("MovieBoxBuildTagInterceptor", "MovieBox build tag failed");
        this.buildTag = buildTag;
    }

    @Override
    protected Object intercept(Object chain) throws ReflectiveOperationException {
        Object request = request(chain);
        return proceed(chain, withStockCertificateTag(request));
    }

    private Object withStockCertificateTag(Object request) throws ReflectiveOperationException {
        if (header(request, BuildTag.HEADER) == null) {
            return request;
        }
        String tag = this.buildTag.next();
        Object builder = Reflect.call(request, "newBuilder");
        setHeader(builder, BuildTag.HEADER, tag);
        String info = header(request, INFO_HEADER);
        if (info != null) {
            setHeader(builder, INFO_HEADER, BuildTag.addTo(info, tag));
        }
        return Reflect.call(builder, "build");
    }

    private static String header(Object request, String name) throws ReflectiveOperationException {
        return (String) Reflect.call(request, "header", new Class<?>[] { String.class }, name);
    }

    private static void setHeader(Object builder, String name, String value) throws ReflectiveOperationException {
        Reflect.call(builder, "header", new Class<?>[] { String.class, String.class }, name, value);
    }

}
