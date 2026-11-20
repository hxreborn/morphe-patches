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
package app.hxreborn.extension.ultimateguitar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public final class ApiResponseRewriterTest {

    private static final String FREE_USER = "{\"user_account\":{\"user_id\":1,\"username\":\"a\"},"
            + "\"service_access\":[{\"service_name\":\"android_ug_pro_account\",\"date_access\":1,\"lifetime\":0},"
            + "{\"service_name\":\"web_ug_tabs\",\"date_access\":5}]}";

    private static final String SERVICE_ACCESS_URL = "https://api.ultimate-guitar.com/api/v1/purchase/service/access"
            + "?token=1_a";

    private static final String API_HOST = "https://api.ultimate-guitar.com";

    private static final String NOT_FOUND = "{\"error\":{\"name\":\"Not Found\",\"status\":404}}";

    private static final String EXPERIMENTS_URL = "https://api.ultimate-guitar.com/api/v1/ab/test?token=1_a";

    private static final String EXPERIMENTS = "{\"experiments\":{\"8153\":2},"
            + "\"closed_experiments\":{\"8078\":2,\"8120\":2}}";

    @After
    public void clearPendingResponsePath() {
        ApiResponseRewriter.rewriteBody((String) null);
    }

    @Test
    public void answersTheServiceAccessRequestWithSuccess() {
        assertEquals(200, ApiResponseRewriter.rewriteStatus(SERVICE_ACCESS_URL, 404));
    }

    @Test
    public void keepsTheStatusOfAPathThatOnlyEndsLikeServiceAccess() {
        assertEquals(404, ApiResponseRewriter.rewriteStatus(API_HOST + "/api/v2/purchase/service/access", 404));
    }

    @Test
    public void keepsTheStatusOfOtherRequests() {
        assertEquals(404, ApiResponseRewriter.rewriteStatus(API_HOST + "/api/v1/purchase/content/access", 404));
    }

    @Test
    public void replacesANotFoundServiceAccessBodyWithEveryService() throws Exception {
        ApiResponseRewriter.rewriteStatus(SERVICE_ACCESS_URL, 404);

        byte[] granted = ApiResponseRewriter.rewriteBody(NOT_FOUND.getBytes(StandardCharsets.UTF_8));

        JSONArray services = new JSONArray(new String(granted, StandardCharsets.UTF_8));
        assertEquals(6, services.length());
        assertEquals(1, find(services, "android_ug_pro_account").getInt("lifetime"));
    }

    @Test
    public void keepsOtherServicesInTheServiceAccessList() throws Exception {
        ApiResponseRewriter.rewriteStatus(SERVICE_ACCESS_URL, 200);

        String granted = ApiResponseRewriter.rewriteBody("[{\"service_name\":\"web_ug_tabs\",\"date_access\":5}]");

        assertEquals(5, find(new JSONArray(granted), "web_ug_tabs").getInt("date_access"));
    }

    @Test
    public void leavesTheNextResponseAlone() {
        ApiResponseRewriter.rewriteStatus(SERVICE_ACCESS_URL, 404);
        ApiResponseRewriter.rewriteBody(NOT_FOUND);

        assertSame(NOT_FOUND, ApiResponseRewriter.rewriteBody(NOT_FOUND));
    }

    @Test
    public void grantsLifetimeProAccess() throws Exception {
        JSONObject granted = new JSONObject(ApiResponseRewriter.rewriteBody(FREE_USER));

        JSONObject pro = find(granted.getJSONArray("service_access"), "android_ug_pro_account");
        assertEquals(1, pro.getInt("lifetime"));
        assertTrue(pro.getLong("date_access") * 1000 > System.currentTimeMillis());
    }

    @Test
    public void replacesTheExistingEntryOfAGrantedService() throws Exception {
        JSONArray services = new JSONObject(ApiResponseRewriter.rewriteBody(FREE_USER)).getJSONArray("service_access");

        assertEquals(1, count(services, "android_ug_pro_account"));
    }

    @Test
    public void keepsServicesItDoesNotGrant() throws Exception {
        JSONArray services = new JSONObject(ApiResponseRewriter.rewriteBody(FREE_USER)).getJSONArray("service_access");

        assertEquals(5, find(services, "web_ug_tabs").getInt("date_access"));
    }

    @Test
    public void grantsANestedList() throws Exception {
        String body = "{\"data\":[{\"json\":{\"service_access\":[]}}]}";

        JSONArray services = new JSONObject(ApiResponseRewriter.rewriteBody(body)).getJSONArray("data")
            .getJSONObject(0)
            .getJSONObject("json")
            .getJSONArray("service_access");

        assertEquals(6, services.length());
    }

    @Test
    public void grantsAByteBody() throws Exception {
        byte[] body = FREE_USER.getBytes(StandardCharsets.UTF_8);

        String granted = new String(ApiResponseRewriter.rewriteBody(body), StandardCharsets.UTF_8);

        assertEquals(1,
                find(new JSONObject(granted).getJSONArray("service_access"), "android_ug_sing").getInt("lifetime"));
    }

    @Test
    public void keepsABodyWithoutServiceAccess() {
        String body = "{\"tabs\":[]}";

        assertSame(body, ApiResponseRewriter.rewriteBody(body));
    }

    @Test
    public void keepsMalformedJson() {
        String body = "{\"service_access\":[";

        assertSame(body, ApiResponseRewriter.rewriteBody(body));
    }

    @Test
    public void turnsOffWebPrintInEveryExperimentList() throws Exception {
        JSONObject experiments = rewrittenExperiments();

        assertEquals(1, experiments.getJSONObject("experiments").getInt("8078"));
        assertEquals(1, experiments.getJSONObject("closed_experiments").getInt("8078"));
    }

    @Test
    public void turnsOffThePdfUpsellInEveryExperimentList() throws Exception {
        JSONObject experiments = rewrittenExperiments();

        assertEquals(1, experiments.getJSONObject("experiments").getInt("7061"));
        assertEquals(1, experiments.getJSONObject("closed_experiments").getInt("7061"));
    }

    @Test
    public void turnsOnSing() throws Exception {
        JSONObject experiments = rewrittenExperiments();

        assertEquals(2, experiments.getJSONObject("experiments").getInt("8000"));
    }

    @Test
    public void keepsOtherExperiments() throws Exception {
        JSONObject experiments = rewrittenExperiments();

        assertEquals(2, experiments.getJSONObject("experiments").getInt("8153"));
        assertEquals(2, experiments.getJSONObject("closed_experiments").getInt("8120"));
    }

    @Test
    public void addsTheExperimentListWhenItIsMissing() throws Exception {
        ApiResponseRewriter.rewriteStatus(EXPERIMENTS_URL, 200);

        JSONObject experiments = new JSONObject(ApiResponseRewriter.rewriteBody("{\"closed_experiments\":{}}"));

        assertEquals(2, experiments.getJSONObject("experiments").getInt("8000"));
    }

    @Test
    public void keepsTheExperimentsStatus() {
        assertEquals(200, ApiResponseRewriter.rewriteStatus(EXPERIMENTS_URL, 200));
    }

    @Test
    public void keepsMalformedExperiments() {
        ApiResponseRewriter.rewriteStatus(EXPERIMENTS_URL, 200);

        assertEquals("{", ApiResponseRewriter.rewriteBody("{"));
    }

    @Test
    public void rewritesExperimentsInAByteBody() throws Exception {
        ApiResponseRewriter.rewriteStatus(EXPERIMENTS_URL, 200);

        byte[] body = ApiResponseRewriter.rewriteBody(EXPERIMENTS.getBytes(StandardCharsets.UTF_8));

        assertEquals(1,
                new JSONObject(new String(body, StandardCharsets.UTF_8)).getJSONObject("experiments").getInt("8078"));
    }

    private static JSONObject rewrittenExperiments() throws Exception {
        ApiResponseRewriter.rewriteStatus(EXPERIMENTS_URL, 200);
        return new JSONObject(ApiResponseRewriter.rewriteBody(EXPERIMENTS));
    }

    private static int count(JSONArray services, String name) throws Exception {
        int matches = 0;
        for (int i = 0; i < services.length(); i++) {
            if (name.equals(services.getJSONObject(i).getString("service_name"))) {
                matches++;
            }
        }
        return matches;
    }

    private static JSONObject find(JSONArray services, String name) throws Exception {
        for (int i = 0; i < services.length(); i++) {
            JSONObject service = services.getJSONObject(i);
            if (name.equals(service.getString("service_name"))) {
                return service;
            }
        }
        throw new AssertionError("no " + name);
    }

}
