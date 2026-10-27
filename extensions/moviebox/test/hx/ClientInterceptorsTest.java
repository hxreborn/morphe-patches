/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package hx;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public final class ClientInterceptorsTest {

    private static final byte[] CERTIFICATE = "stock-certificate".getBytes(StandardCharsets.UTF_8);

    private final ClassLoader loader = getClass().getClassLoader();

    private final List<String> seenByTerminal = new ArrayList<>();

    @Test
    public void tagsTheBuildHeaderAfterTheAppAddedItAndBeforeTheTerminalInterceptor() throws Exception {
        // given
        OkHttpClient client = givenClientWithTagInterceptor();

        // when
        client.newCall(new Request.Builder().url("https://example.com/a").build()).execute().close();

        // then
        Assert.assertEquals(1, this.seenByTerminal.size());
        Assert.assertNotEquals("stock-tag", this.seenByTerminal.get(0));
        Assert.assertTrue(this.seenByTerminal.get(0), this.seenByTerminal.get(0).matches("\\d{17,19}\\.[0-9a-f]{32}"));
    }

    @Test
    public void addsTheInterceptorToAClientWithoutInterceptors() throws Exception {
        // given
        Object proxy = ClientInterceptors.proxy(this.loader,
                new BuildTagInterceptor(BuildTag.fromCertificate(CERTIFICATE)));

        // when
        OkHttpClient client = (OkHttpClient) ClientInterceptors.addBeforeTerminal(new OkHttpClient(), proxy);

        // then
        Assert.assertEquals(1, client.interceptors().size());
    }

    private OkHttpClient givenClientWithTagInterceptor() throws Exception {
        OkHttpClient app = new OkHttpClient.Builder().addInterceptor(this::addStockBuildHeader)
            .addInterceptor(this::answerWithoutNetwork)
            .build();
        Object proxy = ClientInterceptors.proxy(this.loader,
                new BuildTagInterceptor(BuildTag.fromCertificate(CERTIFICATE)));
        return (OkHttpClient) ClientInterceptors.addBeforeTerminal(app, proxy);
    }

    private Response addStockBuildHeader(Interceptor.Chain chain) throws IOException {
        return chain.proceed(chain.request().newBuilder().header(BuildTag.HEADER, "stock-tag").build());
    }

    private Response answerWithoutNetwork(Interceptor.Chain chain) {
        this.seenByTerminal.add(chain.request().header(BuildTag.HEADER));
        return new Response.Builder().request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .build();
    }

}
