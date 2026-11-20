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

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.os.Handler;
import android.os.Looper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.shadows.ShadowLooper;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.PAUSED)
public class ModifierSchedulerTest {

    private static final String COMMAND = "skill.add 2 5";

    private static final String SERVER_TIME_VALID = "{\"is-server-time-valid\": true}";

    private static final String SYNCED = "{\"syncResult\": \"true\"}";

    private static final long HANDLE = 0x7100;

    private static final long REPLACEMENT_HANDLE = 0x7200;

    private final Object application = new Object();

    private final Object replacementApplication = new Object();

    private final RecordingGameThread gameThread = new RecordingGameThread();

    @Before
    public void setUp() {
        ShadowLog.clear();
    }

    @Test
    public void attachedHandleAppliesCommandAndSlotsOnGameThread() {
        ScriptedBridge bridge = new ScriptedBridge().commandStates(ModifierBridge.DONE).slotStates(ModifierBridge.DONE);
        ModifierScheduler scheduler = scheduler(bridge, Collections.singletonList(COMMAND), true);
        scheduler.onNativeHandle(this.application, HANDLE);
        idleMainLooper();
        Assert.assertEquals(1, this.gameThread.postedTasks());
        Assert.assertSame(this.application, this.gameThread.application(0));
        Assert.assertTrue(bridge.executedCommands().isEmpty());
        this.gameThread.runTask(0);
        Assert.assertEquals(Collections.singletonList(COMMAND), bridge.executedCommands());
        Assert.assertEquals(1, bridge.slotCalls());
        Assert.assertTrue(scheduler.isFinished());
        Assert.assertEquals(List.of("Game modifiers finished"), logMessages());
        advance(ModifierScheduler.POST_TIMEOUT_MS);
        Assert.assertEquals(1, this.gameThread.postedTasks());
    }

    @Test
    public void repeatedHandleKeepsPostedTask() {
        ScriptedBridge bridge = new ScriptedBridge().commandStates(ModifierBridge.DONE);
        ModifierScheduler scheduler = scheduler(bridge, Collections.singletonList(COMMAND), false);
        scheduler.onNativeHandle(this.application, HANDLE);
        idleMainLooper();
        scheduler.onNativeHandle(this.application, HANDLE);
        idleMainLooper();
        Assert.assertEquals(1, this.gameThread.postedTasks());
        this.gameThread.runTask(0);
        Assert.assertEquals(Collections.singletonList(COMMAND), bridge.executedCommands());
        Assert.assertTrue(scheduler.isFinished());
    }

    @Test
    public void detachedHandleDiscardsQueuedTask() {
        ScriptedBridge bridge = new ScriptedBridge().commandStates(ModifierBridge.DONE);
        ModifierScheduler scheduler = scheduler(bridge, Collections.singletonList(COMMAND), false);
        scheduler.onNativeHandle(this.application, HANDLE);
        idleMainLooper();
        scheduler.onNativeHandle(this.application, 0);
        this.gameThread.runTask(0);
        advance(ModifierScheduler.POST_TIMEOUT_MS);
        Assert.assertTrue(bridge.executedCommands().isEmpty());
        Assert.assertEquals(1, this.gameThread.postedTasks());
        scheduler.onNativeHandle(this.replacementApplication, REPLACEMENT_HANDLE);
        idleMainLooper();
        Assert.assertEquals(2, this.gameThread.postedTasks());
        Assert.assertSame(this.replacementApplication, this.gameThread.application(1));
        this.gameThread.runTask(1);
        Assert.assertEquals(Collections.singletonList(COMMAND), bridge.executedCommands());
        Assert.assertTrue(scheduler.isFinished());
    }

    @Test
    public void droppedTaskIsPostedAgainAfterTimeout() {
        ScriptedBridge bridge = new ScriptedBridge().commandStates(ModifierBridge.DONE);
        ModifierScheduler scheduler = scheduler(bridge, Collections.singletonList(COMMAND), false);
        scheduler.onNativeHandle(this.application, HANDLE);
        idleMainLooper();
        advance(ModifierScheduler.POST_TIMEOUT_MS);
        Assert.assertEquals(2, this.gameThread.postedTasks());
        this.gameThread.runTask(1);
        this.gameThread.runTask(0);
        Assert.assertEquals(Collections.singletonList(COMMAND), bridge.executedCommands());
        Assert.assertTrue(scheduler.isFinished());
    }

