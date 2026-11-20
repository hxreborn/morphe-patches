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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.util.Log;

final class ServerGrants {

    static final String SERVER_TIME_COMMAND = "time.isservertimevalid";

    static final String SERVER_TIME_VALID = "{\"is-server-time-valid\":true}";

    static final String SYNC_COMMAND = "syncServerInventory";

    static final String SYNC_SUCCESS = "{\"syncResult\":\"true\"}";

    static final String ADD_SUCCESS = "{\"result\":true,\"reason\":\"Boosters added\"}";

    static final int SYNC_WAIT_ATTEMPTS = 10;

    private static final String BALANCE_COMMAND = "booster.get ";

    private static final String ADD_COMMAND = "booster.add ";

    private static final String UNLOCK_AMOUNT = " 0";

    private static final int MAX_BALANCE_DIGITS = 9;

    private static final char QUOTE = '"';

    private final List<String> livesCommands;

    private final List<String> unlockCommands;

    private final List<String> pendingTopUps;

    private final int target;

    private final StringBuilder syncReply = new StringBuilder();

    private int nextSink;

    private int syncSink = ModifierBridge.NO_OUTPUT_SINK;

    private boolean serverTimeValid;

    private boolean synced;

    private int syncWaits;

    private int engineGeneration;

    ServerGrants(List<String> livesCommands, List<String> boosterNames, int target) {
        this.livesCommands = new ArrayList<>(livesCommands);
        this.unlockCommands = new ArrayList<>();
        for (String booster : boosterNames) {
            this.unlockCommands.add(ADD_COMMAND + booster + UNLOCK_AMOUNT);
        }
        this.pendingTopUps = new ArrayList<>(boosterNames);
        this.target = target;
    }

    boolean hasPendingWork() {
        return !this.livesCommands.isEmpty() || !this.unlockCommands.isEmpty() || !this.pendingTopUps.isEmpty();
    }

    void clear() {
        this.livesCommands.clear();
        this.unlockCommands.clear();
        this.pendingTopUps.clear();
    }

    int advance(ModifierBridge bridge, int generation) {
        if (generation != this.engineGeneration) {
            this.engineGeneration = generation;
            this.serverTimeValid = false;
            this.synced = false;
            this.syncSink = ModifierBridge.NO_OUTPUT_SINK;
        }
        if (!this.serverTimeValid) {
            int state = waitForServerTime(bridge);
            if (state != ModifierBridge.DONE) {
                return state;
            }
        }
        if (!this.synced) {
            int state = waitForSync(bridge);
            if (state != ModifierBridge.DONE) {
                return state;
            }
        }
        int state = runAll(bridge, this.livesCommands);
        if (state != ModifierBridge.DONE) {
            return state;
        }
        state = runAll(bridge, this.unlockCommands);
        if (state != ModifierBridge.DONE) {
            return state;
        }
        Iterator<String> boosters = this.pendingTopUps.iterator();
        while (boosters.hasNext()) {
            state = topUp(bridge, boosters.next());
            if (state != ModifierBridge.DONE) {
                return state;
            }
            boosters.remove();
        }
        Log.i(GameModifiers.LOG_TAG, "Lives command executed and booster top-ups accepted");
        return ModifierBridge.DONE;
    }

    private int waitForServerTime(ModifierBridge bridge) {
        int sink = takeSink();
        if (sink == ModifierBridge.NO_OUTPUT_SINK) {
            return ModifierBridge.INVALID;
        }
        int state = bridge.executeCapturing(SERVER_TIME_COMMAND, sink);
        if (state != ModifierBridge.DONE) {
            return state;
        }
        if (!isReply(bridge.takeOutput(sink), SERVER_TIME_VALID)) {
            return ModifierBridge.NOT_READY;
        }
        this.serverTimeValid = true;
        return ModifierBridge.DONE;
    }

