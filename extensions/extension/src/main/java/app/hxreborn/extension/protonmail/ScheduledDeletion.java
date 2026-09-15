/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.protonmail;

import app.morphe.extension.shared.Utils;

import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("unused")
public final class ScheduledDeletion {

    private static final String TAG = "ScheduledDeletion";
    private static final String UNIFFI_PACKAGE = "uniffi.mail_uniffi.";
    static final String[] EMPTIED_LABELS = { "TRASH", "SPAM" };
    private static final long SUSPEND_TIMEOUT_SECONDS = 60L;

    private static final AtomicBoolean DELETING = new AtomicBoolean();

    private static final Map<Object, Object> MAILBOX_SESSIONS = new WeakHashMap<>();

    private ScheduledDeletion() {}

    public static synchronized void captureMailbox(Object result, Object mailUserSession) {
        try {
            final Object mailbox = okValueOrNull(result);
            if (mailbox != null) MAILBOX_SESSIONS.put(mailbox, mailUserSession);
        } catch (Throwable throwable) {
            Log.e(TAG, "Failed to associate mailbox with account", throwable);
        }
    }

    public static Object ioDispatcher() {
        return null;
    }

    private static void throwOnFailure(Object result) throws Exception {
        if (result == null || !"Failure".equals(result.getClass().getSimpleName())) return;

        final Field cause = result.getClass().getDeclaredField("exception");
        cause.setAccessible(true);
        final Object thrown = cause.get(result);

        if (thrown instanceof Exception) throw (Exception) thrown;
        throw new IllegalStateException(String.valueOf(thrown));
    }

    public static synchronized void onMailboxShown(Object mailbox) {
        try {
            final Object sessionSnapshot = MAILBOX_SESSIONS.get(mailbox);
            if (sessionSnapshot == null) return;

            final String account = userIdOf(sessionSnapshot);
            if (account == null) return;

            final long intervalMs =
                    ScheduledDeletionSettings.intervalSeconds(Utils.getContext()) * 1000L;
            if (intervalMs <= 0L) return;
            if (!ScheduledDeletionSettings.anyLabelDue(Utils.getContext(), account, EMPTIED_LABELS,
                    intervalMs)) {
                return;
            }
            if (!DELETING.compareAndSet(false, true)) return;

            final Thread worker = new Thread(() -> {
                try {
                    emptyDueLabels(sessionSnapshot, account, intervalMs);
                } catch (Throwable throwable) {
                    Log.e(TAG, "Failed to empty Trash and Spam", throwable);
                } finally {
                    DELETING.set(false);
                }
            }, "hx-scheduled-deletion");
            try {
                worker.start();
            } catch (Throwable throwable) {
                DELETING.set(false);
                throw throwable;
            }
        } catch (Throwable throwable) {
            Log.e(TAG, "Failed to start scheduled deletion", throwable);
        }
    }

