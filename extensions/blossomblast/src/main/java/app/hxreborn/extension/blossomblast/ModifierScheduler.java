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
package app.hxreborn.extension.blossomblast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import android.os.Handler;
import android.util.Log;

final class ModifierScheduler {

    static final long RETRY_DELAY_MS = 500;

    static final long POST_TIMEOUT_MS = 5000;

    static final int MAX_ATTEMPTS = 120;

    private final Handler handler;

    private final GameThread gameThread;

    private final ModifierBridge bridge;

    private final List<String> pendingCommands;

    private boolean slotsPending;

    private final ServerGrants grants;

    private WeakReference<Object> application = new WeakReference<>(null);

    private long handle;

    private int generation;

    private int postedTask;

    private boolean taskOutstanding;

    private boolean running;

    private int attempts;

    private boolean finished;

    private boolean postFailureLogged;

    private boolean workStopped;

    ModifierScheduler(Handler handler, GameThread gameThread, ModifierBridge bridge, List<String> commands,
            boolean slots, ServerGrants grants) {
        this.handler = handler;
        this.gameThread = gameThread;
        this.bridge = bridge;
        this.pendingCommands = new ArrayList<>(commands);
        this.slotsPending = slots;
        this.grants = grants;
        this.finished = !hasPendingWork();
    }

    void onNativeHandle(Object current, long nativeHandle) {
        int attached;
        synchronized (this) {
            if (this.finished) {
                return;
            }
            boolean sameApplication = this.application.get() == current;
            if (nativeHandle == 0) {
                if (sameApplication && this.handle != 0) {
                    this.handle = 0;
                    this.generation++;
                    this.taskOutstanding = false;
                }
                return;
            }
            if (sameApplication && this.handle == nativeHandle) {
                return;
            }
            this.application = new WeakReference<>(current);
            this.handle = nativeHandle;
            this.attempts = 0;
            this.taskOutstanding = false;
            attached = ++this.generation;
        }
        this.handler.post(() -> post(attached));
    }

    synchronized boolean isFinished() {
        return this.finished;
    }

    private void post(int expectedGeneration) {
        Object current;
        int task;
        synchronized (this) {
            current = this.application.get();
            if (this.finished || this.running || this.taskOutstanding || expectedGeneration != this.generation
                    || this.handle == 0 || current == null) {
                return;
            }
            task = ++this.postedTask;
            this.taskOutstanding = true;
        }
        this.handler.postDelayed(() -> onPostTimeout(expectedGeneration, task), POST_TIMEOUT_MS);
        try {
            this.gameThread.post(current, () -> run(expectedGeneration, task));
        } catch (LinkageError | RuntimeException ex) {
            onPostFailure(expectedGeneration, task);
        }
    }

    private void onPostFailure(int expectedGeneration, int task) {
        synchronized (this) {
            if (this.finished || !isOutstanding(expectedGeneration, task)) {
                return;
            }
            this.taskOutstanding = false;
            if (!this.postFailureLogged) {
                this.postFailureLogged = true;
                Log.w(GameModifiers.LOG_TAG, "Game thread scheduling failed");
            }
            if (!countAttempt()) {
                return;
            }
        }
        this.handler.postDelayed(() -> post(expectedGeneration), RETRY_DELAY_MS);
    }

    private void onPostTimeout(int expectedGeneration, int task) {
        synchronized (this) {
            if (this.finished || !isOutstanding(expectedGeneration, task)) {
                return;
            }
            this.taskOutstanding = false;
            if (!countAttempt()) {
                return;
            }
        }
        post(expectedGeneration);
    }

    private void run(int expectedGeneration, int task) {
        List<String> commands;
        boolean slots;
        synchronized (this) {
            if (this.finished || !isOutstanding(expectedGeneration, task)) {
                return;
            }
            this.taskOutstanding = false;
            this.running = true;
            commands = new ArrayList<>(this.pendingCommands);
            slots = this.slotsPending;
        }
        List<String> executed = new ArrayList<>();
        int commandState = ModifierBridge.DONE;
        for (String command : commands) {
            commandState = executeCommand(command);
            if (commandState != ModifierBridge.DONE) {
                break;
            }
            executed.add(command);
        }
        int slotState = slots ? unlockSlots() : ModifierBridge.DONE;
        int grantState = this.grants.hasPendingWork() ? advanceGrants(expectedGeneration) : ModifierBridge.DONE;
        int retryGeneration;
        synchronized (this) {
            this.running = false;
            this.pendingCommands.removeAll(executed);
            if (commandState == ModifierBridge.INVALID) {
                this.pendingCommands.clear();
                this.workStopped = true;
                Log.w(GameModifiers.LOG_TAG, "Game console validation failed");
            }
            if (slotState == ModifierBridge.DONE || slotState == ModifierBridge.INVALID) {
                this.slotsPending = false;
            }
            if (slotState == ModifierBridge.INVALID) {
                this.workStopped = true;
                Log.w(GameModifiers.LOG_TAG, "Booster slot table validation failed");
            }
            if (grantState == ModifierBridge.INVALID) {
                this.grants.clear();
                this.workStopped = true;
                Log.w(GameModifiers.LOG_TAG, "Booster and lives grants stopped");
            }
            if (!hasPendingWork()) {
                this.finished = true;
                if (this.workStopped) {
                    Log.w(GameModifiers.LOG_TAG, "Game modifiers ended with stopped work");
                } else {
                    Log.i(GameModifiers.LOG_TAG, "Game modifiers finished");
                }
                return;
            }
            if (expectedGeneration == this.generation && !countAttempt()) {
                return;
            }
            retryGeneration = this.generation;
        }
        this.handler.postDelayed(() -> post(retryGeneration), RETRY_DELAY_MS);
    }

    private int executeCommand(String command) {
        try {
            return this.bridge.executeCommand(command);
        } catch (LinkageError | RuntimeException ex) {
            Log.w(GameModifiers.LOG_TAG, "Game console command threw");
            return ModifierBridge.INVALID;
        }
    }

    private int unlockSlots() {
        try {
            return this.bridge.unlockSlots();
        } catch (LinkageError | RuntimeException ex) {
            Log.w(GameModifiers.LOG_TAG, "Booster slot unlock threw");
            return ModifierBridge.INVALID;
        }
    }

    private int advanceGrants(int expectedGeneration) {
        try {
            return this.grants.advance(this.bridge, expectedGeneration);
        } catch (LinkageError | RuntimeException ex) {
            Log.w(GameModifiers.LOG_TAG, "Booster and lives grants threw");
            return ModifierBridge.INVALID;
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean isOutstanding(int expectedGeneration, int task) {
        return this.taskOutstanding && task == this.postedTask && expectedGeneration == this.generation;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean countAttempt() {
        if (++this.attempts < MAX_ATTEMPTS) {
            return true;
        }
        this.finished = true;
        Log.w(GameModifiers.LOG_TAG, "Game modifiers not applied, attempts=" + MAX_ATTEMPTS);
        return false;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean hasPendingWork() {
        return !this.pendingCommands.isEmpty() || this.slotsPending || this.grants.hasPendingWork();
    }

}
