/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.misc.pagecounter

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.readera.shared.disableSignatureChecksPatch
import app.morphe.patches.readera.shared.patchesSettingsPatch
import app.morphe.patches.readera.shared.registerReadEraPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
val pageCounterInLongChaptersPatch = bytecodePatch(
    name = "Page counter in long chapters",
    description = "Keeps the page counter next to the chapter title in chapters of 1,000 pages or more.",
) {
    compatibleWith(AppCompatibilities.READERA)

    availability(requireArm64)

    dependsOn(patchesSettingsPatch, disableSignatureChecksPatch)

    execute {
        registerReadEraPatch("Page counter in long chapters", "Shows the page counter in chapters of 1,000 pages or more.")
        val pageCounter = ChapterPageCounterFingerprint.matchAll(1..1).single()
        val (pageLimit) = pageCounter.instructionMatches
        val limitRegister = (pageLimit.instruction as OneRegisterInstruction).registerA
        pageCounter.method.replaceInstruction(pageLimit.index, "const v$limitRegister, ${Int.MAX_VALUE}")
    }
}
