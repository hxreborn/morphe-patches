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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLog;

@RunWith(RobolectricTestRunner.class)
public class ServerGrantsTest {

    private static final String LIVES_COMMAND = "unlimitedlives 86400";

    private static final List<String> BOOSTERS = Arrays.asList("shovel", "water");

    private static final int TARGET = 999;

    private static final int GENERATION = 1;

    private static final int REPLACEMENT_GENERATION = 2;

    private static final String SERVER_TIME_VALID = "{     \"is-server-time-valid\": true }";

    private static final String SERVER_TIME_INVALID = "{     \"is-server-time-valid\": false }";

    private static final String SYNCED = "{     \"syncResult\": \"true\" }";

    private static final String SYNC_FAILED = "{     \"syncResult\": \"false\" }";

    private static final String NESTED_SUCCESS = "{ \"syncResult\": \"false\", \"old\": { \"syncResult\": \"true\" } }";

    private static final String ADDED = "{     \"result\": true,     \"reason\": \"Boosters added\" }";

    private static final String NOT_ADDED = "{     \"result\": false,     \"reason\": \"Boosters added\" }";

    @Before
    public void setUp() {
        ShadowLog.clear();
    }

    @Test
    public void grantsWaitForInventorySync() {
        ScriptedBridge bridge = bridge().commandStates(ModifierBridge.DONE)
            .output(ServerGrants.SYNC_COMMAND, SYNC_FAILED);
        ServerGrants grants = grants(Collections.singletonList(LIVES_COMMAND), BOOSTERS);
        Assert.assertEquals(ModifierBridge.NOT_READY, grants.advance(bridge, GENERATION));
        Assert.assertEquals(Arrays.asList(ServerGrants.SERVER_TIME_COMMAND, ServerGrants.SYNC_COMMAND),
                bridge.executedCommands());
        Assert.assertTrue(grants.hasPendingWork());
    }

    @Test
    public void syncedInventoryTopsUpEachBoosterToTarget() {
        ScriptedBridge bridge = bridge().commandStates(ModifierBridge.DONE)
            .output(ServerGrants.SYNC_COMMAND, SYNCED)
            .output("booster.get shovel", "12")
            .output("booster.add shovel 987", ADDED)
            .output("booster.get water", "1000");
        ServerGrants grants = grants(Collections.singletonList(LIVES_COMMAND), BOOSTERS);
        Assert.assertEquals(ModifierBridge.DONE, grants.advance(bridge, GENERATION));
        Assert.assertEquals(Arrays.asList(ServerGrants.SERVER_TIME_COMMAND, ServerGrants.SYNC_COMMAND, LIVES_COMMAND,
                "booster.add shovel 0", "booster.add water 0", "booster.get shovel", "booster.add shovel 987",
                "booster.get water"), bridge.executedCommands());
        Assert.assertFalse(grants.hasPendingWork());
    }

    @Test
    public void lateSyncReplyIsAcceptedOnLaterAttempt() {
        ScriptedBridge bridge = bridge().commandStates(ModifierBridge.DONE)
            .output("booster.get shovel", "999")
            .output("booster.get water", "999");
        ServerGrants grants = grants(Collections.emptyList(), BOOSTERS);
        Assert.assertEquals(ModifierBridge.NOT_READY, grants.advance(bridge, GENERATION));
        bridge.appendOutput(bridge.usedSinks().get(1), SYNCED);
        Assert.assertEquals(ModifierBridge.DONE, grants.advance(bridge, GENERATION));
        Assert.assertEquals(1, bridge.executedCommands().stream().filter(ServerGrants.SYNC_COMMAND::equals).count());
        Assert.assertTrue(
                bridge.executedCommands().stream().noneMatch(command -> command.matches("booster.add \\w+ [1-9].*")));
    }

    @Test
    public void unansweredSyncIsRequestedAgain() {
        ScriptedBridge bridge = bridge().commandStates(ModifierBridge.DONE);
        ServerGrants grants = grants(Collections.singletonList(LIVES_COMMAND), Collections.emptyList());
        for (int wait = 0; wait <= ServerGrants.SYNC_WAIT_ATTEMPTS; wait++) {
            Assert.assertEquals(ModifierBridge.NOT_READY, grants.advance(bridge, GENERATION));
        }
        Assert.assertEquals(2, bridge.executedCommands().stream().filter(ServerGrants.SYNC_COMMAND::equals).count());
        Assert.assertFalse(bridge.executedCommands().contains(LIVES_COMMAND));
    }

