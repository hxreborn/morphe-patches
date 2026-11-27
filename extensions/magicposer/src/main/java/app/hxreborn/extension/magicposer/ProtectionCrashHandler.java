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

import android.util.Log;

final class ProtectionCrashHandler implements Thread.UncaughtExceptionHandler {

    private static final String MAIN_THREAD_NAME = "main";

    private static final String PROTECTION_PACKAGE_PREFIX = "o.";

    private final Thread.UncaughtExceptionHandler delegate;

    ProtectionCrashHandler(Thread.UncaughtExceptionHandler delegate) {
        this.delegate = delegate;
    }

    static boolean isProtectionFailure(Thread thread, Throwable error) {
        if (thread == null || MAIN_THREAD_NAME.equals(thread.getName())) {
            return false;
        }
        for (StackTraceElement frame : error.getStackTrace()) {
            if (frame.getClassName().startsWith(PROTECTION_PACKAGE_PREFIX)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void uncaughtException(Thread thread, Throwable error) {
        if (isProtectionFailure(thread, error)) {
            Log.w(ProtectionCrashFilter.LOG_TAG,
                    "Ignored " + error.getClass().getName() + " from a protection check");
            return;
        }
        if (this.delegate != null) {
            this.delegate.uncaughtException(thread, error);
        }
    }

}
