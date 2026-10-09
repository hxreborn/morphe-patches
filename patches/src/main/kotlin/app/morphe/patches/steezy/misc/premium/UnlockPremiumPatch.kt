/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.steezy.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
import app.morphe.util.matchSingle
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val EXTENSION_CLASS = "Lapp/hxreborn/extension/steezy/PremiumInterceptor;"

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks all classes and programs. Requires a STEEZY account.",
) {
    compatibleWith(AppCompatibilities.STEEZY)
    extendWith("extensions/extension.mpe")

    execute {
        val cachedClientBuilder = CachedClientBuilderFingerprint.matchSingle()
        val clientBuilder = navigate(cachedClientBuilder.method)
            .to(cachedClientBuilder.instructionMatches.first().index)
            .stop()

        val returnIndex = clientBuilder.indexOfFirstInstructionReversedOrThrow(Opcode.RETURN_OBJECT)
        val builderRegister = clientBuilder.getInstruction<OneRegisterInstruction>(returnIndex).registerA

        clientBuilder.addInstructions(
            returnIndex,
            """
                invoke-static { v$builderRegister }, $EXTENSION_CLASS->install(Lokhttp3/OkHttpClient${'$'}Builder;)Lokhttp3/OkHttpClient${'$'}Builder;
                move-result-object v$builderRegister
            """,
        )
    }
}
