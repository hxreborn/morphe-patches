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

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
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

    private final Map<String, String> sharedHeaders = new ConcurrentHashMap<>();

    private final BuildTagInterceptor interceptor = new BuildTagInterceptor(BuildTag.fromCertificate(CERTIFICATE),
            this.sharedHeaders);

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
    public void tagsARequestFromAnotherClientThatCopiesTheSharedHeaders()
            throws NoSuchAlgorithmException, IOException, ClassNotFoundException {
        // given
        this.sharedHeaders.put(BuildTag.HEADER, "own-certificate-tag");
        intercept(new Chain(request("own-certificate-tag", "{}")));

        // when
        Request sent = sendThroughAnotherClient();

        // then
        Assertions.assertThat(isSignedWithTheCertificate(sent.header(BuildTag.HEADER))).isTrue();
    }

    @Test
    public void retagsTheSharedHeadersAfterTheAppOverwritesThem()
            throws NoSuchAlgorithmException, IOException, ClassNotFoundException {
        // given
        intercept(new Chain(request("stock-tag", "{}")));
        this.sharedHeaders.put(BuildTag.HEADER, "own-certificate-tag");

        // when
        intercept(new Chain(request("stock-tag", "{}")));

        // then
        Assertions.assertThat(isSignedWithTheCertificate(this.sharedHeaders.get(BuildTag.HEADER))).isTrue();
    }

    @Test
    public void keepsTheSharedTagWhileTheAppLeavesItAlone() throws IOException, ClassNotFoundException {
        // given
        intercept(new Chain(request("stock-tag", "{}")));
        String first = this.sharedHeaders.get(BuildTag.HEADER);

        // when
        intercept(new Chain(request("stock-tag", "{}")));

        // then
        Assertions.assertThat(this.sharedHeaders.get(BuildTag.HEADER)).isEqualTo(first);
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
                this.interceptor);
    }

    private Request sendThroughAnotherClient() throws IOException {
        Request[] sent = new Request[1];
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor((chain) -> {
            Request.Builder copy = chain.request().newBuilder();
            for (Map.Entry<String, String> header : this.sharedHeaders.entrySet()) {
                copy.header(header.getKey(), header.getValue());
            }
            return chain.proceed(copy.build());
        }).addInterceptor((chain) -> {
            sent[0] = chain.request();
            return new Response.Builder().request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .build();
        }).build();
        client.newCall(new Request.Builder().url("https://example.com/b").build()).execute().close();
        return sent[0];
    }

    private static boolean isSignedWithTheCertificate(String tag) throws NoSuchAlgorithmException {
        String[] parts = tag.split("\\.");
        String seed = Digests.sha256Hex(CERTIFICATE) + "oneroom" + parts[0];
        return parts.length == 2 && parts[1].equals(Digests.md5Hex(seed.getBytes(StandardCharsets.UTF_8)));
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
