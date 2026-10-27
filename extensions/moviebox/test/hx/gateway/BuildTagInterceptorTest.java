/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package hx.gateway;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;

import okhttp3.Interceptor;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import org.assertj.core.api.Assertions;
import org.json.JSONException;
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
    public void replacesTheStockBuildHeader() throws IOException, ClassNotFoundException {
        // given
        Request request = request("stock-tag", "{\"device_id\":\"d1\"}");
        Chain chain = new Chain(request);

        // when
        intercept(chain);

        // then
        String tag = chain.proceeded.header(BuildTag.HEADER);
        Assertions.assertThat(tag).isNotEqualTo("stock-tag");
        Assertions.assertThat(tag.matches("\\d{17,19}\\.[0-9a-f]{32}")).as(tag).isTrue();
    }

    @Test
    public void signsTheTagWithTheCertificateDigest()
            throws NoSuchAlgorithmException, IOException, ClassNotFoundException {
        // given
        Chain chain = new Chain(request("stock-tag", "{}"));

        // when
        intercept(chain);

        // then
        String[] parts = chain.proceeded.header(BuildTag.HEADER).split("\\.");
        String seed = Digests.sha256Hex(CERTIFICATE) + "oneroom" + parts[0];
        Assertions.assertThat(parts[1]).isEqualTo(Digests.md5Hex(seed.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    public void putsTheSameTagInsideTheClientInfo() throws JSONException, IOException, ClassNotFoundException {
        // given
        Chain chain = new Chain(request("stock-tag", "{\"device_id\":\"d1\"}"));

        // when
        intercept(chain);

        // then
        JSONObject info = new JSONObject(chain.proceeded.header("X-Client-Info"));
        Assertions.assertThat(info.getString("device_id")).isEqualTo("d1");
        Assertions.assertThat(info.getString(BuildTag.HEADER)).isEqualTo(chain.proceeded.header(BuildTag.HEADER));
    }

    @Test
    public void tagsARequestThatHasNoClientInfo() throws IOException, ClassNotFoundException {
        // given
        Request request = new Request.Builder().url("https://example.com/a").header(BuildTag.HEADER, "stock").build();
        Chain chain = new Chain(request);

        // when
        intercept(chain);

        // then
        Assertions.assertThat(chain.proceeded.header(BuildTag.HEADER)).isNotEqualTo("stock");
        Assertions.assertThat(chain.proceeded.header("X-Client-Info")).isEqualTo(null);
    }

    @Test
    public void passesARequestWithoutTheBuildHeaderThrough() throws IOException, ClassNotFoundException {
        // given
        Request request = new Request.Builder().url("https://example.com/a").header("X-Client-Info", "{}").build();
        Chain chain = new Chain(request);

        // when
        intercept(chain);

        // then
        Assertions.assertThat(chain.proceeded).isSameAs(request);
    }

    @Test
    public void reportsAFailureAsAnIoException() throws ClassNotFoundException {
        // given
        Chain chain = new Chain(request("stock-tag", "{}"));
        chain.failure = new IllegalStateException("boom");

        // when
        Throwable failure = Assertions.catchThrowable(() -> intercept(chain));

        // then
        Assertions.assertThat(failure).isInstanceOf(IOException.class).hasMessage("MovieBox build tag failed");
    }

    @Test
    public void describesItselfWhenAskedForItsString() throws ClassNotFoundException {
        // when
        Object proxy = proxy();

        // then
        Assertions.assertThat(proxy.toString()).isEqualTo("MovieBoxBuildTagInterceptor");
        Assertions.assertThat(proxy).isEqualTo(proxy);
    }

    private Request request(String tag, String clientInfo) {
        return new Request.Builder().url("https://example.com/a")
            .header(BuildTag.HEADER, tag)
            .header("X-Client-Info", clientInfo)
            .build();
    }

    private Object proxy() {
        return Proxy.newProxyInstance(this.loader, new Class<?>[] { Interceptor.class },
                new BuildTagInterceptor(BuildTag.fromCertificate(CERTIFICATE)));
    }

    private void intercept(Chain chain) throws IOException, ClassNotFoundException {
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
