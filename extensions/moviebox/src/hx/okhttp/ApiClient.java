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
import java.lang.reflect.InvocationTargetException;

import hx.util.Reflect;

public final class ApiClient {

    private static final String REQUEST_BUILDER = "okhttp3.Request$Builder";

    private final ClassLoader loader;

    private final Object client;

    private final String base;

    public ApiClient(ClassLoader loader, Object client, String apiBase) {
        this.loader = loader;
        this.client = client;
        this.base = apiBase.endsWith("/") ? apiBase : apiBase + "/";
    }

    public String url(String pathAndQuery) {
        return this.base + pathAndQuery;
    }

    public String get(String url) throws IOException {
        try {
            Object response = execute(url);
            try {
                return successfulBody(response, url);
            } finally {
                Reflect.call(response, "close");
            }
        } catch (InvocationTargetException ex) {
            throw new IOException(ex.getCause());
        } catch (ReflectiveOperationException ex) {
            throw new IOException(ex);
        }
    }

    private Object execute(String url) throws ReflectiveOperationException {
        Object builder = Class.forName(REQUEST_BUILDER, false, this.loader).getConstructor().newInstance();
        Reflect.call(builder, "url", new Class<?>[] { String.class }, url);
        Object request = Reflect.call(builder, "build");
        Object call = Reflect.call(this.client, "newCall", new Class<?>[] { request.getClass() }, request);
        return Reflect.call(call, "execute");
    }

    private static String successfulBody(Object response, String url) throws ReflectiveOperationException, IOException {
        int code = (Integer) Reflect.call(response, "code");
        if (code < 200 || code >= 300) {
            throw new IOException("HTTP " + code + " for " + url);
        }
        return (String) Reflect.call(Reflect.call(response, "body"), "string");
    }

}
