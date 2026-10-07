/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.photoeditorpro;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.util.TypedValue;
import android.widget.TextView;
import app.morphe.extension.shared.Utils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.shadows.ShadowSystemClock;

@RunWith(RobolectricTestRunner.class)
public final class ProgressTraceTest {

    private static final int DELIVERED = 0x400;

    private static final int UPLOAD_TARGET = 45;

    private static final int POLL_TARGET = 60;

    private static final int POLL_CEILING = 95;

    private static final String TAG = "hxreborn/progress";

    @Before
    public void startWithoutARun() throws ReflectiveOperationException {
        Utils.setContext(RuntimeEnvironment.getApplication());
        PatchSettings.SHOW_AI_PROGRESS.save(true);
        PatchSettings.LOG_ENDPOINTS.save(false);
        PatchSettings.RUN_HISTORY.save("");
        forgetRun();
        ShadowLog.clear();
        advance(1000L);
    }

    @After
    public void leaveNoRun() throws ReflectiveOperationException {
        PatchSettings.SHOW_AI_PROGRESS.save(true);
        PatchSettings.LOG_ENDPOINTS.save(false);
        PatchSettings.RUN_HISTORY.save("");
        forgetRun();
    }

    private static void forgetRun() throws ReflectiveOperationException {
        set(RunHistory.class, "cache", null);
        set(ProgressTrace.class, "stage", -1);
        set(ProgressTrace.class, "runStartedAt", 0L);
        set(ProgressTrace.class, "stageStartedAt", 0L);
        set(ProgressTrace.class, "pollCount", 0);
        set(ProgressTrace.class, "settled", false);
        set(ProgressTrace.class, "feature", null);
        set(ProgressTrace.class, "liveView", null);
        set(ProgressTrace.class, "liveStock", null);
        Field lastRun = ProgressTrace.class.getDeclaredField("LAST_RUN");
        lastRun.setAccessible(true);
        ((List<?>) lastRun.get(null)).clear();
    }

    private static void set(Class<?> owner, String name, Object value) throws ReflectiveOperationException {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        field.set(null, value);
    }

    private static void advance(long millis) {
        ShadowSystemClock.advanceBy(Duration.ofMillis(millis));
    }

    private static void startPolling(String feature) {
        ProgressTrace.beginUpload(feature);
        ProgressTrace.predict();
        ProgressTrace.polling();
    }

    private static TextView textView() {
        return new TextView(RuntimeEnvironment.getApplication());
    }

    private static List<String> progressLog() {
        List<String> messages = new ArrayList<>();
        for (ShadowLog.LogItem item : ShadowLog.getLogsForTag(TAG)) {
            messages.add(item.msg);
        }
        return messages;
    }

    @Test
    public void pollProgressStartsAtTheStageTarget() {
        // given
        RunHistory.add("Enhance", 10_000L);
        startPolling("Enhance");

        // when
        int progress = ProgressTrace.progress(0);

        // then
        assertEquals(POLL_TARGET, progress);
    }

    @Test
    public void pollProgressGrowsLinearlyWithTheWaitedShare() {
        RunHistory.add("Enhance", 10_000L);
        startPolling("Enhance");
        advance(1000L);
        assertEquals(POLL_TARGET + 3, ProgressTrace.progress(0));
        advance(4000L);
        assertEquals(POLL_TARGET + 17, ProgressTrace.progress(0));
        advance(2500L);
        assertEquals(POLL_TARGET + 26, ProgressTrace.progress(0));
    }

    @Test
    public void pollProgressReachesTheCeilingAtTheExpectedDuration() {
        // given
        RunHistory.add("Enhance", 10_000L);
        startPolling("Enhance");
        advance(10_000L);

        // when
        int progress = ProgressTrace.progress(0);

        // then
        assertEquals(POLL_CEILING, progress);
    }

