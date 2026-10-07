/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.raindrop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import android.app.Application;
import android.net.Uri;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class ProUnlockTest {

    private static final String USER_URL = RaindropApi.API_URL + "user";

    private static final String LIBRARY_PATH = "raindrops/0?perpage=50&page=0";

    private static final String REFUSED_URL = RaindropApi.API_URL + "raindrops/0/hx-refused";

    private static final String PROBE_LINK = "https://probe.local/page";

    private static final String BROKEN_LINK = "https://dead.local/page";

    private static final String ARCHIVED_LINK = "https://saved.local/page";

    private static final String SHARED_LINK = "https://wiki.local/page";

    private static final int MAX_BODY_BYTES = 4 * 1024 * 1024;

    private static final AtomicLong ACCOUNTS = new AtomicLong(10000);

    private long account;

    private LinkStore seeds;

    @BeforeClass
    public static void installFakeApi() {
        FakeRaindropApi.install();
    }

    private static String api(String path) {
        return RaindropApi.API_URL + path;
    }

    private static byte[] utf8(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    private static String description(int code, String url) {
        return "Response{protocol=h2, code=" + code + ", message=, url=" + url + "}";
    }

    private static byte[] rewrite(String description, byte[] body) {
        ProUnlock.onResponse(null, description);
        return ProUnlock.rewriteResponse(body);
    }

    private static JSONObject rewrittenAfter(String description, JSONObject body) throws JSONException {
        byte[] original = utf8(body.toString());
        byte[] result = rewrite(description, original);
        return (result != original) ? new JSONObject(new String(result, StandardCharsets.UTF_8)) : null;
    }

    private static JSONObject rewritten(String url, JSONObject body) throws JSONException {
        return rewrittenAfter(description(200, url), body);
    }

    private static JSONObject bookmark(long id, String link, int collection) throws JSONException {
        return new JSONObject().put("_id", id)
            .put("link", link)
            .put("collectionId", collection)
            .put("domain", Uri.parse(link).getHost())
            .put("title", "Title " + id)
            .put("tags", new JSONArray());
    }

    private static JSONObject itemResponse(long id, String link) throws JSONException {
        return new JSONObject().put("result", true).put("item", new JSONObject().put("_id", id).put("link", link));
    }

    private static JSONObject emptyList() throws JSONException {
        return new JSONObject().put("result", true).put("items", new JSONArray()).put("count", 0);
    }

    private static String bulkUrl(String segment, String search) {
        return api("raindrops/" + segment + "?dangerAll=true&search=" + Uri.encode(search));
    }

    private static List<Integer> idsOf(JSONObject page) throws JSONException {
        JSONArray items = page.getJSONArray("items");
        List<Integer> ids = new ArrayList<>();
        for (int i = 0; i < items.length(); i++) {
            ids.add(items.getJSONObject(i).getInt("_id"));
        }
        return ids;
    }

    private static void loadLibrary(JSONObject... bookmarks) throws JSONException, InterruptedException {
        FakeRaindropApi.respond(LIBRARY_PATH,
                new JSONObject().put("result", true).put("items", new JSONArray(Arrays.asList(bookmarks))).toString());
        long deadline = System.nanoTime() + 10_000_000_000L;
        while (!BookmarkIndex.isLoaded()) {
            assertTrue("bookmark index did not load", System.nanoTime() < deadline);
            BookmarkIndex.refreshInBackground();
            Thread.sleep(10);
        }
    }

    private static void ensureLibrary() throws JSONException, InterruptedException {
        if (!BookmarkIndex.isLoaded()) {
            loadLibrary(bookmark(5, PROBE_LINK, 3), bookmark(6, BROKEN_LINK, 3), bookmark(7, ARCHIVED_LINK, 3),
                    bookmark(8, BROKEN_LINK, 3));
        }
    }

    @Before
    public void startFreeAccount() throws JSONException {
        FakeRaindropApi.reset();
        Application application = RuntimeEnvironment.getApplication();
        LinkChecker.init(application);
        ProUnlock.replaceRequestBody("");
        this.account = ACCOUNTS.incrementAndGet();
        assertNotNull(unlock(false, null));
        this.seeds = new LinkStore(application, this.account);
        this.seeds.recordHealth(BROKEN_LINK, true, Long.MAX_VALUE, null);
        this.seeds.recordArchive(ARCHIVED_LINK, true, Long.MAX_VALUE);
    }

    @After
    public void wipeIndex() {
        FakeRaindropApi.reset();
        BookmarkIndex.useAccount(ACCOUNTS.incrementAndGet());
        this.seeds.close();
    }

    private JSONObject user(boolean result, Boolean pro, JSONObject config) throws JSONException {
        JSONObject user = new JSONObject().put("_id", this.account);
        if (pro != null) {
            user.put("pro", pro);
        }
        if (config != null) {
            user.put("config", config);
        }
        return new JSONObject().put("result", result).put("user", user);
    }

    private JSONObject unlock(Boolean pro, JSONObject config) throws JSONException {
        return rewritten(USER_URL, user(true, pro, config));
    }

    private JSONObject freeUser() throws JSONException {
        return user(true, false, null);
    }

    private byte[] userBodyOfSize(int size) throws JSONException {
        JSONObject user = freeUser();
        user.getJSONObject("user").put("pad", "");
        int padding = size - utf8(user.toString()).length;
        user.getJSONObject("user").put("pad", new String(new char[padding]).replace('\0', 'x'));
        return utf8(user.toString());
    }

    private boolean isNativePro() throws JSONException {
        return rewritten(api("raindrop/5"), itemResponse(5, PROBE_LINK)) == null;
    }

    private boolean brokenCheckEnabled() throws JSONException {
        JSONObject response = rewritten(api("raindrop/6"), itemResponse(6, BROKEN_LINK));
        return response.getJSONObject("item").has("broken");
    }

    private boolean suggestionsEnabled() throws JSONException, InterruptedException {
        ensureLibrary();
        return rewritten(api("raindrop/5/suggest"), new JSONObject().put("result", false)) != null;
    }

    @Test
    public void rewritesUserResponsesOfSuccessfulApiCalls() throws JSONException {
        // when
        JSONObject response = rewrittenAfter(description(200, USER_URL), user(true, false, null));

        // then
        assertTrue(response.getJSONObject("user").getBoolean("pro"));
    }

    @Test
    public void keepsQueryParametersThatLookLikeAStatusCode() throws JSONException {
        assertNotNull(rewrittenAfter(description(200, USER_URL + "?perpage=50&code=404"), freeUser()));
        assertNull(rewrittenAfter(description(404, USER_URL + "?code=200"), freeUser()));
    }

    @Test
    public void takesTheUrlUpToTheLastClosingBrace() throws JSONException {
        assertNotNull(rewrittenAfter("Response{code=200, url=" + USER_URL + "}", freeUser()));
        assertNull(rewrittenAfter("Response{code=200, url=" + USER_URL + "}x}", freeUser()));
    }

    @Test
    public void rejectsResponsesThatAreNotPlain200() throws JSONException {
        for (int code : new int[] { 100, 201, 204, 301, 304, 404, 500, 2000, 20 }) {
            assertNull("code " + code, rewrittenAfter(description(code, USER_URL), freeUser()));
        }
        assertNotNull(rewrittenAfter(description(200, USER_URL), freeUser()));
    }

    @Test
    public void rejectsDescriptionsMissingCodeUrlOrClosingBrace() throws JSONException {
        for (String text : new String[] { "", "null", "Response{protocol=h2, code=200, message=}",
                "Response{url=" + USER_URL + "}", "Response{code=200, url=" + USER_URL, "}code=200 url=" }) {
            assertNull(text, rewrittenAfter(text, freeUser()));
        }
        assertNotNull(rewrittenAfter("Response{code=200, url=" + USER_URL + "}", freeUser()));
    }

    @Test
    public void returnsAResponseWithoutObservedUrlUntouched() throws JSONException {
        // given
        byte[] body = utf8(freeUser().toString());

        // when
        byte[] result = ProUnlock.rewriteResponse(body);

        // then
        assertSame(body, result);
    }

    @Test
    public void toleratesANullResponseDescription() throws JSONException {
        // given
        ProUnlock.onResponse(null, null);
        byte[] body = utf8(freeUser().toString());

        // when
        byte[] result = ProUnlock.rewriteResponse(body);

        // then
        assertSame(body, result);
    }

    @Test
    public void skipsRewritingUnsuccessfulResponses() throws JSONException {
        assertNull(rewrittenAfter(description(404, USER_URL), freeUser()));
    }

    @Test
    public void skipsRewritingResponsesFromOtherHosts() throws JSONException {
        for (String url : new String[] { "https://example.com/v1/user", "https://api.raindrop.io.evil.example/v1/user",
                "http://api.raindrop.io/v1/user", "https://api.raindrop.io/v2/user", "https://api.raindrop.io/v1" }) {
            assertNull(url, rewritten(url, freeUser()));
        }
        assertNotNull(rewritten(USER_URL, freeUser()));
    }

    @Test
    public void skipsRewritingResponsesWithoutAResource() throws JSONException {
        assertNull(rewritten(RaindropApi.API_URL, freeUser()));
        assertNull(rewritten(api("user/stats/extra"), freeUser()));
        assertNull(rewritten(api("unknown/5"), freeUser()));
    }

    @Test
    public void returnsNullOrEmptyBodiesAsIs() {
        byte[] empty = new byte[0];
        assertNull(rewrite(description(200, USER_URL), null));
        assertSame(empty, rewrite(description(200, USER_URL), empty));
    }

    @Test
    public void returnsBodiesOverFourMebibytesAsIs() throws JSONException {
        byte[] exactly = userBodyOfSize(MAX_BODY_BYTES);
        assertEquals(MAX_BODY_BYTES, exactly.length);
        assertNotSame(exactly, rewrite(description(200, USER_URL), exactly));
        byte[] over = userBodyOfSize(MAX_BODY_BYTES + 1);
        assertEquals(MAX_BODY_BYTES + 1, over.length);
        assertSame(over, rewrite(description(200, USER_URL), over));
    }

    @Test
    public void returnsMalformedJsonAsIs() {
        for (String text : new String[] { "not json", "[1,2]", "{\"result\":", "<html></html>", "   " }) {
            byte[] body = utf8(text);
            assertSame(text, body, rewrite(description(200, USER_URL), body));
        }
    }

    @Test
    public void consumesTheObservedUrlInOneRewrite() throws JSONException {
        ProUnlock.onResponse(null, description(200, USER_URL));
        byte[] malformed = utf8("oops");
        assertSame(malformed, ProUnlock.rewriteResponse(malformed));
        byte[] valid = utf8(freeUser().toString());
        assertSame(valid, ProUnlock.rewriteResponse(valid));
    }

    @Test
    public void replacesTheObservedUrlWithTheLaterResponse() throws JSONException {
        // given
        ProUnlock.onResponse(null, description(200, USER_URL));
        ProUnlock.onResponse(null, description(404, USER_URL));
        byte[] body = utf8(freeUser().toString());

        // when
        byte[] result = ProUnlock.rewriteResponse(body);

        // then
        assertSame(body, result);
    }

    @Test
    public void keepsTheUrlOfRequestsItDoesNotRewrite() {
        String url = bulkUrl("0", "duplicate:true");
        assertEquals(REFUSED_URL, ProUnlock.onRequest("PUT", url, 1, null, null));
        assertEquals(REFUSED_URL, ProUnlock.onRequest("DELETE", url, 1, null, null));
        assertEquals(url, ProUnlock.onRequest("GET", url, 1, null, null));
        assertEquals(url, ProUnlock.onRequest("POST", url, 1, null, null));
        assertEquals(url, ProUnlock.onRequest(null, url, 1, null, null));
        assertEquals("https://example.com/v1/raindrops/0?dangerAll=true&search=duplicate%3Atrue", ProUnlock.onRequest(
                "PUT", "https://example.com/v1/raindrops/0?dangerAll=true&search=duplicate%3Atrue", 1, null, null));
        String lookalike = "https://api.raindrop.io.evil.example/v1/raindrops/0?dangerAll=true&search=duplicate%3Atrue";
        assertEquals(lookalike, ProUnlock.onRequest("DELETE", lookalike, 1, null, null));
    }

    @Test
    public void inspectsOnlyBulkRequestsOnACollection() {
        String search = "&search=" + Uri.encode("duplicate:true");
        for (String url : new String[] { api("raindrops/0?dangerAll=false" + search), api("raindrops/0?" + search),
                api("raindrops/0/export.html?dangerAll=true" + search), api("raindrops?dangerAll=true" + search),
                api("raindrop/0?dangerAll=true" + search), api("raindrops/0?dangerAll=TRUE" + search) }) {
            assertEquals(url, ProUnlock.onRequest("PUT", url, 1, null, null));
        }
    }

    @Test
    public void keepsANullUrlNull() {
        assertNull(ProUnlock.onRequest("PUT", null, 1, null, null));
        assertNull(ProUnlock.onRequest("DELETE", null, 1, null, null));
        assertNull(ProUnlock.onRequest("GET", null, 1, null, null));
    }

    @Test
    public void keepsTheBulkRequestsOfNativeProAccounts() throws JSONException {
        String url = bulkUrl("0", "duplicate:true");
        assertEquals(REFUSED_URL, ProUnlock.onRequest("PUT", url, 1, null, null));
        assertNull(unlock(true, null));
        assertEquals(url, ProUnlock.onRequest("PUT", url, 1, null, null));
        assertEquals(url, ProUnlock.onRequest("DELETE", url, 1, null, null));
    }

    @Test
    public void keepsTheRequestBodyWhenNothingWasReplaced() {
        assertEquals("{\"ids\":[1]}", ProUnlock.replaceRequestBody("{\"ids\":[1]}"));
        assertNull(ProUnlock.replaceRequestBody(null));
        assertEquals("", ProUnlock.replaceRequestBody(""));
    }

    @Test
    public void recognisesLocalSearchQueries() {
        for (String query : new String[] { "duplicate:true", "broken:true", "_id:5 duplicate:5 match:OR",
                "_id:123456 duplicate:123456 match:OR", " broken:true", "duplicate:true " }) {
            assertEquals(query, REFUSED_URL, ProUnlock.onRequest("PUT", bulkUrl("0", query), 1, null, null));
        }
    }

    @Test
    public void rejectsOtherQueriesAsLocal() {
        for (String query : new String[] { "", "rust", "duplicate:false", "broken:false", "Duplicate:true",
                "duplicate:true broken:true", "_id:5 duplicate:6 match:OR", "_id:5 duplicate:5 match:AND",
                "_id: duplicate: match:OR", "_id:5 duplicate:5 match:OR extra", "x _id:5 duplicate:5 match:OR",
                "_id:5 duplicate:55 match:OR", "_id:55 duplicate:5 match:OR" }) {
            String url = bulkUrl("0", query);
            assertEquals(query, url, ProUnlock.onRequest("PUT", url, 1, null, null));
        }
    }

    @Test
    public void treatsQueriesWithoutOperatorsAsPlainText() {
        for (String query : new String[] { "rust", "rust async", "foo-bar", "a   b", "100%", "café", "C++" }) {
            String url = ProUnlock.onRequest("PUT", bulkUrl("0", query), 1, null, new BodyMap("{\"ids\":[1]}"));
            assertEquals(query, api("raindrops/0?dangerAll=true"), url);
        }
    }

    @Test
    public void leavesQueriesWithOperatorsUntouched() {
        for (String query : new String[] { "", "tag:rust", "#tag", "\"exact phrase\"", "-excluded", "rust -java", " -x",
                "foo -", "title:foo", "a \"b" }) {
            String url = bulkUrl("0", query);
            assertEquals(query, url, ProUnlock.onRequest("PUT", url, 1, null, new BodyMap("{\"ids\":[1]}")));
        }
    }

    @Test
    public void refusesLocalBulkRequestsWhileTheIndexIsLoading() {
        String local = bulkUrl("0", "duplicate:true");
        assertEquals(REFUSED_URL, ProUnlock.onRequest("PUT", local, 1, null, new BodyMap("{}")));
        assertEquals(REFUSED_URL, ProUnlock.onRequest("DELETE", local, 1, null, new BodyMap("{\"ids\":[1]}")));
        assertNull(ProUnlock.replaceRequestBody(null));
    }

    @Test
    public void handlesBulkRequestsWithoutBodyOrIdsPerQueryKind() throws JSONException, InterruptedException {
        String plain = bulkUrl("0", "rust");
        assertEquals(plain, ProUnlock.onRequest("PUT", plain, 1, null, null));
        assertEquals(plain, ProUnlock.onRequest("PUT", plain, 1, null, new BodyMap("{\"important\":true}")));
        assertNull(ProUnlock.replaceRequestBody(null));
        loadLibrary(bookmark(1, SHARED_LINK, 5));
        String local = bulkUrl("0", "duplicate:true");
        assertEquals(REFUSED_URL, ProUnlock.onRequest("PUT", local, 1, null, null));
        assertEquals(REFUSED_URL, ProUnlock.onRequest("PUT", local, 1, null, new BodyMap("not json")));
        assertNull(ProUnlock.replaceRequestBody(null));
    }

    @Test
    public void keepsAnExplicitIdListInLocalBulkRequests() throws JSONException, InterruptedException {
        // given
        loadLibrary(bookmark(1, SHARED_LINK, 5), bookmark(2, SHARED_LINK, 5));

        // when
        String url = ProUnlock.onRequest("PUT", bulkUrl("0", "duplicate:true"), 1, null, new BodyMap("{\"ids\":[9]}"));

        // then
        assertEquals(api("raindrops/0?dangerAll=true"), url);
        assertEquals("{\"ids\":[9]}", ProUnlock.replaceRequestBody("{\"ids\":[9]}"));
    }

    @Test
    public void targetsTheMatchingBookmarksInLocalBulkRequests() throws JSONException, InterruptedException {
        loadLibrary(bookmark(1, SHARED_LINK, 5), bookmark(2, SHARED_LINK, 5), bookmark(3, SHARED_LINK, 6),
                bookmark(4, BROKEN_LINK, 5));
        String url = ProUnlock.onRequest("PUT", bulkUrl("0", "duplicate:true"), 1, null,
                new BodyMap("{\"important\":true}"));
        assertEquals(api("raindrops/0?dangerAll=true"), url);
        JSONObject body = new JSONObject(ProUnlock.replaceRequestBody("unused"));
        assertEquals("[2,3]", body.getJSONArray("ids").toString());
        assertTrue(body.getBoolean("important"));
        ProUnlock.onRequest("DELETE", bulkUrl("0", "broken:true"), 1, null, new BodyMap("{}"));
        assertEquals("[4]", new JSONObject(ProUnlock.replaceRequestBody("unused")).getJSONArray("ids").toString());
        ProUnlock.onRequest("PUT", bulkUrl("0", "_id:1 duplicate:1 match:OR"), 1, null, new BodyMap("{}"));
        assertEquals("[1,2,3]", new JSONObject(ProUnlock.replaceRequestBody("unused")).getJSONArray("ids").toString());
    }

    @Test
    public void targetsNoBookmarkInLocalBulkRequestsWithoutMatch() throws JSONException, InterruptedException {
        // given
        loadLibrary(bookmark(1, SHARED_LINK, 5));

        // when
        ProUnlock.onRequest("PUT", bulkUrl("0", "duplicate:true"), 1, null, new BodyMap("{}"));

        // then
        assertEquals("[0]", new JSONObject(ProUnlock.replaceRequestBody("unused")).getJSONArray("ids").toString());
    }

    @Test
    public void parsesNumericCollectionSegments() throws JSONException, InterruptedException {
        loadLibrary(bookmark(1, SHARED_LINK, 5), bookmark(2, SHARED_LINK, 5), bookmark(3, SHARED_LINK, 6),
                bookmark(4, SHARED_LINK, BookmarkIndex.UNSORTED), bookmark(5, SHARED_LINK, BookmarkIndex.TRASH));
        assertEquals("[2]", bulkIds("5"));
        assertEquals("[3]", bulkIds("6"));
        assertEquals("[2,3,4]", bulkIds("0"));
        assertEquals("[4]", bulkIds(String.valueOf(BookmarkIndex.UNSORTED)));
        assertEquals("[5]", bulkIds(String.valueOf(BookmarkIndex.TRASH)));
        assertEquals("[0]", bulkIds("2147483647"));
    }

    @Test
    public void treatsNonNumericCollectionSegmentsAsAllBookmarks() throws JSONException, InterruptedException {
        loadLibrary(bookmark(1, SHARED_LINK, 5), bookmark(2, SHARED_LINK, 5), bookmark(3, SHARED_LINK, 6),
                bookmark(4, SHARED_LINK, BookmarkIndex.UNSORTED), bookmark(5, SHARED_LINK, BookmarkIndex.TRASH));
        for (String segment : new String[] { "abc", "1.5", "99999999999", "0x10", "-" }) {
            assertEquals(segment, "[2,3,4]", bulkIds(segment));
        }
    }

    @Test
    public void keepsUrlsThatAreNotArchiveRequestsUnchanged() {
        for (String url : new String[] { "", "https://example.com/raindrop/5/cache", RaindropApi.API_URL,
                RaindropApi.API_URL + "raindrops/0", RaindropApi.API_URL + "raindrop/5",
                RaindropApi.API_URL + "raindrop/5/cache/extra", RaindropApi.API_URL + "raindrop/abc/cache",
                RaindropApi.API_URL + "raindrop//cache", RaindropApi.API_URL + "raindrop/5/cache?x=1",
                RaindropApi.API_URL + "x/raindrop/5/cache" }) {
            assertEquals(url, ProUnlock.archiveUrl(url));
        }
        assertNull(ProUnlock.archiveUrl(null));
    }

    @Test
    public void keepsArchiveRequestsForBookmarksNotInTheIndex() {
        // given
        String url = RaindropApi.API_URL + "raindrop/5/cache";

        // when
        String result = ProUnlock.archiveUrl(url);

        // then
        assertEquals(url, result);
    }

    @Test
    public void pointsArchivedBookmarksToTheirArchivedCopy() throws JSONException, InterruptedException {
        ensureLibrary();
        assertEquals("https://web.archive.org/web/2/" + ARCHIVED_LINK, ProUnlock.archiveUrl(api("raindrop/7/cache")));
        assertEquals(api("raindrop/5/cache"), ProUnlock.archiveUrl(api("raindrop/5/cache")));
        assertEquals(api("raindrop/99/cache"), ProUnlock.archiveUrl(api("raindrop/99/cache")));
    }

    @Test
    public void keepsArchiveRequestsOfNativeProAccounts() throws JSONException, InterruptedException {
        // given
        ensureLibrary();
        assertNull(unlock(true, null));
        String url = RaindropApi.API_URL + "raindrop/7/cache";

        // when
        String result = ProUnlock.archiveUrl(url);

        // then
        assertEquals(url, result);
    }

    @Test
    public void keepsArchiveHeadersWhenTheUrlIsNotRewritten() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Cookie", "a=b");
        assertSame(headers, ProUnlock.archiveHeaders(RaindropApi.API_URL + "raindrop/5/cache", headers));
        assertSame(headers, ProUnlock.archiveHeaders(null, headers));
        assertSame(headers, ProUnlock.archiveHeaders("https://example.com/a", headers));
        assertNull(ProUnlock.archiveHeaders(null, null));
    }

    @Test
    public void dropsArchiveHeadersWhenTheUrlPointsToTheArchivedCopy() throws JSONException, InterruptedException {
        ensureLibrary();
        Map<String, String> headers = new HashMap<>();
        headers.put("Cookie", "a=b");
        assertEquals(Collections.emptyMap(), ProUnlock.archiveHeaders(api("raindrop/7/cache"), headers));
        assertSame(headers, ProUnlock.archiveHeaders(api("raindrop/5/cache"), headers));
    }

    @Test
    public void ignoresUserResponsesWithoutSuccess() throws JSONException, InterruptedException {
        JSONObject config = new JSONObject().put("broken_level", "off").put("ai_suggestions", false);
        assertNull(rewritten(USER_URL, user(false, false, config)));
        assertNull(rewritten(USER_URL, new JSONObject().put("user", new JSONObject().put("pro", false))));
        assertTrue(brokenCheckEnabled());
        assertTrue(suggestionsEnabled());
    }

    @Test
    public void ignoresUserResponsesWithoutUser() throws JSONException {
        assertNull(rewritten(USER_URL, new JSONObject().put("result", true)));
        assertNull(rewritten(USER_URL, new JSONObject()));
    }

    @Test
    public void ignoresUsersWithoutProField() throws JSONException, InterruptedException {
        assertNull(unlock(true, null));
        JSONObject config = new JSONObject().put("broken_level", "off").put("ai_suggestions", false);
        JSONObject response = user(true, null, config);
        assertNull(rewritten(USER_URL, response));
        assertFalse(response.getJSONObject("user").has("pro"));
        assertTrue(isNativePro());
        assertNotNull(unlock(false, null));
        assertNull(rewritten(USER_URL, response));
        assertFalse(isNativePro());
        assertTrue(brokenCheckEnabled());
        assertTrue(suggestionsEnabled());
    }

    @Test
    public void keepsNativeProStatusOnIgnoredUserResponses() throws JSONException {
        assertNull(unlock(true, null));
        assertNull(rewritten(USER_URL, user(false, false, null)));
        assertNull(rewritten(USER_URL, user(true, null, null)));
        assertTrue(isNativePro());
    }

    @Test
    public void leavesNativeProUsersAlone() throws JSONException {
        // given
        JSONObject config = new JSONObject().put("broken_level", "off").put("ai_suggestions", false);
        byte[] body = utf8(user(true, true, config).toString());

        // when
        byte[] result = rewrite(description(200, USER_URL), body);

        // then
        assertSame(body, result);
        assertTrue(isNativePro());
        assertNull(rewritten(api("raindrop/5/suggest"), new JSONObject().put("result", false)));
    }

    @Test
    public void marksFreeUsersAsProInTheSameResponse() throws JSONException {
        // when
        JSONObject response = unlock(false, null);

        // then
        assertTrue(response.getJSONObject("user").getBoolean("pro"));
        assertEquals(this.account, response.getJSONObject("user").getLong("_id"));
        assertFalse(isNativePro());
    }

    @Test
    public void keepsAllFeaturesEnabledForFreeUsersWithoutConfig() throws JSONException, InterruptedException {
        unlock(false, null);
        assertTrue(brokenCheckEnabled());
        assertTrue(suggestionsEnabled());
        unlock(false, new JSONObject());
        assertTrue(brokenCheckEnabled());
        assertTrue(suggestionsEnabled());
    }

    @Test
    public void disablesBrokenLinkChecksWhenBrokenLevelIsOff() throws JSONException, InterruptedException {
        unlock(false, new JSONObject().put("broken_level", "off"));
        assertFalse(brokenCheckEnabled());
        assertTrue(suggestionsEnabled());
        for (String level : new String[] { "basic", "default", "strict", "" }) {
            unlock(false, new JSONObject().put("broken_level", level));
            assertTrue(level, brokenCheckEnabled());
        }
    }

    @Test
    public void controlsSuggestionsWithTheAiSuggestionsSetting() throws JSONException, InterruptedException {
        unlock(false, new JSONObject().put("ai_suggestions", false));
        assertFalse(suggestionsEnabled());
        assertTrue(brokenCheckEnabled());
        unlock(false, new JSONObject().put("ai_suggestions", true));
        assertTrue(suggestionsEnabled());
        unlock(false, new JSONObject().put("ai_suggestions", false));
        unlock(false, new JSONObject().put("other", 1));
        assertTrue(suggestionsEnabled());
    }

    @Test
    public void followsTheLatestUserResponseForFeatureFlags() throws JSONException, InterruptedException {
        unlock(false, new JSONObject().put("broken_level", "off").put("ai_suggestions", false));
        assertFalse(brokenCheckEnabled());
        assertFalse(suggestionsEnabled());
        unlock(false, null);
        assertTrue(brokenCheckEnabled());
        assertTrue(suggestionsEnabled());
    }

    @Test
    public void followsTheLatestUserResponseForNativeProStatus() throws JSONException {
        unlock(true, null);
        assertTrue(isNativePro());
        unlock(false, null);
        assertFalse(isNativePro());
        unlock(true, null);
        assertTrue(isNativePro());
    }

    @Test
    public void givesIdenticalResultsForIdenticalUserResponses() throws JSONException {
        // given
        JSONObject config = new JSONObject().put("broken_level", "off");
        JSONObject first = unlock(false, config);
        boolean brokenAfterFirst = brokenCheckEnabled();

        // when
        JSONObject second = unlock(false, config);

        // then
        assertTrue(first.getJSONObject("user").getBoolean("pro"));
        assertTrue(second.getJSONObject("user").getBoolean("pro"));
        assertFalse(brokenAfterFirst);
        assertEquals(brokenAfterFirst, brokenCheckEnabled());
        assertFalse(isNativePro());
    }

    @Test
    public void ignoresBookmarkPathsBeyondASingleBookmark() throws JSONException, InterruptedException {
        ensureLibrary();
        JSONObject response = itemResponse(5, PROBE_LINK);
        assertNotNull(rewritten(api("raindrop/5"), response));
        assertNull(rewritten(api("raindrop/5/cache"), response));
        assertNull(rewritten(api("raindrop/5/cache"), new JSONObject().put("result", false)));
        assertNull(rewritten(api("raindrop/5/a/b"), response));
    }

    @Test
    public void ignoresFailedOrItemlessBookmarkResponses() throws JSONException {
        String url = api("raindrop/5");
        JSONObject item = new JSONObject().put("_id", 5);
        assertNull(rewritten(url, new JSONObject().put("result", false).put("item", item)));
        assertNull(rewritten(url, new JSONObject().put("item", item)));
        assertNull(rewritten(url, new JSONObject().put("result", true)));
        assertNull(rewritten(url, new JSONObject().put("result", true).put("item", "text")));
    }

    @Test
    public void returnsAnUnknownBookmarkWithoutLocalFlags() throws JSONException {
        // when
        JSONObject item = rewritten(api("raindrop/5"), itemResponse(5, "https://example.com/a")).getJSONObject("item");

        // then
        assertEquals(5, item.getInt("_id"));
        assertEquals("https://example.com/a", item.getString("link"));
        assertFalse(item.has("duplicate"));
        assertFalse(item.has("broken"));
        assertFalse(item.has("cache"));
    }

    @Test
    public void addsTheLocalFlagsToBookmarkResponses() throws JSONException, InterruptedException {
        ensureLibrary();
        JSONObject duplicate = rewritten(api("raindrop/8"), itemResponse(8, BROKEN_LINK)).getJSONObject("item");
        assertEquals(6, duplicate.getLong("duplicate"));
        assertTrue(duplicate.getBoolean("broken"));
        assertFalse(duplicate.has("cache"));
        JSONObject archived = rewritten(api("raindrop/7"), itemResponse(7, ARCHIVED_LINK)).getJSONObject("item");
        assertEquals("ready", archived.getJSONObject("cache").getString("status"));
        assertFalse(archived.has("duplicate"));
        assertFalse(archived.has("broken"));
    }

    @Test
    public void buildsSuggestionsOnlyForFailedSuggestionRequests() throws JSONException, InterruptedException {
        ensureLibrary();
        JSONObject suggested = rewritten(api("raindrop/5/suggest"), new JSONObject().put("result", false));
        assertTrue(suggested.getBoolean("result"));
        assertTrue(suggested.getJSONObject("item").has("tags"));
        assertNull(rewritten(api("raindrop/5/suggest"), new JSONObject().put("result", true)));
        assertNull(rewritten(api("raindrop/5/suggest"), new JSONObject()));
        assertNull(rewritten(api("raindrop/99/suggest"), new JSONObject().put("result", false)));
    }

    @Test
    public void slicesLocalSearchPagesInOrder() throws JSONException, InterruptedException {
        loadSharedLinkLibrary();
        JSONObject first = listPage("7", "duplicate:true", "&perpage=2&page=0");
        assertTrue(first.getBoolean("result"));
        assertEquals(Arrays.asList(2, 3), idsOf(first));
        assertEquals(5, first.getInt("count"));
        assertEquals(7, first.getInt("collectionId"));
        assertEquals(Arrays.asList(4, 5), idsOf(listPage("7", "duplicate:true", "&perpage=2&page=1")));
        assertEquals(Collections.singletonList(6), idsOf(listPage("7", "duplicate:true", "&perpage=2&page=2")));
    }

    @Test
    public void returnsEmptyLocalSearchPagesPastTheEndWithTheTotal() throws JSONException, InterruptedException {
        loadSharedLinkLibrary();
        JSONObject beyond = listPage("7", "duplicate:true", "&perpage=2&page=3");
        assertEquals(Collections.emptyList(), idsOf(beyond));
        assertEquals(5, beyond.getInt("count"));
        assertEquals(Collections.emptyList(), idsOf(listPage("7", "duplicate:true", "&perpage=25&page=1")));
    }

    @Test
    public void fitsMatchesOnOnePageWhenThePageSizeCoversThemExactly() throws JSONException, InterruptedException {
        loadSharedLinkLibrary();
        List<Integer> all = Arrays.asList(2, 3, 4, 5, 6);
        assertEquals(all, idsOf(listPage("7", "duplicate:true", "&perpage=5&page=0")));
        assertEquals(Collections.emptyList(), idsOf(listPage("7", "duplicate:true", "&perpage=5&page=1")));
        assertEquals(all, idsOf(listPage("7", "duplicate:true", "&perpage=100&page=0")));
    }

    @Test
    public void returnsAnEmptyPageForALocalSearchWithoutMatches() throws JSONException, InterruptedException {
        // given
        loadSharedLinkLibrary();

        // when
        JSONObject page = listPage("7", "broken:true", "&perpage=25&page=0");

        // then
        assertTrue(page.getBoolean("result"));
        assertEquals(0, page.getJSONArray("items").length());
        assertEquals(0, page.getInt("count"));
        assertEquals(7, page.getInt("collectionId"));
    }

    @Test
    public void defaultsLocalSearchPagesToTheFirstPageOf25() throws JSONException, InterruptedException {
        loadSharedLinkLibrary();
        List<Integer> all = Arrays.asList(2, 3, 4, 5, 6);
        assertEquals(all, idsOf(listPage("7", "duplicate:true", "")));
        assertEquals(all, idsOf(listPage("7", "duplicate:true", "&perpage=many&page=first")));
    }

    @Test
    public void treatsANonPositivePageSizeAsOne() throws JSONException, InterruptedException {
        loadSharedLinkLibrary();
        assertEquals(Collections.singletonList(2), idsOf(listPage("7", "duplicate:true", "&perpage=0&page=0")));
        assertEquals(Collections.singletonList(3), idsOf(listPage("7", "duplicate:true", "&perpage=-5&page=1")));
    }

    @Test
    public void treatsANegativePageNumberAsTheFirstPage() throws JSONException, InterruptedException {
        // given
        loadSharedLinkLibrary();

        // when
        JSONObject page = listPage("7", "duplicate:true", "&perpage=2&page=-3");

        // then
        assertEquals(Arrays.asList(2, 3), idsOf(page));
    }

    @Test
    public void handlesHugePageNumbersWithoutOverflow() throws JSONException, InterruptedException {
        loadSharedLinkLibrary();
        String last = "&page=" + Integer.MAX_VALUE;
        assertEquals(Collections.emptyList(), idsOf(listPage("7", "duplicate:true", "&perpage=50" + last)));
        assertEquals(Collections.emptyList(),
                idsOf(listPage("7", "duplicate:true", "&perpage=" + Integer.MAX_VALUE + last)));
        assertEquals(Arrays.asList(2, 3, 4, 5, 6),
                idsOf(listPage("7", "duplicate:true", "&perpage=" + Integer.MAX_VALUE + "&page=0")));
    }

    @Test
    public void keepsTheIndexUnchangedByLocalSearchPageItems() throws JSONException, InterruptedException {
        // given
        loadSharedLinkLibrary();

        // when
        JSONObject page = listPage("7", "duplicate:true", "&perpage=2&page=0");

        // then
        JSONObject item = page.getJSONArray("items").getJSONObject(0);
        assertEquals(1, item.getLong("duplicate"));
        assertEquals(SHARED_LINK, item.getString("link"));
        for (JSONObject indexed : BookmarkIndex.snapshot()) {
            assertFalse(indexed.has("duplicate"));
        }
    }

    @Test
    public void givesTheSamePageForTheSameSearchTwice() throws JSONException, InterruptedException {
        loadSharedLinkLibrary();
        assertEquals(listPage("7", "duplicate:true", "&perpage=2&page=1").toString(),
                listPage("7", "duplicate:true", "&perpage=2&page=1").toString());
        assertEquals(6, BookmarkIndex.snapshot().size());
    }

    @Test
    public void listsTheOriginalWithItsCopiesInALocalSearchOfADuplicateGroup()
            throws JSONException, InterruptedException {
        loadSharedLinkLibrary();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6),
                idsOf(listPage("7", "_id:1 duplicate:1 match:OR", "&perpage=25&page=0")));
        assertEquals(Arrays.asList(4), idsOf(listPage("7", "_id:4 duplicate:4 match:OR", "&perpage=25&page=0")));
    }

    private void loadSharedLinkLibrary() throws JSONException, InterruptedException {
        List<JSONObject> bookmarks = new ArrayList<>();
        for (int id = 1; id <= 6; id++) {
            bookmarks.add(bookmark(id, SHARED_LINK, 7));
        }
        loadLibrary(bookmarks.toArray(new JSONObject[0]));
    }

    private static JSONObject listPage(String segment, String search, String paging) throws JSONException {
        String url = api("raindrops/" + segment + "?search=" + Uri.encode(search) + paging);
        return rewritten(url, emptyList());
    }

    private static String bulkIds(String segment) throws JSONException {
        String url = ProUnlock.onRequest("PUT", bulkUrl(segment, "duplicate:true"), 1, null, new BodyMap("{}"));
        assertEquals(api("raindrops/" + segment + "?dangerAll=true"), url);
        return new JSONObject(ProUnlock.replaceRequestBody("unused")).getJSONArray("ids").toString();
    }

    static final class BodyMap {

        private final String json;

        BodyMap(String json) {
            this.json = json;
        }

        public boolean hasKey(String key) {
            return "string".equals(key);
        }

        public String getString(String key) {
            return this.json;
        }

    }

}
