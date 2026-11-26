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
package app.hxreborn.extension.magicposer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.util.Log;

@SuppressWarnings("unused")
public final class ProtectionCrashFilter {

    static final String LOG_TAG = "hxMagicPoser";

    private static final String WATCHER_THREAD_NAME = "hxProtectionCrashWatch";

    private static final long WATCH_DURATION_MS = 60_000L;

    private static final long WATCH_INTERVAL_MS = 20L;

    private static volatile Thread.UncaughtExceptionHandler installed;

    private ProtectionCrashFilter() {
    }

    public static void install() {
        wrapDefaultHandler();
        Thread watcher = new Thread(ProtectionCrashFilter::keepHandlerInstalled, WATCHER_THREAD_NAME);
        watcher.setDaemon(true);
        watcher.start();
    }

    public static void startProtectionThread(Method start, Object target, Object[] arguments) {
        try {
            start.invoke(target, arguments);
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof IllegalThreadStateException) {
                Log.w(LOG_TAG, "Ignored a restart of a finished protection thread");
                return;
            }
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new IllegalStateException("Protection thread start failed", ex);
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException("Protection thread start is not accessible", ex);
        }
    }

    private static void keepHandlerInstalled() {
        long end = System.currentTimeMillis() + WATCH_DURATION_MS;
        while (System.currentTimeMillis() < end) {
            wrapDefaultHandler();
            try {
                Thread.sleep(WATCH_INTERVAL_MS);
            } catch (InterruptedException ex) {
                Log.w(LOG_TAG, "Handler watcher interrupted", ex);
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private static void wrapDefaultHandler() {
        Thread.UncaughtExceptionHandler current = Thread.getDefaultUncaughtExceptionHandler();
        if (current == installed) {
            return;
        }
        installed = new ProtectionCrashHandler(current);
        Thread.setDefaultUncaughtExceptionHandler(installed);
    }

}
