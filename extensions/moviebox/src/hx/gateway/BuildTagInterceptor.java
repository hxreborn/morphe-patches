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
package hx.gateway;

import java.util.Map;

import hx.okhttp.ChainInterceptor;
import hx.util.Reflect;

public final class BuildTagInterceptor extends ChainInterceptor {

    private static final String INFO_HEADER = "X-Client-Info";

    private final BuildTag buildTag;

    private final Map<String, String> sharedHeaders;

    private volatile String sharedTag;

    public BuildTagInterceptor(BuildTag buildTag, Map<String, String> sharedHeaders) {
        super("MovieBoxBuildTagInterceptor", "MovieBox build tag failed");
        this.buildTag = buildTag;
        this.sharedHeaders = sharedHeaders;
    }

    public void tagSharedHeaders() {
        String current = this.sharedHeaders.get(BuildTag.HEADER);
        if (current == null || !current.equals(this.sharedTag)) {
            String tag = this.buildTag.next();
            this.sharedTag = tag;
            this.sharedHeaders.put(BuildTag.HEADER, tag);
        }
    }

    @Override
    protected Object intercept(Object chain) throws ReflectiveOperationException {
        tagSharedHeaders();
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
        String clientInfo = header(request, INFO_HEADER);
        if (clientInfo != null) {
            setHeader(builder, INFO_HEADER, BuildTag.addTo(clientInfo, tag));
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