    @Test
    public void unreadableBalanceStopsWithoutAdding() {
        ScriptedBridge bridge = bridge().commandStates(ModifierBridge.DONE)
            .output(ServerGrants.SYNC_COMMAND, SYNCED)
            .output("booster.get shovel", "{ \"itemId\": 32732 }");
        ServerGrants grants = grants(Collections.emptyList(), Collections.singletonList("shovel"));
        Assert.assertEquals(ModifierBridge.INVALID, grants.advance(bridge, GENERATION));
        Assert.assertTrue(
                bridge.executedCommands().stream().noneMatch(command -> command.startsWith("booster.add shovel 9")));
        Assert.assertEquals(1, ShadowLog.getLogsForTag(GameModifiers.LOG_TAG).size());
    }

    @Test
    public void failedTopUpIsRetriedWithoutRepeatingLives() {
        ScriptedBridge bridge = bridge()
            .commandStates(ModifierBridge.DONE, ModifierBridge.DONE, ModifierBridge.DONE, ModifierBridge.DONE,
                    ModifierBridge.DONE, ModifierBridge.NOT_READY, ModifierBridge.DONE)
            .output(ServerGrants.SYNC_COMMAND, SYNCED)
            .output("booster.get shovel", "0")
            .output("booster.add shovel 999", ADDED);
        ServerGrants grants = grants(Collections.singletonList(LIVES_COMMAND), Collections.singletonList("shovel"));
        Assert.assertEquals(ModifierBridge.NOT_READY, grants.advance(bridge, GENERATION));
        Assert.assertEquals(ModifierBridge.DONE, grants.advance(bridge, GENERATION));
        Assert.assertEquals(1, bridge.executedCommands().stream().filter(LIVES_COMMAND::equals).count());
        Assert.assertEquals(Arrays.asList(ServerGrants.SERVER_TIME_COMMAND, ServerGrants.SYNC_COMMAND, LIVES_COMMAND,
                "booster.add shovel 0", "booster.get shovel", "booster.add shovel 999", "booster.get shovel",
                "booster.add shovel 999"), bridge.executedCommands());
    }

    @Test
    public void nestedSuccessInsideFailedSyncKeepsWaiting() {
        ScriptedBridge bridge = bridge().commandStates(ModifierBridge.DONE)
            .output(ServerGrants.SYNC_COMMAND, NESTED_SUCCESS);
        ServerGrants grants = grants(Collections.singletonList(LIVES_COMMAND), BOOSTERS);
        Assert.assertEquals(ModifierBridge.NOT_READY, grants.advance(bridge, GENERATION));
        Assert.assertEquals(ModifierBridge.NOT_READY, grants.advance(bridge, GENERATION));
        Assert.assertEquals(Arrays.asList(ServerGrants.SERVER_TIME_COMMAND, ServerGrants.SYNC_COMMAND),
                bridge.executedCommands());
    }

    @Test
    public void syncReplyOnAnotherSinkIsIgnored() {
        ScriptedBridge bridge = bridge().commandStates(ModifierBridge.DONE);
        ServerGrants grants = grants(Collections.singletonList(LIVES_COMMAND), BOOSTERS);
        Assert.assertEquals(ModifierBridge.NOT_READY, grants.advance(bridge, GENERATION));
        bridge.appendOutput(bridge.usedSinks().get(1) + 1, SYNCED);
        Assert.assertEquals(ModifierBridge.NOT_READY, grants.advance(bridge, GENERATION));
        Assert.assertFalse(bridge.executedCommands().contains(LIVES_COMMAND));
    }

    @Test
    public void everyCaptureUsesItsOwnSink() {
        ScriptedBridge bridge = bridge().commandStates(ModifierBridge.DONE)
            .output(ServerGrants.SYNC_COMMAND, SYNCED)
            .output("booster.get shovel", "12")
            .output("booster.add shovel 987", ADDED)
            .output("booster.get water", "998")
            .output("booster.add water 1", ADDED);
        Assert.assertEquals(ModifierBridge.DONE, grants(Collections.emptyList(), BOOSTERS).advance(bridge, GENERATION));
        Assert.assertEquals(Arrays.asList(0, 1, 2, 3, 4, 5), bridge.usedSinks());
    }

