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
package app.hxreborn.extension.protonmail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import android.util.Log;
import app.morphe.extension.shared.Utils;

@SuppressWarnings("unused")
public final class ScheduledDeletion {

    static final String TRASH = "TRASH";
    static final String SPAM = "SPAM";
    static final String[] EMPTIED_LABELS = { TRASH, SPAM };

    static final String TAG = "ScheduledDeletion";

    private static final String AUTHENTICATED = "AUTHENTICATED";

    private static final String APP_OPEN_THREAD = "hx-scheduled-deletion";

    private static final String BACKGROUND_THREAD = "hx-scheduled-deletion-background";

    private static final String LABEL_IDS_THREAD = "hx-label-ids";

    private static final String SYSTEM_LABEL_CLASS = "SystemLabel";

    private static final String GET_SESSIONS = "getSessions";

    private static final String USER_SESSION_FROM_STORED_SESSION = "userSessionFromStoredSession";

    private static final String RESOLVE_SYSTEM_LABEL_ID = "resolveSystemLabelId";

    private static final String DELETE_ALL_MESSAGES_IN_LABEL = "deleteAllMessagesInLabel";

    private static final String LABEL_ID_GETTER = "labelId";

    private static final String STATE_GETTER = "state";

    private static final String USER_ID_GETTER = "userId";

    private static final AtomicBoolean DELETING = new AtomicBoolean();

    private static final Map<Object, Object> MAILBOX_SESSIONS = new WeakHashMap<>();

    private static final Map<String, Object> SYSTEM_LABEL_IDS = new ConcurrentHashMap<>();

    private static volatile Object shownLabelId;

    private ScheduledDeletion() {

    }

    public static synchronized void captureMailbox(Object newMailboxResult, Object mailUserSession) {
        try {
            final Object mailbox = UniffiCalls.okValueOrNull(newMailboxResult);
            if (mailbox != null) {
                MAILBOX_SESSIONS.put(mailbox, mailUserSession);
            }
        } catch (Throwable throwable) {
            Log.e(TAG, "Failed to associate mailbox with account", throwable);
        }
    }

    public static synchronized void onMailboxShown(Object mailbox) {
        try {
            final Object mailUserSession = MAILBOX_SESSIONS.get(mailbox);
            if (mailUserSession == null) {
                return;
            }

            final String accountId = userIdOf(mailUserSession);
            if (accountId == null) {
                return;
            }

            shownLabelId = UniffiCalls.callNoArg(mailbox, LABEL_ID_GETTER);
            cacheSystemLabelIds(mailUserSession);

            final List<String> deletedInBackground = ScheduledDeletionSettings
                .takeDeletedInBackground(Utils.getContext(), accountId);
            final boolean deletionStarted = ScheduledDeletionSettings.anyLabelDue(Utils.getContext(), accountId)
                    && startDeletionWorker(APP_OPEN_THREAD, "Failed to empty Trash and Spam", () -> {
                        ScheduledDeletionEditor.showDeleted(
                                combinedLabels(deletedInBackground, emptyDueLabels(mailUserSession, accountId)));
                        return null;
                    });
            if (!deletionStarted) {
                ScheduledDeletionEditor.showDeleted(deletedInBackground);
            }
        } catch (Throwable throwable) {
            Log.e(TAG, "Failed to start scheduled deletion", throwable);
        }
    }

    public static void onBackgroundExecution(Object mailSession) {
        try {
            if (!ScheduledDeletionSettings.isActive(Utils.getContext())) {
                return;
            }
            startDeletionWorker(BACKGROUND_THREAD, "Failed to empty Trash and Spam in the background", () -> {
                emptyDueLabelsOfEveryAccount(mailSession);
                return null;
            });
        } catch (Throwable throwable) {
            Log.e(TAG, "Failed to start background scheduled deletion", throwable);
        }
    }

    private static boolean startDeletionWorker(String threadName, String failureMessage, Callable<Void> deletion) {
        if (!DELETING.compareAndSet(false, true)) {
            return false;
        }

        final Thread worker = new Thread(() -> {
            try {
                deletion.call();
            } catch (Throwable throwable) {
                Log.e(TAG, failureMessage, throwable);
            } finally {
                DELETING.set(false);
            }
        }, threadName);
        try {
            worker.start();
        } catch (Throwable throwable) {
            DELETING.set(false);
            throw throwable;
        }
        return true;
    }

    static List<String> combinedLabels(List<String> first, List<String> second) {
        final List<String> labels = new ArrayList<>(first);
        for (String label : second) {
            if (!labels.contains(label)) {
                labels.add(label);
            }
        }
        return labels;
    }

    private static void emptyDueLabelsOfEveryAccount(Object mailSession) throws Exception {
        final Object sessions = UniffiCalls.okValueOrNull(UniffiCalls
            .invokeSuspending(UniffiCalls.methodNamed(mailSession.getClass(), GET_SESSIONS), mailSession));
        if (!(sessions instanceof List)) {
            Log.w(TAG, GET_SESSIONS + " returned no session list");
            return;
        }

        for (Object storedSession : (List<?>) sessions) {
            try {
                emptyDueLabelsOfStoredSession(mailSession, storedSession);
            } catch (Exception ex) {
                Log.e(TAG, "Background deletion failed for one account", ex);
            }
        }
    }

