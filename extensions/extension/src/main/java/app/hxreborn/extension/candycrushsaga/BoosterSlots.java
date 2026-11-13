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
public final class BoosterSlots {

    private static final int MAX_ATTEMPTS = 12;

    private static final BoosterSlots INSTANCE = new BoosterSlots();

    private WeakReference<Activity> activity = new WeakReference<>(null);

    private boolean queued;

    private boolean finished;

    private int attempts;

    private BoosterSlots() {
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
            runOnGameThread(current, () -> unlock(current));
        } catch (LinkageError | RuntimeException ex) {
            synchronized (this) {
                this.queued = false;
                this.finished = true;
            }
            Log.w(Boosters.LOG_TAG, "Booster slot scheduling failed");
        }
    }

    private synchronized void unlock(Activity current) {
        this.queued = false;
        if (this.activity.get() != current || current.isFinishing() || current.isDestroyed() || this.finished) {
            return;
        }
        if (++this.attempts > MAX_ATTEMPTS) {
            this.finished = true;
            Log.w(Boosters.LOG_TAG, "Booster slot activation limit reached, limit=" + MAX_ATTEMPTS);
            return;
        }
        try {
            System.loadLibrary("ccsslots");
            int result = activate();
            this.finished = result != 0;
            if (result < 0) {
                Log.w(Boosters.LOG_TAG, "Booster slot library validation failed");
            }
        } catch (LinkageError | SecurityException ex) {
            this.finished = true;
            Log.w(Boosters.LOG_TAG, "Booster slot activation failed");
        }
    }

    private static void runOnGameThread(Activity current, Runnable runnable) {
    }

    private static native int activate();

}
