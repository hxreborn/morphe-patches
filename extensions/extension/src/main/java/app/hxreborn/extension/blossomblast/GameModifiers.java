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

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

@SuppressWarnings("unused")
public final class GameModifiers implements GameThread, ModifierBridge {

    static final String LOG_TAG = "hxBlossomBlast";

    private static final String LIBRARY = "blossomblast";

    private static final GameModifiers BRIDGE = new GameModifiers();

    private static final ModifierScheduler SCHEDULER = new ModifierScheduler(new Handler(Looper.getMainLooper()),
            BRIDGE, BRIDGE, Arrays.asList(movesCommands()), slotsEnabled(),
            new ServerGrants(Arrays.asList(livesCommands()), Arrays.asList(boosterNames()), boosterTarget()));

    private static volatile boolean libraryLoaded;

    private static volatile boolean libraryMissing;

    private GameModifiers() {
    }

    public static void onNativeHandle(Object application, long handle) {
        SCHEDULER.onNativeHandle(application, handle);
    }

    private static String[] movesCommands() {
        return new String[0];
    }

    private static String[] livesCommands() {
        return new String[0];
    }

    private static String[] boosterNames() {
        return new String[0];
    }

    private static int boosterTarget() {
        return 0;
    }

    private static boolean slotsEnabled() {
        return false;
    }

    private static byte[] gameBuildId() {
        return new byte[0];
    }

    private static String engineTypeName() {
        return "";
    }

    private static String engineReference() {
        return "";
    }

    private static String boosterSlotTypeName() {
        return "";
    }

    private static int[] boosterSlotLayout() {
        return new int[0];
    }

    private static int[] boosterSlotTypes() {
        return new int[0];
    }

    private static int[] boosterSlotLevels() {
        return new int[0];
    }

    private static void runOnGameThread(Object application, Runnable task) {
    }

    @Override
    public void post(Object application, Runnable task) {
        runOnGameThread(application, task);
    }

    @Override
    public int executeCommand(String command) {
        return executeCapturing(command, NO_OUTPUT_SINK);
    }

    @Override
    public int executeCapturing(String command, int sink) {
        if (!loadLibrary()) {
            return INVALID;
        }
        return executeGameCommand(gameBuildId(), engineTypeName(), engineReference(), command, sink);
    }

    @Override
    public String takeOutput(int sink) {
        if (!loadLibrary()) {
            return "";
        }
        return takeCapturedOutput(sink);
    }

    @Override
    public int unlockSlots() {
        if (!loadLibrary()) {
            return INVALID;
        }
        return unlockGameSlots(gameBuildId(), boosterSlotTypeName(), boosterSlotLayout(), boosterSlotTypes(),
                boosterSlotLevels());
    }

    private static boolean loadLibrary() {
        if (libraryLoaded) {
            return true;
        }
        if (libraryMissing) {
            return false;
        }
        try {
            System.loadLibrary(LIBRARY);
            libraryLoaded = true;
        } catch (LinkageError | SecurityException ex) {
            libraryMissing = true;
            Log.w(LOG_TAG, "Game modifier library load failed");
        }
        return libraryLoaded;
    }

    private static native int executeGameCommand(byte[] buildId, String engineType, String engineReference,
            String command, int sink);

    private static native String takeCapturedOutput(int sink);

    private static native int unlockGameSlots(byte[] buildId, String slotType, int[] layout, int[] types, int[] levels);

}
