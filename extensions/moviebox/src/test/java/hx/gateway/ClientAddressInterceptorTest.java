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
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public final class ClientAddressInterceptorTest {

    private static final String API = "https://api.example.com/wefeed-mobile-bff/subject-api/";

    private static final String RESOURCE_LIST = API + "resource/v2?subjectId=77&isVip=true";

    private static final String V1_RESOURCE_LIST = API + "resource?subjectId=77";

    private static final String RESOURCE_POSITION = API + "resource-position/v2?subjectId=77&resourceId=5";

    private static final String PLAY_INFO = API + "play-info/v2?subjectId=77";

    private static final String APP_ADDRESS = "103.145.50.154";

    private static final String LINKED_ROWS = "{\"data\":{\"list\":[{\"resolution\":360,"
            + "\"resourceLink\":\"https://bcdn.example.com/360.mp4\"},{\"resolution\":1080,"
            + "\"resourceLink\":\"https://bcdn.example.com/1080.mp4\"}]}}";

    private static final String ROWS_WITHOUT_HD_LINKS = "{\"data\":{\"list\":[{\"resolution\":360,"
            + "\"resourceLink\":\"https://bcdn.example.com/360.mp4\"},{\"resolution\":720,\"resourceLink\":\"\"},"
            + "{\"resolution\":1080,\"resourceLink\":\"\"}]}}";

    private static final String NO_LINK_WARNING = "2 of 3 rows have no link with the client address withheld";

    private final ClassLoader loader = getClass().getClassLoader();

    private final List<String> addressesSent = new ArrayList<>();

    @Test
    public void withholdsTheClientAddressFromResourceListRequests() throws IOException {
        // when
        send(RESOURCE_LIST, true, answering(200, LINKED_ROWS));

        // then
        Assertions.assertThat(this.addressesSent).containsExactly(ClientAddressInterceptor.WITHHELD_ADDRESS);
    }

    @Test
    public void withholdsTheClientAddressFromTheV1ResourceList() throws IOException {
        // when
        send(V1_RESOURCE_LIST, true, answering(200, LINKED_ROWS));

        // then
        Assertions.assertThat(this.addressesSent).containsExactly(ClientAddressInterceptor.WITHHELD_ADDRESS);
    }

    @Test
    public void withholdsTheClientAddressFromResourcePositionRequests() throws IOException {
        // when
        send(RESOURCE_POSITION, true, answering(200, LINKED_ROWS));

        // then
        Assertions.assertThat(this.addressesSent).containsExactly(ClientAddressInterceptor.WITHHELD_ADDRESS);
    }

    @Test
    public void keepsTheClientAddressOnOtherRequests() throws IOException {
        // when
        send(PLAY_INFO, true, answering(200, LINKED_ROWS));

        // then
        Assertions.assertThat(this.addressesSent).containsExactly(APP_ADDRESS);
    }

    @Test
    public void keepsTheClientAddressWithoutPremium() throws IOException {
        // when
        send(RESOURCE_LIST, false, answering(200, ROWS_WITHOUT_HD_LINKS));

        // then
        Assertions.assertThat(this.addressesSent).containsExactly(APP_ADDRESS);
    }

    @Test
    public void resendsTheAppRequestWhenTheWithheldAddressIsRefused() throws IOException {
        // given
        Interceptor refusingWithheldAddress = (chain) -> {
            boolean withheld = ClientAddressInterceptor.WITHHELD_ADDRESS
                .equals(chain.request().header(ClientAddressInterceptor.FORWARDED_FOR_HEADER));
            return withheld ? response(chain.request(), 403, "{}") : response(chain.request(), 200, LINKED_ROWS);
        };

        // when
        Response response = send(RESOURCE_LIST, true, refusingWithheldAddress);

        // then
        Assertions.assertThat(response.code()).isEqualTo(200);
        Assertions.assertThat(this.addressesSent)
            .containsExactly(ClientAddressInterceptor.WITHHELD_ADDRESS, APP_ADDRESS);
    }

    @Test
    public void failsWithoutResendingWhenTheWithheldRequestCannotConnect() {
        // given
        Interceptor unreachable = (chain) -> {
            throw new IOException("connection reset");
        };

        // when
        Throwable failure = Assertions.catchThrowable(() -> send(RESOURCE_LIST, true, unreachable));

        // then
        Assertions.assertThat(failure).isInstanceOf(IOException.class);
        Assertions.assertThat(this.addressesSent).containsExactly(ClientAddressInterceptor.WITHHELD_ADDRESS);
    }

    @Test
    public void warnsWhenRowsStillHaveNoLinkWithTheAddressWithheld() throws IOException {
        // when
        Response response = send(RESOURCE_LIST, true, answering(200, ROWS_WITHOUT_HD_LINKS));

        // then
        Assertions.assertThat(response.body().string()).isEqualTo(ROWS_WITHOUT_HD_LINKS);
        Assertions.assertThat(ShadowLog.getLogs()).anyMatch((entry) -> entry.msg.endsWith(NO_LINK_WARNING));
    }

    @Test
    public void staysQuietWhenEveryRowHasALink() throws IOException {
        // when
        send(RESOURCE_LIST, true, answering(200, LINKED_ROWS));

        // then
        Assertions.assertThat(ShadowLog.getLogs()).noneMatch((entry) -> entry.msg.contains("have no link"));
    }

    private Response send(String url, boolean premiumUnlocked, Interceptor server) throws IOException {
        Interceptor appAddress = (chain) -> chain.proceed(chain.request()
            .newBuilder()
            .header(ClientAddressInterceptor.FORWARDED_FOR_HEADER, APP_ADDRESS)
            .build());
        Interceptor recorder = (chain) -> {
            this.addressesSent.add(chain.request().header(ClientAddressInterceptor.FORWARDED_FOR_HEADER));
            return server.intercept(chain);
        };
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(appAddress)
            .addInterceptor(proxy(premiumUnlocked))
            .addInterceptor(recorder)
            .build();
        return client.newCall(new Request.Builder().url(url).build()).execute();
    }

    private Interceptor proxy(boolean premiumUnlocked) {
        return (Interceptor) Proxy.newProxyInstance(this.loader, new Class<?>[] { Interceptor.class },
                new ClientAddressInterceptor(premiumUnlocked));
    }

    private static Interceptor answering(int code, String body) {
        return (chain) -> response(chain.request(), code, body);
    }

    private static Response response(Request request, int code, String body) {
        return new Response.Builder().request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("HTTP " + code)
            .body(ResponseBody.create(body, MediaType.get("application/json")))
            .build();
    }

}
