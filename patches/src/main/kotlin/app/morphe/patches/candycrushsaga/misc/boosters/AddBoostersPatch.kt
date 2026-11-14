/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.candycrushsaga.misc.boosters

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.candycrushsaga.shared.candyCrushNativePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64StandardInstall
import app.morphe.patches.shared.misc.smali.descriptor
import app.morphe.patches.shared.misc.smali.replaceStub
import app.morphe.util.addInstructionsAtControlFlowLabel
import app.morphe.util.findInstructionIndicesReversedOrThrow
import app.morphe.util.inputStreamFromBundledResource
import com.android.tools.smali.dexlib2.Opcode

private const val BOOSTERS = "Lapp/hxreborn/extension/candycrushsaga/Boosters;"

private val addBoosterLibraryPatch = resourcePatch {
    dependsOn(candyCrushNativePatch)
    execute {
        val resource = inputStreamFromBundledResource("candycrushsaga", "libccsboosters.so")
            ?: throw PatchException("Missing Candy Crush booster library")
        get("lib/arm64-v8a/libccsboosters.so", copy = false).writeBytes(resource.use { it.readBytes() })
    }
}

internal val candyCrushRewardsPatch = bytecodePatch {
    extendWith("extensions/extension.mpe")
    dependsOn(addBoosterLibraryPatch)

    execute {
        val nativeHandleSetter = NativeHandleSetterFingerprint.matchSingle().method
        val runOnGameThread = RunOnGameThreadFingerprint.matchSingle().originalMethod
        val nativeApplicationGetter = NativeApplicationGetterFingerprint.matchSingle().originalMethod
        val nativeHandleGetter = NativeHandleGetterFingerprint.matchSingle().originalMethod
        val activityType = runOnGameThread.definingClass
        mutableClassDefBy(BOOSTERS).apply {
            replaceStub("runOnGameThread", 2, """
                check-cast p0, $activityType
                invoke-virtual {p0, p1}, ${descriptor(runOnGameThread)}
                return-void
            """)
            replaceStub("getNativeHandle", 3, """
                check-cast p0, $activityType
                invoke-virtual {p0}, ${descriptor(nativeApplicationGetter)}
                move-result-object v0
                if-eqz v0, :missing
                invoke-virtual {v0}, ${descriptor(nativeHandleGetter)}
                move-result-wide v0
                return-wide v0
                :missing
                const-wide/16 v0, 0x0
                return-wide v0
            """)
        }
        nativeHandleSetter.findInstructionIndicesReversedOrThrow(Opcode.RETURN_VOID).forEach { index ->
            nativeHandleSetter.addInstructionsAtControlFlowLabel(
                index,
                "invoke-static/range { p0 .. p2 }, $BOOSTERS->onNativeHandle(Landroid/app/Activity;J)V",
            )
        }
        HttpRequestFingerprint.matchSingle().method.addInstructions(
            0,
            "invoke-static/range { p1 .. p8 }, " +
                "$BOOSTERS->onRequest(JILjava/lang/String;IZLjava/lang/Object;[B)V",
        )
        HttpDataFingerprint.matchSingle().method.addInstructions(
            0,
            "invoke-static/range { p1 .. p4 }, $BOOSTERS->onData(J[BI)V",
        )
        val response = HttpResponseFingerprint.matchSingle().method
        response.findInstructionIndicesReversedOrThrow(Opcode.RETURN_VOID).forEach { index ->
            response.addInstructionsAtControlFlowLabel(
                index,
                "invoke-static/range { p1 .. p3 }, $BOOSTERS->onResponse(JZ)V",
            )
        }
    }
}

@Suppress("unused")
val addBoostersPatch = bytecodePatch(
    name = "Add boosters",
    description = "Adds 1,000 to booster counts every time the game starts. " +
        "Each booster unlocks at its usual level. Requires a standard install.",
) {
    compatibleWith(AppCompatibilities.CANDY_CRUSH_SAGA)
    availability(requireArm64StandardInstall)
    dependsOn(candyCrushRewardsPatch)

    execute {
        mutableClassDefBy(BOOSTERS).replaceStub("boostersEnabled", 1, """
            const/4 v0, 0x1
            return v0
        """)
    }
}
