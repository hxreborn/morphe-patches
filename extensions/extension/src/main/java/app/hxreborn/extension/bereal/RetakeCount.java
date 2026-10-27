/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
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
