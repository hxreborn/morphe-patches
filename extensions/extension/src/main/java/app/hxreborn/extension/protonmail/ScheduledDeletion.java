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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import android.util.Log;
import app.morphe.extension.shared.Utils;

@SuppressWarnings("unused")
public final class ScheduledDeletion {

    static final String TRASH = "TRASH";
    static final String SPAM = "SPAM";
    static final String[] EMPTIED_LABELS = { TRASH, SPAM };

    private static final String TAG = "ScheduledDeletion";

    private static final AtomicBoolean DELETING = new AtomicBoolean();

    private static final Map<Object, Object> MAILBOX_SESSIONS = new WeakHashMap<>();

    private static final Map<String, Object> SYSTEM_LABEL_IDS = new ConcurrentHashMap<>();

    private static volatile Object shownLabelId;

    private ScheduledDeletion() {

    }

    public static synchronized void captureMailbox(Object result, Object mailUserSession) {
        try {
            final Object mailbox = UniffiCalls.okValueOrNull(result);
            if (mailbox != null) {
                MAILBOX_SESSIONS.put(mailbox, mailUserSession);
            }
        } catch (Throwable throwable) {
            Log.e(TAG, "Failed to associate mailbox with account", throwable);
        }
    }

    public static synchronized void onMailboxShown(Object mailbox) {
        try {
            final Object sessionSnapshot = MAILBOX_SESSIONS.get(mailbox);
            if (sessionSnapshot == null) {
                return;
            }

            final String account = userIdOf(sessionSnapshot);
            if (account == null) {
                return;
            }

            shownLabelId = UniffiCalls.callNoArg(mailbox, "labelId");
            cacheSystemLabelIds(sessionSnapshot);

            if (!ScheduledDeletionSettings.anyLabelDue(Utils.getContext(), account)) {
                return;
            }
            if (!DELETING.compareAndSet(false, true)) {
                return;
            }

            final Thread worker = new Thread(() -> {
                try {
                    emptyDueLabels(sessionSnapshot, account);
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

    private static void emptyDueLabels(Object mailSession, String account) throws Exception {
        final List<String> deletedLabels = new ArrayList<>();

        for (String label : EMPTIED_LABELS) {
            final long intervalMs = intervalMsOf(label);
            if (intervalMs <= 0L) {
                continue;
            }
            if (!ScheduledDeletionSettings.due(Utils.getContext(), account, label, intervalMs)) {
                continue;
            }

            final Object labelId = systemLabelId(mailSession, label);
            if (intervalMsOf(label) != intervalMs) {
                continue;
            }

            final Object result = UniffiCalls.invokeSuspending(UniffiCalls.uniffiMethod("deleteAllMessagesInLabel"),
                    null, mailSession, labelId);
            final boolean emptied = "Ok".equals(UniffiCalls.simpleNameOf(result));

            if (!emptied) {
                Log.w(TAG, "deleteAllMessagesInLabel(" + label + ") returned " + UniffiCalls.simpleNameOf(result));
                continue;
            }

            ScheduledDeletionSettings.recordEmptied(Utils.getContext(), account, label);
            deletedLabels.add(label);
        }
        ScheduledDeletionEditor.showDeleted(deletedLabels);
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
        }, "hx-label-ids").start();
    }

    private static long intervalMsOf(String label) {
        return ScheduledDeletionSettings.intervalSeconds(Utils.getContext(), label) * 1000L;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static Object systemLabelId(Object mailSession, String label) throws Exception {
        final Class<?> systemLabel = Class.forName(UniffiCalls.UNIFFI_PACKAGE + "SystemLabel");
        final Object constant = Enum.valueOf((Class) systemLabel, label);
        final Object result = UniffiCalls.invokeSuspending(UniffiCalls.uniffiMethod("resolveSystemLabelId"), null,
                mailSession, constant);
        final Object labelId = UniffiCalls.okValueOrNull(result);
        if (labelId == null) {
            throw new IllegalStateException(
                    "resolveSystemLabelId(" + label + ") returned " + UniffiCalls.simpleNameOf(result));
        }
        return labelId;
    }

    private static String userIdOf(Object mailUserSession) {
        try {
            return (String) UniffiCalls.okValueOrNull(UniffiCalls.callNoArg(mailUserSession, "userId"));
        } catch (Throwable throwable) {
            Log.e(TAG, "Failed to read the account ID", throwable);
            return null;
        }
    }

}
