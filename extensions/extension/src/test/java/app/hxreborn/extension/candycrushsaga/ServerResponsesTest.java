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
package app.hxreborn.extension.candycrushsaga;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.junit.Assert;

@RunWith(RobolectricTestRunner.class)
public class ServerResponsesTest {

    private static final int EXCESSIVE_JSON_DEPTH = 100;

    private static final int ACCEPTED_JSON_DEPTH = 64;

    private static final int JSON_NODE_LIMIT = 100000;

    private static final int RPC_RESULT_DEPTH = 2;

    private static final String JSON_ESCAPE = "\\";

    private static final String METHOD = "CandyPopupConfigApi.getPopupUpdatedConfig";

    private static final String CONFIG = "{\"configuration_states\":[{\"name\":\"win\","
            + "\"block_list\":{\"categories\":[\"buy_lives\"],\"modal_ids\":[\"keep\"]}}]}";

    private static final int MAX_RESPONSE_BYTES = 8 * 1024 * 1024;

    @Test
    public void preserveUnrelatedReplyBytes() throws JSONException {
        String transactions = "{\"id\":2,\"result\":{\"contentResolutionId\":1234567890123456789}}";
        String response = "[" + transactions + ",{\"id\":1,\"result\":{\"configJsonString\":" + JSONObject.quote(CONFIG)
                + "}}]";
        String edited = ServerResponses.edit(response, Collections.singletonMap("1", METHOD), Collections.emptySet());
        Assert.assertTrue(edited.startsWith("[" + transactions + ","));
        Assert.assertTrue(edited.contains("dynamic_offer"));
    }

    @Test
    public void preserveRepliesAroundMultipleEditedResults() throws JSONException {
        String before = "{\"id\":0,\"result\":1234567890123456789}";
        String between = "{\"id\":2,\"result\":5782680582980523681}";
        String after = "{\"id\":4,\"result\":9223372036854775807}";
        String result = "\"result\":{\"configJsonString\":" + JSONObject.quote(CONFIG) + "}";
        String response = "[" + before + ",{\"id\":1," + result + "}," + between + ",{\"id\":3," + result + "}," + after
                + "]";
        Map<String, String> methods = new HashMap<>();
        methods.put("1", METHOD);
        methods.put("3", METHOD);
        String edited = ServerResponses.edit(response, methods, Collections.emptySet());
        Assert.assertTrue(edited.startsWith("[" + before + ","));
        Assert.assertTrue(edited.contains("," + between + ","));
        Assert.assertTrue(edited.endsWith("," + after + "]"));
        Assert.assertFalse(edited.contains(JSONObject.quote(CONFIG)));
    }

    @Test
    public void preserveLargeCatalogWhileEditingPopup() throws JSONException {
        String catalog = "{\"id\":0,\"result\":{\"items\":" + flatArray(JSON_NODE_LIMIT) + "}}";
        String response = "[" + catalog + ",{\"id\":1,\"result\":{\"configJsonString\":" + JSONObject.quote(CONFIG)
                + "}}]";
        String edited = ServerResponses.edit(response, Collections.singletonMap("1", METHOD), Collections.emptySet());
        Assert.assertTrue(edited.startsWith("[" + catalog + ","));
        Assert.assertTrue(edited.contains("dynamic_offer"));
    }

    @Test
    public void selectConfigBesideLargeUnrelatedRequest() throws JSONException {
        String request = "[{\"id\":0,\"method\":\"inventory\",\"params\":" + flatArray(JSON_NODE_LIMIT)
                + "},{\"id\":1,\"method\":\"" + METHOD + "\"}]";
        Assert.assertEquals(Collections.singletonMap("1", METHOD),
                ServerResponses.methods(request, Collections.singleton(METHOD)));
    }

    @Test
    public void preserveEscapedStringValues() throws JSONException {
        String value = "\n\t\r\\\"/\b\f";
        Assert.assertEquals(value, JsonText.parse(JSONObject.quote(value)).string());
    }

    @Test(expected = JSONException.class)
    public void rejectMalformedEscapeInSkippedResult() throws JSONException {
        JsonText.parseShallow("[{\"id\":0,\"result\":{\"value\":\"\\q\"}}]");
    }

