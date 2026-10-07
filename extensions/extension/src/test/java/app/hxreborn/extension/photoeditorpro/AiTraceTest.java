/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.photoeditorpro;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import app.morphe.extension.shared.Utils;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public final class AiTraceTest {

    private static final String PREDICT = "https://api.example.com/api/v1/predict/inpaint";

    private static final String INPAINT = "https://h/inpaint";

    private static final String POLL_A = "https://h/api/v1/inpaint/rst_a.png";

    private static final String POLL_B = "https://h/api/v1/inpaint/rst_b.png";

    private static final String UUID = "3f2a9c1e-7b4d-4e8a-9c1f-0a1b2c3d4e5f";

    private static TimeZone defaultZone;

    private final Deque<Reply> replies = new ArrayDeque<>();

    private OkHttpClient client;

    @BeforeClass
    public static void pinZoneToUtc() {
        defaultZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @AfterClass
    public static void restoreZone() {
        TimeZone.setDefault(defaultZone);
    }

    @Before
    public void startWithoutSessions() throws ReflectiveOperationException {
        Utils.setContext(RuntimeEnvironment.getApplication());
        PatchSettings.LOG_ENDPOINTS.save(true);
        clearSessions();
        replies.clear();
        OkHttpClient.Builder builder = newBuilder();
        AiTrace.install(builder);
        builder.addInterceptor(this::transport);
        client = builder.build();
    }

    @After
    public void leaveNoSessions() throws ReflectiveOperationException {
        PatchSettings.LOG_ENDPOINTS.save(false);
        clearSessions();
    }

    private static OkHttpClient.Builder newBuilder() {
        return new OkHttpClient.Builder();
    }

    private static void clearSessions() throws ReflectiveOperationException {
        Field field = AiTrace.class.getDeclaredField("SESSIONS");
        field.setAccessible(true);
        ((List<?>) field.get(null)).clear();
        Field lastRun = ProgressTrace.class.getDeclaredField("LAST_RUN");
        lastRun.setAccessible(true);
        ((List<?>) lastRun.get(null)).clear();
        ProgressTrace.progress(100);
    }

    private Response transport(Interceptor.Chain chain) throws IOException {
        Reply reply = this.replies.removeFirst();
        if (reply.failure != null) {
            throw reply.failure;
        }
        if (reply.crash != null) {
            throw reply.crash;
        }
        return new Response.Builder().request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .code(reply.code)
            .message(reply.message)
            .headers(reply.headers.build())
            .body(ResponseBody.create(reply.body, MediaType.get(reply.contentType)))
            .sentRequestAtMillis(reply.sentAtMs)
            .receivedResponseAtMillis(reply.sentAtMs + reply.latencyMs)
            .build();
    }

    private static Reply ok() {
        return new Reply(200);
    }

    private static Reply status(int code) {
        return new Reply(code);
    }

    private void get(String url, Reply... answers) throws IOException {
        for (Reply answer : answers) {
            send(new Request.Builder().url(url).get().build(), answer);
        }
    }

    private void post(String url, Reply... answers) throws IOException {
        for (Reply answer : answers) {
            send(new Request.Builder().url(url).post(new FormBody.Builder().build()).build(), answer);
        }
    }

    private void send(Request request, Reply answer) throws IOException {
        this.replies.addLast(answer);
        try (Response response = this.client.newCall(request).execute()) {
            response.body().string();
        }
    }

    private static List<AiTrace.Session> sessions() {
        return AiTrace.sessions();
    }

    private static AiTrace.Session onlySession() {
        assertEquals(1, sessions().size());
        return sessions().get(0);
    }

    private static AiTrace.Exchange onlyExchange() {
        List<AiTrace.Exchange> exchanges = onlySession().exchanges();
        assertEquals(1, exchanges.size());
        return exchanges.get(0);
    }

    private static AiTrace.Attempt onlyAttempt() {
        List<AiTrace.Attempt> attempts = onlyExchange().attempts();
        assertEquals(1, attempts.size());
        return attempts.get(0);
    }

    private Map<String, String> recordedForm(String... namesAndValues)
            throws IOException, ReflectiveOperationException {
        clearSessions();
        FormBody.Builder form = new FormBody.Builder();
        for (int i = 0; i < namesAndValues.length; i += 2) {
            form.add(namesAndValues[i], namesAndValues[i + 1]);
        }
        send(new Request.Builder().url(INPAINT).post(form.build()).build(), ok());
        Map<String, String> recorded = new LinkedHashMap<>();
        for (String[] pair : onlyExchange().payload()) {
            recorded.put(pair[0], pair[1]);
        }
        return recorded;
    }

    private String recordedResource(String url) throws IOException, ReflectiveOperationException {
        clearSessions();
        get(url, ok());
        return onlySession().resource;
    }

    private static String headerValue(List<String[]> headers, String name) {
        for (String[] pair : headers) {
            if (pair[0].equals(name)) {
                return pair[1];
            }
        }
        return null;
    }

    private static String headline(String feature, int requests, String elapsed) {
        String separator = " " + (char) 0xB7 + " ";
        return String.join(separator, feature, requests + " requests", elapsed);
    }

    private static AiTrace.Attempt attempt(long startedAtMs, long endedAtMs, int statusCode) {
        AiTrace.Attempt attempt = new AiTrace.Attempt(startedAtMs, endedAtMs);
        attempt.statusCode = statusCode;
        return attempt;
    }

    private static AiTrace.Exchange record(AiTrace.Session session, String method, String url,
            AiTrace.Attempt... attempts) {
        AiTrace.Exchange exchange = session.exchange(method, url);
        for (AiTrace.Attempt attempt : attempts) {
            exchange.add(attempt);
            session.touch(Math.max(attempt.startedAtMs, attempt.endedAtMs));
        }
        return exchange;
    }

    @Test
    public void redactsValuesOfSecretNamedFields() throws IOException, ReflectiveOperationException {
        // given
        String[] names = { "Authorization", "X-Auth-Token", "Set-Cookie", "Cookie", "password", "clientSecret",
                "x-signature", "sign", "SESSION_ID", "access_token" };
        String[] fields = new String[names.length * 2];
        for (int i = 0; i < names.length; i++) {
            fields[i * 2] = names[i];
            fields[i * 2 + 1] = "s3cr3t";
        }

        // when
        Map<String, String> recorded = recordedForm(fields);

        // then
        assertEquals(names.length, recorded.size());
        for (String name : names) {
            assertEquals(name, "<redacted, 6 chars>", recorded.get(name));
        }
    }

    @Test
    public void redactsBearerToken() throws IOException {
        // given
        String secret = "Bearer eyJhbGciOi.payload.signature";

        // when
        send(new Request.Builder().url(INPAINT).header("Authorization", secret).build(), ok());

        // then
        String redacted = headerValue(onlyAttempt().requestHeaders, "Authorization");
        assertEquals("<redacted, " + secret.length() + " chars>", redacted);
        assertFalse(redacted.contains("eyJ"));
        assertFalse(redacted.contains("Bearer"));
    }

    @Test
    public void redactsSecretsInResponseHeaders() throws IOException {
        // when
        get(INPAINT, ok().header("Set-Cookie", "id=abc").header("X-Trace", "t-1"));

        // then
        List<String[]> headers = onlyAttempt().responseHeaders;
        assertEquals("<redacted, 6 chars>", headerValue(headers, "Set-Cookie"));
        assertEquals("t-1", headerValue(headers, "X-Trace"));
    }

    @Test
    public void emptySecretStaysEmpty() throws IOException, ReflectiveOperationException {
        // when
        Map<String, String> recorded = recordedForm("Authorization", "", "cookie", "");

        // then
        assertEquals("", recorded.get("Authorization"));
        assertEquals("", recorded.get("cookie"));
    }

    @Test
    public void secretLongerThanTheLimitReportsItsFullLength() throws IOException, ReflectiveOperationException {
        assertEquals("<redacted, 600 chars>", recordedForm("token", "x".repeat(600)).get("token"));
    }

    @Test
    public void keepsValuesOfOtherFieldsVerbatim() throws IOException, ReflectiveOperationException {
        // when
        Map<String, String> recorded = recordedForm("Content-Type", "application/json", "prompt", "a photo of a cat",
                "empty", "", "title", "café 😀");

        // then
        assertEquals("application/json", recorded.get("Content-Type"));
        assertEquals("a photo of a cat", recorded.get("prompt"));
        assertEquals("", recorded.get("empty"));
        assertEquals("café 😀", recorded.get("title"));
    }

    @Test
    public void truncatesLongValuesOfOtherFields() throws IOException, ReflectiveOperationException {
        assertEquals("x".repeat(512) + "…", recordedForm("prompt", "x".repeat(600)).get("prompt"));
    }

    @Test
    public void secretNamesMatchUnderTurkishLocale() throws IOException, ReflectiveOperationException {
        // given
        Locale saved = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("tr-TR"));

        try {
            // when
            Map<String, String> recorded = recordedForm("SESSION", "abc", "COOKIE", "abc", "SIGNATURE", "abc",
                    "AUTHORIZATION", "abc");

            // then
            for (String name : recorded.keySet()) {
                assertEquals(name, "<redacted, 3 chars>", recorded.get(name));
            }
            assertEquals(4, recorded.size());
        } finally {
            Locale.setDefault(saved);
        }
    }

    @Test
    public void valuesUpToTheLimitAreKept() throws IOException, ReflectiveOperationException {
        // when
        Map<String, String> recorded = recordedForm("a", "", "b", "short", "c", "a".repeat(512));

        // then
        assertEquals("", recorded.get("a"));
        assertEquals("short", recorded.get("b"));
        assertEquals("a".repeat(512), recorded.get("c"));
    }

    @Test
    public void longerValuesAreCutWithAMarker() throws IOException, ReflectiveOperationException {
        // when
        Map<String, String> recorded = recordedForm("a", "a".repeat(513), "b", "a".repeat(5000));

        // then
        assertEquals("a".repeat(512) + "…", recorded.get("a"));
        assertEquals("a".repeat(512) + "…", recorded.get("b"));
    }

    @Test
    public void responseMessageIsCutAtTheLimit() throws IOException {
        // when
        get(INPAINT, ok().message("a".repeat(600)));

        // then
        assertEquals("200 " + "a".repeat(512) + "…", onlyAttempt().statusLine());
    }

    @Test
    public void failureMessageIsCutAtTheLimit() throws IOException {
        // given
        replies.addLast(Reply.failing(new IOException("b".repeat(600))));

        // when
        assertThrows(IOException.class, () -> client.newCall(new Request.Builder().url(INPAINT).build()).execute());

        // then
        String message = onlyAttempt().statusLine();
        assertTrue(message, message.startsWith("java.io.IOException: bbb"));
        assertEquals(513, message.length());
        assertTrue(message.endsWith("b…"));
    }

    @Test
    public void safeUrlDropsTheQueryString() {
        assertEquals("https://h/a/b?<redacted>", AiTrace.safeUrl("https://h/a/b?token=abc&x=1"));
        assertEquals("https://h/a?<redacted>", AiTrace.safeUrl("https://h/a?"));
        assertEquals("https://h/a?<redacted>", AiTrace.safeUrl("https://h/a?x=1?y=2"));
    }

    @Test
    public void safeUrlLeavesQuerylessUrlsAlone() {
        assertEquals("https://h/a/b", AiTrace.safeUrl("https://h/a/b"));
        assertEquals("https://h/a#frag", AiTrace.safeUrl("https://h/a#frag"));
        assertEquals("", AiTrace.safeUrl(""));
    }

    @Test
    public void formatsEpochMillisAsIsoWithOffset() {
        assertEquals("1970-01-01T00:00:00.000+0000", AiTrace.iso(0L));
        assertEquals("2023-11-14T22:13:20.123+0000", AiTrace.iso(1_700_000_000_123L));
    }

    @Test
    public void formatsDurationsBelowOneSecondInMillis() {
        assertEquals("0ms", AiTrace.duration(0L));
        assertEquals("1ms", AiTrace.duration(1L));
        assertEquals("999ms", AiTrace.duration(999L));
    }

    @Test
    public void formatsDurationsFromOneSecondInSeconds() {
        assertEquals("1.00s", AiTrace.duration(1000L));
        assertEquals("1.50s", AiTrace.duration(1500L));
        assertEquals("12.34s", AiTrace.duration(12_340L));
        assertEquals("61.00s", AiTrace.duration(61_000L));
    }

    @Test
    public void extractsThePathWithoutHostOrQuery() {
        assertEquals("/a/b", AiTrace.path("https://h/a/b?x=1"));
        assertEquals("/a/b", AiTrace.path("https://h:8080/a/b"));
        assertEquals("/", AiTrace.path("https://h/"));
        assertEquals("/", AiTrace.path("https://h"));
    }

    @Test
    public void pathAcceptsAMissingOrSchemelessUrl() {
        assertEquals("/", AiTrace.path(null));
        assertEquals("/a/b", AiTrace.path("/a/b?x=1"));
        assertEquals("a/b", AiTrace.path("a/b"));
    }

    @Test
    public void operationIsTheMeaningfulPathSegment() {
        assertEquals("inpaint", AiTrace.operationOf(PREDICT));
        assertEquals("inpaint", AiTrace.operationOf("https://h/api/v1/predict/inpaint?task=revoke"));
        assertEquals("inpaint", AiTrace.operationOf("https://h/Inpaint/Result"));
    }

    @Test
    public void operationSkipsVersionDateOrNumericSegments() {
        assertEquals("sketch", AiTrace.operationOf("https://h/v2/2024-01-31/12345/sketch"));
        assertEquals("request", AiTrace.operationOf("https://h/v2/2024-01-31/12345"));
    }

    @Test
    public void operationSkipsFilesOrResourceIds() {
        assertEquals("request", AiTrace.operationOf("https://h/rst_abc123"));
        assertEquals("request", AiTrace.operationOf("https://h/result.png"));
        assertEquals("enhance", AiTrace.operationOf("https://h/enhance/rst_abc.png"));
    }

    @Test
    public void operationFallsBackToRoutingSegmentsThenRequest() {
        assertEquals("new", AiTrace.operationOf("https://h/api/new"));
        assertEquals("query", AiTrace.operationOf("https://h/api/query"));
        assertEquals("request", AiTrace.operationOf("https://h/"));
        assertEquals("request", AiTrace.operationOf("https://h"));
        assertEquals("request", AiTrace.operationOf(null));
    }

    @Test
    public void operationPrefersTheFirstTokenUnlessAPolishSegmentFollows() {
        assertEquals("enhance", AiTrace.operationOf("https://h/enhance/inpaint"));
        assertEquals("inpaint", AiTrace.operationOf("https://h/enhance/polish_inpaint"));
    }

    @Test
    public void operationStripsPolishPrefixOrModelSuffix() {
        assertEquals("inpaint", AiTrace.operationOf("https://h/polish_inpaint_trt/predict"));
        assertEquals("expand", AiTrace.operationOf("https://h/expand_v2"));
        assertEquals("expand", AiTrace.operationOf("https://h/expand_v3"));
        assertEquals("expand", AiTrace.operationOf("https://h/expand_trt"));
    }

    @Test
    public void operationIgnoresCollageOrPolishVersionSegments() {
        assertEquals("sketch", AiTrace.operationOf("https://h/collage-ai/sketch"));
        assertEquals("sketch", AiTrace.operationOf("https://h/polishv2/sketch"));
        assertEquals("remove-ai-bg", AiTrace.operationOf("https://h/remove-ai-bg"));
    }

    @Test
    public void labelsKnownOperationsWithTheirUnderscoreVariants() {
        String[][] expected = { { "inpaint", "AI Remove" }, { "remove", "AI Remove" }, { "expand", "AI Expand" },
                { "outpaint", "AI Expand" }, { "enhance", "Enhance" }, { "color", "Enhance" }, { "sketch", "Sketch" },
                { "segmentation", "Cutout" }, { "eraser", "Cutout" }, { "bgeraser", "Cutout" }, { "subject", "Cutout" },
                { "inpaint_pro", "AI Remove" }, { "color_enhance", "Enhance" } };
        for (String[] pair : expected) {
            assertEquals(pair[0], pair[1], AiTrace.labelOf(pair[0]));
        }
    }

    @Test
    public void labelPrefixMatchNeedsAnUnderscoreBoundary() {
        assertEquals("Inpainting", AiTrace.labelOf("inpainting"));
        assertEquals("Removebg", AiTrace.labelOf("removebg"));
    }

    @Test
    public void labelsUnknownOperationsInTitleCase() {
        assertEquals("Style Transfer", AiTrace.labelOf("style_transfer"));
        assertEquals("Face Swap Pro", AiTrace.labelOf("face-swap_pro"));
        assertEquals("Upscale", AiTrace.labelOf("upscale"));
        assertEquals("Éclair Noir", AiTrace.labelOf("éclair_noir"));
    }

    @Test
    public void labelsEmptyOperationsAsAGenericRequest() {
        assertEquals("AI request", AiTrace.labelOf(""));
        assertEquals("AI request", AiTrace.labelOf("_"));
        assertEquals("AI request", AiTrace.labelOf("-_-"));
    }

    @Test
    public void resourceIsTheResourceIdWithoutExtensionOrQuery() throws IOException, ReflectiveOperationException {
        assertEquals("rst_abc123", recordedResource("https://h/results/rst_abc123.png"));
        assertEquals("rst_abc123", recordedResource("https://h/results/rst_abc123.png?sig=x.y"));
        assertEquals("rst_abc123", recordedResource("https://h/results/rst_abc123?sig=x.y"));
        assertEquals("rst_abc123", recordedResource("https://h/results/rst_abc123"));
    }

    @Test
    public void resourceFallsBackToAUuidInTheQueryOrPath() throws IOException, ReflectiveOperationException {
        assertEquals(UUID, recordedResource("https://h/query?task_id=" + UUID + "&x=1"));
        assertEquals(UUID, recordedResource("https://h/tasks/" + UUID));
    }

    @Test
    public void resourcePrefersResourceIdOverUuid() throws IOException, ReflectiveOperationException {
        assertEquals("rst_abc", recordedResource("https://h/rst_abc.png?task_id=" + UUID));
    }

    @Test
    public void resourceIsNullWithoutAnIdentifier() throws IOException, ReflectiveOperationException {
        assertNull(recordedResource("https://h/api/v1/predict/inpaint"));
        assertNull(recordedResource("https://h/query?task_id=1234"));
    }

    @Test
    public void submittedRequestsOfOneOperationShareASession() throws IOException {
        post(PREDICT, ok());
        post(PREDICT, ok());
        assertEquals(1, sessions().size());
        post("https://h/enhance", ok());
        assertEquals(2, sessions().size());
        assertEquals(2, sessions().get(0).requestCount());
        assertEquals(1, sessions().get(1).requestCount());
        assertEquals("inpaint", sessions().get(0).operation);
        assertEquals("enhance", sessions().get(1).operation);
    }

    @Test
    public void postedSessionEndsAtItsOpeningTime() throws IOException {
        // when
        post(PREDICT, ok());

        // then
        AiTrace.Session session = onlySession();
        assertEquals("inpaint", session.operation);
        assertEquals("AI Remove", session.feature);
        assertEquals(session.startedAtMs, session.endedAtMs());
        assertEquals(0L, session.durationMs());
        assertTrue(session.postObserved());
        assertNull(session.resource);
    }

    @Test
    public void pollingAdoptsAPendingSessionOfTheSameOperation() throws IOException {
        // given
        post(PREDICT, ok());
        AiTrace.Session submitted = onlySession();

        // when
        get(POLL_A, ok());

        // then
        AiTrace.Session polled = onlySession();
        assertSame(submitted, polled);
        assertEquals("rst_a", polled.resource);
        assertTrue(polled.postObserved());
        assertEquals(2, polled.requestCount());
    }

    @Test
    public void pollingAnAdoptedResourceKeepsReturningTheSameSession() throws IOException {
        // given
        get(POLL_A, ok());
        AiTrace.Session session = onlySession();

        // when
        get(POLL_A, ok());

        // then
        assertSame(session, onlySession());
        assertEquals(2, session.requestCount());
    }

    @Test
    public void pollingWithoutAPriorSubmitOpensASessionMarkedUnobserved() throws IOException {
        // when
        get(POLL_A, ok());

        // then
        AiTrace.Session session = onlySession();
        assertEquals("rst_a", session.resource);
        assertFalse(session.postObserved());
    }

    @Test
    public void differentResourcesOfOneOperationGetSeparateSessions() throws IOException {
        // given
        get(POLL_A, ok());
        get(POLL_B, ok());
        get(POLL_A, ok());
        get(POLL_B, ok());

        // when
        List<AiTrace.Session> sessions = sessions();

        // then
        assertEquals(2, sessions.size());
        assertNotSame(sessions.get(0), sessions.get(1));
        assertEquals("rst_a", sessions.get(0).resource);
        assertEquals("rst_b", sessions.get(1).resource);
        assertEquals(2, sessions.get(0).requestCount());
        assertEquals(2, sessions.get(1).requestCount());
    }

    @Test
    public void submittingAgainAfterAdoptionStartsANewRun() throws IOException {
        // given
        post(PREDICT, ok());
        get(POLL_A, ok());

        // when
        post(PREDICT, ok());

        // then
        List<AiTrace.Session> sessions = sessions();
        assertEquals(2, sessions.size());
        assertEquals("rst_a", sessions.get(0).resource);
        assertNull(sessions.get(1).resource);
    }

    @Test
    public void submittingAgainBeforeAnyPollJoinsThePendingRun() throws IOException {
        // given
        post(PREDICT, ok());

        // when
        post("https://h/api/v2/predict/inpaint", ok());

        // then
        assertEquals(1, sessions().size());
        assertEquals(2, sessions().get(0).exchanges().size());
    }

    @Test
    public void controlOperationsAreFlagged() throws IOException {
        // when
        for (String operation : new String[] { "revoke", "cancel", "query", "task", "inpaint" }) {
            post("https://h/" + operation, ok());
        }

        // then
        List<AiTrace.Session> sessions = sessions();
        assertEquals(5, sessions.size());
        for (int i = 0; i < 4; i++) {
            assertTrue(sessions.get(i).operation, sessions.get(i).control);
        }
        assertFalse(sessions.get(4).control);
    }

    @Test
    public void pollingAControlOperationIsFlagged() throws IOException {
        // when
        get("https://h/revoke/rst_a.png", ok());

        // then
        assertTrue(onlySession().control);
    }

    @Test
    public void keepsOnlyTheEightNewestSessions() throws IOException {
        // when
        for (int i = 0; i < 10; i++) {
            post("https://h/op" + i, ok());
        }

        // then
        List<AiTrace.Session> sessions = sessions();
        assertEquals(8, sessions.size());
        assertEquals("op2", sessions.get(0).operation);
        assertEquals("op9", sessions.get(7).operation);
    }

    @Test
    public void sessionsListIsASnapshot() throws IOException {
        // given
        post(PREDICT, ok());
        List<AiTrace.Session> snapshot = sessions();

        // when
        snapshot.clear();

        // then
        assertEquals(1, sessions().size());
    }

    @Test
    public void tracingIsOffUntilEndpointLoggingIsEnabled() throws IOException {
        // given
        PatchSettings.LOG_ENDPOINTS.save(false);
        post(PREDICT, ok().body("kept"));
        get(POLL_A, ok());

        // when
        List<AiTrace.Session> sessions = sessions();

        // then
        assertTrue(sessions.isEmpty());
    }

    @Test
    public void uploadsAreRecordedWithoutAResponse() {
        // when
        AiTrace.upload("https://h/inpaint");

        // then
        AiTrace.Session session = onlySession();
        AiTrace.Exchange exchange = onlyExchange();
        assertEquals("putFile", exchange.method);
        assertTrue(exchange.dispatchOnly());
        assertEquals(HttpStatus.NONE, session.outcomeCode());
        assertEquals(1, session.requestCount());
    }

    @Test
    public void uploadsJoinThePendingSessionOfTheirOperation() throws IOException {
        // given
        AiTrace.upload("https://h/inpaint");

        // when
        post(PREDICT, ok());

        // then
        AiTrace.Session session = onlySession();
        assertEquals(2, session.exchanges().size());
        assertEquals(2, session.requestCount());
    }

    @Test
    public void uploadsAreIgnoredWhileEndpointLoggingIsOff() {
        // given
        PatchSettings.LOG_ENDPOINTS.save(false);

        // when
        AiTrace.upload("https://h/inpaint");

        // then
        assertTrue(sessions().isEmpty());
    }

    @Test
    public void recordsTheResponseOfAnAttempt() throws IOException {
        // when
        get(INPAINT + "?token=abc",
                ok().message("Fine")
                    .header("X-Trace", "t-1")
                    .body("{\"a\":1}")
                    .contentType("application/json")
                    .sent(5000L, 250L));

        // then
        AiTrace.Attempt attempt = onlyAttempt();
        assertEquals(200, attempt.statusCode());
        assertEquals("200 Fine", attempt.statusLine());
        assertEquals("http/1.1", attempt.protocol);
        assertTrue(attempt.contentType, attempt.contentType.startsWith("application/json"));
        assertEquals(7L, attempt.contentLength);
        assertEquals("{\"a\":1}", attempt.body);
        assertEquals("t-1", headerValue(attempt.responseHeaders, "X-Trace"));
        assertEquals(5000L, attempt.startedAtMs);
        assertEquals(5250L, attempt.endedAtMs);
        assertEquals(250L, attempt.durationMs());
        assertFalse(attempt.inFlight());
    }

    @Test
    public void attemptTimesFallBackToTheWallClockWithoutOkHttpTimestamps() throws IOException {
        // given
        long before = System.currentTimeMillis();

        // when
        get(INPAINT, ok());

        // then
        long after = System.currentTimeMillis();
        AiTrace.Attempt attempt = onlyAttempt();
        assertTrue(attempt.startedAtMs >= before - 1000L);
        assertTrue(attempt.startedAtMs <= attempt.endedAtMs);
        assertTrue(attempt.endedAtMs <= after + 1000L);
    }

    @Test
    public void recordsTheRequestHeadersOfAnAttempt() throws IOException {
        // when
        send(new Request.Builder().url(INPAINT).header("X-Client", "c-1").build(), ok());

        // then
        assertEquals("c-1", headerValue(onlyAttempt().requestHeaders, "X-Client"));
    }

    @Test
    public void bodyIsKeptOnOneLine() throws IOException {
        // when
        get(INPAINT, ok().body("one\ntwo\nthree"));

        // then
        assertEquals("one two three", onlyAttempt().body);
    }

    @Test
    public void longBodyIsCutAtTheLimit() throws IOException {
        // when
        get(INPAINT, ok().body("b".repeat(3000)));

        // then
        assertEquals("b".repeat(2048) + "…", onlyAttempt().body);
    }

    @Test
    public void recordsAtMostThirtyTwoHeaders() throws IOException {
        // given
        Reply reply = ok();
        for (int i = 0; i < 40; i++) {
            reply.header("X-H" + i, "v" + i);
        }

        // when
        get(INPAINT, reply);

        // then
        List<String[]> headers = onlyAttempt().responseHeaders;
        assertEquals(32, headers.size());
        assertEquals("X-H0", headers.get(0)[0]);
        assertEquals("X-H31", headers.get(31)[0]);
    }

    @Test
    public void recordsAtMostTwentyFourFormFields() throws IOException, ReflectiveOperationException {
        // given
        String[] fields = new String[60];
        for (int i = 0; i < 30; i++) {
            fields[i * 2] = "f" + i;
            fields[i * 2 + 1] = "v" + i;
        }

        // when
        Map<String, String> recorded = recordedForm(fields);

        // then
        assertEquals(24, recorded.size());
        assertTrue(recorded.containsKey("f0"));
        assertTrue(recorded.containsKey("f23"));
        assertFalse(recorded.containsKey("f24"));
    }

    @Test
    public void jsonBodiesAreNotRecordedAsPayload() throws IOException {
        // given
        RequestBody json = RequestBody.create("{\"a\":1}", MediaType.get("application/json"));

        // when
        send(new Request.Builder().url(INPAINT).post(json).build(), ok());

        // then
        assertTrue(onlyExchange().payload().isEmpty());
    }

    @Test
    public void exchangeKeepsThePayloadOfItsFirstAttempt() throws IOException {
        // when
        for (String value : new String[] { "first", "second" }) {
            FormBody form = new FormBody.Builder().add("prompt", value).build();
            send(new Request.Builder().url(INPAINT).post(form).build(), ok());
        }

        // then
        AiTrace.Exchange exchange = onlyExchange();
        assertEquals(2, exchange.attempts().size());
        assertEquals("first", exchange.payload().get(0)[1]);
    }

    @Test
    public void failedRequestIsRethrownAfterBeingRecorded() {
        // given
        IOException failure = new IOException("connection reset");
        replies.addLast(Reply.failing(failure));

        // when
        IOException thrown = assertThrows(IOException.class,
                () -> client.newCall(new Request.Builder().url(INPAINT).build()).execute());

        // then
        assertSame(failure, thrown);
        AiTrace.Attempt attempt = onlyAttempt();
        assertTrue(attempt.dispatchOnly());
        assertFalse(attempt.inFlight());
        assertEquals("java.io.IOException: connection reset", attempt.statusLine());
    }

    @Test
    public void crashingRequestIsRethrownAfterBeingRecorded() {
        // given
        IllegalStateException crash = new IllegalStateException("closed");
        replies.addLast(Reply.crashing(crash));

        // when
        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> client.newCall(new Request.Builder().url(INPAINT).build()).execute());

        // then
        assertSame(crash, thrown);
        assertEquals("java.lang.IllegalStateException: closed", onlyAttempt().statusLine());
    }

    @Test
    public void sessionEndTimeIgnoresEarlierTouch() {
        AiTrace.Session session = new AiTrace.Session("inpaint", 1000L);
        session.touch(5000L);
        assertEquals(5000L, session.endedAtMs());
        assertEquals(4000L, session.durationMs());
        session.touch(3000L);
        assertEquals(5000L, session.endedAtMs());
        assertEquals(4000L, session.durationMs());
    }

    @Test
    public void sessionEndsAtItsOpeningTime() {
        // when
        AiTrace.Session session = new AiTrace.Session("inpaint", 1000L);

        // then
        assertEquals("inpaint", session.operation);
        assertEquals("AI Remove", session.feature);
        assertEquals(1000L, session.startedAtMs);
        assertEquals(1000L, session.endedAtMs());
        assertEquals(0L, session.durationMs());
    }

    @Test
    public void sessionReusesAnExchangeForTheSameMethodWithTheSameUrl() {
        AiTrace.Session session = new AiTrace.Session("inpaint", 1000L);
        AiTrace.Exchange get = session.exchange("GET", "https://h/a");
        assertSame(get, session.exchange("GET", "https://h/a"));
        assertNotSame(get, session.exchange("POST", "https://h/a"));
        assertNotSame(get, session.exchange("GET", "https://h/b"));
        assertEquals(3, session.exchanges().size());
    }

    @Test
    public void sessionKeepsOnlyTheSixteenNewestExchanges() {
        // given
        AiTrace.Session session = new AiTrace.Session("inpaint", 1000L);

        // when
        for (int i = 0; i < 20; i++) {
            session.exchange("GET", "https://h/e" + i);
        }

        // then
        List<AiTrace.Exchange> exchanges = session.exchanges();
        assertEquals(16, exchanges.size());
        assertEquals("https://h/e4", exchanges.get(0).url);
        assertEquals("https://h/e19", exchanges.get(15).url);
    }

    @Test
    public void exchangeListsAreSnapshots() {
        AiTrace.Session session = new AiTrace.Session("inpaint", 1000L);
        AiTrace.Exchange exchange = record(session, "GET", "https://h/a", attempt(1000L, 1100L, 200));
        session.exchanges().clear();
        exchange.attempts().clear();
        exchange.payload().add(new String[] { "k", "v" });
        assertEquals(1, session.exchanges().size());
        assertEquals(1, exchange.attempts().size());
        assertTrue(exchange.payload().isEmpty());
    }

    @Test
    public void exchangeStopsRecordingAfterTwoHundredFiftyAttempts() {
        // given
        AiTrace.Exchange exchange = new AiTrace.Exchange("GET", "https://h/a");

        // when
        for (int i = 0; i < 300; i++) {
            exchange.add(attempt(1000L + i, 1001L + i, 404));
        }

        // then
        assertEquals(250, exchange.attempts().size());
    }

    @Test
    public void emptyExchangeHasNoLastAttemptOrDuration() {
        AiTrace.Exchange exchange = new AiTrace.Exchange("GET", "https://h/a");
        assertNull(exchange.last());
        assertEquals(0L, exchange.durationMs());
        assertFalse(exchange.dispatchOnly());
    }

    @Test
    public void exchangeIsDispatchOnlyUntilAnAttemptGetsAResponse() {
        AiTrace.Exchange exchange = new AiTrace.Exchange("POST", "https://h/a");
        exchange.add(attempt(1000L, 1000L, HttpStatus.NONE));
        exchange.add(attempt(1100L, 1100L, HttpStatus.NONE));
        assertTrue(exchange.dispatchOnly());
        exchange.add(attempt(1200L, 1300L, 200));
        assertFalse(exchange.dispatchOnly());
    }

    @Test
    public void exchangeDurationSpansFirstStartToLastEnd() {
        AiTrace.Exchange exchange = new AiTrace.Exchange("GET", "https://h/a");
        exchange.add(attempt(1000L, 1500L, 404));
        exchange.add(attempt(2000L, 2600L, 200));
        assertEquals(1600L, exchange.durationMs());
        assertEquals(200, exchange.last().statusCode());
    }

    @Test
    public void exchangeDurationClampsReversedAttemptToZero() {
        // given
        AiTrace.Exchange exchange = new AiTrace.Exchange("GET", "https://h/a");
        exchange.add(attempt(5000L, 4000L, 200));

        // when
        long duration = exchange.durationMs();

        // then
        assertEquals(0L, duration);
    }

    @Test
    public void exchangePathKeepsOnlyThePath() {
        assertEquals("/a/b", new AiTrace.Exchange("GET", "https://h/a/b?x=1").path());
    }

    @Test
    public void attemptStartsWithoutAResponse() {
        // when
        AiTrace.Attempt attempt = new AiTrace.Attempt(1000L, 0L);

        // then
        assertEquals(HttpStatus.NONE, attempt.statusCode());
        assertTrue(attempt.dispatchOnly());
        assertTrue(attempt.inFlight());
        assertEquals("", attempt.status());
    }

    @Test
    public void attemptWithAnEndTimeIsNotInFlight() {
        assertFalse(new AiTrace.Attempt(1000L, 1200L).inFlight());
    }

    @Test
    public void attemptDurationIsEndMinusStart() {
        assertEquals(200L, new AiTrace.Attempt(1000L, 1200L).durationMs());
        assertEquals(0L, new AiTrace.Attempt(1000L, 1000L).durationMs());
    }

    @Test
    public void attemptDurationClampsReversedTimesToZero() {
        assertEquals(0L, new AiTrace.Attempt(2000L, 1000L).durationMs());
        assertEquals(0L, new AiTrace.Attempt(Long.MAX_VALUE, 0L).durationMs());
    }

    @Test
    public void statusLineWithoutAResponseShowsTheMessageOrAPlaceholder() {
        AiTrace.Attempt attempt = new AiTrace.Attempt(1000L, 1200L);
        assertEquals("no response captured", attempt.statusLine());
        attempt.message = "timeout";
        assertEquals("timeout", attempt.statusLine());
    }

    @Test
    public void statusLineUsesTheServerMessageBeforeTheStandardReason() {
        AiTrace.Attempt attempt = attempt(1000L, 1200L, 200);
        assertEquals("200 OK", attempt.statusLine());
        attempt.message = "Fine";
        assertEquals("200 Fine", attempt.statusLine());
    }

    @Test
    public void statusLineOfAnUnknownCodeIsJustTheCode() {
        AiTrace.Attempt attempt = attempt(1000L, 1200L, 418);
        assertEquals("418", attempt.statusLine());
        attempt.message = "I'm a teapot";
        assertEquals("418 I'm a teapot", attempt.statusLine());
    }

    @Test
    public void sessionRequestCountSumsTheAttemptsOfItsExchanges() {
        // given
        AiTrace.Session session = new AiTrace.Session("inpaint", 1000L);
        record(session, "POST", "https://h/a", attempt(1000L, 1100L, 200));
        record(session, "GET", "https://h/b", attempt(1200L, 1300L, 404), attempt(1400L, 1500L, 404),
                attempt(1600L, 1700L, 200));

        // when
        int count = session.requestCount();

        // then
        assertEquals(4, count);
    }

    @Test
    public void outcomeIsTheNewestResponseAcrossExchanges() {
        // given
        AiTrace.Session session = new AiTrace.Session("inpaint", 1000L);
        record(session, "POST", "https://h/a", attempt(1000L, 1100L, 200));
        record(session, "GET", "https://h/b", attempt(1200L, 1300L, 404), attempt(1400L, 1500L, 500));

        // when
        int outcome = session.outcomeCode();

        // then
        assertEquals(500, outcome);
    }

    @Test
    public void outcomeSkipsAttemptsWithoutAResponse() {
        // given
        AiTrace.Session session = new AiTrace.Session("inpaint", 1000L);
        record(session, "POST", "https://h/a", attempt(1000L, 1100L, 202));
        record(session, "GET", "https://h/b", attempt(1200L, 1300L, 404), attempt(1400L, 0L, HttpStatus.NONE));
        record(session, "GET", "https://h/c", attempt(1500L, 0L, HttpStatus.NONE));

        // when
        int outcome = session.outcomeCode();

        // then
        assertEquals(404, outcome);
    }

    @Test
    public void outcomeWithoutAnyResponseIsNone() {
        AiTrace.Session session = new AiTrace.Session("inpaint", 1000L);
        assertEquals(HttpStatus.NONE, session.outcomeCode());
        record(session, "POST", "https://h/a", attempt(1000L, 0L, HttpStatus.NONE));
        assertEquals(HttpStatus.NONE, session.outcomeCode());
    }

    @Test
    public void headlineSummarisesTheNewestRun() throws IOException {
        // given
        post(PREDICT, ok());
        get(POLL_A, ok());
        AiTrace.Session session = onlySession();
        session.touch(session.startedAtMs + 12_340L);

        // when
        String actual = AiTrace.headline();

        // then
        assertEquals(headline("AI Remove", 2, "12.34s"), actual);
    }

    @Test
    public void headlineUsesMillisForShortRuns() throws IOException {
        // given
        post("https://h/enhance", ok());
        AiTrace.Session session = onlySession();
        session.touch(session.startedAtMs + 750L);

        // when
        String actual = AiTrace.headline();

        // then
        assertEquals(headline("Enhance", 1, "750ms"), actual);
    }

    @Test
    public void headlineSkipsNewerControlSessions() throws IOException {
        // given
        post("https://h/sketch", ok(), ok());
        AiTrace.Session run = sessions().get(0);
        run.touch(run.startedAtMs + 61_000L);
        post("https://h/revoke", ok(), ok(), ok());

        // when
        String actual = AiTrace.headline();

        // then
        assertEquals(headline("Sketch", 2, "61.00s"), actual);
    }

    @Test
    public void headlineFallsBackToTheNewestControlSession() throws IOException {
        // given
        post("https://h/cancel", ok(), ok());
        post("https://h/revoke", ok(), ok(), ok());
        AiTrace.Session newest = sessions().get(1);
        newest.touch(newest.startedAtMs + 61_000L);

        // when
        String actual = AiTrace.headline();

        // then
        assertEquals(headline("Revoke", 3, "61.00s"), actual);
    }

    @Test
    public void headlineWithoutSessionsExplainsHowToRecord() {
        assertEquals("No AI request recorded yet this session", AiTrace.headline());
        PatchSettings.LOG_ENDPOINTS.save(false);
        assertEquals("Turn on AI request logging above to record requests", AiTrace.headline());
    }

    @Test
    public void liveRowsFoldRepeatedPollsIntoOneRow() throws IOException {
        // given
        post(PREDICT, ok());
        get("https://h/api/v1/inpaint/rst_abc.png?task=1", status(404), status(404), ok());

        // when
        List<AiTrace.LiveRow> rows = AiTrace.liveRows(0L, 8);

        // then
        assertEquals(2, rows.size());
        assertEquals("POST", rows.get(0).method);
        assertEquals("/inpaint", rows.get(0).path);
        assertEquals(1, rows.get(0).count);
        assertEquals("200 OK", rows.get(0).status());
        assertEquals("GET", rows.get(1).method);
        assertEquals("/inpaint/rst_abc.png", rows.get(1).path);
        assertEquals(3, rows.get(1).count);
        assertEquals("200 OK", rows.get(1).status());
        assertEquals(200, rows.get(1).statusCode);
    }

    @Test
    public void liveRowTreatsPendingNotFoundAsInFlight() throws IOException {
        // given
        get(INPAINT, status(404));

        // when
        AiTrace.LiveRow row = AiTrace.liveRows(0L, 8).get(0);

        // then
        assertTrue(row.inFlight());
        assertFalse(row.unobserved());
        assertEquals("", row.status());
        assertEquals(404, row.statusCode);
    }

    @Test
    public void liveRowKeepsTheLastRealStatusWhenLaterPollsPend() throws IOException {
        // given
        get(INPAINT, ok(), status(404));

        // when
        AiTrace.LiveRow row = AiTrace.liveRows(0L, 8).get(0);

        // then
        assertEquals(2, row.count);
        assertEquals("200 OK", row.status());
        assertEquals(200, row.statusCode);
    }

    @Test
    public void liveRowOfAnUploadWithoutResponseIsUnobserved() {
        // given
        AiTrace.upload(INPAINT);

        // when
        AiTrace.LiveRow row = AiTrace.liveRows(0L, 8).get(0);

        // then
        assertTrue(row.unobserved());
        assertFalse(row.inFlight());
        assertNull(row.status());
    }

    @Test
    public void liveRowOfAnUploadIsInFlightWhileTheUploadStageRuns() {
        ProgressTrace.beginUpload("AI Remove");
        AiTrace.upload(INPAINT);
        AiTrace.LiveRow row = AiTrace.liveRows(0L, 8).get(0);
        assertTrue(row.inFlight());
        assertFalse(row.unobserved());
        assertEquals("", row.status());
        ProgressTrace.predict();
        assertTrue(AiTrace.liveRows(0L, 8).get(0).unobserved());
    }

    @Test
    public void liveRowOfAnAnsweredButUnfinishedAttemptIsInFlight() throws IOException {
        // given
        get(INPAINT, ok());
        onlyAttempt().endedAtMs = 0L;

        // when
        AiTrace.LiveRow row = AiTrace.liveRows(0L, 8).get(0);

        // then
        assertTrue(row.inFlight());
        assertEquals("", row.status());
    }

    @Test
    public void liveRowShowsJustTheCodeForUnknownStatuses() throws IOException {
        // given
        get(INPAINT, status(418));

        // when
        String status = AiTrace.liveRows(0L, 8).get(0).status();

        // then
        assertEquals("418", status);
    }

    @Test
    public void liveRowsListOnlyAttemptsFromTheRunStartOnwards() throws IOException {
        get("https://h/inpaint/old.png", ok().sent(1000L, 100L));
        get("https://h/inpaint/new.png", ok().sent(5000L, 100L));
        get("https://h/inpaint/mixed.png", status(404).sent(1000L, 100L), ok().sent(6000L, 100L));
        List<AiTrace.LiveRow> rows = AiTrace.liveRows(5000L, 8);
        assertEquals(2, rows.size());
        assertEquals("/inpaint/new.png", rows.get(0).path);
        assertEquals("/inpaint/mixed.png", rows.get(1).path);
        assertEquals(1, rows.get(1).count);
        assertEquals(1, AiTrace.liveRows(6000L, 8).size());
        assertTrue(AiTrace.liveRows(7000L, 8).isEmpty());
    }

    @Test
    public void liveRowsIncludeAttemptsStartingExactlyAtTheRunStart() throws IOException {
        get(INPAINT, ok().sent(5000L, 100L));
        assertEquals(1, AiTrace.liveRows(5000L, 8).size());
        assertTrue(AiTrace.liveRows(5001L, 8).isEmpty());
    }

    @Test
    public void liveRowsSkipControlSessions() throws IOException {
        post("https://h/revoke", ok());
        assertTrue(AiTrace.liveRows(0L, 8).isEmpty());
        post(PREDICT, ok());
        assertEquals(1, AiTrace.liveRows(0L, 8).size());
    }

    @Test
    public void liveRowsSeparateRoutesByMethod() throws IOException {
        // given
        post(INPAINT, ok());
        get(INPAINT, ok());

        // when
        List<AiTrace.LiveRow> rows = AiTrace.liveRows(0L, 8);

        // then
        assertEquals(2, rows.size());
    }

    @Test
    public void liveRowsKeepTheNewestRoutesWhenOverTheLimit() throws IOException {
        for (int i = 1; i <= 5; i++) {
            get("https://h/a" + i, ok());
        }
        List<AiTrace.LiveRow> rows = AiTrace.liveRows(0L, 2);
        assertEquals(2, rows.size());
        assertEquals("/a4", rows.get(0).path);
        assertEquals("/a5", rows.get(1).path);
        assertEquals(5, AiTrace.liveRows(0L, 5).size());
        assertTrue(AiTrace.liveRows(0L, 0).isEmpty());
    }

    @Test
    public void liveRowPathKeepsTheLastTwoMeaningfulSegments() throws IOException {
        // given
        get("https://h/api/v1/enhance/predict/inpaint/sketch", ok());
        get("https://h/api/v1/enhance/predict/inpaint", ok());
        get("https://h/api/v1/inpaint", ok());

        // when
        List<AiTrace.LiveRow> rows = AiTrace.liveRows(0L, 8);

        // then
        assertEquals("/inpaint/sketch", rows.get(0).path);
        assertEquals("/enhance/inpaint", rows.get(1).path);
        assertEquals("/inpaint", rows.get(2).path);
    }

    @Test
    public void liveRowPathShortensLongFileSegments() throws IOException {
        // given
        get("https://h/inpaint/rst_0123456789abcdef0123.png", ok());

        // when
        String path = AiTrace.liveRows(0L, 8).get(0).path;

        // then
        assertEquals("/inpaint/rst_0123…0123.png", path);
    }

    @Test
    public void liveRowPathKeepsFileSegmentsUpToTwentyCharacters() throws IOException {
        // given
        String twenty = "rst_0123456789ab.png";
        String twentyOne = "rst_0123456789abc.png";
        assertEquals(20, twenty.length());
        get("https://h/inpaint/" + twenty, ok());
        get("https://h/inpaint/" + twentyOne, ok());

        // when
        List<AiTrace.LiveRow> rows = AiTrace.liveRows(0L, 8);

        // then
        assertEquals("/inpaint/" + twenty, rows.get(0).path);
        assertEquals("/inpaint/rst_0123…9abc.png", rows.get(1).path);
    }

    @Test
    public void liveRowPathFallsBackToTheFullPathWithoutMeaningfulSegments() throws IOException {
        // given
        get("https://h/v1/2024-01-31/42", ok());
        get("https://h/", ok());

        // when
        List<AiTrace.LiveRow> rows = AiTrace.liveRows(0L, 8);

        // then
        assertEquals("/v1/2024-01-31/42", rows.get(0).path);
        assertEquals("/", rows.get(1).path);
    }

    private static final class Reply {

        private final int code;

        private String message = "";

        private long sentAtMs;

        private long latencyMs;

        private String body = "";

        private String contentType = "text/plain";

        private final Headers.Builder headers = new Headers.Builder();

        private IOException failure;

        private RuntimeException crash;

        Reply(int code) {
            this.code = code;
        }

        static Reply failing(IOException failure) {
            Reply reply = new Reply(0);
            reply.failure = failure;
            return reply;
        }

        static Reply crashing(RuntimeException crash) {
            Reply reply = new Reply(0);
            reply.crash = crash;
            return reply;
        }

        Reply message(String message) {
            this.message = message;
            return this;
        }

        Reply sent(long sentAtMs, long latencyMs) {
            this.sentAtMs = sentAtMs;
            this.latencyMs = latencyMs;
            return this;
        }

        Reply body(String body) {
            this.body = body;
            return this;
        }

        Reply contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        Reply header(String name, String value) {
            this.headers.add(name, value);
            return this;
        }

    }

}