    @Test
    public void pollProgressStopsAtTheCeilingWhenTheRunOverruns() {
        RunHistory.add("Enhance", 10_000L);
        startPolling("Enhance");
        advance(20_000L);
        assertEquals(POLL_CEILING, ProgressTrace.progress(0));
        advance(3_600_000L);
        assertEquals(POLL_CEILING, ProgressTrace.progress(0));
    }

    @Test
    public void pollProgressRisesToTheCeilingWithoutShrinking() {
        RunHistory.add("Enhance", 9000L);
        startPolling("Enhance");
        int previous = 0;
        for (long waited = 0; waited <= 20_000L; waited += 250L) {
            int progress = ProgressTrace.progress(0);
            assertTrue("waited=" + waited, progress >= previous);
            assertTrue("waited=" + waited, progress <= POLL_CEILING);
            previous = progress;
            advance(250L);
        }
        assertEquals(POLL_CEILING, previous);
    }

    @Test
    public void shorterExpectedRunsAdvanceFaster() {
        RunHistory.add("Enhance", 4000L);
        RunHistory.add("Sketch", 12_000L);
        startPolling("Enhance");
        advance(3000L);
        int fast = ProgressTrace.progress(0);
        startPolling("Sketch");
        advance(3000L);
        int slow = ProgressTrace.progress(0);
        assertTrue(fast + " > " + slow, fast > slow);
        assertEquals(POLL_TARGET + 26, fast);
        assertEquals(POLL_TARGET + 8, slow);
    }

    @Test
    public void timeAlreadySpentShrinksTheExpectedWait() {
        // given
        RunHistory.add("Enhance", 10_000L);
        ProgressTrace.beginUpload("Enhance");
        advance(4000L);
        ProgressTrace.predict();
        ProgressTrace.polling();
        advance(3000L);

        // when
        int progress = ProgressTrace.progress(0);

        // then
        assertEquals(POLL_TARGET + 17, progress);
    }

    @Test
    public void expectedWaitFloorsAtOnePointFiveSeconds() {
        RunHistory.add("Enhance", 500L);
        startPolling("Enhance");
        advance(750L);
        assertEquals(POLL_TARGET + 17, ProgressTrace.progress(0));
        advance(10_000L);
        assertEquals(POLL_CEILING, ProgressTrace.progress(0));
    }

    @Test
    public void pollProgressUsesTheNineSecondFallbackWithoutHistory() {
        // given
        startPolling("Enhance");
        advance(4500L);

        // when
        int progress = ProgressTrace.progress(0);

        // then
        assertEquals(POLL_TARGET + 17, progress);
    }

    @Test
    public void progressIsTheHigherOfTheStockValueOrTheStageTarget() {
        ProgressTrace.beginUpload("Enhance");
        assertEquals(UPLOAD_TARGET, ProgressTrace.progress(10));
        assertEquals(UPLOAD_TARGET, ProgressTrace.progress(UPLOAD_TARGET));
        assertEquals(90, ProgressTrace.progress(90));
        ProgressTrace.predict();
        assertEquals(POLL_TARGET, ProgressTrace.progress(10));
        assertEquals(99, ProgressTrace.progress(99));
    }

    @Test
    public void progressJumpsToTheDeliveredStageTarget() {
        // given
        ProgressTrace.beginUpload("Enhance");
        ProgressTrace.finished(DELIVERED);

        // when
        int progress = ProgressTrace.progress(20);

        // then
        assertEquals(100, progress);
    }

    @Test
    public void stockProgressWithoutARunHasAFloorOfFifty() {
        assertEquals(50, ProgressTrace.progress(0));
        assertEquals(50, ProgressTrace.progress(49));
        assertEquals(50, ProgressTrace.progress(50));
        assertEquals(51, ProgressTrace.progress(51));
        assertEquals(99, ProgressTrace.progress(99));
    }

    @Test
    public void stockProgressAtOneHundredIsFinal() {
        assertEquals(100, ProgressTrace.progress(100));
        assertEquals(100, ProgressTrace.progress(250));
    }

