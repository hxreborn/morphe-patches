/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.liftoff.misc.customizations

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.liftoff.misc.settings.markPatchApplied
import app.morphe.patches.liftoff.misc.settings.patchesSettingsPatch
import app.morphe.patches.shared.misc.reactnative.BlobResponseDataFingerprint
import app.morphe.patches.shared.misc.reactnative.NetworkResponseReceivedFingerprint
import app.morphe.patches.shared.misc.reactnative.TextResponseReceivedFingerprint
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val EXTENSION_CLASS = "Lapp/hxreborn/extension/liftoff/Customizations;"

@Suppress("unused")
val unlockCustomizationsPatch = bytecodePatch(
    name = "Unlock customizations",
    description = "Unlocks themes, profile borders and profile banners.",
) {
    dependsOn(patchesSettingsPatch)
    compatibleWith(AppCompatibilities.LIFTOFF)
    extendWith("extensions/extension.mpe")

    execute {
        markPatchApplied("unlockCustomizations")
        NetworkResponseReceivedFingerprint.matchSingle().method.addInstructions(
            0,
            "invoke-static/range { p4 .. p4 }, $EXTENSION_CLASS->onResponse(Ljava/lang/String;)V",
        )
        TextResponseReceivedFingerprint.matchSingle().method.addInstructions(
            0,
            """
                invoke-static/range { p2 .. p2 }, $EXTENSION_CLASS->rewriteBody(Ljava/lang/String;)Ljava/lang/String;
                move-result-object p2
            """,
        )
        BlobResponseDataFingerprint.matchSingle().let { match ->
            val (_, bytesResult, _) = match.instructionMatches
            val method = match.method
            val bytesRegister = method.getInstruction<OneRegisterInstruction>(bytesResult.index).registerA
            method.addInstructions(
                bytesResult.index + 1,
                """
                    invoke-static/range { v$bytesRegister .. v$bytesRegister }, $EXTENSION_CLASS->rewriteBody([B)[B
                    move-result-object v$bytesRegister
                """,
            )
        }
    }
}
