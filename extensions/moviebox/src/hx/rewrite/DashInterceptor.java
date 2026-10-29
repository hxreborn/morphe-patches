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
package hx.rewrite;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import android.net.Uri;
import android.util.Log;
import hx.okhttp.ChainInterceptor;
import hx.source.Endpoints;
import hx.source.MovieBoxSource;
import hx.source.SignedResource;
import hx.util.Logs;
import hx.util.Reflect;

public final class DashInterceptor extends ChainInterceptor {

    private static final String RESPONSE_BODY = "okhttp3.ResponseBody";

    private static final String MEDIA_TYPE = "okhttp3.MediaType";

    private final ClassLoader loader;

    private final MovieBoxSource source;

    private final boolean routeDownloads;

    private final Set<String> hiddenHomeTabs;

    public DashInterceptor(ClassLoader loader, MovieBoxSource source, boolean routeDownloads,
            Set<String> hiddenHomeTabs) {
        super("MovieBoxDashInterceptor", "MovieBox interceptor failed");
        this.loader = loader;
        this.source = source;
        this.routeDownloads = routeDownloads;
        this.hiddenHomeTabs = hiddenHomeTabs;
    }

    @Override
    protected Object intercept(Object chain) throws ReflectiveOperationException {
        Object request = request(chain);
        String url = String.valueOf(Reflect.call(request, "url"));
        Object response = proceed(chain, withV1ResourceList(request, url));
        Endpoint endpoint = classify(url);
        if (endpoint == Endpoint.OTHER) {
            return response;
        }
        Object body = Reflect.call(response, "body");
        if (body == null) {
            return response;
        }
        byte[] bytes = (byte[]) Reflect.call(body, "bytes");
        return withBody(response, body, rewriteOrKeep(endpoint, url, response, bytes));
    }

    private static Object withV1ResourceList(Object request, String url) throws ReflectiveOperationException {
        String listing = ResourceListRequest.listingUrl(url);
        if (listing == null) {
            return request;
        }
        Object builder = Reflect.call(request, "newBuilder");
        Reflect.call(builder, "url", new Class<?>[] { String.class }, listing);
        return Reflect.call(builder, "build");
    }

    private Endpoint classify(String url) {
        if (url.contains(Endpoints.PLAY_INFO_SEGMENT)) {
            return Endpoint.PLAY_INFO;
        }
        if (url.contains(Endpoints.RESOURCE_LIST_SEGMENT)) {
            return Endpoint.RESOURCE_LIST;
        }
        boolean appTab = !this.hiddenHomeTabs.isEmpty() && url.contains(Endpoints.APP_TAB);
        boolean memberInfo = url.contains(Endpoints.USER_PROFILE) || url.contains(Endpoints.MEMBER_DETAIL);
        if (!appTab && !memberInfo) {
            return Endpoint.OTHER;
        }
        return url.contains(Endpoints.APP_TAB) ? Endpoint.APP_TAB : Endpoint.MEMBER_INFO;
    }

    private byte[] rewriteOrKeep(Endpoint endpoint, String url, Object response, byte[] bytes) {
        try {
            return rewriteBody(endpoint, url, response, bytes);
        } catch (Throwable ex) {
            Log.e(Logs.TAG, Uri.parse(url).getPath() + " not rewritten", ex);
            return bytes;
        }
    }

    private byte[] rewriteBody(Endpoint endpoint, String url, Object response, byte[] bytes)
            throws ReflectiveOperationException {
        String text = new String(bytes, StandardCharsets.UTF_8);
        String rewritten = rewriteText(endpoint, url, response, text);
        return rewritten.equals(text) ? bytes : rewritten.getBytes(StandardCharsets.UTF_8);
    }

    private String rewriteText(Endpoint endpoint, String url, Object response, String text)
            throws ReflectiveOperationException {
        switch (endpoint) {
            case PLAY_INFO:
                return rewritePlayInfo(url, response, text);
            case RESOURCE_LIST:
                return this.routeDownloads ? ResourceListRewriter.rewrite(this.source.withDirectFileSizes(text)) : text;
            case APP_TAB:
                return HomeTabs.withoutTabs(text, this.hiddenHomeTabs);
            case MEMBER_INFO:
                return MemberInfoRewriter.rewrite(text);
            default:
                throw new IllegalArgumentException("no rewrite for " + endpoint);
        }
    }

    private String rewritePlayInfo(String url, Object response, String text) throws ReflectiveOperationException {
        Uri requested = Uri.parse(url);
        String playInfo = withUnpacedCdn(requested, text);
        String subjectId = requested.getQueryParameter("subjectId");
        int season = queryInt(requested, "se");
        int episode = queryInt(requested, "ep");
        if (SignedResource.fromPlayInfo(playInfo) == null) {
            String direct = this.source.directFilePlayInfo(playInfo, subjectId, season, episode);
            if (direct != null) {
                return direct;
            }
        }
        Object sent = Reflect.call(response, "request");
        String apiHost = Uri.parse(String.valueOf(Reflect.call(sent, "url"))).getHost();
        this.source.observePlayInfo(url, playInfo, apiHost);
        return this.routeDownloads ? PlayInfoRewriter.rewrite(playInfo, subjectId, season, episode)
                : PlayInfoRewriter.pointAtManifests(playInfo);
    }

    private String withUnpacedCdn(Uri requested, String playInfo) {
        if (!SignedResource.isEdgeCachePlayInfo(playInfo)) {
            return playInfo;
        }
        String replacement = this.source
            .fetchReplacementPlayInfo(requested.getEncodedPath() + "?" + requested.getEncodedQuery());
        return (replacement != null) ? replacement : playInfo;
    }

    private static int queryInt(Uri uri, String name) {
        try {
            return Integer.parseInt(uri.getQueryParameter(name));
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private Object withBody(Object response, Object body, byte[] payload) throws ReflectiveOperationException {
        Object newBody = createResponseBody(Reflect.call(body, "contentType"), payload);
        Object builder = Reflect.call(response, "newBuilder");
        Class<?> bodyClass = Class.forName(RESPONSE_BODY, false, this.loader);
        Reflect.call(builder, "body", new Class<?>[] { bodyClass }, newBody);
        Reflect.call(builder, "removeHeader", new Class<?>[] { String.class }, "Content-Length");
        return Reflect.call(builder, "build");
    }

    private Object createResponseBody(Object mediaType, byte[] bytes) throws ReflectiveOperationException {
        Class<?> bodyClass = Class.forName(RESPONSE_BODY, false, this.loader);
        Class<?> mediaTypeClass = Class.forName(MEDIA_TYPE, false, this.loader);
        for (Method create : bodyClass.getMethods()) {
            Class<?>[] parameters = create.getParameterTypes();
            if (!"create".equals(create.getName()) || parameters.length != 2) {
                continue;
            }
            if (parameters[0] == mediaTypeClass && parameters[1] == byte[].class) {
                return create.invoke(null, mediaType, bytes);
            }
            if (parameters[0] == byte[].class && parameters[1] == mediaTypeClass) {
                return create.invoke(null, bytes, mediaType);
            }
        }
        throw new IllegalStateException("no ResponseBody.create(byte[], MediaType) overload");
    }

    private enum Endpoint {

        PLAY_INFO, RESOURCE_LIST, APP_TAB, MEMBER_INFO, OTHER

    }

}