    @Test
    public void unloadedLibraryIsRetriedUntilCommandRuns() {
        ScriptedBridge bridge = new ScriptedBridge().commandStates(ModifierBridge.NOT_LOADED, ModifierBridge.NOT_READY,
                ModifierBridge.DONE);
        ModifierScheduler scheduler = scheduler(bridge, Collections.singletonList(COMMAND), false);
        scheduler.onNativeHandle(this.application, HANDLE);
        idleMainLooper();
        this.gameThread.runLastTask();
        Assert.assertFalse(scheduler.isFinished());
        advance(ModifierScheduler.RETRY_DELAY_MS);
        Assert.assertEquals(2, this.gameThread.postedTasks());
        this.gameThread.runLastTask();
        advance(ModifierScheduler.RETRY_DELAY_MS);
        this.gameThread.runLastTask();
        Assert.assertEquals(3, bridge.executedCommands().size());
        Assert.assertTrue(scheduler.isFinished());
        advance(ModifierScheduler.POST_TIMEOUT_MS);
        Assert.assertEquals(3, this.gameThread.postedTasks());
    }

    @Test
    public void invalidConsoleStopsCommandsWithOneWarning() {
        ScriptedBridge bridge = new ScriptedBridge().commandStates(ModifierBridge.INVALID);
        ModifierScheduler scheduler = scheduler(bridge, Collections.singletonList(COMMAND), false);
        scheduler.onNativeHandle(this.application, HANDLE);
        idleMainLooper();
        this.gameThread.runLastTask();
        advance(ModifierScheduler.POST_TIMEOUT_MS);
        Assert.assertTrue(scheduler.isFinished());
        Assert.assertEquals(1, this.gameThread.postedTasks());
        Assert.assertEquals(1,
                ShadowLog.getLogsForTag(GameModifiers.LOG_TAG)
                    .stream()
                    .filter(log -> log.msg.equals("Game console validation failed"))
                    .count());
    }

    @Test
    public void invalidSlotTableKeepsCommandsRunning() {
        ScriptedBridge bridge = new ScriptedBridge().commandStates(ModifierBridge.NOT_READY, ModifierBridge.DONE)
            .slotStates(ModifierBridge.INVALID);
        ModifierScheduler scheduler = scheduler(bridge, Collections.singletonList(COMMAND), true);
        scheduler.onNativeHandle(this.application, HANDLE);
        idleMainLooper();
        this.gameThread.runLastTask();
        advance(ModifierScheduler.RETRY_DELAY_MS);
        this.gameThread.runLastTask();
        Assert.assertEquals(1, bridge.slotCalls());
        Assert.assertEquals(2, bridge.executedCommands().size());
        Assert.assertTrue(scheduler.isFinished());
        Assert.assertEquals(List.of("Booster slot table validation failed", "Game modifiers ended with stopped work"),
                logMessages());
    }

    @Test
    public void slotsOnlySelectionNeverRunsConsoleCommands() {
        ScriptedBridge bridge = new ScriptedBridge().slotStates(ModifierBridge.NOT_READY, ModifierBridge.DONE);
        ModifierScheduler scheduler = scheduler(bridge, Collections.emptyList(), true);
        scheduler.onNativeHandle(this.application, HANDLE);
        idleMainLooper();
        this.gameThread.runLastTask();
        advance(ModifierScheduler.RETRY_DELAY_MS);
        this.gameThread.runLastTask();
        Assert.assertTrue(bridge.executedCommands().isEmpty());
        Assert.assertEquals(2, bridge.slotCalls());
        Assert.assertTrue(scheduler.isFinished());
    }

