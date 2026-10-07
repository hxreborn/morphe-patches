/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.photoeditorpro;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.TimeZone;

import android.app.Activity;
import android.app.Dialog;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import app.morphe.extension.shared.Utils;
import okhttp3.FormBody;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.shadows.ShadowSystemClock;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public final class RequestLogTest {

    private static final String ENHANCE = "https://h/enhance";

    private static final String NO_RESPONSE = "no response";

    private static final String TIMES = "×";

    private static TimeZone defaultZone;

    private final Deque<Object> replies = new ArrayDeque<>();

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
    public void startWithoutTraces() throws ReflectiveOperationException {
        Utils.setContext(RuntimeEnvironment.getApplication());
        PatchSettings.LOG_ENDPOINTS.save(true);
        clearTraces();
        replies.clear();
        OkHttpClient.Builder builder = newBuilder();
        AiTrace.install(builder);
        builder.addInterceptor(this::transport);
        client = builder.build();
    }

    @After
    public void leaveNoTraces() throws ReflectiveOperationException {
        PatchSettings.LOG_ENDPOINTS.save(false);
        clearTraces();
    }

    private static OkHttpClient.Builder newBuilder() {
        return new OkHttpClient.Builder();
    }

    private static void clearTraces() throws ReflectiveOperationException {
        clear(AiTrace.class, "SESSIONS");
        clear(ProgressTrace.class, "LAST_RUN");
        ProgressTrace.progress(100);
    }

    private static void clear(Class<?> owner, String name) throws ReflectiveOperationException {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        ((List<?>) field.get(null)).clear();
    }

    private Response transport(Interceptor.Chain chain) throws IOException {
        Object reply = replies.removeFirst();
        if (reply instanceof TransportFailure failure) {
            throw failure;
        }
        long[] answer = (long[]) reply;
        return new Response.Builder().request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .code((int) answer[0])
            .message("")
            .body(ResponseBody.create("", MediaType.get("text/plain")))
            .sentRequestAtMillis(answer[1])
            .receivedResponseAtMillis(answer[1] + answer[2])
            .build();
    }

    private static long[] status(int code) {
        return new long[] { code, 0L, 0L };
    }

    private static long[] status(int code, long sentAtMs, long latencyMs) {
        return new long[] { code, sentAtMs, latencyMs };
    }

    private static TransportFailure failure() {
        return new TransportFailure();
    }

    private void post(String url, Object... answers) throws IOException {
        for (Object answer : answers) {
            send(new Request.Builder().url(url).post(new FormBody.Builder().build()).build(), answer);
        }
    }

    private void get(String url, Object... answers) throws IOException {
        for (Object answer : answers) {
            send(new Request.Builder().url(url).get().build(), answer);
        }
    }

    private void send(Request request, Object answer) throws IOException {
        replies.addLast(answer);
        if (answer instanceof TransportFailure) {
            assertThrows(TransportFailure.class, () -> execute(request));
        } else {
            execute(request);
        }
    }

    private void execute(Request request) throws IOException {
        try (Response response = client.newCall(request).execute()) {
            response.body().string();
        }
    }

    private static final class TransportFailure extends IOException {

        TransportFailure() {
            super("boom");
        }

    }

    private static void advance(long millis) {
        ShadowSystemClock.advanceBy(Duration.ofMillis(millis));
    }

    private static String separator() {
        return " " + (char) 0xB7 + " ";
    }

    private static List<View> shownViews() {
        Activity activity = Robolectric.buildActivity(Activity.class).create().get();
        RequestLog.show(activity);
        Dialog dialog = ShadowDialog.getLatestDialog();
        List<View> views = new ArrayList<>();
        collect(dialog.getWindow().getDecorView(), views);
        return views;
    }

    private static void collect(View view, List<View> into) {
        into.add(view);
        if (view instanceof ViewGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) {
                collect(group.getChildAt(i), into);
            }
        }
    }

    private static List<String> texts(List<View> views) {
        List<String> texts = new ArrayList<>();
        for (View view : views) {
            if (view instanceof TextView text) {
                texts.add(text.getText().toString());
            }
        }
        return texts;
    }

    private static List<String> descriptionsStartingWith(List<View> views, String prefix) {
        List<String> found = new ArrayList<>();
        for (View view : views) {
            CharSequence description = view.getContentDescription();
            if (description != null && description.toString().startsWith(prefix)) {
                found.add(description.toString());
            }
        }
        return found;
    }

    private String outcomeOf(Object... answers) throws IOException, ReflectiveOperationException {
        clearTraces();
        get(ENHANCE, answers);
        List<String> outcomes = descriptionsStartingWith(shownViews(), "outcome ");
        assertEquals(1, outcomes.size());
        return outcomes.get(0).substring("outcome ".length());
    }

    @Test
    public void outcomeChipNamesKnownCodes() throws IOException {
        // given
        post("https://h/enhance", status(200));
        post("https://h/sketch", status(404));
        post("https://h/inpaint", status(503));

        // when
        List<String> texts = texts(shownViews());

        // then
        assertTrue(texts.toString(), texts.contains("200 OK"));
        assertTrue(texts.toString(), texts.contains("404 Not Found"));
        assertTrue(texts.toString(), texts.contains("503 Service Unavailable"));
    }

    @Test
    public void unknownCodesAreShownAsTheBareCode() throws IOException {
        // given
        post(ENHANCE, status(418));

        // when
        List<View> views = shownViews();

        // then
        List<String> rows = descriptionsStartingWith(views, "Attempt ");
        assertEquals(1, rows.size());
        assertTrue(rows.get(0), rows.get(0).contains(", 418, "));
    }

    @Test
    public void attemptsWithoutAResponseAreDescribedAsSuch() throws IOException {
        // given
        get(ENHANCE, failure(), status(200));

        // when
        List<String> rows = descriptionsStartingWith(shownViews(), "Attempt ");

        // then
        assertEquals(2, rows.size());
        assertTrue(rows.get(0), rows.get(0).startsWith("Attempt 1, "));
        assertTrue(rows.get(0), rows.get(0).contains(", " + NO_RESPONSE + ", "));
        assertTrue(rows.get(1), rows.get(1).contains(", 200 OK, "));
    }

    @Test
    public void attemptTimesShowTimeOfDayWithMillis() throws IOException {
        // given
        get(ENHANCE, status(200, 1L, 100L), status(200, 3_723_004L, 100L), status(200, 86_399_999L, 100L));

        // when
        List<String> rows = descriptionsStartingWith(shownViews(), "Attempt ");

        // then
        assertEquals(List.of("Attempt 1, 00:00:00.001, 200 OK, 100ms", "Attempt 2, 01:02:03.004, 200 OK, 100ms",
                "Attempt 3, 23:59:59.999, 200 OK, 100ms"), rows);
    }

    @Test
    public void attemptTimesWrapAtMidnight() throws IOException {
        // given
        get(ENHANCE, status(200, 86_400_001L, 100L), status(200, 1_700_000_000_123L, 100L));

        // when
        List<String> rows = descriptionsStartingWith(shownViews(), "Attempt ");

        // then
        assertEquals(List.of("Attempt 1, 00:00:00.001, 200 OK, 100ms", "Attempt 2, 22:13:20.123, 200 OK, 100ms"), rows);
    }

    @Test
    public void attemptTableFoldsTheMiddleAttemptsIntoACount() throws IOException {
        // given
        get(ENHANCE, status(404), status(404), status(404), status(404), status(404), status(200));

        // when
        List<View> views = shownViews();

        // then
        assertEquals(6, descriptionsStartingWith(views, "Attempt ").size());
        assertEquals(1, descriptionsStartingWith(views, "2 hidden").size());
    }

    @Test
    public void tallyOfASingleAttemptIsItsStatus() throws IOException, ReflectiveOperationException {
        assertEquals("200", outcomeOf(status(200)));
        assertEquals("503", outcomeOf(status(503)));
    }

    @Test
    public void tallyCountsEarlierAttemptsBeforeTheClosingOne() throws IOException, ReflectiveOperationException {
        assertEquals("404 " + TIMES + "2 then 200", outcomeOf(status(404), status(404), status(200)));
        assertEquals("404 " + TIMES + "1 then 200", outcomeOf(status(404), status(200)));
    }

    @Test
    public void tallyListsStatusesInFirstSeenOrder() throws IOException, ReflectiveOperationException {
        assertEquals("404 " + TIMES + "2, 500 " + TIMES + "1 then 200",
                outcomeOf(status(404), status(500), status(404), status(200)));
        assertEquals("500 " + TIMES + "1, 404 " + TIMES + "2 then 200",
                outcomeOf(status(500), status(404), status(404), status(200)));
    }

    @Test
    public void tallyNamesAttemptsWithoutAResponse() throws IOException, ReflectiveOperationException {
        assertEquals(NO_RESPONSE + " " + TIMES + "1 then 200", outcomeOf(failure(), status(200)));
        assertEquals("200 " + TIMES + "1 then " + NO_RESPONSE, outcomeOf(status(200), failure()));
        assertEquals(NO_RESPONSE + " " + TIMES + "2 then 404", outcomeOf(failure(), failure(), status(404)));
    }

    @Test
    public void tallyKeepsTheClosingStatusOutOfTheCounts() throws IOException, ReflectiveOperationException {
        assertEquals("200 " + TIMES + "2 then 200", outcomeOf(status(200), status(200), status(200)));
    }

    @Test
    public void sessionEndedTimeUsesTheSameClockFormat() throws IOException {
        // given
        post(ENHANCE, status(200));

        // when
        List<View> views = shownViews();

        // then
        List<String> ended = descriptionsStartingWith(views, "ended ");
        assertEquals(1, ended.size());
        assertTrue(ended.get(0), ended.get(0).matches("ended \\d{2}:\\d{2}:\\d{2}\\.\\d{3}"));
    }

    @Test
    public void summaryDescribesTheLatestRequestWhenRequestsAreRecorded() throws IOException {
        // given
        post(ENHANCE, status(200), status(200));
        AiTrace.Session session = AiTrace.sessions().get(0);
        session.touch(session.startedAtMs + 12_340L);
        ProgressTrace.beginUpload("Enhance");
        advance(1200L);
        ProgressTrace.predict();

        // when
        String summary = RequestLog.summary();

        // then
        assertEquals("Enhance" + separator() + "2 requests" + separator() + "12.34s", summary);
    }

    @Test
    public void summaryFallsBackToTheProgressStagesWithoutRequests() {
        // given
        ProgressTrace.beginUpload("Enhance");
        advance(1200L);
        ProgressTrace.predict();
        advance(800L);
        ProgressTrace.polling();

        // when
        String summary = RequestLog.summary();

        // then
        assertEquals("Processing 0.80s", summary);
    }

    @Test
    public void summaryExplainsTheEmptyLogWhenNothingWasRecorded() {
        assertEquals("No AI request recorded yet this session", RequestLog.summary());
        PatchSettings.LOG_ENDPOINTS.save(false);
        assertEquals("Turn on AI request logging above to record requests", RequestLog.summary());
    }

}
