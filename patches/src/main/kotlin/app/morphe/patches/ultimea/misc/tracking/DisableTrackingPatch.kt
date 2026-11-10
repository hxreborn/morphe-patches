/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.ultimea.misc.tracking

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.analytics.deactivateFirebaseAnalyticsPatch
import app.morphe.patches.shared.misc.analytics.disableCrashlyticsCollectionPatch
import app.morphe.patches.shared.misc.analytics.disableFirebaseSessionsPatch
import app.morphe.patches.shared.misc.dart.dartAotLibrary
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.patches.shared.readInt
import app.morphe.patches.shared.writeInt
import app.morphe.patches.ultimea.misc.settings.markPatchApplied
import app.morphe.patches.ultimea.misc.settings.nativeMethods
import app.morphe.patches.ultimea.misc.settings.patchesSettingsPatch

private const val INSTRUCTION_BYTES = 4
private const val RETURN_FALSE = 0x9100c2c0.toInt()
private const val BRANCH = 0x14000000
private const val BRANCH_MASK = 0xfc000000.toInt()
private const val BRANCH_IMMEDIATE_MASK = 0x03ffffff
private const val BRANCH_SIGN_SHIFT = 6
private const val BRANCH_SCALE_SHIFT = 4
private const val RETURN_NULL = 0xaa1603e0.toInt()

@Suppress("unused")
val disableTrackingPatch: BytecodePatch = bytecodePatch(
    name = "Disable tracking",
    description = "Disables Firebase Analytics, Crashlytics, Logan and usage event uploads.",
) {
    compatibleWith(AppCompatibilities.ULTIMEA)
    availability(requireArm64)
    dependsOn(
        patchesSettingsPatch,
        deactivateFirebaseAnalyticsPatch,
        disableCrashlyticsCollectionPatch,
        disableFirebaseSessionsPatch,
        resourcePatch {
            execute {
                val library = dartAotLibrary()
                library.writeBytes(library.readBytes().apply {
                    val upload = nativeMethods.single("/api/data/v1/buried_point")
                    val batch = nativeMethods.single("/api/data/v1/batch_buried_point")
                    val logan = nativeMethods.single("/logan/upload.json")
                    val asyncReturns = mutableSetOf<Int>()
                    listOf(upload to RETURN_NULL, batch to RETURN_NULL, logan to RETURN_FALSE).forEach { (function, result) ->
                        val initialized = nativeMethods.asyncBodyStart(function)
                        val completed = (initialized until function.end step INSTRUCTION_BYTES).first {
                            readInt(it) == result && readInt(it + INSTRUCTION_BYTES) and BRANCH_MASK == BRANCH
                        }
                        writeInt(initialized, BRANCH or ((completed - initialized) / INSTRUCTION_BYTES))
                        if (result == RETURN_NULL) {
                            val branch = completed + INSTRUCTION_BYTES
                            asyncReturns += branch + ((readInt(branch) shl BRANCH_SIGN_SHIFT) shr BRANCH_SCALE_SHIFT)
                        }
                    }
                    val asyncReturn = asyncReturns.singleOrNull()
                        ?: throw PatchException("Expected common Dart async return stub")
                    listOf(
                        "/api/base/v1/popup/record",
                        "/api/base/v1/app_upgrade_popup_record",
                        "/api/device/v1/firmware_upgrade_popup_record",
                    ).forEach { endpoint ->
                        val initialized = nativeMethods.asyncBodyStart(nativeMethods.single(endpoint))
                        writeInt(initialized, RETURN_NULL)
                        val branch = initialized + INSTRUCTION_BYTES
                        writeInt(branch, BRANCH or (((asyncReturn - branch) / INSTRUCTION_BYTES) and BRANCH_IMMEDIATE_MASK))
                    }
                })
            }
        },
    )
    execute {
        markPatchApplied("disableTracking", disableTrackingPatch)
        AutomaticCrashlyticsCollectionFingerprint.matchAll(1..1).single().method.returnEarly(false)
        CrashlyticsBooleanCollectionFingerprint.matchAll(1..1).single().method.addInstruction(0, "const/4 p1, 0x0")
        CrashlyticsBoxedCollectionFingerprint.matchAll(1..1).single().method.addInstruction(
            0,
            "sget-object p1, Ljava/lang/Boolean;->FALSE:Ljava/lang/Boolean;",
        )
        listOf(CrashlyticsSendReportsFingerprint, CrashlyticsLogFingerprint, CrashlyticsExceptionFingerprint).forEach {
            it.matchAll(1..1).single().method.returnEarly()
        }
    }
}
