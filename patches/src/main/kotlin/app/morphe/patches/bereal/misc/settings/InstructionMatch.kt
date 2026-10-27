/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.bereal.misc.settings

import app.morphe.patcher.Match
import com.android.tools.smali.dexlib2.Opcode

internal fun Match.instructionMatch(opcode: Opcode) = instructionMatches.single { it.instruction.opcode == opcode }
