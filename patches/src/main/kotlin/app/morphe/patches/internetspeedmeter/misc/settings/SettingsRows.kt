/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.internetspeedmeter.misc.settings

import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.util.getReference
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

internal fun BytecodePatchContext.removeSettingsRow(titleName: String) {
    val rowFingerprint = settingsRowFingerprint(titleName)
    val row = rowFingerprint.instructionMatches.last().instruction.getReference<FieldReference>()!!

    val addFingerprint = settingsRowAddFingerprint(row)
    addFingerprint.method.removeInstructions(addFingerprint.instructionMatches.first().index, 2)
}
