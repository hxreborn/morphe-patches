/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package hx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;

import okhttp3.Interceptor;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public final class BuildTagInterceptorTest {

    private static final byte[] CERTIFICATE = "stock-certificate".getBytes(StandardCharsets.UTF_8);

    private final ClassLoader loader = getClass().getClassLoader();

    @Test
    public void replacesTheStockBuildHeader() throws Exception {
        // given
        Request request = request("stock-tag", "{\"device_id\":\"d1\"}");
        Chain chain = new Chain(request);

        // when
        intercept(chain);

        // then
        String tag = chain.proceeded.header(BuildTag.HEADER);
        assertNotEquals("stock-tag", tag);
        assertTrue(tag, tag.matches("\\d{17,19}\\.[0-9a-f]{32}"));
    }

    @Test
    public void signsTheTagWithTheCertificateDigest() throws Exception {
        // given
        Chain chain = new Chain(request("stock-tag", "{}"));

        // when
        intercept(chain);

        // then
        String[] parts = chain.proceeded.header(BuildTag.HEADER).split("\\.");
        String seed = Digests.sha256Hex(CERTIFICATE) + "oneroom" + parts[0];
        assertEquals(Digests.md5Hex(seed.getBytes(StandardCharsets.UTF_8)), parts[1]);
    }

    @Test
    public void putsTheSameTagInsideTheClientInfo() throws Exception {
        // given
        Chain chain = new Chain(request("stock-tag", "{\"device_id\":\"d1\"}"));

        // when
        intercept(chain);

        // then
        JSONObject info = new JSONObject(chain.proceeded.header("X-Client-Info"));
        assertEquals("d1", info.getString("device_id"));
        assertEquals(chain.proceeded.header(BuildTag.HEADER), info.getString(BuildTag.HEADER));
    }

    @Test
    public void tagsARequestThatHasNoClientInfo() throws Exception {
        // given
        Request request = new Request.Builder().url("https://example.com/a").header(BuildTag.HEADER, "stock").build();
        Chain chain = new Chain(request);

        // when
        intercept(chain);

        // then
        assertNotEquals("stock", chain.proceeded.header(BuildTag.HEADER));
        assertEquals(null, chain.proceeded.header("X-Client-Info"));
    }

    @Test
    public void passesARequestWithoutTheBuildHeaderThrough() throws Exception {
        // given
        Request request = new Request.Builder().url("https://example.com/a").header("X-Client-Info", "{}").build();
        Chain chain = new Chain(request);

        // when
        intercept(chain);

        // then
        assertSame(request, chain.proceeded);
    }

    @Test
    public void reportsAFailureAsAnIoException() throws Exception {
        // given
        Chain chain = new Chain(request("stock-tag", "{}"));
        chain.failure = new IllegalStateException("boom");

        // when
        IOException failure = null;
        try {
            intercept(chain);
        } catch (IOException ex) {
            failure = ex;
        }

        // then
        assertNotNull(failure);
        assertEquals("MovieBox build tag failed", failure.getMessage());
    }

    @Test
    public void describesItselfWhenAskedForItsString() throws Exception {
        // when
        Object proxy = proxy();

        // then
        assertEquals("MovieBoxBuildTagInterceptor", proxy.toString());
        assertEquals(proxy, proxy);
    }

    private Request request(String tag, String clientInfo) {
        return new Request.Builder().url("https://example.com/a")
            .header(BuildTag.HEADER, tag)
            .header("X-Client-Info", clientInfo)
            .build();
    }

    private Object proxy() throws ClassNotFoundException {
        return ClientInterceptors.proxy(this.loader, new BuildTagInterceptor(BuildTag.fromCertificate(CERTIFICATE)));
    }

    private void intercept(Chain chain) throws Exception {
        Interceptor.Chain proxy = (Interceptor.Chain) Proxy.newProxyInstance(this.loader,
                new Class<?>[] { Interceptor.Chain.class }, chain);
        ((Interceptor) proxy()).intercept(proxy);
    }

    private static final class Chain implements InvocationHandler {

        final Request original;

        Request proceeded;

        RuntimeException failure;

        Chain(Request original) {
            this.original = original;
        }

        @Override
        public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
            if (this.failure != null) {
                throw this.failure;
            }
            if ("request".equals(method.getName())) {
                return this.original;
            }
            this.proceeded = (Request) args[0];
            return new Response.Builder().request(this.proceeded)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .build();
        }

    }

}
