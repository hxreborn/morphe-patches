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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class ScriptedBridge implements ModifierBridge {

    private final Deque<Integer> commandStates = new ArrayDeque<>();

    private final Deque<Integer> slotStates = new ArrayDeque<>();

    private final List<String> executedCommands = new ArrayList<>();

    private final Map<String, Deque<String>> outputs = new HashMap<>();

    private final Map<Integer, StringBuilder> sinkOutputs = new HashMap<>();

    private final List<Integer> usedSinks = new ArrayList<>();

    private int slotCalls;

    private Runnable onCommand = () -> {
    };

    ScriptedBridge commandStates(Integer... states) {
        this.commandStates.addAll(Arrays.asList(states));
        return this;
    }

    ScriptedBridge slotStates(Integer... states) {
        this.slotStates.addAll(Arrays.asList(states));
        return this;
    }

    ScriptedBridge output(String command, String... texts) {
        this.outputs.put(command, new ArrayDeque<>(Arrays.asList(texts)));
        return this;
    }

    ScriptedBridge onCommand(Runnable action) {
        this.onCommand = action;
        return this;
    }

    @Override
    public int executeCommand(String command) {
        this.executedCommands.add(command);
        this.onCommand.run();
        return next(this.commandStates);
    }

    @Override
    public int executeCapturing(String command, int sink) {
        this.usedSinks.add(sink);
        int state = executeCommand(command);
        Deque<String> texts = this.outputs.get(command);
        if (state == DONE && texts != null && !texts.isEmpty()) {
            appendOutput(sink, texts.size() > 1 ? texts.removeFirst() : texts.getFirst());
        }
        return state;
    }

    @Override
    public String takeOutput(int sink) {
        StringBuilder output = this.sinkOutputs.remove(sink);
        return output == null ? "" : output.toString();
    }

    void appendOutput(int sink, String text) {
        StringBuilder output = this.sinkOutputs.get(sink);
        if (output == null) {
            output = new StringBuilder();
            this.sinkOutputs.put(sink, output);
        }
        output.append(text);
    }

    List<Integer> usedSinks() {
        return this.usedSinks;
    }

    @Override
    public int unlockSlots() {
        this.slotCalls++;
        return next(this.slotStates);
    }

    List<String> executedCommands() {
        return this.executedCommands;
    }

    int slotCalls() {
        return this.slotCalls;
    }

    private static int next(Deque<Integer> states) {
        return states.size() > 1 ? states.removeFirst() : states.getFirst();
    }

}