    @Test
    public void appliedCommandIsNotRepeatedWhileSlotsWait() {
        ScriptedBridge bridge = new ScriptedBridge().commandStates(ModifierBridge.DONE)
            .slotStates(ModifierBridge.NOT_READY, ModifierBridge.DONE);
        ModifierScheduler scheduler = scheduler(bridge, Collections.singletonList(COMMAND), true);
        scheduler.onNativeHandle(this.application, HANDLE);
        idleMainLooper();
        this.gameThread.runLastTask();
        advance(ModifierScheduler.RETRY_DELAY_MS);
        this.gameThread.runLastTask();
        Assert.assertEquals(Collections.singletonList(COMMAND), bridge.executedCommands());
        Assert.assertEquals(2, bridge.slotCalls());
        Assert.assertTrue(scheduler.isFinished());
    }

    @Test
    public void replacementDuringRunRetriesOnReplacement() {
        ScriptedBridge bridge = new ScriptedBridge().commandStates(ModifierBridge.NOT_READY, ModifierBridge.DONE);
        ModifierScheduler scheduler = scheduler(bridge, Collections.singletonList(COMMAND), false);
        bridge.onCommand(() -> scheduler.onNativeHandle(this.replacementApplication, REPLACEMENT_HANDLE));
        scheduler.onNativeHandle(this.application, HANDLE);
        idleMainLooper();
        this.gameThread.runLastTask();
        idleMainLooper();
        advance(ModifierScheduler.RETRY_DELAY_MS);
        Assert.assertEquals(2, this.gameThread.postedTasks());
        Assert.assertSame(this.replacementApplication, this.gameThread.application(1));
        bridge.onCommand(() -> {
        });
        this.gameThread.runLastTask();
        Assert.assertTrue(scheduler.isFinished());
    }

    @Test
    public void oldPostFailureLeavesReplacementRunning() {
        ScriptedBridge bridge = new ScriptedBridge().commandStates(ModifierBridge.DONE);
        ModifierScheduler scheduler = scheduler(bridge, Collections.singletonList(COMMAND), false);
        this.gameThread.failPosts(this.application, 1,
                () -> scheduler.onNativeHandle(this.replacementApplication, REPLACEMENT_HANDLE));
        scheduler.onNativeHandle(this.application, HANDLE);
        idleMainLooper();
        Assert.assertEquals(1, this.gameThread.postedTasks());
        Assert.assertSame(this.replacementApplication, this.gameThread.application(0));
        this.gameThread.runTask(0);
        Assert.assertEquals(Collections.singletonList(COMMAND), bridge.executedCommands());
        Assert.assertTrue(scheduler.isFinished());
    }

    @Test
    public void rejectedPostIsRetriedOnSameHandle() {
        ScriptedBridge bridge = new ScriptedBridge().commandStates(ModifierBridge.DONE);
        ModifierScheduler scheduler = scheduler(bridge, Collections.singletonList(COMMAND), false);
        this.gameThread.failPosts(this.application, 2, () -> {
        });
        scheduler.onNativeHandle(this.application, HANDLE);
        idleMainLooper();
        advance(ModifierScheduler.RETRY_DELAY_MS);
        advance(ModifierScheduler.RETRY_DELAY_MS);
        Assert.assertEquals(1, this.gameThread.postedTasks());
        this.gameThread.runTask(0);
        Assert.assertTrue(scheduler.isFinished());
        Assert.assertEquals(1,
                ShadowLog.getLogsForTag(GameModifiers.LOG_TAG)
                    .stream()
                    .filter(log -> log.msg.equals("Game thread scheduling failed"))
                    .count());
    }