    @Test
    public void rejectNonStringMethodBeforeExpandingItsContents() {
        String request = "[{\"id\":0,\"method\":{\"items\":" + flatArray(JSON_NODE_LIMIT) + "}},{\"id\":1,\"method\":\""
                + METHOD + "\"}]";
        try {
            ServerResponses.methods(request, Collections.singleton(METHOD));
            Assert.fail("Non-string method accepted");
        } catch (JSONException ex) {
            Assert.assertEquals("JSON string expected", ex.getMessage());
        }
    }

    @Test
    public void acceptUnicodeEscapeInSkippedValue() throws JSONException {
        String text = "[{\"result\":{\"value\":\"" + JSON_ESCAPE + "u0061\"}}]";
        Assert.assertEquals(text, JsonText.parseShallow(text).raw());
    }

    @Test(expected = JSONException.class)
    public void rejectMalformedUnicodeEscapeInSkippedValue() throws JSONException {
        JsonText.parseShallow("[{\"result\":{\"value\":\"" + JSON_ESCAPE + "u12x4\"}}]");
    }

    @Test(expected = JSONException.class)
    public void rejectTruncatedUnicodeEscapeInSkippedValue() throws JSONException {
        JsonText.parseShallow("[{\"result\":{\"value\":\"" + JSON_ESCAPE + "u12\"}}]");
    }

    @Test
    public void acceptShallowDepthBoundary() throws JSONException {
        String text = "[{\"result\":" + nestedArray(ACCEPTED_JSON_DEPTH - RPC_RESULT_DEPTH) + "}]";
        Assert.assertEquals(text, JsonText.parseShallow(text).raw());
    }

    @Test(expected = JSONException.class)
    public void rejectFirstExcessShallowDepth() throws JSONException {
        JsonText.parseShallow("[{\"result\":" + nestedArray(ACCEPTED_JSON_DEPTH - RPC_RESULT_DEPTH + 1) + "}]");
    }

    private static String nestedArray(int depth) {
        StringBuilder text = new StringBuilder();
        for (int index = 0; index < depth; index++) {
            text.append('[');
        }
        for (int index = 0; index < depth; index++) {
            text.append(']');
        }
        return text.toString();
    }

    @Test
    public void preserveExistingBlockedCategories() throws JSONException {
        String edited = ServerResponses.blockOffers(CONFIG);
        Assert.assertTrue(edited.contains("\"buy_lives\""));
        Assert.assertTrue(edited.contains("\"modal_ids\":[\"keep\"]"));
    }

    @Test
    public void keepRepeatedEditsUnchanged() throws JSONException {
        String edited = ServerResponses.blockOffers(CONFIG);
        Assert.assertEquals(edited, ServerResponses.blockOffers(edited));
    }

    @Test
    public void preserveUntargetedBatches() throws JSONException {
        String response = "[ {\"id\":2, \"result\":1234567890123456789} ]";
        Assert.assertEquals(response,
                ServerResponses.edit(response, Collections.singletonMap("1", METHOD), Collections.emptySet()));
    }

    @Test
    public void addMissingDefaultBlockList() throws JSONException {
        String config = "{\"configuration_states\":[{\"name\":\"default\"}]}";
        Assert.assertTrue(ServerResponses.blockOffers(config).contains("dynamic_offer"));
    }

    @Test
    public void disableAdOffersWithoutChangingAdConfiguration() throws JSONException {
        String response = "[{\"id\":1,\"result\":[{\"name\":\"ads_ool\",\"enabled\":true,"
                + "\"contentResolutionId\":5782680582980523681},{\"name\":\"ads_config\",\"enabled\":true}]}]";
        String edited = ServerResponses.edit(response,
                Collections.singletonMap("1", "UopFeatureStatusApi.getAllPlugins"), Collections.singleton("ads_ool"));
        Assert.assertEquals(response.replace("\"ads_ool\",\"enabled\":true", "\"ads_ool\",\"enabled\":false"), edited);
    }

