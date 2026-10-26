/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.pleco.misc.addons

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.shared.compat.AppCompatibilities

private val unlockedPurchaseIds = listOf(
    0x8002F001.toInt(),
    0x80002001.toInt(),
    0x80003001.toInt(),
    0x80033001.toInt(),
)

private val unlockNativeAddOnFlagsPatch = rawResourcePatch {
    execute {
        val library = get(AddOnFlags.LIBRARY, true)
        if (!library.exists()) throw PatchException("${AddOnFlags.LIBRARY} not found in the APK")
        val bytes = library.readBytes()
        AddOnFlags.forceOwnedFlags(bytes)
        library.writeBytes(bytes)
    }
}

@Suppress("unused")
val unlockAddOnsPatch = bytecodePatch(
    name = "Unlock add-ons",
    description = "Unlocks the Flashcard System, Document Reader, Optical Character Recognizer and " +
        "Enhanced Handwriting Recognizer. Excludes paid dictionaries, audio and voices.",
) {
    compatibleWith(AppCompatibilities.PLECO)

    dependsOn(unlockNativeAddOnFlagsPatch)

    execute {
        val method = PurchaseOwnedFingerprint.matchSingle().method
        val constantRegister = method.getFreeRegisterProvider(0, 1).getFreeRegister4Bit()
        val checks = unlockedPurchaseIds.joinToString("\n") { id ->
            """
                const v$constantRegister, $id
                if-eq p1, v$constantRegister, :owned
            """
        }
        method.addInstructionsWithLabels(
            0,
            """
                $checks
                goto :original
                :owned
                const/4 v$constantRegister, 0x1
                return v$constantRegister
            """,
            ExternalLabel("original", method.getInstruction(0)),
        )
    }
}
