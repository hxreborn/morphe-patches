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

interface ModifierBridge {

    int INVALID = -1;

    int NOT_LOADED = 0;

    int NOT_READY = 1;

    int DONE = 2;

    int FAILED = 3;

    int NO_OUTPUT_SINK = -1;

    int OUTPUT_SINKS = 256;

    int executeCommand(String command);

    int executeCapturing(String command, int sink);

    String takeOutput(int sink);

    int unlockSlots();

}