    @Test
    public void unconfirmedTopUpStopsGrants() {
        ScriptedBridge bridge = bridge().commandStates(ModifierBridge.DONE)
            .output(ServerGrants.SYNC_COMMAND, SYNCED)
            .output("booster.get shovel", "12")
            .output("booster.add shovel 987", NOT_ADDED);
        ServerGrants grants = grants(Collections.emptyList(), BOOSTERS);
        Assert.assertEquals(ModifierBridge.INVALID, grants.advance(bridge, GENERATION));
        Assert.assertFalse(bridge.executedCommands().contains("booster.get water"));
        Assert.assertEquals(1, ShadowLog.getLogsForTag(GameModifiers.LOG_TAG).size());
    }

    @Test
    public void grantsWaitForValidServerTime() {
        ScriptedBridge bridge = new ScriptedBridge().commandStates(ModifierBridge.DONE)
            .output(ServerGrants.SERVER_TIME_COMMAND, SERVER_TIME_INVALID, SERVER_TIME_VALID)
            .output(ServerGrants.SYNC_COMMAND, SYNCED);
        ServerGrants grants = grants(Collections.singletonList(LIVES_COMMAND), Collections.emptyList());
        Assert.assertEquals(ModifierBridge.NOT_READY, grants.advance(bridge, GENERATION));
        Assert.assertEquals(Collections.singletonList(ServerGrants.SERVER_TIME_COMMAND), bridge.executedCommands());
        Assert.assertEquals(ModifierBridge.DONE, grants.advance(bridge, GENERATION));
        Assert.assertEquals(Arrays.asList(ServerGrants.SERVER_TIME_COMMAND, ServerGrants.SERVER_TIME_COMMAND,
                ServerGrants.SYNC_COMMAND, LIVES_COMMAND), bridge.executedCommands());
    }

    @Test
    public void usedUpSinksStopGrants() {
        ScriptedBridge bridge = new ScriptedBridge().commandStates(ModifierBridge.DONE)
            .output(ServerGrants.SERVER_TIME_COMMAND, SERVER_TIME_INVALID);
        ServerGrants grants = grants(Collections.singletonList(LIVES_COMMAND), Collections.emptyList());
        int state = ModifierBridge.NOT_READY;
        int advances = 0;
        while (state == ModifierBridge.NOT_READY) {
            state = grants.advance(bridge, GENERATION);
            advances++;
        }
        Assert.assertEquals(ModifierBridge.INVALID, state);
        Assert.assertEquals(ModifierBridge.OUTPUT_SINKS + 1, advances);
        Assert.assertEquals(ModifierBridge.OUTPUT_SINKS, bridge.usedSinks().size());
        Assert.assertFalse(bridge.executedCommands().contains(LIVES_COMMAND));
        Assert.assertEquals(1, ShadowLog.getLogsForTag(GameModifiers.LOG_TAG).size());
    }

    @Test
    public void replacementEngineIgnoresEarlierSyncReply() {
        ScriptedBridge bridge = bridge().commandStates(ModifierBridge.DONE)
            .output("booster.get shovel", "12")
            .output("booster.add shovel 987", ADDED);
        ServerGrants grants = grants(Collections.singletonList(LIVES_COMMAND), Collections.singletonList("shovel"));
        Assert.assertEquals(ModifierBridge.NOT_READY, grants.advance(bridge, GENERATION));
        bridge.appendOutput(bridge.usedSinks().get(1), SYNCED);
        Assert.assertEquals(ModifierBridge.NOT_READY, grants.advance(bridge, REPLACEMENT_GENERATION));
        Assert.assertEquals(ModifierBridge.NOT_READY, grants.advance(bridge, REPLACEMENT_GENERATION));
        Assert.assertEquals(Arrays.asList(ServerGrants.SERVER_TIME_COMMAND, ServerGrants.SYNC_COMMAND,
                ServerGrants.SERVER_TIME_COMMAND, ServerGrants.SYNC_COMMAND), bridge.executedCommands());
        bridge.appendOutput(bridge.usedSinks().get(3), SYNCED);
        Assert.assertEquals(ModifierBridge.DONE, grants.advance(bridge, REPLACEMENT_GENERATION));
        Assert.assertEquals(1, bridge.executedCommands().stream().filter(LIVES_COMMAND::equals).count());
        Assert.assertEquals(2, bridge.executedCommands().stream().filter(ServerGrants.SYNC_COMMAND::equals).count());
    }

    private static ScriptedBridge bridge() {
        return new ScriptedBridge().output(ServerGrants.SERVER_TIME_COMMAND, SERVER_TIME_VALID);
    }

    private static ServerGrants grants(List<String> lives, List<String> boosters) {
        return new ServerGrants(lives, boosters, TARGET);
    }

}