    @Test
    public void commandLinkageFailureStillUnlocksSlots() {
        ScriptedBridge bridge = new ScriptedBridge().commandStates(ModifierBridge.DONE)
            .slotStates(ModifierBridge.DONE)
            .onCommand(() -> {
                throw new UnsatisfiedLinkError("missing native method");
            });
        ModifierScheduler scheduler = scheduler(bridge, Collections.singletonList(COMMAND), true);
        scheduler.onNativeHandle(this.application, HANDLE);
        idleMainLooper();
        this.gameThread.runTask(0);
        advance(ModifierScheduler.POST_TIMEOUT_MS);
        Assert.assertTrue(scheduler.isFinished());
        Assert.assertEquals(1, this.gameThread.postedTasks());
        Assert.assertEquals(1, bridge.slotCalls());
        Assert.assertTrue(ShadowLog.getLogsForTag(GameModifiers.LOG_TAG)
            .stream()
            .anyMatch(log -> log.msg.equals("Game console command threw")));
    }

    @Test
    public void replacementDuringSyncIgnoresEarlierSyncReply() {
        ScriptedBridge bridge = new ScriptedBridge().commandStates(ModifierBridge.DONE)
            .output(ServerGrants.SERVER_TIME_COMMAND, SERVER_TIME_VALID);
        ModifierScheduler scheduler = new ModifierScheduler(new Handler(Looper.getMainLooper()), this.gameThread,
                bridge, Collections.emptyList(), false,
                new ServerGrants(Collections.singletonList(COMMAND), Collections.emptyList(), 0));
        scheduler.onNativeHandle(this.application, HANDLE);
        idleMainLooper();
        this.gameThread.runLastTask();
        scheduler.onNativeHandle(this.replacementApplication, REPLACEMENT_HANDLE);
        bridge.appendOutput(bridge.usedSinks().get(1), SYNCED);
        idleMainLooper();
        this.gameThread.runLastTask();
        Assert.assertSame(this.replacementApplication, this.gameThread.application(this.gameThread.postedTasks() - 1));
        Assert.assertFalse(bridge.executedCommands().contains(COMMAND));
        Assert.assertEquals(2, bridge.executedCommands().stream().filter(ServerGrants.SYNC_COMMAND::equals).count());
        Assert.assertFalse(scheduler.isFinished());
    }

    @Test
    public void attemptLimitStopsRetriesWithOneWarning() {
        ScriptedBridge bridge = new ScriptedBridge().commandStates(ModifierBridge.NOT_READY);
        ModifierScheduler scheduler = scheduler(bridge, Collections.singletonList(COMMAND), false);
        scheduler.onNativeHandle(this.application, HANDLE);
        idleMainLooper();
        for (int attempt = 0; attempt < ModifierScheduler.MAX_ATTEMPTS; attempt++) {
            this.gameThread.runLastTask();
            advance(ModifierScheduler.RETRY_DELAY_MS);
        }
        Assert.assertTrue(scheduler.isFinished());
        Assert.assertEquals(ModifierScheduler.MAX_ATTEMPTS, bridge.executedCommands().size());
        Assert.assertEquals(ModifierScheduler.MAX_ATTEMPTS, this.gameThread.postedTasks());
        Assert.assertEquals(1, ShadowLog.getLogsForTag(GameModifiers.LOG_TAG).size());
    }

    @Test
    public void unpatchedSelectionPostsNothing() {
        GameModifiers.onNativeHandle(this.application, HANDLE);
        idleMainLooper();
        Assert.assertTrue(ShadowLooper.shadowMainLooper().isIdle());
        Assert.assertTrue(ShadowLog.getLogsForTag(GameModifiers.LOG_TAG).isEmpty());
    }

    private static List<String> logMessages() {
        List<String> messages = new ArrayList<>();
        for (ShadowLog.LogItem item : ShadowLog.getLogsForTag(GameModifiers.LOG_TAG)) {
            messages.add(item.msg);
        }
        return messages;
    }

    private ModifierScheduler scheduler(ScriptedBridge bridge, List<String> commands, boolean slots) {
        return new ModifierScheduler(new Handler(Looper.getMainLooper()), this.gameThread, bridge, commands, slots,
                new ServerGrants(Collections.emptyList(), Collections.emptyList(), 0));
    }

    private static void idleMainLooper() {
        ShadowLooper.shadowMainLooper().idle();
    }

    private static void advance(long millis) {
        ShadowLooper.shadowMainLooper().idleFor(Duration.ofMillis(millis));
    }

}
