/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.raindrop.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.raindrop.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.matchSingle
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val REACT_CONTEXT_TYPE = "Lcom/facebook/react/bridge/ReactApplicationContext;"
private const val EXTENSION_CLASS = "Lapp/hxreborn/extension/raindrop/ProUnlock;"

@Suppress("unused")
val unlockProPatch = bytecodePatch(
    name = "Unlock pro",
    description = "Unlocks reminders and highlight notes. Adds duplicate and broken link filters, collection and tag " +
        "suggestions, full-text search of saved pages, Wayback Machine copies and a weekly bookmark export to Downloads. " +
        "Requires a signed-in account.",
) {
    compatibleWith(AppCompatibilities.RAINDROP)
    dependsOn(spoofSignaturePatch)
    extendWith("extensions/extension.mpe")

    execute {
        ResponseHandlerFingerprint.matchSingle().apply {
            val index = instructionMatches[0].index
            val register = method.getInstruction<FiveRegisterInstruction>(index).registerD
            val contextField = classDef.fields.single { it.type == REACT_CONTEXT_TYPE }

            method.addInstructions(
                index,
                "invoke-static { v$register }, $EXTENSION_CLASS->rewriteResponse([B)[B\n" +
                    "move-result-object v$register",
            )
            method.addInstructions(
                0,
                "iget-object v0, p0, $contextField\n" +
                    "invoke-static { v0, p2 }, " +
                    "$EXTENSION_CLASS->onResponse(Landroid/content/Context;Ljava/lang/Object;)V",
            )
        }

        SendRequestFingerprint.matchSingle().apply {
            val bodyIndex = instructionMatches[1].index + 1
            val bodyRegister = method.getInstruction<OneRegisterInstruction>(bodyIndex).registerA

            method.addInstructions(
                bodyIndex + 1,
                "invoke-static { v$bodyRegister }, $EXTENSION_CLASS->replaceRequestBody(" +
                    "Ljava/lang/String;)Ljava/lang/String;\n" +
                    "move-result-object v$bodyRegister",
            )
            method.addInstructions(
                0,
                "invoke-static/range { p1 .. p5 }, $EXTENSION_CLASS->onRequest(" +
                    "Ljava/lang/String;Ljava/lang/String;ILjava/lang/Object;Ljava/lang/Object;)Ljava/lang/String;\n" +
                    "move-result-object p2",
            )
        }

        WebViewSourceFingerprint.matchSingle().apply {
            val index = instructionMatches[0].index
            val loadUrl = method.getInstruction<FiveRegisterInstruction>(index)
            val urlRegister = loadUrl.registerD
            val headersRegister = loadUrl.registerE

            method.addInstructions(
                index,
                "invoke-static { v$urlRegister, v$headersRegister }, $EXTENSION_CLASS->archiveHeaders(" +
                    "Ljava/lang/String;Ljava/util/Map;)Ljava/util/Map;\n" +
                    "move-result-object v$headersRegister\n" +
                    "invoke-static { v$urlRegister }, $EXTENSION_CLASS->archiveUrl(" +
                    "Ljava/lang/String;)Ljava/lang/String;\n" +
                    "move-result-object v$urlRegister",
            )
        }
    }
}