    private int waitForSync(ModifierBridge bridge) {
        if (this.syncSink == ModifierBridge.NO_OUTPUT_SINK) {
            int sink = takeSink();
            if (sink == ModifierBridge.NO_OUTPUT_SINK) {
                return ModifierBridge.INVALID;
            }
            int state = bridge.executeCapturing(SYNC_COMMAND, sink);
            if (state != ModifierBridge.DONE) {
                return state;
            }
            this.syncSink = sink;
            this.syncReply.setLength(0);
            this.syncWaits = 0;
        }
        this.syncReply.append(bridge.takeOutput(this.syncSink));
        if (isReply(this.syncReply.toString(), SYNC_SUCCESS)) {
            this.synced = true;
            return ModifierBridge.DONE;
        }
        if (++this.syncWaits >= SYNC_WAIT_ATTEMPTS) {
            this.syncSink = ModifierBridge.NO_OUTPUT_SINK;
        }
        return ModifierBridge.NOT_READY;
    }

    private static int runAll(ModifierBridge bridge, List<String> commands) {
        Iterator<String> pending = commands.iterator();
        while (pending.hasNext()) {
            int state = bridge.executeCommand(pending.next());
            if (state != ModifierBridge.DONE) {
                return state;
            }
            pending.remove();
        }
        return ModifierBridge.DONE;
    }

    private int topUp(ModifierBridge bridge, String booster) {
        int balanceSink = takeSink();
        if (balanceSink == ModifierBridge.NO_OUTPUT_SINK) {
            return ModifierBridge.INVALID;
        }
        int state = bridge.executeCapturing(BALANCE_COMMAND + booster, balanceSink);
        if (state != ModifierBridge.DONE) {
            return state;
        }
        String balanceText = bridge.takeOutput(balanceSink).trim();
        if (balanceText.isEmpty() || balanceText.length() > MAX_BALANCE_DIGITS || !isDigits(balanceText)) {
            Log.w(GameModifiers.LOG_TAG, "Booster balance unreadable, length=" + balanceText.length());
            return ModifierBridge.INVALID;
        }
        int balance = Integer.parseInt(balanceText);
        if (balance >= this.target) {
            Log.i(GameModifiers.LOG_TAG, "Booster balance already at target, balance=" + balance);
            return ModifierBridge.DONE;
        }
        int addSink = takeSink();
        if (addSink == ModifierBridge.NO_OUTPUT_SINK) {
            return ModifierBridge.INVALID;
        }
        state = bridge.executeCapturing(ADD_COMMAND + booster + " " + (this.target - balance), addSink);
        if (state != ModifierBridge.DONE) {
            return state;
        }
        if (!isReply(bridge.takeOutput(addSink), ADD_SUCCESS)) {
            Log.w(GameModifiers.LOG_TAG, "Booster addition not accepted");
            return ModifierBridge.INVALID;
        }
        Log.i(GameModifiers.LOG_TAG,
                "Booster addition accepted, balance=" + balance + " added=" + (this.target - balance));
        return ModifierBridge.DONE;
    }

    private int takeSink() {
        if (this.nextSink >= ModifierBridge.OUTPUT_SINKS) {
            Log.w(GameModifiers.LOG_TAG, "Console output sinks used up, count=" + ModifierBridge.OUTPUT_SINKS);
            return ModifierBridge.NO_OUTPUT_SINK;
        }
        return this.nextSink++;
    }

    private static boolean isReply(String output, String expected) {
        StringBuilder compact = new StringBuilder(output.length());
        boolean quoted = false;
        for (int index = 0; index < output.length(); index++) {
            char character = output.charAt(index);
            if (character == QUOTE) {
                quoted = !quoted;
            }
            if (quoted || !Character.isWhitespace(character)) {
                compact.append(character);
            }
        }
        return compact.toString().equals(expected);
    }

    private static boolean isDigits(String text) {
        for (int index = 0; index < text.length(); index++) {
            if (!Character.isDigit(text.charAt(index))) {
                return false;
            }
        }
        return true;
    }

}