    @Test
    public void clearRatingLevelsWithoutChangingOtherSettings() throws JSONException {
        String response = "[{\"id\":1,\"result\":[{\"name\":\"ccsm.rating.levels\",\"value\":\"36,68\"},"
                + "{\"name\":\"showSplashScreen\",\"value\":\"true\"}]}]";
        Assert.assertEquals(response.replace("36,68", ""), ServerResponses.edit(response,
                Collections.singletonMap("1", "ApplicationSettingsApi.getSettings"), Collections.emptySet()));
    }

    @Test
    public void keepAdOffersWhenOnlyRatingPromptsAreSelected() throws JSONException {
        String response = "[{\"id\":1,\"result\":[{\"name\":\"rate_app\",\"enabled\":true},"
                + "{\"name\":\"ads_ool\",\"enabled\":true}]}]";
        Assert.assertEquals(response.replace("\"rate_app\",\"enabled\":true", "\"rate_app\",\"enabled\":false"),
                ServerResponses.edit(response, Collections.singletonMap("1", "UopFeatureStatusApi.getAllPlugins"),
                        Collections.singleton("rate_app")));
    }

    @Test
    public void disableSelectedEventsWithoutChangingDailyRewards() throws JSONException {
        String response = "[{\"id\":1,\"result\":[{\"name\":\"episode_race\",\"enabled\":true},"
                + "{\"name\":\"dailystamp\",\"enabled\":true}]}]";
        Assert.assertEquals(response.replace("\"episode_race\",\"enabled\":true", "\"episode_race\",\"enabled\":false"),
                ServerResponses.edit(response, Collections.singletonMap("1", "UopFeatureStatusApi.getAllPlugins"),
                        Collections.singleton("episode_race")));
    }

    @Test
    public void acceptDepthBoundary() throws JSONException {
        StringBuilder text = new StringBuilder();
        for (int index = 0; index < ACCEPTED_JSON_DEPTH; index++) {
            text.append('[');
        }
        for (int index = 0; index < ACCEPTED_JSON_DEPTH; index++) {
            text.append(']');
        }
        Assert.assertEquals(text.toString(), JsonText.parse(text.toString()).raw());
    }

    @Test
    public void acceptNodeBoundary() throws JSONException {
        String text = flatArray(JSON_NODE_LIMIT - 1);
        Assert.assertEquals(text, JsonText.parse(text).raw());
    }

    @Test(expected = JSONException.class)
    public void rejectFirstExcessNode() throws JSONException {
        JsonText.parse(flatArray(JSON_NODE_LIMIT));
    }

    private static String flatArray(int entries) {
        StringBuilder text = new StringBuilder("[");
        for (int index = 0; index < entries; index++) {
            if (index > 0) {
                text.append(',');
            }
            text.append('0');
        }
        return text.append(']').toString();
    }

    @Test(expected = JSONException.class)
    public void rejectDuplicateKeys() throws JSONException {
        JsonText.parse("{\"a\":1,\"a\":2}");
    }

    @Test(expected = JSONException.class)
    public void rejectExcessiveDepth() throws JSONException {
        StringBuilder text = new StringBuilder();
        for (int index = 0; index < EXCESSIVE_JSON_DEPTH; index++) {
            text.append('[');
        }
        for (int index = 0; index < EXCESSIVE_JSON_DEPTH; index++) {
            text.append(']');
        }
        JsonText.parse(text.toString());
    }

    @Test(expected = JSONException.class)
    public void rejectAlternateSeparators() throws JSONException {
        JsonText.parse("{\"a\"=>[]}");
    }

    @Test
    public void clearSodaRatingLevelsWithoutChangingSagaSettings() throws JSONException {
        String response = "[{\"id\":1,\"result\":[{\"name\":\"mobile.rate.app.levels\",\"value\":\"25,75\"},"
                + "{\"name\":\"ccsm.rating.levels\",\"value\":\"36,68\"}]}]";
        Assert.assertEquals(response.replace("25,75", ""),
                ServerResponses.edit(response, Collections.singletonMap("1", "ApplicationSettingsApi.getSettings"),
                        Collections.emptySet(), true, false));
    }

