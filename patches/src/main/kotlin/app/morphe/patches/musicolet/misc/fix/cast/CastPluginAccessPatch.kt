/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.musicolet.misc.fix.cast

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val CAST_PLUGIN_PERMISSION = "in.krosbits.musicolet.ALLOW_CAST_AUDIO"
private const val OUTDATED_PLUGIN_VERSION = 1

val castPluginAccessPatch = bytecodePatch {
    compatibleWith(AppCompatibilities.MUSICOLET)

    execute {
        CastPluginVersionFingerprint.matchSingle().apply {
            val applicationField = instructionMatches.first().instruction.getReference<FieldReference>()!!
            val versionRead = instructionMatches.last()
            val versionRegister = versionRead.getInstruction<TwoRegisterInstruction>().registerA
            val insertIndex = versionRead.index + 1
            val registers = method.getFreeRegisterProvider(insertIndex, 2)
            val contextRegister = registers.getFreeRegister4Bit()
            val permissionRegister = registers.getFreeRegister4Bit()

            method.addInstructionsWithLabels(
                insertIndex,
                """
                    sget-object v$contextRegister, $applicationField
                    const-string v$permissionRegister, "$CAST_PLUGIN_PERMISSION"
                    invoke-virtual { v$contextRegister, v$permissionRegister }, Landroid/content/Context;->checkSelfPermission(Ljava/lang/String;)I
                    move-result v$contextRegister
                    if-eqz v$contextRegister, :granted
                    const/4 v$versionRegister, $OUTDATED_PLUGIN_VERSION
                """,
                ExternalLabel("granted", method.getInstruction(insertIndex)),
            )
        }
    }
}
