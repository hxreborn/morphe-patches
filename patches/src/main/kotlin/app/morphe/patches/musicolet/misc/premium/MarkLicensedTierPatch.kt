/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.musicolet.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val STATIC_INITIALIZER = "<clinit>"
private const val REGISTERS_FOR_MARKER_WRITE = 1

internal val markLicensedTierPatch = bytecodePatch {
    execute {
        val usageQuotaCheck = UsageQuotaCheckFingerprint.matchSingle()
        val (_, licensedTierLoad, _, _, tierMarkerRead) = usageQuotaCheck.instructionMatches
        val licensedTier = (licensedTierLoad.instruction as NarrowLiteralInstruction).narrowLiteral
        val tierMarker = tierMarkerRead.instruction.getReference<FieldReference>()!!

        val staticInitializer = mutableClassDefBy(tierMarker.definingClass).methods
            .singleOrNull { it.name == STATIC_INITIALIZER }
        if (staticInitializer == null || staticInitializer.implementation!!.registerCount < REGISTERS_FOR_MARKER_WRITE) {
            throw PatchException("${tierMarker.definingClass} has no static initializer with a free register")
        }

        staticInitializer.addInstructions(
            0,
            """
                const/16 v0, $licensedTier
                invoke-static {v0}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;
                move-result-object v0
                sput-object v0, $tierMarker
            """,
        )
    }
}
