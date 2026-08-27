/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.photoeditorpro.misc.fix.platform

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.photoeditorpro.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.photoeditorpro.shared.markPatchInstalled
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.indexOfFirstStringInstructionOrThrow
import app.morphe.util.matchSingle
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val IOS_PLATFORM = "iOS"

@Suppress("unused")
val spoofIosPlatformPatch = bytecodePatch(
    name = "Spoof iOS platform",
    description = "Reports the AI requests as coming from the iOS app, so the server does " +
        "not ask for a Play Integrity token. Required for the AI tools on any device that " +
        "passes Play Integrity.",
) {
    compatibleWith(AppCompatibilities.PHOTO_EDITOR_PRO)
    dependsOn(spoofSignaturePatch)
    extendWith("extensions/extension.mpe")

    execute {
        AiRequestInterceptorFingerprint.matchSingle().method.apply {
            val valueIndex = indexOfFirstStringInstructionOrThrow(OBFUSCATED_ANDROID_VALUE)
            val decodedIndex = indexOfFirstInstructionOrThrow(valueIndex) {
                opcode == Opcode.MOVE_RESULT_OBJECT
            }
            val register = getInstruction<OneRegisterInstruction>(decodedIndex).registerA

            replaceInstruction(decodedIndex, "const-string v$register, \"$IOS_PLATFORM\"")
        }

        markPatchInstalled("pep_spoof_ios_platform")
    }
}
