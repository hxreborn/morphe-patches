/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.protonmail;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.content.SharedPreferences;

import app.hxreborn.extension.proton.PatchSettings;
import app.hxreborn.extension.proton.PatchedBuild;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLog;

@RunWith(RobolectricTestRunner.class)
public final class ScheduledDeletionTest {

    private static final String TAG = "ScheduledDeletion";

    private static final long HOUR_MS = 3_600_000L;

    public interface Continuation {

        Object getContext();

        void resumeWith(Object result);

        Object unrelated();

    }

    enum Marker {

        COROUTINE_SUSPENDED, OTHER

    }

    static final class Failure {

        final Throwable exception;

        Failure(Throwable exception) {
            this.exception = exception;
        }

    }

    static final class Ok {

        private final Object value;

        Ok(Object value) {
            this.value = value;
        }

        public Object getV1() {
            return this.value;
        }

    }

    static final class Broken {

        static final class Ok {

        }

    }

    static final class Mailbox {

        public Object labelId() {
            return "label";
        }

    }

    static final class Session {

        private final Object userId;

        Session(Object userId) {
            this.userId = userId;
        }

        public Object userId() {
            return this.userId;
        }

    }

    private Context context;

    private SharedPreferences preferences;

    @Before
    public void useApplicationContext() {
        this.context = PatchedBuild.useApplicationContext();
        this.preferences = this.context.getSharedPreferences(PatchSettings.PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    private void scheduleTrash(int seconds) {
        ScheduledDeletionSettings.saveIntervalSeconds(this.context, ScheduledDeletion.TRASH, seconds);
    }

    private Object stamped(String label, String account) {
        return this.preferences.getAll().get("last_emptied_ms_" + label + "_" + account);
    }

    private static void showMailbox(Object mailbox, Object session) throws InterruptedException {
        ScheduledDeletion.captureMailbox(new Ok(mailbox), session);
        ScheduledDeletion.onMailboxShown(mailbox);
        joinBackgroundThreads();
    }

    private static void joinBackgroundThreads() throws InterruptedException {
        for (Thread thread : Thread.getAllStackTraces().keySet()) {
            if (thread.getName().equals("hx-scheduled-deletion") || thread.getName().equals("hx-label-ids")) {
                thread.join(10_000);
                assertFalse(thread.getName(), thread.isAlive());
            }
        }
    }

    private static ShadowLog.LogItem logged(String message) {
        for (ShadowLog.LogItem item : ShadowLog.getLogsForTag(TAG)) {
            if (message.equals(item.msg)) {
                return item;
            }
        }
        return null;
    }

    private static void assertNothingFailedExceptTheLabelIdLookup() {
        for (ShadowLog.LogItem item : ShadowLog.getLogsForTag(TAG)) {
            assertEquals(item.msg, "Failed to resolve the Trash and Spam label IDs", item.msg);
        }
    }

    private static void throwOnFailure(Object result) throws Exception {
        try {
            final Method method = ScheduledDeletion.class.getDeclaredMethod("throwOnFailure", Object.class);
            method.setAccessible(true);
            method.invoke(null, result);
        } catch (InvocationTargetException ex) {
            throw (Exception) ex.getCause();
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
    }

    private static boolean isCoroutineSuspended(Object value) throws ReflectiveOperationException {
        final Method method = ScheduledDeletion.class.getDeclaredMethod("isCoroutineSuspended", Object.class);
        method.setAccessible(true);
        return (Boolean) method.invoke(null, value);
    }

    private static Object newParkedContinuation(Object dispatcher) throws ReflectiveOperationException {
        final Class<?> type = Class.forName(ScheduledDeletion.class.getName() + "$ParkedContinuation");
        final Constructor<?> constructor = type.getDeclaredConstructor(Object.class);
        constructor.setAccessible(true);
        return constructor.newInstance(dispatcher);
    }

    private static Object parkedField(Object parked, String name) throws ReflectiveOperationException {
        final Field field = parked.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(parked);
    }

    private static Continuation continuationFor(Object parked) {
        return (Continuation) Proxy.newProxyInstance(ScheduledDeletionTest.class.getClassLoader(),
                new Class<?>[] { Continuation.class }, (InvocationHandler) parked);
    }

    @Test
    public void emptiesTrashThenSpamUsingTheMailSystemLabelNames() {
        assertArrayEquals(new String[] { "TRASH", "SPAM" }, ScheduledDeletion.EMPTIED_LABELS);
        assertEquals("TRASH", ScheduledDeletion.TRASH);
        assertEquals("SPAM", ScheduledDeletion.SPAM);
    }

    @Test
    public void noLabelIsShownBeforeAMailboxOpens() {
        assertNull(ScheduledDeletion.shownLabel());
    }

    @Test
    public void mailboxWithoutACapturedAccountIsIgnored() throws InterruptedException {
        // given
        scheduleTrash(3600);

        // when
        ScheduledDeletion.onMailboxShown(new Mailbox());

        // then
        joinBackgroundThreads();
        assertNull(ScheduledDeletion.shownLabel());
        assertTrue(ShadowLog.getLogsForTag(TAG).isEmpty());
        assertNull(stamped("TRASH", "account"));
    }

    @Test
    public void firstSightOfTheAccountStartsItsCountdownWithoutDeleting() throws InterruptedException {
        // given
        scheduleTrash(3600);
        final long before = System.currentTimeMillis();

        // when
        showMailbox(new Mailbox(), new Session(new Ok("account")));

        // then
        final long after = System.currentTimeMillis();
        final Object stamp = stamped("TRASH", "account");
        assertTrue(String.valueOf(stamp), stamp instanceof Long);
        assertTrue((Long) stamp >= before && (Long) stamp <= after);
        assertNull(stamped("SPAM", "account"));
        assertNull(logged("Failed to empty Trash and Spam"));
        assertNothingFailedExceptTheLabelIdLookup();
    }

    @Test
    public void overdueLabelStartsTheDeletionWorker() throws InterruptedException {
        // given
        scheduleTrash(3600);
        final long overdue = System.currentTimeMillis() - 2 * HOUR_MS;
        this.preferences.edit().putLong("last_emptied_ms_TRASH_account", overdue).apply();

        // when
        showMailbox(new Mailbox(), new Session(new Ok("account")));

        // then
        final ShadowLog.LogItem failure = logged("Failed to empty Trash and Spam");
        assertNotNull(failure);
        assertTrue(failure.throwable instanceof ClassNotFoundException);
        assertTrue(failure.throwable.getMessage(), failure.throwable.getMessage().contains("Mail_uniffiKt"));
        assertEquals(overdue, stamped("TRASH", "account"));
    }

    @Test
    public void labelThatIsNotDueDoesNotStartTheWorker() throws InterruptedException {
        // given
        scheduleTrash(3600);
        this.preferences.edit()
            .putLong("last_emptied_ms_TRASH_account", System.currentTimeMillis() - HOUR_MS / 2)
            .apply();

        // when
        showMailbox(new Mailbox(), new Session(new Ok("account")));

        // then
        assertNull(logged("Failed to empty Trash and Spam"));
        assertNothingFailedExceptTheLabelIdLookup();
    }

    @Test
    public void scheduleThatIsOffDoesNotStartTheWorker() throws InterruptedException {
        // given
        this.preferences.edit().putLong("last_emptied_ms_TRASH_account", 1L).apply();

        // when
        showMailbox(new Mailbox(), new Session(new Ok("account")));

        // then
        assertNull(logged("Failed to empty Trash and Spam"));
        assertEquals(1L, stamped("TRASH", "account"));
    }

    @Test
    public void workerStartsAgainAfterAFailedRun() throws InterruptedException {
        // given
        scheduleTrash(3600);
        this.preferences.edit()
            .putLong("last_emptied_ms_TRASH_account", System.currentTimeMillis() - 2 * HOUR_MS)
            .apply();
        showMailbox(new Mailbox(), new Session(new Ok("account")));
        ShadowLog.clear();

        // when
        showMailbox(new Mailbox(), new Session(new Ok("account")));

        // then
        assertNotNull(logged("Failed to empty Trash and Spam"));
    }

    @Test
    public void accountIdFromAFailedResultIsIgnored() throws InterruptedException {
        // given
        scheduleTrash(3600);

        // when
        for (Object userId : new Object[] { new Failure(new IOException("offline")), new Ok(null), null, "account" }) {
            showMailbox(new Mailbox(), new Session(userId));
        }

        // then
        assertTrue(this.preferences.getAll().keySet().toString(),
                this.preferences.getAll().keySet().stream().noneMatch((key) -> key.startsWith("last_emptied_ms_")));
        assertNull(logged("Failed to empty Trash and Spam"));
    }

    @Test
    public void accountIdOfTheWrongTypeIsReported() throws InterruptedException {
        // given
        scheduleTrash(3600);

        // when
        showMailbox(new Mailbox(), new Session(new Ok(42)));

        // then
        final ShadowLog.LogItem failure = logged("Failed to read the account ID");
        assertNotNull(failure);
        assertTrue(failure.throwable instanceof ClassCastException);
        assertNull(stamped("TRASH", "42"));
    }

    @Test
    public void sessionWithoutAnAccountGetterIsReported() throws InterruptedException {
        // given
        scheduleTrash(3600);

        // when
        showMailbox(new Mailbox(), new Object());

        // then
        final ShadowLog.LogItem failure = logged("Failed to read the account ID");
        assertNotNull(failure);
        assertTrue(failure.throwable instanceof IllegalStateException);
        assertEquals("Failed to invoke Object.userId()", failure.throwable.getMessage());
    }

    @Test
    public void mailboxWithoutALabelGetterIsReported() throws InterruptedException {
        // given
        scheduleTrash(3600);
        final Object mailbox = new Object();

        // when
        showMailbox(mailbox, new Session(new Ok("account")));

        // then
        final ShadowLog.LogItem failure = logged("Failed to start scheduled deletion");
        assertNotNull(failure);
        assertTrue(failure.throwable instanceof IllegalStateException);
        assertEquals("Failed to invoke Object.labelId()", failure.throwable.getMessage());
        assertNull(stamped("TRASH", "account"));
    }

    @Test
    public void resultsThatAreNotOkAssociateNothing() throws InterruptedException {
        // given
        scheduleTrash(3600);

        // when
        for (Object result : new Object[] { null, new Failure(new IOException()), "Ok", Marker.COROUTINE_SUSPENDED,
                new Ok(null) }) {
            ScheduledDeletion.captureMailbox(result, new Session(new Ok("account")));
            ScheduledDeletion.onMailboxShown(result);
        }

        // then
        joinBackgroundThreads();
        assertNull(stamped("TRASH", "account"));
        assertTrue(ShadowLog.getLogsForTag(TAG).isEmpty());
    }

    @Test
    public void okResultAssociatesItsValueNotTheWrapper() throws InterruptedException {
        scheduleTrash(3600);
        final Mailbox mailbox = new Mailbox();
        final Ok result = new Ok(mailbox);
        ScheduledDeletion.captureMailbox(result, new Session(new Ok("account")));

        ScheduledDeletion.onMailboxShown(result);
        joinBackgroundThreads();
        assertNull(stamped("TRASH", "account"));

        ScheduledDeletion.onMailboxShown(mailbox);
        joinBackgroundThreads();
        assertNotNull(stamped("TRASH", "account"));
    }

    @Test
    public void okResultMissingItsGetterIsReportedWithTheMemberName() {
        // when
        ScheduledDeletion.captureMailbox(new Broken.Ok(), new Session(new Ok("account")));

        // then
        final ShadowLog.LogItem failure = logged("Failed to associate mailbox with account");
        assertNotNull(failure);
        assertTrue(failure.throwable instanceof IllegalStateException);
        assertEquals("Failed to invoke Ok.getV1()", failure.throwable.getMessage());
        assertTrue(failure.throwable.getCause() instanceof NoSuchMethodException);
    }

    @Test
    public void systemLabelIdLookupFailureIsReportedWithNoLabelShown() throws InterruptedException {
        // when
        showMailbox(new Mailbox(), new Session(new Ok("account")));

        // then
        final ShadowLog.LogItem failure = logged("Failed to resolve the Trash and Spam label IDs");
        assertNotNull(failure);
        assertTrue(failure.throwable instanceof ClassNotFoundException);
        assertNull(ScheduledDeletion.shownLabel());
    }

    @Test
    public void successfulOrUnrelatedResultsDoNotThrow() throws Exception {
        throwOnFailure(null);
        throwOnFailure(new Ok("value"));
        throwOnFailure("Failure");
        throwOnFailure(Marker.COROUTINE_SUSPENDED);
    }

    @Test
    public void failureRethrowsItsExceptionUnchanged() {
        // given
        final IOException cause = new IOException("disk");

        // when
        try {
            throwOnFailure(new Failure(cause));
            fail("expected the wrapped exception");
        } catch (Exception thrown) {
            assertSame(cause, thrown);
        }
    }

    @Test
    public void failureRethrowsRuntimeExceptionsUnchanged() {
        // given
        final IllegalArgumentException cause = new IllegalArgumentException("bad");

        // when
        try {
            throwOnFailure(new Failure(cause));
            fail("expected the wrapped exception");
        } catch (Exception thrown) {
            assertSame(cause, thrown);
        }
    }

    @Test
    public void failureWrapsNonExceptionThrowablesInIllegalState() {
        // given
        final AssertionError cause = new AssertionError("boom");

        // when
        try {
            throwOnFailure(new Failure(cause));
            fail("expected an IllegalStateException");
        } catch (IllegalStateException thrown) {
            assertEquals(String.valueOf(cause), thrown.getMessage());
        } catch (Exception thrown) {
            fail("unexpected " + thrown);
        }
    }

    @Test
    public void failureWithoutExceptionStillThrows() {
        try {
            throwOnFailure(new Failure(null));
            fail("expected an IllegalStateException");
        } catch (IllegalStateException thrown) {
            assertEquals("null", thrown.getMessage());
        } catch (Exception thrown) {
            fail("unexpected " + thrown);
        }
    }

    @Test
    public void onlyTheCoroutineSuspendedMarkerIsSuspended() throws ReflectiveOperationException {
        assertTrue(isCoroutineSuspended(Marker.COROUTINE_SUSPENDED));
        assertFalse(isCoroutineSuspended(Marker.OTHER));
        assertFalse(isCoroutineSuspended("COROUTINE_SUSPENDED"));
        assertFalse(isCoroutineSuspended(new Ok("COROUTINE_SUSPENDED")));
        assertFalse(isCoroutineSuspended(null));
    }

    @Test
    public void continuationReportsTheDispatcherItWasGiven() throws ReflectiveOperationException {
        final Object dispatcher = new Object();
        final Continuation continuation = continuationFor(newParkedContinuation(dispatcher));
        assertSame(dispatcher, continuation.getContext());
        assertSame(dispatcher, continuation.getContext());
    }

    @Test
    public void continuationStaysParkedUntilResumed() throws Exception {
        final Object parked = newParkedContinuation(new Object());
        final Continuation continuation = continuationFor(parked);
        final CountDownLatch resumed = (CountDownLatch) parkedField(parked, "resumed");

        assertEquals(1, resumed.getCount());
        assertNull(parkedField(parked, "result"));
        continuation.getContext();
        continuation.unrelated();
        assertEquals(1, resumed.getCount());
        assertFalse(resumed.await(1, TimeUnit.MILLISECONDS));
    }

    @Test
    public void resumeReleasesTheWaiterWithTheStoredResult() throws Exception {
        // given
        final Object parked = newParkedContinuation(new Object());
        final Object result = new Ok("value");

        // when
        continuationFor(parked).resumeWith(result);

        // then
        final CountDownLatch resumed = (CountDownLatch) parkedField(parked, "resumed");
        assertEquals(0, resumed.getCount());
        assertSame(result, parkedField(parked, "result"));
        assertTrue(resumed.await(0, TimeUnit.MILLISECONDS));
    }

    @Test
    public void resumeKeepsAFailureResultForTheCallerToUnwrap() throws ReflectiveOperationException {
        // given
        final Object parked = newParkedContinuation(new Object());
        final Failure failure = new Failure(new IOException("offline"));

        // when
        continuationFor(parked).resumeWith(failure);

        // then
        assertSame(failure, parkedField(parked, "result"));
    }

    @Test
    public void continuationAnswersObjectMethodsItself() throws ReflectiveOperationException {
        final Continuation first = continuationFor(newParkedContinuation(new Object()));
        final Continuation second = continuationFor(newParkedContinuation(new Object()));

        assertEquals("ScheduledDeletion", first.toString());
        assertEquals(System.identityHashCode(first), first.hashCode());
        assertTrue(first.equals(first));
        assertFalse(first.equals(second));
        assertFalse(first.equals(null));
        assertFalse(first.equals("ScheduledDeletion"));
    }

    @Test
    public void continuationIgnoresMethodsItDoesNotModel() throws ReflectiveOperationException {
        // given
        final Continuation continuation = continuationFor(newParkedContinuation(new Object()));

        // when
        final Object result = continuation.unrelated();

        // then
        assertNull(result);
    }

}