    @Test
    public void blockSodaOfferPlacementsWithoutDisablingFeatures() throws JSONException {
        String blocked = JSONObject.quote("[\"keep\"]");
        String response = "[{\"id\":1,\"result\":[{\"name\":\"pf_diorama_popup\",\"enabled\":true,"
                + "\"parameters\":[{\"key\":\"blocklist-filter\",\"value\":" + blocked + "}]}]}]";
        String edited = ServerResponses.edit(response,
                Collections.singletonMap("1", "UopFeatureStatusApi.getAllPlugins"), Collections.emptySet(), true, true);
        Assert.assertTrue(edited.contains("DynamicOfferPostGameModalPlacement"));
        Assert.assertTrue(edited.contains("\"enabled\":true"));
        Assert.assertTrue(edited.contains("keep"));
        Assert.assertEquals(edited,
                ServerResponses.edit(edited, Collections.singletonMap("1", "UopFeatureStatusApi.getAllPlugins"),
                        Collections.emptySet(), true, true));
    }

    @Test
    public void preserveSodaOfferPlacementsWhenOnlyAdsAreSelected() throws JSONException {
        String response = "[{\"id\":1,\"result\":[{\"name\":\"pf_diorama_popup\",\"parameters\":["
                + "{\"key\":\"blocklist-filter\",\"value\":\"[]\"}]}]}]";
        Assert.assertEquals(response,
                ServerResponses.edit(response, Collections.singletonMap("1", "UopFeatureStatusApi.getAllPlugins"),
                        Collections.emptySet(), true, false));
    }

    @Test(expected = JSONException.class)
    public void rejectSodaPopupBlocklistWithWrongShape() throws JSONException {
        String response = "[{\"id\":1,\"result\":[{\"name\":\"pf_diorama_popup\",\"parameters\":["
                + "{\"key\":\"blocklist-filter\",\"value\":\"{}\"}]}]}]";
        ServerResponses.edit(response, Collections.singletonMap("1", "UopFeatureStatusApi.getAllPlugins"),
                Collections.emptySet(), true, true);
    }

    @Test
    public void addSodaPopupBlocklistToEmptyParameters() throws JSONException {
        String response = "[{\"id\":1,\"result\":[{\"name\":\"pf_diorama_popup\",\"parameters\":[]}]}]";
        String edited = ServerResponses.edit(response,
                Collections.singletonMap("1", "UopFeatureStatusApi.getAllPlugins"), Collections.emptySet(), true, true);
        Assert.assertTrue(edited.contains("DynamicOfferPostGameModalPlacement"));
        Assert.assertTrue(edited.contains("blocklist-filter"));
    }

    @Test
    public void addSodaPopupBlocklistWithoutRemovingOtherParameters() throws JSONException {
        String response = "[{\"id\":1,\"result\":[{\"name\":\"pf_diorama_popup\",\"parameters\":["
                + "{\"key\":\"keep\",\"value\":\"same\"}]}]}]";
        String edited = ServerResponses.edit(response,
                Collections.singletonMap("1", "UopFeatureStatusApi.getAllPlugins"), Collections.emptySet(), true, true);
        Assert.assertTrue(edited.contains("DynamicOfferPostGameModalPlacement"));
        Assert.assertTrue(edited.contains("{\"key\":\"keep\",\"value\":\"same\"}"));
    }

    @Test
    public void addSodaPopupBlocklistWhenParametersAreAbsent() throws JSONException {
        String response = "[{\"id\":1,\"result\":[{\"name\":\"pf_diorama_popup\",\"enabled\":true}]}]";
        String edited = ServerResponses.edit(response,
                Collections.singletonMap("1", "UopFeatureStatusApi.getAllPlugins"), Collections.emptySet(), true, true);
        Assert.assertTrue(edited.contains("DynamicOfferPostGameModalPlacement"));
        Assert.assertTrue(edited.contains("\"enabled\":true"));
    }

    @Test
    public void acceptSodaPopupEditsAtResponseByteLimit() throws JSONException {
        String response = sodaPopupAtEditedLimit(0);
        String edited = ServerResponses.edit(response,
                Collections.singletonMap("1", "UopFeatureStatusApi.getAllPlugins"), Collections.emptySet(), true, true);
        Assert.assertEquals(MAX_RESPONSE_BYTES, edited.length());
    }

