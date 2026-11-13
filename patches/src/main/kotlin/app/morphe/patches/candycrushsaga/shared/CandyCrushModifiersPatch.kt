/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.candycrushsaga.shared

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.candycrushsaga.misc.boosters.HttpResponseFingerprint
import app.morphe.patches.candycrushsaga.misc.boosters.NativeHandleSetterFingerprint
import app.morphe.patches.candycrushsaga.misc.boosters.RunOnGameThreadFingerprint
import app.morphe.patches.shared.misc.smali.descriptor
import app.morphe.patches.shared.misc.smali.replaceStub
import app.morphe.util.addInstructionsAtControlFlowLabel
import app.morphe.util.findInstructionIndicesReversedOrThrow
import app.morphe.util.inputStreamFromBundledResource
import com.android.tools.smali.dexlib2.Opcode

private const val MODIFIERS = "Lapp/hxreborn/extension/candycrushsaga/GameModifiers;"

private val addModifiersLibraryPatch = resourcePatch {
    dependsOn(candyCrushNativePatch)
    execute {
        val resource = inputStreamFromBundledResource("candycrushsaga", "libccsmodifiers.so")
            ?: throw PatchException("Missing Candy Crush game modifier library")
        get("lib/arm64-v8a/libccsmodifiers.so", copy = false).writeBytes(resource.use { it.readBytes() })
    }
}

internal val candyCrushModifiersPatch = bytecodePatch {
    extendWith("extensions/extension.mpe")
    dependsOn(addModifiersLibraryPatch)

    execute {
        val gameThread = RunOnGameThreadFingerprint.matchAll(1..1).single().originalMethod
        mutableClassDefBy(MODIFIERS).replaceStub("runOnGameThread", 2, """
            check-cast p0, ${gameThread.definingClass}
            invoke-virtual {p0, p1}, ${descriptor(gameThread)}
            return-void
        """)
        val setter = NativeHandleSetterFingerprint.matchAll(1..1).single().method
        setter.findInstructionIndicesReversedOrThrow(Opcode.RETURN_VOID).forEach { index ->
            setter.addInstructionsAtControlFlowLabel(
                index,
                "invoke-static/range { p0 .. p2 }, $MODIFIERS->onNativeHandle(Landroid/app/Activity;J)V",
            )
        }
        val response = HttpResponseFingerprint.matchAll(1..1).single().method
        response.findInstructionIndicesReversedOrThrow(Opcode.RETURN_VOID).forEach { index ->
            response.addInstructionsAtControlFlowLabel(
                index,
                "invoke-static/range { p1 .. p3 }, $MODIFIERS->onResponse(JZ)V",
            )
        }
    }
}
