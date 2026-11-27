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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public final class ProtectionCrashHandlerTest {

    private static final String WORKER_THREAD_NAME = "Thread-14";

    private static final String PROTECTION_CLASS = "o.exampleGuard";

    private static final String HOST_CLASS = "com.example.Host";

    private static Throwable errorFrom(String className) {
        Throwable error = new ArithmeticException("divide by zero");
        error.setStackTrace(new StackTraceElement[] { new StackTraceElement(className, "run", null, 1) });
        return error;
    }

    @Test
    public void ignoresProtectionFailureOnWorkerThread() {
        Throwable[] forwarded = new Throwable[1];
        ProtectionCrashHandler handler = new ProtectionCrashHandler((thread, error) -> forwarded[0] = error);

        handler.uncaughtException(new Thread(() -> { }, WORKER_THREAD_NAME), errorFrom(PROTECTION_CLASS));

        Assert.assertNull(forwarded[0]);
    }

    @Test
    public void forwardsProtectionFailureOnMainThread() {
        Throwable[] forwarded = new Throwable[1];
        ProtectionCrashHandler handler = new ProtectionCrashHandler((thread, error) -> forwarded[0] = error);
        Throwable error = errorFrom(PROTECTION_CLASS);

        handler.uncaughtException(new Thread(() -> { }, "main"), error);

        Assert.assertSame(error, forwarded[0]);
    }

    @Test
    public void forwardsHostFailureOnWorkerThread() {
        Throwable[] forwarded = new Throwable[1];
        ProtectionCrashHandler handler = new ProtectionCrashHandler((thread, error) -> forwarded[0] = error);
        Throwable error = errorFrom(HOST_CLASS);

        handler.uncaughtException(new Thread(() -> { }, WORKER_THREAD_NAME), error);

        Assert.assertSame(error, forwarded[0]);
    }

    @Test
    public void hostFailureWithoutDelegateIsDropped() {
        ProtectionCrashHandler handler = new ProtectionCrashHandler(null);

        handler.uncaughtException(new Thread(() -> { }, WORKER_THREAD_NAME), errorFrom(HOST_CLASS));
    }

}
