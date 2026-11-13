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
package app.hxreborn.extension.candycrushsaga;

import java.lang.ref.WeakReference;

import android.app.Activity;
import android.util.Log;

@SuppressWarnings("unused")
public final class GameModifiers {

    private static final int MAX_ATTEMPTS = 12;

    private static final int UNLOCK_SLOTS = 1;

    private static final int ADD_MOVES = 2;

    private static final int VALIDATION_REJECTED = -1;

    private static final int RECOVERY_PENDING = -2;

    private static final int ACTIVATION_ROLLED_BACK = -3;

    private static final GameModifiers INSTANCE = new GameModifiers();

    private final int features;

    private WeakReference<Activity> activity = new WeakReference<>(null);

    private boolean queued;

    private boolean finished;

    private boolean recovering;

    private int attempts;

    private GameModifiers() {
        this.features = (slotsEnabled() ? UNLOCK_SLOTS : 0) | (extraMovesEnabled() ? ADD_MOVES : 0);
    }

    private static boolean slotsEnabled() {
        return false;
    }

    private static boolean extraMovesEnabled() {
        return false;
    }

    public static void onNativeHandle(Activity current, long handle) {
        synchronized (INSTANCE) {
            if (handle == 0) {
                if (INSTANCE.activity.get() == current) {
                    INSTANCE.activity.clear();
                }
                return;
            }
            INSTANCE.activity = new WeakReference<>(current);
        }
        INSTANCE.schedule();
    }

    public static void onResponse(long requestId, boolean open) {
        if (!open) {
            INSTANCE.schedule();
        }
    }

    private void schedule() {
        Activity current;
        synchronized (this) {
            current = this.activity.get();
            if (this.finished || this.queued || current == null || current.isFinishing() || current.isDestroyed()) {
                return;
            }
            this.queued = true;
        }
        try {
            runOnGameThread(current, () -> activateModifiers(current));
        } catch (LinkageError | RuntimeException ex) {
            synchronized (this) {
                this.queued = false;
                this.finished = !this.recovering;
            }
            Log.w(Boosters.LOG_TAG, "Game modifier scheduling failed");
        }
    }

    private synchronized void activateModifiers(Activity current) {
        this.queued = false;
        if (this.activity.get() != current || current.isFinishing() || current.isDestroyed() || this.finished) {
            return;
        }
        if (!this.recovering && ++this.attempts > MAX_ATTEMPTS) {
            this.finished = true;
            Log.w(Boosters.LOG_TAG, "Game modifier activation limit reached, limit=" + MAX_ATTEMPTS);
            return;
        }
        try {
            System.loadLibrary("ccsmodifiers");
            int result = activate(this.features);
            recordActivation(result);
        } catch (LinkageError | SecurityException ex) {
            this.finished = !this.recovering;
            Log.w(Boosters.LOG_TAG, "Game modifier activation failed");
        }
    }

    private void recordActivation(int result) {
        this.finished = result != 0 && result != RECOVERY_PENDING;
        if (result == RECOVERY_PENDING && !this.recovering) {
            Log.w(Boosters.LOG_TAG, "Game modifier recovery pending, redirects or permissions remain changed");
        } else if (result == ACTIVATION_ROLLED_BACK) {
            Log.w(Boosters.LOG_TAG, "Game modifier activation rolled back, pointers and permissions restored");
        } else if (result == VALIDATION_REJECTED) {
            Log.w(Boosters.LOG_TAG, "Game modifier library validation failed");
        }
        this.recovering = result == RECOVERY_PENDING;
    }

    private static void runOnGameThread(Activity current, Runnable runnable) {
    }

    private static native int activate(int features);

}
