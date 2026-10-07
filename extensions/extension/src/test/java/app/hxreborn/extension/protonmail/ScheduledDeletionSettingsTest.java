/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.protonmail;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;

import app.hxreborn.extension.proton.PatchSettings;
import app.hxreborn.extension.proton.PatchedBuild;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
public final class ScheduledDeletionSettingsTest {

    private static final long HOUR_MS = 3_600_000L;

    private static final long MARGIN_MS = 10_000L;

    private static final int HOUR_SECONDS = 3600;

    private static final String TRASH = ScheduledDeletion.TRASH;

    private static final String SPAM = ScheduledDeletion.SPAM;

    private Context context;

    private SharedPreferences preferences;

    @Before
    public void createEmptyPreferences() {
        this.context = PatchedBuild.useApplicationContext();
        this.preferences = this.context.getSharedPreferences(PatchSettings.PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    private void stamp(String label, String account, long millis) {
        this.preferences.edit().putLong("last_emptied_ms_" + label + "_" + account, millis).apply();
    }

    private Object stamped(String label, String account) {
        return this.preferences.getAll().get("last_emptied_ms_" + label + "_" + account);
    }

    private void assertStampedBetween(String label, String account, long earliest, long latest) {
        final Object stamp = stamped(label, account);
        assertTrue(label + " " + account + " " + stamp, stamp instanceof Long);
        assertTrue(label + " " + account + " " + stamp, (Long) stamp >= earliest && (Long) stamp <= latest);
    }

    @Test
    public void usesTheSharedPreferencesFile() {
        ScheduledDeletionSettings.saveIntervalSeconds(this.context, TRASH, HOUR_SECONDS);
        ScheduledDeletionSettings.saveShowsToast(this.context, false);

        final SharedPreferences named = this.context.getSharedPreferences("hx_protonmail_patches",
                Context.MODE_PRIVATE);
        assertEquals(HOUR_SECONDS, named.getInt("interval_seconds_TRASH", -1));
        assertFalse(named.getBoolean("show_toast", true));
    }

    @Test
    public void intervalIsOffUntilSaved() {
        assertEquals(ScheduledDeletionSettings.OFF, ScheduledDeletionSettings.intervalSeconds(this.context, TRASH));
        assertEquals(ScheduledDeletionSettings.OFF, ScheduledDeletionSettings.intervalSeconds(null, TRASH));
    }

    @Test
    public void intervalsAreKeptPerLabel() {
        // when
        final boolean saved = ScheduledDeletionSettings.saveIntervalSeconds(this.context, TRASH, 7200);

        // then
        assertTrue(saved);
        assertEquals(7200, ScheduledDeletionSettings.intervalSeconds(this.context, TRASH));
        assertEquals(ScheduledDeletionSettings.OFF, ScheduledDeletionSettings.intervalSeconds(this.context, SPAM));
        assertEquals(7200, this.preferences.getAll().get("interval_seconds_TRASH"));
    }

    @Test
    public void legacySharedIntervalApplies() {
        this.preferences.edit().putInt("interval_seconds", HOUR_SECONDS).apply();
        assertEquals(HOUR_SECONDS, ScheduledDeletionSettings.intervalSeconds(this.context, TRASH));
        assertEquals(HOUR_SECONDS, ScheduledDeletionSettings.intervalSeconds(this.context, SPAM));
    }

    @Test
    public void perLabelIntervalOverridesTheLegacyOne() {
        // given
        this.preferences.edit().putInt("interval_seconds", HOUR_SECONDS).apply();

        // when
        ScheduledDeletionSettings.saveIntervalSeconds(this.context, TRASH, 7200);

        // then
        assertEquals(7200, ScheduledDeletionSettings.intervalSeconds(this.context, TRASH));
        assertEquals(HOUR_SECONDS, ScheduledDeletionSettings.intervalSeconds(this.context, SPAM));
    }

    @Test
    public void perLabelOffOverridesTheLegacyInterval() {
        // given
        this.preferences.edit().putInt("interval_seconds", HOUR_SECONDS).apply();

        // when
        ScheduledDeletionSettings.saveIntervalSeconds(this.context, TRASH, ScheduledDeletionSettings.OFF);

        // then
        assertEquals(ScheduledDeletionSettings.OFF, ScheduledDeletionSettings.intervalSeconds(this.context, TRASH));
        assertEquals(HOUR_SECONDS, ScheduledDeletionSettings.intervalSeconds(this.context, SPAM));
    }

    @Test
    public void storedIntervalsAreClampedWhenRead() {
        final int[][] storedAndRead = { { Integer.MIN_VALUE, 0 }, { -60, 0 }, { 0, 0 }, { 1, 60 }, { 30, 60 },
                { 59, 60 }, { 60, 60 }, { 61, 61 }, { 31_536_000, 31_536_000 }, { 31_536_001, 31_536_000 },
                { Integer.MAX_VALUE, 31_536_000 } };
        for (int[] pair : storedAndRead) {
            this.preferences.edit().putInt("interval_seconds_TRASH", pair[0]).apply();
            assertEquals("stored " + pair[0], pair[1], ScheduledDeletionSettings.intervalSeconds(this.context, TRASH));
        }
    }

    @Test
    public void saveAcceptsOffOrTheInclusiveBounds() {
        for (int seconds : new int[] { 0, 60, 61, HOUR_SECONDS, 31_536_000 }) {
            assertTrue("seconds " + seconds,
                    ScheduledDeletionSettings.saveIntervalSeconds(this.context, TRASH, seconds));
            assertEquals(seconds, ScheduledDeletionSettings.intervalSeconds(this.context, TRASH));
        }
    }

    @Test
    public void saveRejectsValuesOutsideTheBoundsWithoutWriting() {
        for (int seconds : new int[] { Integer.MIN_VALUE, -1, 1, 30, 59, 31_536_001, Integer.MAX_VALUE }) {
            assertFalse("seconds " + seconds,
                    ScheduledDeletionSettings.saveIntervalSeconds(this.context, TRASH, seconds));
        }
        assertTrue(this.preferences.getAll().isEmpty());
    }

    @Test
    public void saveFailsWithoutAContext() {
        assertFalse(ScheduledDeletionSettings.saveIntervalSeconds(null, TRASH, HOUR_SECONDS));
    }

    @Test
    public void changingTheIntervalRestartsEveryAccountsCountdownForThatLabel() {
        // given
        ScheduledDeletionSettings.saveIntervalSeconds(this.context, TRASH, HOUR_SECONDS);
        final long spamStamp = System.currentTimeMillis() - 5 * HOUR_MS;
        stamp(TRASH, "a", System.currentTimeMillis() - 5 * HOUR_MS);
        stamp(TRASH, "ab", System.currentTimeMillis() - 9 * HOUR_MS);
        stamp(SPAM, "a", spamStamp);
        final long before = System.currentTimeMillis();

        // when
        final boolean saved = ScheduledDeletionSettings.saveIntervalSeconds(this.context, TRASH, 7200);

        // then
        final long after = System.currentTimeMillis();
        assertTrue(saved);
        assertStampedBetween(TRASH, "a", before, after);
        assertStampedBetween(TRASH, "ab", before, after);
        assertEquals(spamStamp, stamped(SPAM, "a"));
    }

    @Test
    public void changingTheIntervalCreatesNoCountdownForUnseenAccounts() {
        // given
        stamp(TRASH, "a", System.currentTimeMillis() - HOUR_MS);

        // when
        ScheduledDeletionSettings.saveIntervalSeconds(this.context, TRASH, HOUR_SECONDS);

        // then
        assertNull(stamped(TRASH, "b"));
        assertNull(stamped(SPAM, "a"));
    }

    @Test
    public void savingTheSameIntervalKeepsTheCountdown() {
        // given
        ScheduledDeletionSettings.saveIntervalSeconds(this.context, TRASH, HOUR_SECONDS);
        final long stamp = System.currentTimeMillis() - 2 * HOUR_MS;
        stamp(TRASH, "a", stamp);

        // when
        final boolean saved = ScheduledDeletionSettings.saveIntervalSeconds(this.context, TRASH, HOUR_SECONDS);

        // then
        assertTrue(saved);
        assertEquals(stamp, stamped(TRASH, "a"));
    }

    @Test
    public void savingOffWhileAlreadyOffKeepsTheCountdown() {
        // given
        final long stamp = System.currentTimeMillis() - 2 * HOUR_MS;
        stamp(TRASH, "a", stamp);

        // when
        final boolean saved = ScheduledDeletionSettings.saveIntervalSeconds(this.context, TRASH,
                ScheduledDeletionSettings.OFF);

        // then
        assertTrue(saved);
        assertEquals(stamp, stamped(TRASH, "a"));
    }

    @Test
    public void turningTheScheduleOnRestartsTheCountdown() {
        // given
        stamp(TRASH, "a", System.currentTimeMillis() - 40 * HOUR_MS);
        final long before = System.currentTimeMillis();

        // when
        ScheduledDeletionSettings.saveIntervalSeconds(this.context, TRASH, HOUR_SECONDS);

        // then
        final long after = System.currentTimeMillis();
        assertStampedBetween(TRASH, "a", before, after);
        assertFalse(ScheduledDeletionSettings.due(this.context, "a", TRASH, HOUR_MS));
    }

    @Test
    public void savingTheLegacyIntervalUnderTheLabelKeepsTheCountdown() {
        this.preferences.edit().putInt("interval_seconds", HOUR_SECONDS).apply();
        final long stamp = System.currentTimeMillis() - 2 * HOUR_MS;
        stamp(TRASH, "a", stamp);

        assertTrue(ScheduledDeletionSettings.saveIntervalSeconds(this.context, TRASH, HOUR_SECONDS));
        assertEquals(stamp, stamped(TRASH, "a"));

        final long before = System.currentTimeMillis();
        assertTrue(ScheduledDeletionSettings.saveIntervalSeconds(this.context, TRASH, 7200));
        assertStampedBetween(TRASH, "a", before, System.currentTimeMillis());
    }

    @Test
    public void rejectedSaveLeavesTheCountdownAlone() {
        // given
        final long stamp = System.currentTimeMillis() - 2 * HOUR_MS;
        stamp(TRASH, "a", stamp);

        // when
        final boolean saved = ScheduledDeletionSettings.saveIntervalSeconds(this.context, TRASH, 30);

        // then
        assertFalse(saved);
        assertEquals(stamp, stamped(TRASH, "a"));
        assertNull(this.preferences.getAll().get("interval_seconds_TRASH"));
    }

    @Test
    public void firstSightOfAnAccountStartsItsCountdownInsteadOfDeleting() {
        // given
        final long before = System.currentTimeMillis();

        // when
        final boolean due = ScheduledDeletionSettings.due(this.context, "a", TRASH, HOUR_MS);

        // then
        final long after = System.currentTimeMillis();
        assertFalse(due);
        assertStampedBetween(TRASH, "a", before, after);
        assertFalse(ScheduledDeletionSettings.due(this.context, "a", TRASH, HOUR_MS));
        assertTrue(ScheduledDeletionSettings.due(this.context, "a", TRASH, 0));
    }

    @Test
    public void nonPositiveStampCountsAsMissing() {
        for (long invalid : new long[] { 0L, -1L, Long.MIN_VALUE }) {
            stamp(TRASH, "a", invalid);

            final long before = System.currentTimeMillis();
            assertFalse("stamp " + invalid, ScheduledDeletionSettings.due(this.context, "a", TRASH, 1));
            assertStampedBetween(TRASH, "a", before, System.currentTimeMillis());
        }
    }

    @Test
    public void stampInTheFutureIsReplacedByNow() {
        // given
        stamp(TRASH, "a", System.currentTimeMillis() + 60_000L);
        final long before = System.currentTimeMillis();

        // when
        final boolean due = ScheduledDeletionSettings.due(this.context, "a", TRASH, HOUR_MS);

        // then
        assertFalse(due);
        assertStampedBetween(TRASH, "a", before, System.currentTimeMillis());
    }

    @Test
    public void stampEqualToNowIsNotInTheFuture() {
        final long stamp = System.currentTimeMillis();
        stamp(TRASH, "a", stamp);

        assertFalse(ScheduledDeletionSettings.due(this.context, "a", TRASH, HOUR_MS));
        assertTrue(ScheduledDeletionSettings.due(this.context, "a", TRASH, 0));
        assertEquals(stamp, stamped(TRASH, "a"));
    }

    @Test
    public void dueFlipsOnceTheIntervalHasElapsed() {
        final long now = System.currentTimeMillis();

        stamp(TRASH, "a", now - HOUR_MS + MARGIN_MS);
        assertFalse(ScheduledDeletionSettings.due(this.context, "a", TRASH, HOUR_MS));

        stamp(TRASH, "a", now - HOUR_MS - MARGIN_MS);
        assertTrue(ScheduledDeletionSettings.due(this.context, "a", TRASH, HOUR_MS));

        stamp(TRASH, "a", now - 1000 * HOUR_MS);
        assertTrue(ScheduledDeletionSettings.due(this.context, "a", TRASH, HOUR_MS));
    }

    @Test
    public void dueDoesNotRewriteTheStoredTimestamp() {
        final long stamp = System.currentTimeMillis() - 2 * HOUR_MS;
        stamp(TRASH, "a", stamp);

        assertTrue(ScheduledDeletionSettings.due(this.context, "a", TRASH, HOUR_MS));
        assertTrue(ScheduledDeletionSettings.due(this.context, "a", TRASH, HOUR_MS));

        assertEquals(stamp, stamped(TRASH, "a"));
    }

    @Test
    public void dueIgnoresMissingAccountOrContext() {
        assertFalse(ScheduledDeletionSettings.due(this.context, null, TRASH, HOUR_MS));
        assertFalse(ScheduledDeletionSettings.due(null, "a", TRASH, HOUR_MS));
        assertTrue(this.preferences.getAll().isEmpty());
    }

    @Test
    public void countdownsAreKeptPerAccountLabelPair() {
        stamp(TRASH, "a", System.currentTimeMillis() - 2 * HOUR_MS);

        final long before = System.currentTimeMillis();
        assertTrue(ScheduledDeletionSettings.due(this.context, "a", TRASH, HOUR_MS));
        assertFalse(ScheduledDeletionSettings.due(this.context, "b", TRASH, HOUR_MS));
        assertFalse(ScheduledDeletionSettings.due(this.context, "a", SPAM, HOUR_MS));
        final long after = System.currentTimeMillis();

        assertStampedBetween(TRASH, "b", before, after);
        assertStampedBetween(SPAM, "a", before, after);
    }

    @Test
    public void recordEmptiedRestartsTheCountdown() {
        // given
        stamp(TRASH, "a", System.currentTimeMillis() - 5 * HOUR_MS);
        final long before = System.currentTimeMillis();

        // when
        ScheduledDeletionSettings.recordEmptied(this.context, "a", TRASH);

        // then
        final long after = System.currentTimeMillis();
        assertStampedBetween(TRASH, "a", before, after);
        assertFalse(ScheduledDeletionSettings.due(this.context, "a", TRASH, HOUR_MS));
    }

    @Test
    public void recordEmptiedIgnoresMissingAccountOrContext() {
        ScheduledDeletionSettings.recordEmptied(this.context, null, TRASH);
        ScheduledDeletionSettings.recordEmptied(null, "a", TRASH);

        assertTrue(this.preferences.getAll().isEmpty());
    }

    @Test
    public void nothingIsDueWhileEveryLabelIsOff() {
        // given
        stamp(TRASH, "a", 1);
        stamp(SPAM, "a", 1);

        // when
        final boolean due = ScheduledDeletionSettings.anyLabelDue(this.context, "a");

        // then
        assertFalse(due);
        assertEquals(1L, stamped(TRASH, "a"));
        assertEquals(1L, stamped(SPAM, "a"));
    }

    @Test
    public void anyLabelDueFollowsTheLabelsThatAreOn() {
        ScheduledDeletionSettings.saveIntervalSeconds(this.context, TRASH, HOUR_SECONDS);
        ScheduledDeletionSettings.saveIntervalSeconds(this.context, SPAM, 2 * HOUR_SECONDS);
        final long now = System.currentTimeMillis();

        stamp(TRASH, "a", now - HOUR_MS + MARGIN_MS);
        stamp(SPAM, "a", now - 2 * HOUR_MS + MARGIN_MS);
        assertFalse(ScheduledDeletionSettings.anyLabelDue(this.context, "a"));

        stamp(SPAM, "a", now - 2 * HOUR_MS - MARGIN_MS);
        assertTrue(ScheduledDeletionSettings.anyLabelDue(this.context, "a"));

        stamp(SPAM, "a", now - 2 * HOUR_MS + MARGIN_MS);
        stamp(TRASH, "a", now - HOUR_MS - MARGIN_MS);
        assertTrue(ScheduledDeletionSettings.anyLabelDue(this.context, "a"));
    }

    @Test
    public void anOverdueLabelThatIsOffDoesNotCount() {
        // given
        ScheduledDeletionSettings.saveIntervalSeconds(this.context, SPAM, HOUR_SECONDS);
        final long now = System.currentTimeMillis();
        stamp(TRASH, "a", now - 100 * HOUR_MS);
        stamp(SPAM, "a", now - 1000);

        // when
        final boolean due = ScheduledDeletionSettings.anyLabelDue(this.context, "a");

        // then
        assertFalse(due);
    }

    @Test
    public void anyLabelDueStartsTheCountdownOfEnabledLabelsOnFirstSight() {
        ScheduledDeletionSettings.saveIntervalSeconds(this.context, TRASH, HOUR_SECONDS);

        final long before = System.currentTimeMillis();
        assertFalse(ScheduledDeletionSettings.anyLabelDue(this.context, "a"));
        final long after = System.currentTimeMillis();

        assertStampedBetween(TRASH, "a", before, after);
        assertNull(stamped(SPAM, "a"));

        stamp(TRASH, "a", after - HOUR_MS - MARGIN_MS);
        assertTrue(ScheduledDeletionSettings.anyLabelDue(this.context, "a"));
    }

    @Test
    public void anyLabelDueNeedsAnAccount() {
        // given
        ScheduledDeletionSettings.saveIntervalSeconds(this.context, TRASH, HOUR_SECONDS);
        stamp(TRASH, "a", System.currentTimeMillis() - 1000 * HOUR_MS);

        // when
        final boolean due = ScheduledDeletionSettings.anyLabelDue(this.context, null);

        // then
        assertFalse(due);
    }

    @Test
    public void maximumIntervalDoesNotOverflowMilliseconds() {
        ScheduledDeletionSettings.saveIntervalSeconds(this.context, TRASH, ScheduledDeletionSettings.MAXIMUM_SECONDS);
        final long intervalMs = 31_536_000_000L;
        final long now = System.currentTimeMillis();

        stamp(TRASH, "a", now - intervalMs + MARGIN_MS);
        assertFalse(ScheduledDeletionSettings.anyLabelDue(this.context, "a"));

        stamp(TRASH, "a", now - intervalMs - MARGIN_MS);
        assertTrue(ScheduledDeletionSettings.anyLabelDue(this.context, "a"));
    }

    @Test
    public void minimumIntervalIsOneMinute() {
        ScheduledDeletionSettings.saveIntervalSeconds(this.context, TRASH, ScheduledDeletionSettings.MINIMUM_SECONDS);
        final long now = System.currentTimeMillis();

        stamp(TRASH, "a", now - 60_000L + MARGIN_MS);
        assertFalse(ScheduledDeletionSettings.anyLabelDue(this.context, "a"));

        stamp(TRASH, "a", now - 60_000L - MARGIN_MS);
        assertTrue(ScheduledDeletionSettings.anyLabelDue(this.context, "a"));
    }

    @Test
    public void toastIsShownUnlessTurnedOff() {
        assertTrue(ScheduledDeletionSettings.showsToast(this.context));
        assertTrue(ScheduledDeletionSettings.showsToast(null));

        ScheduledDeletionSettings.saveShowsToast(this.context, false);
        assertFalse(ScheduledDeletionSettings.showsToast(this.context));

        ScheduledDeletionSettings.saveShowsToast(this.context, true);
        assertTrue(ScheduledDeletionSettings.showsToast(this.context));
        ScheduledDeletionSettings.saveShowsToast(null, false);
        assertTrue(ScheduledDeletionSettings.showsToast(this.context));
    }

    @Test
    public void scheduleIsInactiveWhileThePatchIsNotApplied() {
        ScheduledDeletionSettings.saveIntervalSeconds(this.context, TRASH, HOUR_SECONDS);

        assertFalse(ScheduledDeletionSettings.isActive(this.context, TRASH));
        assertFalse(ScheduledDeletionSettings.isActive(this.context));
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void scheduleIsInactiveWhileEveryLabelIsOff() {
        assertFalse(ScheduledDeletionSettings.isActive(this.context, TRASH));
        assertFalse(ScheduledDeletionSettings.isActive(this.context, SPAM));
        assertFalse(ScheduledDeletionSettings.isActive(this.context));
        assertFalse(ScheduledDeletionSettings.isActive(null, TRASH));
        assertFalse(ScheduledDeletionSettings.isActive(null));
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void scheduleIsActiveForTheLabelsThatAreOn() {
        ScheduledDeletionSettings.saveIntervalSeconds(this.context, TRASH, HOUR_SECONDS);

        assertTrue(ScheduledDeletionSettings.isActive(this.context, TRASH));
        assertFalse(ScheduledDeletionSettings.isActive(this.context, SPAM));
        assertTrue(ScheduledDeletionSettings.isActive(this.context));

        ScheduledDeletionSettings.saveIntervalSeconds(this.context, TRASH, ScheduledDeletionSettings.OFF);
        assertFalse(ScheduledDeletionSettings.isActive(this.context));
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void legacyIntervalActivatesEveryLabel() {
        this.preferences.edit().putInt("interval_seconds", HOUR_SECONDS).apply();

        assertTrue(ScheduledDeletionSettings.isActive(this.context, TRASH));
        assertTrue(ScheduledDeletionSettings.isActive(this.context, SPAM));
    }

}
