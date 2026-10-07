/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.photoeditorpro;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Field;

import app.morphe.extension.shared.Utils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class RunHistoryTest {

    private static final long FALLBACK_MS = 9000L;

    @Before
    public void startWithoutHistory() throws ReflectiveOperationException {
        Utils.setContext(RuntimeEnvironment.getApplication());
        reload("");
    }

    @After
    public void leaveNoHistory() throws ReflectiveOperationException {
        reload("");
    }

    private static void reload(String persisted) throws ReflectiveOperationException {
        PatchSettings.RUN_HISTORY.save(persisted);
        Field cache = RunHistory.class.getDeclaredField("cache");
        cache.setAccessible(true);
        cache.set(null, null);
    }

    private static String persisted() {
        return PatchSettings.RUN_HISTORY.get();
    }

    private static void addAll(String feature, long... runs) {
        for (long run : runs) {
            RunHistory.add(feature, run);
        }
    }

    private static String loadedFrom(String raw) throws ReflectiveOperationException {
        reload(raw);
        RunHistory.add("Probe", 1L);
        return persisted();
    }

    @Test
    public void expectedFallsBackWithoutHistory() {
        assertEquals(FALLBACK_MS, RunHistory.expected("Enhance"));
    }

    @Test
    public void expectedFallsBackForAFeatureWithoutRuns() {
        RunHistory.add("Sketch", 4000L);
        assertEquals(FALLBACK_MS, RunHistory.expected("Enhance"));
        assertEquals(FALLBACK_MS, RunHistory.expected(null));
    }

    @Test
    public void rejectsAMissingFeatureOrANonPositiveDuration() {
        RunHistory.add(null, 5000L);
        RunHistory.add("Enhance", 0L);
        RunHistory.add("Enhance", -1L);
        RunHistory.add("Enhance", Long.MIN_VALUE);
        assertEquals("", persisted());
        assertEquals(FALLBACK_MS, RunHistory.expected("Enhance"));
    }

    @Test
    public void rejectsDurationsOverFiveMinutes() {
        RunHistory.add("Enhance", 300_001L);
        RunHistory.add("Enhance", Long.MAX_VALUE);
        assertEquals("", persisted());
        assertEquals(FALLBACK_MS, RunHistory.expected("Enhance"));
    }

    @Test
    public void acceptsDurationsInsideTheRange() {
        RunHistory.add("Enhance", 1L);
        assertEquals(1L, RunHistory.expected("Enhance"));
        RunHistory.add("Sketch", 300_000L);
        assertEquals(300_000L, RunHistory.expected("Sketch"));
        assertEquals("Enhance=1;Sketch=300000", persisted());
    }

    @Test
    public void addStartsAListPerFeature() {
        RunHistory.add("Enhance", 4000L);
        RunHistory.add("Sketch", 6000L);
        RunHistory.add("Enhance", 5000L);
        assertEquals("Enhance=4000,5000;Sketch=6000", persisted());
    }

    @Test
    public void addKeepsTheFiveNewestRunsInOrder() {
        // when
        for (long run = 1; run <= 8; run++) {
            RunHistory.add("Enhance", run * 1000L);
        }

        // then
        assertEquals("Enhance=4000,5000,6000,7000,8000", persisted());
    }

    @Test
    public void addTrimsOnlyTheFeatureThatOverflowed() {
        // given
        RunHistory.add("Sketch", 100L);

        // when
        for (long run = 1; run <= 6; run++) {
            RunHistory.add("Enhance", run);
        }

        // then
        assertEquals("Sketch=100;Enhance=2,3,4,5,6", persisted());
    }

    @Test
    public void addTrimsAnOverlongPersistedHistory() throws ReflectiveOperationException {
        // given
        reload("Enhance=1,2,3,4,5,6,7");

        // when
        RunHistory.add("Enhance", 8L);

        // then
        assertEquals("Enhance=4,5,6,7,8", persisted());
    }

    @Test
    public void expectedOfOneRunIsThatRun() {
        // given
        RunHistory.add("Enhance", 4321L);

        // when
        long expected = RunHistory.expected("Enhance");

        // then
        assertEquals(4321L, expected);
    }

    @Test
    public void expectedIsTheMiddleRunOfAnOddCount() {
        addAll("Enhance", 9000L, 1000L, 5000L);
        assertEquals(5000L, RunHistory.expected("Enhance"));
        addAll("Sketch", 1000L, 2000L, 3000L, 4000L, 90_000L);
        assertEquals(3000L, RunHistory.expected("Sketch"));
    }

    @Test
    public void expectedOfAnEvenCountIsTheUpperMiddleRun() {
        addAll("Enhance", 100L, 200L);
        assertEquals(200L, RunHistory.expected("Enhance"));
        addAll("Sketch", 4000L, 1000L, 3000L, 2000L);
        assertEquals(3000L, RunHistory.expected("Sketch"));
    }

    @Test
    public void expectedIgnoresOutliersAroundRepeatedRuns() {
        // given
        addAll("Enhance", 4000L, 4000L, 4000L, 1L, 299_999L);

        // when
        long expected = RunHistory.expected("Enhance");

        // then
        assertEquals(4000L, expected);
    }

    @Test
    public void expectedLeavesTheHistoryOrderUntouched() {
        addAll("Enhance", 5000L, 1000L, 3000L);
        assertEquals(3000L, RunHistory.expected("Enhance"));
        RunHistory.add("Enhance", 2000L);
        assertEquals("Enhance=5000,1000,3000,2000", persisted());
    }

    @Test
    public void expectedReadsHistoryPersistedByAnEarlierSession() throws ReflectiveOperationException {
        // given
        addAll("Enhance", 4000L, 5000L, 6000L);
        String saved = persisted();
        reload(saved);

        // when
        long expected = RunHistory.expected("Enhance");

        // then
        assertEquals(5000L, expected);
    }

    @Test
    public void loadsFeaturesInPersistedOrder() throws ReflectiveOperationException {
        assertEquals("Enhance=4000,5000;Sketch=6000;Probe=1", loadedFrom("Enhance=4000,5000;Sketch=6000"));
    }

    @Test
    public void loadsNothingFromEmptyHistory() throws ReflectiveOperationException {
        assertEquals("Probe=1", loadedFrom(""));
    }

    @Test
    public void loadSkipsBlocksWithoutAFeatureName() throws ReflectiveOperationException {
        assertEquals("Enhance=4000;Probe=1", loadedFrom("=5000;noequals;Enhance=4000;;"));
    }

    @Test
    public void loadKeepsGoodRunsAroundCorruptOnes() throws ReflectiveOperationException {
        assertEquals("Enhance=4000,5000;Sketch=6000;Probe=1", loadedFrom("Enhance=4000,abc,,5000,12x;Sketch=6000"));
    }

    @Test
    public void loadDropsFeaturesWhoseRunsAreAllCorrupt() throws ReflectiveOperationException {
        assertEquals("Cutout=7000;Probe=1", loadedFrom("Enhance=abc,def;Sketch=;Cutout=7000"));
    }

    @Test
    public void loadRejectsNumbersTooLargeForALong() throws ReflectiveOperationException {
        assertEquals("Enhance=4000;Probe=1", loadedFrom("Enhance=99999999999999999999,4000"));
    }

    @Test
    public void loadTrimsWhitespaceAroundRuns() throws ReflectiveOperationException {
        assertEquals("Enhance=4000,5000;Probe=1", loadedFrom("Enhance= 4000 , 5000 "));
    }

    @Test
    public void savedHistoryLoadsBackToTheSameRuns() throws ReflectiveOperationException {
        addAll("AI Remove", 4200L, 5100L);
        RunHistory.add("Cutout", 3000L);
        String saved = persisted();
        assertEquals("AI Remove=4200,5100;Cutout=3000", saved);
        assertEquals(saved + ";Probe=1", loadedFrom(saved));
    }

}