    @Test
    public void stockCompletionEndsTheRunStage() {
        ProgressTrace.beginUpload("Enhance");
        assertTrue(ProgressTrace.uploading());
        assertEquals(100, ProgressTrace.progress(100));
        assertFalse(ProgressTrace.uploading());
        assertEquals(50, ProgressTrace.progress(10));
    }

    @Test
    public void disabledProgressKeepsTheStockBehaviour() {
        PatchSettings.SHOW_AI_PROGRESS.save(false);
        ProgressTrace.beginUpload("Enhance");
        assertEquals(50, ProgressTrace.progress(10));
        assertEquals(70, ProgressTrace.progress(70));
        assertEquals(777L, ProgressTrace.progressDurationMs(777L));
        CharSequence stock = "45%";
        assertSame(stock, ProgressTrace.label(textView(), stock));
    }

    @Test
    public void stageDurationsFollowTheRun() {
        assertEquals(777L, ProgressTrace.progressDurationMs(777L));
        ProgressTrace.beginUpload("Enhance");
        assertEquals(4000L, ProgressTrace.progressDurationMs(777L));
        ProgressTrace.predict();
        assertEquals(1500L, ProgressTrace.progressDurationMs(777L));
        ProgressTrace.polling();
        assertEquals(300L, ProgressTrace.progressDurationMs(777L));
        ProgressTrace.finished(DELIVERED);
        assertEquals(400L, ProgressTrace.progressDurationMs(777L));
    }

    @Test
    public void stockLabelPassesThroughWhenNoRunIsActive() {
        CharSequence stock = "45%";
        assertSame(stock, ProgressTrace.label(textView(), stock));
        assertSame(stock, ProgressTrace.label(null, stock));
        assertNull(ProgressTrace.label(null, null));
    }

    @Test
    public void labelNamesTheStageBeforeThePercentage() {
        ProgressTrace.beginUpload("Enhance");
        assertEquals("Uploading… 45%", ProgressTrace.label(textView(), "45%").toString());
        ProgressTrace.predict();
        assertEquals("Processing… 45%", ProgressTrace.label(textView(), "45%").toString());
        ProgressTrace.polling();
        assertEquals("Waiting for result… 45%", ProgressTrace.label(textView(), "45%").toString());
        ProgressTrace.finished(DELIVERED);
        assertEquals("Finishing… 45%", ProgressTrace.label(textView(), "45%").toString());
    }

    @Test
    public void labelKeepsEverythingFromTheFirstDigit() {
        ProgressTrace.beginUpload("Enhance");
        assertEquals("Uploading… 45%", ProgressTrace.label(textView(), "Processing 45%").toString());
        assertEquals("Uploading… 7", ProgressTrace.label(textView(), "AI Remove 7").toString());
        assertEquals("Uploading… 100%", ProgressTrace.label(textView(), "⏳ 100%").toString());
        assertEquals("Uploading… 2 of 5: 45%", ProgressTrace.label(textView(), "Step 2 of 5: 45%").toString());
        assertEquals("Uploading… 45%", ProgressTrace.label(textView(), new StringBuilder("Working 45%")).toString());
    }

    @Test
    public void labelWithoutADigitIsJustTheStageName() {
        ProgressTrace.beginUpload("Enhance");
        assertEquals("Uploading…", ProgressTrace.label(textView(), "").toString());
        assertEquals("Uploading…", ProgressTrace.label(textView(), "Processing").toString());
        assertEquals("Uploading…", ProgressTrace.label(textView(), "%").toString());
    }

    @Test
    public void labelOfANullStockStaysNull() {
        // given
        ProgressTrace.beginUpload("Enhance");

        // when
        CharSequence label = ProgressTrace.label(textView(), null);

        // then
        assertNull(label);
    }