    @Test(expected = JSONException.class)
    public void rejectSodaPopupEditsAboveResponseByteLimit() throws JSONException {
        ServerResponses.edit(sodaPopupAtEditedLimit(1),
                Collections.singletonMap("1", "UopFeatureStatusApi.getAllPlugins"), Collections.emptySet(), true, true);
    }

    private static String sodaPopupAtEditedLimit(int excessBytes) throws JSONException {
        String response = "[{\"id\":1,\"padding\":\"\",\"result\":[{\"name\":\"pf_diorama_popup\",\"parameters\":[]}]}]";
        String edited = ServerResponses.edit(response,
                Collections.singletonMap("1", "UopFeatureStatusApi.getAllPlugins"), Collections.emptySet(), true, true);
        char[] padding = new char[MAX_RESPONSE_BYTES - edited.length() + excessBytes];
        Arrays.fill(padding, 'a');
        return response.replace("\"padding\":\"\"", "\"padding\":\"" + new String(padding) + "\"");
    }

    @Test
    public void blockAllSodaOfferPlacementsInBothPopupPlugins() throws JSONException {
        Set<String> expected = new HashSet<>(Arrays.asList("keep", "DynamicOfferPostGameModalPlacement",
                "DynamicOfferPreLevelModalPlacement", "LockedOffersModalPlacement", "JourneyOfferStartModalPlacement",
                "JourneyOfferLastChanceModalPlacement", "JourneyOfferFinishModalPlacement",
                "AchievementOfferFinishModalPlacement", "AchievementOfferReminderModalPlacement",
                "AchievementOfferLastChanceModalPlacement", "AchievementOfferStartModalPlacement",
                "GamePassStartModalPlacement", "GamePassFinishModalPlacement", "GamePassLastChanceModalPlacement",
                "StreakShieldPurchaseOfferModalPlacement", "CrossPromoModalPlacement", "MilestoneOfferRare",
                "MilestoneOfferEpic", "MilestoneOfferLegendary", "MilestoneOfferReminder",
                "PiggyBankRedesignIntroModal", "PiggyBankRedesignOfferModal", "PiggyBankRedesignTimesUpModal"));
        for (String plugin : Arrays.asList("pf_diorama_popup", "pf_back_to_diorama_popup")) {
            String response = "[{\"id\":1,\"result\":[{\"name\":\"" + plugin + "\",\"parameters\":["
                    + "{\"key\":\"blocklist-filter\",\"value\":" + JSONObject.quote("[\"keep\"]") + "}]}]}]";
            String edited = ServerResponses.edit(response,
                    Collections.singletonMap("1", "UopFeatureStatusApi.getAllPlugins"), Collections.emptySet(), true,
                    true);
            JSONArray parameters = new JSONArray(edited).getJSONObject(0)
                .getJSONArray("result")
                .getJSONObject(0)
                .getJSONArray("parameters");
            JSONArray blocked = new JSONArray(parameters.getJSONObject(0).getString("value"));
            Set<String> actual = new HashSet<>();
            for (int index = 0; index < blocked.length(); index++) {
                actual.add(blocked.getString(index));
            }
            Assert.assertEquals(expected, actual);
        }
    }

    @Test
    public void preserveOtherSodaPluginsAndPopupParameters() throws JSONException {
        String response = "[{\"id\":1,\"result\":[{\"name\":\"pf_diorama_popup\",\"parameters\":["
                + "{\"key\":\"keep\",\"value\":\"same\"}]},{\"name\":\"bank_configuration\",\"enabled\":true}]}]";
        String edited = ServerResponses.edit(response,
                Collections.singletonMap("1", "UopFeatureStatusApi.getAllPlugins"), Collections.emptySet(), true, true);
        Assert.assertTrue(edited.contains("{\"key\":\"keep\",\"value\":\"same\"}"));
        Assert.assertTrue(edited.contains("{\"name\":\"bank_configuration\",\"enabled\":true}"));
    }

}
