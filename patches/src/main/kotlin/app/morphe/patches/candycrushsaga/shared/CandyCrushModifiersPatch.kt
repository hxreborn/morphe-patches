/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.candycrushsaga.shared

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.mutableClassDefBy
import app.morphe.patcher.patch.packageMetadata
import app.morphe.patcher.patch.patch
import app.morphe.patcher.patch.resourceFile
import app.morphe.patcher.util.cloneParameters
import app.morphe.patches.candycrushsaga.misc.boosters.HttpResponseFingerprint
import app.morphe.patches.candycrushsaga.misc.boosters.NativeHandleSetterFingerprint
import app.morphe.patches.candycrushsaga.misc.boosters.RunOnGameThreadFingerprint
import app.morphe.patches.shared.misc.smali.descriptor
import app.morphe.patches.shared.misc.smali.replaceStub
import app.morphe.util.addInstructionsAtControlFlowLabel
import app.morphe.util.findInstructionIndicesReversedOrThrow
import app.morphe.util.inputStreamFromBundledResource
import com.android.tools.smali.dexlib2.Opcode
import app.morphe.patches.shared.misc.extension.candycrushsagaGameModifiersExtensionPatch

private const val MODIFIERS = "Lapp/hxreborn/extension/candycrushsaga/GameModifiers;"

private val addModifiersLibraryPatch = patch {
    dependsOn(candyCrushNativePatch)
    execute {
        val build = candyCrushNativeBuild(packageMetadata.packageName)
        val resource = inputStreamFromBundledResource(build.resourceDirectory, "libccsmodifiers.so")
            ?: throw PatchException("Missing Candy Crush game modifier library")
        resourceFile("lib/arm64-v8a/libccsmodifiers.so", copy = false).writeBytes(resource.use { it.readBytes() })
    }
}

internal val candyCrushModifiersPatch = patch {
    dependsOn(candycrushsagaGameModifiersExtensionPatch)
    dependsOn(addModifiersLibraryPatch)

    execute {
        val gameThread = RunOnGameThreadFingerprint.originalMethod
        mutableClassDefBy(MODIFIERS).replaceStub("runOnGameThread", 2, """
            check-cast p0, ${gameThread.definingClass}
            invoke-virtual {p0, p1}, ${descriptor(gameThread)}
            return-void
        """)
        val nativeHandleSetter = NativeHandleSetterFingerprint.method.cloneParameters()
        nativeHandleSetter.findInstructionIndicesReversedOrThrow(Opcode.RETURN_VOID).forEach { index ->
            nativeHandleSetter.addInstructionsAtControlFlowLabel(
                index,
                "invoke-static/range { p0 .. p2 }, $MODIFIERS->onNativeHandle(Landroid/app/Activity;J)V",
            )
        }
        val response = HttpResponseFingerprint.method
        response.findInstructionIndicesReversedOrThrow(Opcode.RETURN_VOID).forEach { index ->
            response.addInstructionsAtControlFlowLabel(
                index,
                "invoke-static/range { p1 .. p3 }, $MODIFIERS->onResponse(JZ)V",
            )
        }
    }
}
