/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.movieboxtv.misc.quality

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.movieboxtv.misc.premium.unlockPremiumPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.smali.addStaticMethod
import app.morphe.patches.shared.misc.smali.descriptor

@Suppress("unused")
val preferHighestResolutionPatch = patch(
    name = "Prefer highest resolution",
    description = "Starts videos at the highest available quality until you pick one in the player.",
) {
    compatibleWith(AppCompatibilities.MOVIEBOX_TV)
    dependsOn(unlockPremiumPatch)

    execute {
        val defaultStream = DefaultStreamFingerprint.method
        val stream = defaultStream.returnType
        val (playableStreams, savedResolution, streamResolution, _) = DefaultStreamFingerprint.instructionMatches
        val resolution = streamResolution.methodReference
        if (resolution.definingClass != stream) {
            throw PatchException("Default stream picker reads the resolution from ${resolution.definingClass}, expected $stream")
        }

        val highestStream = DefaultStreamFingerprint.classDef.addStaticMethod(
            "highestStream",
            listOf(LIST),
            stream,
            6,
            """
                invoke-interface { p0 }, $LIST->iterator()Ljava/util/Iterator;
                move-result-object v0
                const/4 v1, 0x0
                const/4 v2, -0x1
                :next
                invoke-interface { v0 }, Ljava/util/Iterator;->hasNext()Z
                move-result v3
                if-eqz v3, :done
                invoke-interface { v0 }, Ljava/util/Iterator;->next()Ljava/lang/Object;
                move-result-object v3
                check-cast v3, $stream
                invoke-virtual { v3 }, ${descriptor(resolution)}
                move-result-object v4
                :parse_start
                invoke-static { v4 }, Ljava/lang/Integer;->parseInt(Ljava/lang/String;)I
                move-result v4
                :parse_end
                .catch Ljava/lang/NumberFormatException; { :parse_start .. :parse_end } :next
                if-le v4, v2, :next
                move v2, v4
                move-object v1, v3
                goto :next
                :done
                return-object v1
            """,
        )

        defaultStream.addInstructionsWithLabels(
            0,
            """
                invoke-virtual/range { p0 .. p0 }, ${descriptor(savedResolution.methodReference)}
                move-result-object v0
                if-nez v0, :stock
                invoke-virtual/range { p0 .. p1 }, ${descriptor(playableStreams.methodReference)}
                move-result-object v0
                invoke-static { v0 }, $highestStream
                move-result-object v0
                if-eqz v0, :stock
                return-object v0
            """,
            ExternalLabel("stock", defaultStream.getInstruction(0)),
        )
    }
}
