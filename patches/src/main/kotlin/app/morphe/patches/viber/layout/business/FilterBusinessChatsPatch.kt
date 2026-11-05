/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.layout.business

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.addFreeRegisters
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.viber.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.viber.misc.settings.patchesSettingsPatch
import app.morphe.patches.viber.shared.markPatchApplied
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val EXTENSION_CLASS = "Lapp/hxreborn/extension/viber/BusinessChats;"

@Suppress("unused")
val filterBusinessChatsPatch: BytecodePatch = bytecodePatch(
    name = "Filter business chats",
    description = "Filters selected business chats after reading and removes inbox discovery prompts. Shows unread chats and verification-code chats.",
) {
    compatibleWith(AppCompatibilities.VIBER)
    dependsOn(spoofSignaturePatch, patchesSettingsPatch)
    execute {
        markPatchApplied("filterBusinessChats", filterBusinessChatsPatch)
        ChatSelectionFingerprint.matchSingle().apply {
            val (selectionPrefix, _) = instructionMatches
            val register = (selectionPrefix.instruction as OneRegisterInstruction).registerA
            method.addInstructions(selectionPrefix.index + 1, """
                invoke-static/range { v$register .. v$register }, $EXTENSION_CLASS->filterSelection(Ljava/lang/String;)Ljava/lang/String;
                move-result-object v$register
            """)
        }
        val preferenceType = BusinessInboxPreferencesFingerprint.matchSingle().let {
            val (_, storePreference) = it.instructionMatches
            storePreference.instruction.getReference<FieldReference>()!!.type
        }
        businessPreferenceReaderFingerprint(preferenceType).matchSingle().apply {
            val (preferenceKey, _) = instructionMatches
            val keyRegister = (preferenceKey.instruction as OneRegisterInstruction).registerA
            val register = method.implementation!!.registerCount
            val (expanded, parameterMoves) = method.addFreeRegisters(1)
            val index = parameterMoves + preferenceKey.index + 1
            expanded.addInstructionsWithLabels(index, """
                invoke-static/range { v$keyRegister .. v$keyRegister }, $EXTENSION_CLASS->suppressPrompt(Ljava/lang/String;)Z
                move-result v$register
                if-eqz v$register, :original
                const/16 v$register, 0x0
                return v$register
            """, ExternalLabel("original", expanded.getInstruction(index)))
        }
    }
}
