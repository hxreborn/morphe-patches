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
package app.hxreborn.extension.bereal;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import android.os.Looper;

@SuppressWarnings("unused")
public final class RetakeCount {

    private static final long WAIT_SECONDS = 60;

    private static final Object LOCK = new Object();

    private static CountDownLatch pending;

    private static int realCount;

    private static Integer chosen;

    private RetakeCount() {
    }

    public static boolean isPatchIncluded() {
        return false;
    }

    public static void sendStarted(int real) {
        if (!PatchSettings.CUSTOM_RETAKE_COUNT.get()) {
            return;
        }
        synchronized (LOCK) {
            realCount = real;
            chosen = null;
            pending = new CountDownLatch(1);
        }
        PatchContext.open(RetakeCountActivity.class);
    }

    public static int retakeCount(int original) {
        final CountDownLatch latch;
        synchronized (LOCK) {
            latch = pending;
        }
        if (latch == null || !PatchSettings.CUSTOM_RETAKE_COUNT.get()) {
            return original;
        }
        if (Looper.myLooper() != Looper.getMainLooper()) {
            try {
                latch.await(WAIT_SECONDS, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
        synchronized (LOCK) {
            final Integer value = chosen;
            pending = null;
            chosen = null;
            return (value != null) ? Math.max(0, value) : original;
        }
    }

    static int realCount() {
        synchronized (LOCK) {
            return realCount;
        }
    }

    static void choose(Integer value) {
        synchronized (LOCK) {
            if (pending == null) {
                return;
            }
            chosen = value;
            pending.countDown();
        }
    }

}
