/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.misc.gms

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.gms.gmsCoreSupportPatchFor
import app.morphe.patches.viber.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.viber.shared.markPatchApplied
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val EXTENSION_CLASS = "Lapp/hxreborn/extension/viber/ChooseAccountIntent;"

private val systemAccountChooserPatch = bytecodePatch {
    extendWith("extensions/extension.mpe")

    execute {
        markPatchApplied("gmsCoreSupport", gmsCoreSupportPatch)
        GmsAccountPickerIntentFingerprint.matchSingle().method.apply {
            val returnIndex = indexOfFirstInstructionReversedOrThrow(Opcode.RETURN_OBJECT)
            val intentRegister = getInstruction<OneRegisterInstruction>(returnIndex).registerA

            addInstructions(
                returnIndex,
                """
                    invoke-static/range { v$intentRegister .. v$intentRegister }, $EXTENSION_CLASS->fromGmsPickerIntent(Landroid/content/Intent;)Landroid/content/Intent;
                    move-result-object v$intentRegister
                """,
            )
        }
    }
}

@Suppress("unused")
val gmsCoreSupportPatch: BytecodePatch =
    gmsCoreSupportPatchFor(AppCompatibilities.VIBER, spoofSignaturePatch, systemAccountChooserPatch)
