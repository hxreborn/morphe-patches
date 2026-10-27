/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package hx;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import hx.okhttp.ChainInterceptor;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public final class ClientInterceptorsTest {

    private static final String BUILD_HEADER = "X-Client-Build";

    private static final String REWRITTEN = "rewritten-tag";

    private final ClassLoader loader = getClass().getClassLoader();

    private final List<String> seenByTerminal = new ArrayList<>();

    @Test
    public void tagsTheBuildHeaderAfterTheAppAddedItAndBeforeTheTerminalInterceptor()
            throws IOException, ClassNotFoundException, ReflectiveOperationException {
        // given
        OkHttpClient client = givenClientWithTagInterceptor();

        // when
        client.newCall(new Request.Builder().url("https://example.com/a").build()).execute().close();

        // then
        Assertions.assertThat(this.seenByTerminal.size()).isEqualTo(1);
        Assertions.assertThat(this.seenByTerminal.get(0)).isEqualTo(REWRITTEN);
    }

    @Test
    public void addsTheInterceptorToAClientWithoutInterceptors()
            throws ClassNotFoundException, ReflectiveOperationException {
        // given
        Object proxy = ClientInterceptors.proxy(this.loader, new RewritingInterceptor());

        // when
        OkHttpClient client = (OkHttpClient) ClientInterceptors.addBeforeTerminal(new OkHttpClient(), proxy);

        // then
        Assertions.assertThat(client.interceptors().size()).isEqualTo(1);
    }

    private OkHttpClient givenClientWithTagInterceptor() throws ClassNotFoundException, ReflectiveOperationException {
        OkHttpClient app = new OkHttpClient.Builder().addInterceptor(this::addStockBuildHeader)
            .addInterceptor(this::answerWithoutNetwork)
            .build();
        Object proxy = ClientInterceptors.proxy(this.loader, new RewritingInterceptor());
        return (OkHttpClient) ClientInterceptors.addBeforeTerminal(app, proxy);
    }

    private Response addStockBuildHeader(Interceptor.Chain chain) throws IOException {
        return chain.proceed(chain.request().newBuilder().header(BUILD_HEADER, "stock-tag").build());
    }

    private Response answerWithoutNetwork(Interceptor.Chain chain) {
        this.seenByTerminal.add(chain.request().header(BUILD_HEADER));
        return new Response.Builder().request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .build();
    }

    private static final class RewritingInterceptor extends ChainInterceptor {

        RewritingInterceptor() {
            super("RewritingInterceptor", "rewriting failed");
        }

        @Override
        protected Object intercept(Object chain) throws ReflectiveOperationException {
            Request request = (Request) request(chain);
            if (request.header(BUILD_HEADER) == null) {
                return proceed(chain, request);
            }
            return proceed(chain, request.newBuilder().header(BUILD_HEADER, REWRITTEN).build());
        }

    }

}
