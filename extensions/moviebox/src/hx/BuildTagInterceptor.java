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

final class BuildTagInterceptor extends ChainInterceptor {

    private static final String INFO_HEADER = "X-Client-Info";

    private final BuildTag buildTag;

    BuildTagInterceptor(BuildTag buildTag) {
        super("MovieBoxBuildTagInterceptor", "MovieBox build tag failed");
        this.buildTag = buildTag;
    }

    @Override
    Object intercept(Object chain) throws ReflectiveOperationException {
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