    private static void emptyDueLabelsOfStoredSession(Object mailSession, Object storedSession) throws Exception {
        if (!AUTHENTICATED.equals(String.valueOf(UniffiCalls.callNoArg(storedSession, STATE_GETTER)))) {
            return;
        }
        final String accountId = (String) UniffiCalls.callNoArg(storedSession, USER_ID_GETTER);
        if (!ScheduledDeletionSettings.anyLabelDue(Utils.getContext(), accountId)) {
            return;
        }

        final Object userSession = UniffiCalls.okValueOrNull(UniffiCalls.invokeSuspending(
                UniffiCalls.methodNamed(mailSession.getClass(), USER_SESSION_FROM_STORED_SESSION), mailSession,
                storedSession));
        if (userSession == null) {
            Log.w(TAG, USER_SESSION_FROM_STORED_SESSION + " returned no session");
            return;
        }
        final List<String> deletedLabels = emptyDueLabels(userSession, accountId);
        ScheduledDeletionSettings.addDeletedInBackground(Utils.getContext(), accountId, deletedLabels);
        Log.i(TAG, "Background deletion emptied " + deletedLabels);
    }

    private static List<String> emptyDueLabels(Object mailSession, String accountId) throws Exception {
        final List<String> deletedLabels = new ArrayList<>();

        for (String label : EMPTIED_LABELS) {
            final long intervalMs = intervalMsOf(label);
            if (intervalMs <= 0L || !ScheduledDeletionSettings.due(Utils.getContext(), accountId, label, intervalMs)) {
                continue;
            }

            final Object labelId = systemLabelId(mailSession, label);
            if (!stillDue(accountId, label, intervalMs)) {
                continue;
            }

            LabelFirstPageSync.sync(mailSession, labelId);
            if (!stillDue(accountId, label, intervalMs)) {
                continue;
            }

            final Object deleteResult = UniffiCalls
                .invokeSuspending(UniffiCalls.uniffiMethod(DELETE_ALL_MESSAGES_IN_LABEL), null, mailSession, labelId);
            if (!UniffiCalls.isOk(deleteResult)) {
                Log.w(TAG, DELETE_ALL_MESSAGES_IN_LABEL + "(" + label + ") returned "
                        + UniffiCalls.simpleNameOf(deleteResult));
                continue;
            }

            ScheduledDeletionSettings.recordEmptied(Utils.getContext(), accountId, label);
            deletedLabels.add(label);
        }
        return deletedLabels;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean stillDue(String accountId, String label, long intervalMs) {
        return intervalMsOf(label) == intervalMs
                && ScheduledDeletionSettings.due(Utils.getContext(), accountId, label, intervalMs);
    }

    static String shownLabel() {
        final Object shown = shownLabelId;
        if (shown == null) {
            return null;
        }

        for (Map.Entry<String, Object> known : SYSTEM_LABEL_IDS.entrySet()) {
            if (shown.equals(known.getValue())) {
                return known.getKey();
            }
        }
        return null;
    }

    private static void cacheSystemLabelIds(Object mailSession) {
        if (SYSTEM_LABEL_IDS.size() == EMPTIED_LABELS.length) {
            return;
        }

        new Thread(() -> {
            try {
                for (String label : EMPTIED_LABELS) {
                    SYSTEM_LABEL_IDS.put(label, systemLabelId(mailSession, label));
                }
            } catch (Throwable throwable) {
                Log.e(TAG, "Failed to resolve the Trash and Spam label IDs", throwable);
            }
        }, LABEL_IDS_THREAD).start();
    }

    private static long intervalMsOf(String label) {
        return ScheduledDeletionSettings.intervalSeconds(Utils.getContext(), label) * 1000L;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static Object systemLabelId(Object mailSession, String label) throws Exception {
        final Class<?> systemLabel = Class.forName(UniffiCalls.UNIFFI_PACKAGE + SYSTEM_LABEL_CLASS);
        final Object constant = Enum.valueOf((Class) systemLabel, label);
        final Object resolveResult = UniffiCalls.invokeSuspending(UniffiCalls.uniffiMethod(RESOLVE_SYSTEM_LABEL_ID),
                null, mailSession, constant);
        final Object labelId = UniffiCalls.okValueOrNull(resolveResult);
        if (labelId == null) {
            throw new IllegalStateException(
                    RESOLVE_SYSTEM_LABEL_ID + "(" + label + ") returned " + UniffiCalls.simpleNameOf(resolveResult));
        }
        return labelId;
    }

    private static String userIdOf(Object mailUserSession) {
        try {
            return (String) UniffiCalls.okValueOrNull(UniffiCalls.callNoArg(mailUserSession, USER_ID_GETTER));
        } catch (Throwable throwable) {
            Log.e(TAG, "Failed to read the account ID", throwable);
            return null;
        }
    }

}
