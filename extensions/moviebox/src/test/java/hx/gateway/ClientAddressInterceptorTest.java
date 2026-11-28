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
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.assertj.core.api.Assertions;
import org.json.JSONException;
import org.json.JSONObject;
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

    private static final String HOME_FEED = "https://api.example.com/wefeed-mobile-bff/tab-operating-page?tabCode=Trending";

    private static final String AD_DISTRIBUTION = "https://ads.example.com/distribute/candidate";

    private static final String REGION_BLOCK = "{\"code\":403,\"reason\":\"FORBIDDEN\",\"message\":\""
            + ClientAddressInterceptor.REGION_BLOCK_MESSAGE + "\",\"metadata\":{}}";

    private static final String FEED = "{\"code\":0,\"message\":\"ok\",\"data\":{\"items\":[]}}";

    private static final String INDIAN_CARRIER_INFO = "{\"sp_code\":\"405840\",\"region\":\"IN\"}";

    private static final String DIRECT_FILE_CARRIER_INFO = "{\"sp_code\":\"21403\"}";

    private static final String UNREADABLE_CLIENT_INFO = "sp_code=405840";

    private static final String NO_LINK_WARNING = "2 of 3 rows have no link with the client address withheld";

    private final ClassLoader loader = getClass().getClassLoader();

    private final List<String> addressesSent = new ArrayList<>();

    private final List<String> clientInfosSent = new ArrayList<>();

    private String laboratoryCountry = "";

    private String appClientInfo;

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
    public void withholdsTheClientAddressFromPlayInfoRequests() throws IOException {
        // when
        send(PLAY_INFO, true, answering(200, FEED));

        // then
        Assertions.assertThat(this.addressesSent).containsExactly(ClientAddressInterceptor.WITHHELD_ADDRESS);
    }

    @Test
    public void keepsTheClientAddressOnOtherRequests() throws IOException {
        // when
        send(HOME_FEED, true, answering(200, FEED));

        // then
        Assertions.assertThat(this.addressesSent).containsExactly(APP_ADDRESS);
    }

    @Test
    public void withholdsTheCarrierFromPlayInfoRequests() throws IOException, JSONException {
        // given
        this.appClientInfo = INDIAN_CARRIER_INFO;

        // when
        send(PLAY_INFO, true, answering(200, FEED));

        // then
        JSONObject sent = new JSONObject(this.clientInfosSent.get(0));
        Assertions.assertThat(sent.getString(ClientAddressInterceptor.CARRIER_FIELD))
            .isEqualTo(ClientAddressInterceptor.WITHHELD_CARRIER);
        Assertions.assertThat(sent.getString("region")).isEqualTo("IN");
    }

    @Test
    public void withholdsTheCarrierFromResourceListRequests() throws IOException, JSONException {
        // given
        this.appClientInfo = INDIAN_CARRIER_INFO;

        // when
        send(RESOURCE_LIST, true, answering(200, LINKED_ROWS));

        // then
        Assertions
            .assertThat(new JSONObject(this.clientInfosSent.get(0)).getString(ClientAddressInterceptor.CARRIER_FIELD))
            .isEqualTo(ClientAddressInterceptor.WITHHELD_CARRIER);
    }

    @Test
    public void keepsTheCarrierOfResourcePositionRequests() throws IOException {
        // given
        this.appClientInfo = DIRECT_FILE_CARRIER_INFO;

        // when
        send(RESOURCE_POSITION, true, answering(200, LINKED_ROWS));

        // then
        Assertions.assertThat(this.clientInfosSent).containsExactly(DIRECT_FILE_CARRIER_INFO);
    }

    @Test
    public void keepsTheCarrierOnOtherRequests() throws IOException {
        // given
        this.appClientInfo = INDIAN_CARRIER_INFO;

        // when
        send(HOME_FEED, true, answering(200, FEED));

        // then
        Assertions.assertThat(this.clientInfosSent).containsExactly(INDIAN_CARRIER_INFO);
    }

    @Test
    public void keepsTheCarrierWithoutPremium() throws IOException {
        // given
        this.appClientInfo = INDIAN_CARRIER_INFO;

        // when
        send(PLAY_INFO, false, answering(200, FEED));

        // then
        Assertions.assertThat(this.clientInfosSent).containsExactly(INDIAN_CARRIER_INFO);
    }

    @Test
    public void sendsUnreadableClientInfoUnchanged() throws IOException {
        // given
        this.appClientInfo = UNREADABLE_CLIENT_INFO;

        // when
        send(PLAY_INFO, true, answering(200, FEED));

        // then
        Assertions.assertThat(this.clientInfosSent).containsExactly(UNREADABLE_CLIENT_INFO);
        Assertions.assertThat(this.addressesSent).containsExactly(ClientAddressInterceptor.WITHHELD_ADDRESS);
    }

    @Test
    public void withholdsTheCarrierFromARegionBlockedRequest() throws IOException, JSONException {
        // given
        this.appClientInfo = INDIAN_CARRIER_INFO;

        // when
        send(HOME_FEED, false, blockingTheAppAddress());

        // then
        Assertions.assertThat(this.clientInfosSent.get(0)).isEqualTo(INDIAN_CARRIER_INFO);
        Assertions
            .assertThat(new JSONObject(this.clientInfosSent.get(1)).getString(ClientAddressInterceptor.CARRIER_FIELD))
            .isEqualTo(ClientAddressInterceptor.WITHHELD_CARRIER);
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
        // when
        Response response = send(RESOURCE_LIST, true, refusingTheWithheldAddress());

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
    public void resendsARegionBlockedRequestWithTheAddressWithheld() throws IOException {
        // when
        Response response = send(HOME_FEED, false, blockingTheAppAddress());

        // then
        Assertions.assertThat(response.body().string()).isEqualTo(FEED);
        Assertions.assertThat(this.addressesSent)
            .containsExactly(APP_ADDRESS, ClientAddressInterceptor.WITHHELD_ADDRESS);
    }

    @Test
    public void withholdsTheAddressFromEveryFirstAttemptAfterARegionBlock() throws IOException {
        // given
        Interceptor interceptor = proxy(false);
        send(HOME_FEED, interceptor, blockingTheAppAddress());
        this.addressesSent.clear();

        // when
        send(PLAY_INFO, interceptor, blockingTheAppAddress());

        // then
        Assertions.assertThat(this.addressesSent).containsExactly(ClientAddressInterceptor.WITHHELD_ADDRESS);
    }

    @Test
    public void resendsTheAppRequestOnceWhenTheWithheldAddressIsRefusedAfterARegionBlock() throws IOException {
        // given
        Interceptor interceptor = proxy(true);
        send(HOME_FEED, interceptor, blockingTheAppAddress());
        this.addressesSent.clear();

        // when
        Response response = send(RESOURCE_LIST, interceptor, refusingTheWithheldAddress());

        // then
        Assertions.assertThat(response.code()).isEqualTo(200);
        Assertions.assertThat(this.addressesSent)
            .containsExactly(ClientAddressInterceptor.WITHHELD_ADDRESS, APP_ADDRESS);
    }

    @Test
    public void resendsTheAppRequestOnceWhenTheWithheldAddressIsRefusedAfterAHomeFeedRegionBlock() throws IOException {
        // given
        Interceptor interceptor = proxy(false);
        send(HOME_FEED, interceptor, blockingTheAppAddress());
        this.addressesSent.clear();

        // when
        Response response = send(HOME_FEED, interceptor, refusingTheWithheldAddress());

        // then
        Assertions.assertThat(response.code()).isEqualTo(200);
        Assertions.assertThat(this.addressesSent)
            .containsExactly(ClientAddressInterceptor.WITHHELD_ADDRESS, APP_ADDRESS);
    }

    @Test
    public void sendsAPostOnceWhenItFailsAfterARegionBlock() throws IOException {
        // given
        Interceptor interceptor = proxy(false);
        send(HOME_FEED, interceptor, blockingTheAppAddress());
        this.addressesSent.clear();

        // when
        Response response = send(post(HOME_FEED), interceptor, answering(500, "{}"));

        // then
        Assertions.assertThat(response.code()).isEqualTo(500);
        Assertions.assertThat(this.addressesSent).containsExactly(ClientAddressInterceptor.WITHHELD_ADDRESS);
    }

    @Test
    public void sendsAGetOnceWhenItFailsWithAServerErrorAfterARegionBlock() throws IOException {
        // given
        Interceptor interceptor = proxy(false);
        send(HOME_FEED, interceptor, blockingTheAppAddress());
        this.addressesSent.clear();

        // when
        Response response = send(HOME_FEED, interceptor, answering(500, "{}"));

        // then
        Assertions.assertThat(response.code()).isEqualTo(500);
        Assertions.assertThat(this.addressesSent).containsExactly(ClientAddressInterceptor.WITHHELD_ADDRESS);
    }

    @Test
    public void sendsTheDeviceAddressWithoutResendingTheBlockedLaboratoryCountryAddress() throws IOException {
        // given
        Interceptor interceptor = proxy(false);
        this.laboratoryCountry = "us";
        send(HOME_FEED, interceptor, blocking(CountryAddresses.addressIn("US")));
        this.addressesSent.clear();

        // when
        Response response = send(HOME_FEED, interceptor,
                refusingTheWithheldAddressAndBlocking(CountryAddresses.addressIn("US")));

        // then
        Assertions.assertThat(response.code()).isEqualTo(200);
        Assertions.assertThat(this.addressesSent)
            .containsExactly(ClientAddressInterceptor.WITHHELD_ADDRESS, APP_ADDRESS);
    }

    @Test
    public void passesOnAForbiddenAnswerThatIsNoRegionBlock() throws IOException {
        // when
        Response response = send(HOME_FEED, false, answering(403, "{}"));

        // then
        Assertions.assertThat(response.code()).isEqualTo(403);
        Assertions.assertThat(this.addressesSent).containsExactly(APP_ADDRESS);
    }

    @Test
    public void sendsAnAddressInTheLaboratoryCountry() throws IOException {
        // given
        this.laboratoryCountry = "nl";

        // when
        send(HOME_FEED, true, answering(200, FEED));

        // then
        Assertions.assertThat(this.addressesSent).containsExactly(CountryAddresses.addressIn("NL"));
    }

    @Test
    public void followsAChangedLaboratoryCountry() throws IOException {
        // given
        Interceptor interceptor = proxy(true);
        this.laboratoryCountry = "nl";
        send(HOME_FEED, interceptor, answering(200, FEED));
        this.laboratoryCountry = "";

        // when
        send(HOME_FEED, interceptor, answering(200, FEED));

        // then
        Assertions.assertThat(this.addressesSent).containsExactly(CountryAddresses.addressIn("NL"), APP_ADDRESS);
    }

    @Test
    public void keepsWithholdingTheAddressFromResourceListsWithALaboratoryCountry() throws IOException {
        // given
        this.laboratoryCountry = "in";

        // when
        send(RESOURCE_LIST, true, answering(200, LINKED_ROWS));

        // then
        Assertions.assertThat(this.addressesSent).containsExactly(ClientAddressInterceptor.WITHHELD_ADDRESS);
    }

    @Test
    public void keepsTheClientAddressForACountryWithoutAnAddress() throws IOException {
        // given
        this.laboratoryCountry = "n/a";

        // when
        send(HOME_FEED, true, answering(200, FEED));

        // then
        Assertions.assertThat(this.addressesSent).containsExactly(APP_ADDRESS);
    }

    @Test
    public void withholdsTheAddressWhenTheLaboratoryCountryIsRegionBlocked() throws IOException {
        // given
        this.laboratoryCountry = "us";

        // when
        Response response = send(HOME_FEED, true, blocking(CountryAddresses.addressIn("US")));

        // then
        Assertions.assertThat(response.body().string()).isEqualTo(FEED);
        Assertions.assertThat(this.addressesSent)
            .containsExactly(CountryAddresses.addressIn("US"), ClientAddressInterceptor.WITHHELD_ADDRESS);
    }

    @Test
    public void keepsTheClientAddressAfterABlockedLaboratoryCountryIsCleared() throws IOException {
        // given
        Interceptor interceptor = proxy(true);
        this.laboratoryCountry = "us";
        send(HOME_FEED, interceptor, blocking(CountryAddresses.addressIn("US")));
        this.addressesSent.clear();
        this.laboratoryCountry = "";

        // when
        send(HOME_FEED, interceptor, blocking(CountryAddresses.addressIn("US")));

        // then
        Assertions.assertThat(this.addressesSent).containsExactly(APP_ADDRESS);
    }

    @Test
    public void resendsWithTheClientAddressWhenTheCountryAddressIsRefused() throws IOException {
        // given
        this.laboratoryCountry = "nl";

        // when
        Response response = send(HOME_FEED, true, refusing(CountryAddresses.addressIn("NL")));

        // then
        Assertions.assertThat(response.code()).isEqualTo(200);
        Assertions.assertThat(this.addressesSent).containsExactly(CountryAddresses.addressIn("NL"), APP_ADDRESS);
    }

    @Test
    public void triesTheCountryAddressAgainAfterARefusal() throws IOException {
        // given
        Interceptor interceptor = proxy(true);
        this.laboratoryCountry = "nl";
        send(HOME_FEED, interceptor, refusing(CountryAddresses.addressIn("NL")));
        this.addressesSent.clear();

        // when
        send(HOME_FEED, interceptor, answering(200, FEED));

        // then
        Assertions.assertThat(this.addressesSent).containsExactly(CountryAddresses.addressIn("NL"));
    }

    @Test
    public void keepsTheClientAddressOutsideTheApi() throws IOException {
        // given
        this.laboratoryCountry = "nl";

        // when
        send(AD_DISTRIBUTION, true, answering(200, FEED));

        // then
        Assertions.assertThat(this.addressesSent).containsExactly(APP_ADDRESS);
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
        return send(url, proxy(premiumUnlocked), server);
    }

    private Response send(String url, Interceptor interceptor, Interceptor server) throws IOException {
        return send(new Request.Builder().url(url).build(), interceptor, server);
    }

    private Response send(Request app, Interceptor interceptor, Interceptor server) throws IOException {
        Interceptor appAddress = (chain) -> {
            Request.Builder request = chain.request()
                .newBuilder()
                .header(ClientAddressInterceptor.FORWARDED_FOR_HEADER, APP_ADDRESS);
            if (this.appClientInfo != null) {
                request.header(ClientAddressInterceptor.CLIENT_INFO_HEADER, this.appClientInfo);
            }
            return chain.proceed(request.build());
        };
        Interceptor recorder = (chain) -> {
            this.addressesSent.add(chain.request().header(ClientAddressInterceptor.FORWARDED_FOR_HEADER));
            this.clientInfosSent.add(chain.request().header(ClientAddressInterceptor.CLIENT_INFO_HEADER));
            return server.intercept(chain);
        };
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(appAddress)
            .addInterceptor(interceptor)
            .addInterceptor(recorder)
            .build();
        return client.newCall(app).execute();
    }

    private Interceptor proxy(boolean premiumUnlocked) {
        return (Interceptor) Proxy.newProxyInstance(this.loader, new Class<?>[] { Interceptor.class },
                new ClientAddressInterceptor(premiumUnlocked, () -> this.laboratoryCountry));
    }

    private static Interceptor blockingTheAppAddress() {
        return (chain) -> {
            boolean withheld = ClientAddressInterceptor.WITHHELD_ADDRESS
                .equals(chain.request().header(ClientAddressInterceptor.FORWARDED_FOR_HEADER));
            return withheld ? response(chain.request(), 200, FEED) : response(chain.request(), 403, REGION_BLOCK);
        };
    }

    private static Interceptor refusingTheWithheldAddress() {
        return (chain) -> {
            boolean withheld = ClientAddressInterceptor.WITHHELD_ADDRESS
                .equals(chain.request().header(ClientAddressInterceptor.FORWARDED_FOR_HEADER));
            return withheld ? response(chain.request(), 403, "{}") : response(chain.request(), 200, LINKED_ROWS);
        };
    }

    private static Interceptor refusingTheWithheldAddressAndBlocking(String address) {
        return (chain) -> {
            String sent = chain.request().header(ClientAddressInterceptor.FORWARDED_FOR_HEADER);
            if (ClientAddressInterceptor.WITHHELD_ADDRESS.equals(sent)) {
                return response(chain.request(), 403, "{}");
            }
            return address.equals(sent) ? response(chain.request(), 403, REGION_BLOCK)
                    : response(chain.request(), 200, FEED);
        };
    }

    private static Request post(String url) {
        return new Request.Builder().url(url).post(RequestBody.create("{}", MediaType.get("application/json"))).build();
    }

    private static Interceptor blocking(String address) {
        return (chain) -> {
            boolean blocked = address.equals(chain.request().header(ClientAddressInterceptor.FORWARDED_FOR_HEADER));
            return blocked ? response(chain.request(), 403, REGION_BLOCK) : response(chain.request(), 200, FEED);
        };
    }

    private static Interceptor refusing(String address) {
        return (chain) -> {
            boolean refused = address.equals(chain.request().header(ClientAddressInterceptor.FORWARDED_FOR_HEADER));
            return refused ? response(chain.request(), 429, "{}") : response(chain.request(), 200, FEED);
        };
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
