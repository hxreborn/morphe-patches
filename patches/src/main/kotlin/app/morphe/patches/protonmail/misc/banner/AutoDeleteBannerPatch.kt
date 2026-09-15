/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonmail.misc.banner

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.protonmail.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.indexOfFirstStringInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

internal const val AUTO_DELETE_BANNER_EXTENSION_CLASS =
    "Lapp/hxreborn/extension/protonmail/AutoDeleteBanner;"

internal object AutoDeleteStatesFingerprint : Fingerprint(
    strings = listOf("AutoDeleteUpsell", "AutoDeleteDisabled", "AutoDeleteEnabled"),
)

internal object AutoDeleteBannerFingerprint : Fingerprint(
    strings = listOf("Getting the auto-delete banner failed."),
)

private fun Method.enumConstant(name: String): FieldReference =
    getInstruction<ReferenceInstruction>(
        indexOfFirstInstructionOrThrow(indexOfFirstStringInstructionOrThrow(name), Opcode.SPUT_OBJECT),
    ).reference as FieldReference

val autoDeleteBannerStatePatch = bytecodePatch {
    compatibleWith(AppCompatibilities.PROTON_MAIL)
    dependsOn(spoofSignaturePatch)

    execute {
        val stateFields = with(AutoDeleteStatesFingerprint.method) {
            setOf(
                enumConstant("AutoDeleteUpsell"),
                enumConstant("AutoDeleteDisabled"),
                enumConstant("AutoDeleteEnabled"),
            )
        }

        with(AutoDeleteBannerFingerprint.method) {
            val stateReadIndices = instructions.withIndex()
                .filter { (_, instruction) ->
                    instruction.opcode == Opcode.SGET_OBJECT &&
                        instruction.getReference<FieldReference>() in stateFields
                }
                .map { it.index }

            if (stateReadIndices.size != stateFields.size) {
                throw PatchException(
                    "Auto-delete banner state read count mismatch: expected ${stateFields.size}, " +
                        "got ${stateReadIndices.size}",
                )
            }

            stateReadIndices.reversed().forEach { index ->
                val register = getInstruction<OneRegisterInstruction>(index).registerA
                val stateType = getInstruction<ReferenceInstruction>(index)
                    .getReference<FieldReference>()!!.type

                addInstructions(
                    index + 1,
                    """
                        invoke-static/range { v$register .. v$register }, $AUTO_DELETE_BANNER_EXTENSION_CLASS->resolveState(Ljava/lang/Object;)Ljava/lang/Object;
                        move-result-object v$register
                        check-cast v$register, $stateType
                    """,
                )
            }
        }
    }
}