    @Test
    public void largeTextPutsTheStageNameOnItsOwnShrunkenLine() {
        // given
        ProgressTrace.beginUpload("Enhance");
        TextView view = textView();
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 32f);

        // when
        CharSequence label = ProgressTrace.label(view, "45%");

        // then
        assertEquals("Uploading…\n45%", label.toString());
        Spanned spanned = (Spanned) label;
        RelativeSizeSpan[] spans = spanned.getSpans(0, label.length(), RelativeSizeSpan.class);
        assertEquals(1, spans.length);
        assertEquals(0.5f, spans[0].getSizeChange(), 0.001f);
        assertEquals(0, spanned.getSpanStart(spans[0]));
        assertEquals("Uploading…".length(), spanned.getSpanEnd(spans[0]));
    }

    @Test
    public void textUpToSixteenSpStaysOnOneLine() {
        ProgressTrace.beginUpload("Enhance");
        TextView view = textView();
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
        assertEquals("Uploading… 45%", ProgressTrace.label(view, "45%").toString());
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 8f);
        assertEquals("Uploading… 45%", ProgressTrace.label(view, "45%").toString());
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16.5f);
        assertEquals("Uploading…\n45%", ProgressTrace.label(view, "45%").toString());
    }

    @Test
    public void noUploadIsInProgressBeforeAnyRun() {
        assertFalse(ProgressTrace.uploading());
    }

    @Test
    public void uploadingHoldsOnlyUntilTheFirstStageEnds() {
        ProgressTrace.beginUpload("Enhance");
        assertTrue(ProgressTrace.uploading());
        ProgressTrace.predict();
        assertFalse(ProgressTrace.uploading());
        ProgressTrace.polling();
        assertFalse(ProgressTrace.uploading());
    }

    @Test
    public void headlineSaysNoRunYetBeforeAnyRun() {
        assertEquals("No AI run yet this session", ProgressTrace.headline());
        assertTrue(ProgressTrace.stages().isEmpty());
    }

    @Test
    public void runningStageIsNotListedUntilItEnds() {
        ProgressTrace.beginUpload("Enhance");
        advance(500L);
        assertTrue(ProgressTrace.stages().isEmpty());
        assertEquals("No AI run yet this session", ProgressTrace.headline());
    }

    @Test
    public void stagesListTheRunInOrderWithTheirDurations() {
        // given
        ProgressTrace.beginUpload("Enhance");
        advance(1200L);
        ProgressTrace.predict();
        advance(800L);
        ProgressTrace.polling();
        advance(2000L);
        ProgressTrace.polling();
        advance(1000L);
        ProgressTrace.finished(DELIVERED);

        // when
        List<String> stages = ProgressTrace.stages();

        // then
        assertEquals(Arrays.asList("Uploading 1.20s", "Processing 0.80s", "Waiting for result 3.00s",
                "server result 5.00s (2 polls) code=0x400"), stages);
    }

    @Test
    public void headlineIsTheNewestStageLine() {
        ProgressTrace.beginUpload("Enhance");
        advance(1200L);
        ProgressTrace.predict();
        assertEquals("Uploading 1.20s", ProgressTrace.headline());
        advance(800L);
        ProgressTrace.polling();
        assertEquals("Processing 0.80s", ProgressTrace.headline());
        advance(2000L);
        ProgressTrace.finished(DELIVERED);
        assertEquals("server result 4.00s (1 polls) code=0x400", ProgressTrace.headline());
    }

    @Test
    public void resultCodeIsReportedInHex() {
        // given
        ProgressTrace.beginUpload("Enhance");
        advance(1000L);
        ProgressTrace.finished(0x1f4);

        // when
        String headline = ProgressTrace.headline();

        // then
        assertEquals("server result 1.00s (0 polls) code=0x1f4", headline);
    }

    @Test
    public void beginningAgainForgetsThePreviousRun() {
        // given
        ProgressTrace.beginUpload("Enhance");
        advance(1000L);
        ProgressTrace.predict();
        ProgressTrace.finished(DELIVERED);
        assertFalse(ProgressTrace.stages().isEmpty());

        // when
        ProgressTrace.beginUpload("Sketch");

        // then
        assertTrue(ProgressTrace.stages().isEmpty());
        assertTrue(ProgressTrace.uploading());
        assertEquals("No AI run yet this session", ProgressTrace.headline());
    }

    @Test
    public void stagesListIsASnapshot() {
        ProgressTrace.beginUpload("Enhance");
        advance(1200L);
        ProgressTrace.predict();
        List<String> snapshot = ProgressTrace.stages();
        snapshot.clear();
        assertEquals(1, ProgressTrace.stages().size());
        List<String> before = ProgressTrace.stages();
        advance(800L);
        ProgressTrace.polling();
        assertEquals(1, before.size());
        assertEquals(2, ProgressTrace.stages().size());
    }

    @Test
    public void finishedWithoutARunIsIgnored() {
        // when
        ProgressTrace.finished(DELIVERED);

        // then
        assertTrue(ProgressTrace.stages().isEmpty());
        assertEquals(9000L, RunHistory.expected("Enhance"));
        assertEquals("", PatchSettings.RUN_HISTORY.get());
    }

    @Test
    public void aRunSettlesOnlyOnce() {
        // given
        ProgressTrace.beginUpload("Enhance");
        advance(3000L);
        ProgressTrace.finished(DELIVERED);
        List<String> settled = ProgressTrace.stages();
        advance(5000L);

        // when
        ProgressTrace.finished(DELIVERED);

        // then
        assertEquals(settled, ProgressTrace.stages());
        assertEquals("Enhance=3000", PatchSettings.RUN_HISTORY.get());
    }

    @Test
    public void aDeliveredRunFeedsTheRunHistory() {
        // given
        ProgressTrace.beginUpload("Enhance");
        advance(3000L);

        // when
        ProgressTrace.finished(DELIVERED);

        // then
        assertEquals("Enhance=3000", PatchSettings.RUN_HISTORY.get());
        assertEquals(3000L, RunHistory.expected("Enhance"));
    }

    @Test
    public void aFailedRunLeavesTheRunHistoryAlone() {
        // given
        ProgressTrace.beginUpload("Enhance");
        advance(3000L);

        // when
        ProgressTrace.finished(0x1f4);

        // then
        assertEquals("", PatchSettings.RUN_HISTORY.get());
        assertEquals(9000L, RunHistory.expected("Enhance"));
    }

    @Test
    public void logsTheRunStageByStage() {
        // given
        PatchSettings.LOG_ENDPOINTS.save(true);
        ProgressTrace.beginUpload("Enhance");
        advance(1200L);
        ProgressTrace.predict();
        advance(800L);
        ProgressTrace.polling();
        advance(500L);
        ProgressTrace.polling();
        advance(500L);
        ProgressTrace.polling();
        advance(1000L);
        ProgressTrace.finished(DELIVERED);

        // when
        List<String> log = progressLog();

        // then
        assertEquals(Arrays.asList("=== Enhance started ===", "-> Uploading          (t=0.00s)", "Uploading 1.20s",
                "-> Processing         (t=1.20s)", "Processing 0.80s", "-> Waiting for result (t=2.00s)",
                "poll #2  +0.50s", "poll #3  +1.00s", "Waiting for result 2.00s", "-> Finishing          (t=4.00s)",
                "=== server result in 4.00s, 3 polls, code=0x400 ==="), log);
    }

    @Test
    public void logsNothingWhileEndpointLoggingIsOff() {
        // given
        ProgressTrace.beginUpload("Enhance");
        advance(1200L);
        ProgressTrace.predict();
        ProgressTrace.polling();
        ProgressTrace.finished(DELIVERED);

        // when
        List<String> log = progressLog();

        // then
        assertTrue(log.isEmpty());
    }

}
