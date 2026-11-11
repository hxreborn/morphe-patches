/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.misc.sleeptimer

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getReference
import app.morphe.patches.readera.shared.disableSignatureChecksPatch
import app.morphe.patches.readera.shared.patchesSettingsPatch
import app.morphe.patches.readera.shared.registerReadEraPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.patches.shared.misc.smali.replaceMethod
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

@Suppress("unused")
val moreSleepTimerChoicesPatch = bytecodePatch(
    name = "More sleep timer choices",
    description = "Adds 5 and 10 minute options to the Read aloud sleep timer.",
) {
    compatibleWith(AppCompatibilities.READERA)

    availability(requireArm64)

    dependsOn(patchesSettingsPatch, disableSignatureChecksPatch)

    execute {
        registerReadEraPatch("More sleep timer choices", "Adds 5 and 10 minute options to the Read aloud sleep timer.")
        val timer = SleepTimerChoicesFingerprint.matchAll(1..1).single()
        val (_, _, _, _, choicesWrite) = timer.instructionMatches
        val choices = choicesWrite.instruction.getReference<FieldReference>()!!
        timer.classDef.replaceMethod(
            timer.method.name,
            1,
            """
                const/4 v0, 0x7
                new-array v0, v0, [I
                fill-array-data v0, :choices
                sput-object v0, $choices
                return-void
                :choices
                .array-data 4
                    5
                    10
                    15
                    30
                    45
                    60
                    0
                .end array-data
            """,
        )
    }
}
