/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.bereal.misc.download

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.bereal.misc.settings.BEREAL_EXTENSION_PACKAGE
import app.morphe.patches.bereal.misc.settings.instructionMatch
import app.morphe.patches.bereal.misc.settings.settingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.setExtensionIsPatchIncluded
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

private const val EXTENSION_CLASS = "${BEREAL_EXTENSION_PACKAGE}download/DownloadFolder;"

@Suppress("unused")
val changeDownloadLocationPatch = bytecodePatch(
    name = "Change download location",
    description = "Saves downloaded BeReals to Pictures/BeReal, DCIM/BeReal or a folder you pick. " +
        "Requires Android 10 or newer.",
) {
    compatibleWith(AppCompatibilities.BEREAL)
    dependsOn(settingsPatch)

    execute {
        setExtensionIsPatchIncluded(EXTENSION_CLASS)

        val match = MediaStoreInsertFingerprint.matchSingle()
        val insertIndex = match.instructionMatch(Opcode.INVOKE_VIRTUAL).index
        val insert = match.method.getInstruction<FiveRegisterInstruction>(insertIndex)
        match.method.replaceInstruction(
            insertIndex,
            "invoke-static { v${insert.registerC}, v${insert.registerD}, v${insert.registerE} }, " +
                "$EXTENSION_CLASS->insert(Landroid/content/ContentResolver;Landroid/net/Uri;" +
                "Landroid/content/ContentValues;)Landroid/net/Uri;",
        )
    }
}