    private static void emptyDueLabels(Object mailSession, String account, long intervalMs)
            throws Exception {
        final SuspendInvoker suspendInvoker = new SuspendInvoker();

        for (String label : EMPTIED_LABELS) {
            if (ScheduledDeletionSettings.intervalSeconds(Utils.getContext()) * 1000L != intervalMs) return;
            if (!ScheduledDeletionSettings.due(Utils.getContext(), account, label, intervalMs)) continue;

            final Object labelId = systemLabelId(mailSession, suspendInvoker, label);
            if (ScheduledDeletionSettings.intervalSeconds(Utils.getContext()) * 1000L != intervalMs) return;

            final Object result = suspendInvoker.invoke(
                    uniffiMethod("deleteAllMessagesInLabel"), null, mailSession, labelId);
            final boolean emptied = "Ok".equals(simpleNameOf(result));

            if (!emptied) {
                Log.w(TAG, "deleteAllMessagesInLabel(" + label + ") returned "
                        + simpleNameOf(result));
                continue;
            }

            ScheduledDeletionSettings.recordEmptied(Utils.getContext(), account, label);
            ScheduledDeletionEditor.showEmptied(label);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static Object systemLabelId(Object mailSession, SuspendInvoker suspendInvoker,
                                        String label)
            throws Exception {
        final Class<?> systemLabel = Class.forName(UNIFFI_PACKAGE + "SystemLabel");
        final Object constant = Enum.valueOf((Class) systemLabel, label);
        final Object result =
                suspendInvoker.invoke(
                        uniffiMethod("resolveSystemLabelId"), null, mailSession, constant);
        final Object labelId = okValueOrNull(result);
        if (labelId == null) {
            throw new IllegalStateException(
                    "resolveSystemLabelId(" + label + ") returned " + simpleNameOf(result));
        }
        return labelId;
    }

    private static final class SuspendInvoker {
        private final Class<?> continuationType;
        private final Object context;

        SuspendInvoker() throws Exception {
            final Class<?>[] parameters =
                    uniffiMethod("deleteAllMessagesInLabel").getParameterTypes();
            continuationType = parameters[parameters.length - 1];
            context = ioDispatcher();
            if (context == null) {
                throw new IllegalStateException("I/O coroutine dispatcher is unavailable");
            }
        }

        Object invoke(Method method, Object target, Object... args) throws Exception {
            final ParkedContinuation parked = new ParkedContinuation(context);
            final Object continuation = Proxy.newProxyInstance(classLoader(),
                    new Class<?>[] { continuationType }, parked);

            final Object[] withContinuation = new Object[args.length + 1];
            System.arraycopy(args, 0, withContinuation, 0, args.length);
            withContinuation[args.length] = continuation;

            final Object immediate = method.invoke(target, withContinuation);
            if (!isCoroutineSuspended(immediate)) return immediate;

            if (!parked.resumed.await(SUSPEND_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new IllegalStateException(
                        method.getName() + " did not resume within "
                                + SUSPEND_TIMEOUT_SECONDS + "s");
            }
            throwOnFailure(parked.result);
            return parked.result;
        }
    }

    private static final class ParkedContinuation implements InvocationHandler {
        private final CountDownLatch resumed = new CountDownLatch(1);
        private final Object context;
        private volatile Object result;

        ParkedContinuation(Object context) {
            this.context = context;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            final String name = method.getName();
            if ("getContext".equals(name)) return context;
            if ("resumeWith".equals(name)) {
                result = args[0];
                resumed.countDown();
                return null;
            }
            if ("toString".equals(name)) return "ScheduledDeletion";
            if ("hashCode".equals(name)) return System.identityHashCode(proxy);
            if ("equals".equals(name)) return proxy == args[0];
            return null;
        }
    }

    private static boolean isCoroutineSuspended(Object value) {
        return value instanceof Enum && "COROUTINE_SUSPENDED".equals(((Enum<?>) value).name());
    }

    private static String userIdOf(Object mailUserSession) {
        try {
            return (String) okValueOrNull(callNoArg(mailUserSession, "userId"));
        } catch (Throwable throwable) {
            Log.e(TAG, "Failed to read the account ID", throwable);
            return null;
        }
    }

    private static Object okValueOrNull(Object result) {
        return "Ok".equals(simpleNameOf(result)) ? callNoArg(result, "getV1") : null;
    }

    private static Method uniffiMethod(String name) throws Exception {
        return methodNamed(Class.forName(UNIFFI_PACKAGE + "Mail_uniffiKt"), name);
    }

    private static Method methodNamed(Class<?> type, String name) throws NoSuchMethodException {
        for (Method candidate : type.getMethods()) {
            if (candidate.getName().equals(name)) {
                candidate.setAccessible(true);
                return candidate;
            }
        }
        throw new NoSuchMethodException(type.getName() + "." + name);
    }

    private static Object callNoArg(Object target, String name) {
        try {
            return methodNamed(target.getClass(), name).invoke(target);
        } catch (Throwable throwable) {
            throw new IllegalStateException(
                    "Failed to invoke " + simpleNameOf(target) + "." + name + "()", throwable);
        }
    }

    private static String simpleNameOf(Object value) {
        return value == null ? "null" : value.getClass().getSimpleName();
    }

    private static ClassLoader classLoader() {
        return ScheduledDeletion.class.getClassLoader();
    }
}
