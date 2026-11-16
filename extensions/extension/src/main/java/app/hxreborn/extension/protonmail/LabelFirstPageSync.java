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

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import android.util.Log;

import app.hxreborn.extension.shared.ProxyHandler;

final class LabelFirstPageSync extends ProxyHandler {

    private static final String LIST_UPDATE = "List";

    private static final String ERROR_UPDATE = "Error";

    private static final String STATUS_UPDATE = "Status";

    private static final String FIRST_PAGE_LOADING_END = "FIRST_PAGE_LOADING_END";

    private static final long FIRST_PAGE_TIMEOUT_SECONDS = 30L;

    private static final String CALLBACK_CLASS = "MessageScrollerLiveQueryCallback";

    private static final String NEW_MAILBOX = "newMailbox";

    private static final String SCROLL_MESSAGES_FOR_LABEL = "scrollMessagesForLabel";

    private static final String FETCH_MORE = "fetchMore";

    private static final String TERMINATE = "terminate";

    private static final String DESTROY = "destroy";

    private static final String ON_UPDATE = "onUpdate";

    private static final String STATUS_GETTER = "getV1";

    private final CountDownLatch firstPageSynced = new CountDownLatch(1);

    private LabelFirstPageSync() {
    }

    static void sync(Object mailUserSession, Object labelId) {
        try {
            syncOrThrow(mailUserSession, labelId);
        } catch (Exception ex) {
            Log.w(ScheduledDeletion.TAG, "Failed to sync the first page before deleting", ex);
        }
    }

    private static void syncOrThrow(Object mailUserSession, Object labelId) throws Exception {
        final Object mailbox = UniffiCalls
            .okValueOrNull(UniffiCalls.uniffiMethod(NEW_MAILBOX).invoke(null, mailUserSession, labelId));
        if (mailbox == null) {
            Log.w(ScheduledDeletion.TAG, NEW_MAILBOX + " returned no mailbox");
            return;
        }

        try {
            new LabelFirstPageSync().loadFirstPage(mailbox);
        } finally {
            UniffiCalls.callNoArg(mailbox, DESTROY);
        }
    }

    private void loadFirstPage(Object mailbox) throws Exception {
        final Class<?> callbackType = Class.forName(UniffiCalls.UNIFFI_PACKAGE + CALLBACK_CLASS);
        final Object callback = Proxy.newProxyInstance(LabelFirstPageSync.class.getClassLoader(),
                new Class<?>[] { callbackType }, this);

        final Object scroller = UniffiCalls.okValueOrNull(UniffiCalls
            .invokeSuspending(UniffiCalls.uniffiMethod(SCROLL_MESSAGES_FOR_LABEL), null, mailbox, null, callback));
        if (scroller == null) {
            Log.w(ScheduledDeletion.TAG, SCROLL_MESSAGES_FOR_LABEL + " returned no scroller");
            return;
        }

        try {
            UniffiCalls.callNoArg(scroller, FETCH_MORE);
            if (!this.firstPageSynced.await(FIRST_PAGE_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                Log.w(ScheduledDeletion.TAG, "First page did not sync within " + FIRST_PAGE_TIMEOUT_SECONDS + "s");
            }
        } finally {
            UniffiCalls.callNoArg(scroller, TERMINATE);
            UniffiCalls.callNoArg(scroller, DESTROY);
        }
    }

    @Override
    protected Object handle(Method method, Object[] args) {
        if (!ON_UPDATE.equals(method.getName())) {
            return null;
        }

        final Object update = args[0];
        final String updateType = UniffiCalls.simpleNameOf(update);
        if (ERROR_UPDATE.equals(updateType)) {
            Log.w(ScheduledDeletion.TAG, "Message scroller reported an error");
            this.firstPageSynced.countDown();
        } else if (LIST_UPDATE.equals(updateType) || isFirstPageLoadingEnd(updateType, update)) {
            this.firstPageSynced.countDown();
        }
        return null;
    }

    private static boolean isFirstPageLoadingEnd(String updateType, Object update) {
        return STATUS_UPDATE.equals(updateType)
                && FIRST_PAGE_LOADING_END.equals(String.valueOf(UniffiCalls.callNoArg(update, STATUS_GETTER)));
    }

}
