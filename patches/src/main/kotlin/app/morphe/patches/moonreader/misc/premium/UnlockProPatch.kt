/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.moonreader.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import app.morphe.patches.shared.misc.extension.moonreaderUnlockProExtensionPatch

private const val PRO_PACKAGE_CONTEXT = "Lapp/hxreborn/extension/moonreader/ProPackageContext;"

@Suppress("unused")
val unlockProPatch = patch(
    name = "Unlock pro",
    description = "Unlocks Pro features, including PDF annotations and form filling, " +
        "WebDAV and FTP sync, text-to-speech controls, the home screen widget " +
        "and extra toolbar and shelf options.",
) {
    dependsOn(moonreaderUnlockProExtensionPatch)
    compatibleWith(AppCompatibilities.MOON_READER)

    dependsOn(homeScreenWidgetPatch)

    execute {
        IsProFingerprint.method.returnEarly(true)

        PdfLicenceInitFingerprint.method.addInstructions(
            0,
            """
                invoke-static { p0 }, $PRO_PACKAGE_CONTEXT->wrap(Landroid/content/Context;)Landroid/content/Context;
                move-result-object p0
            """,
        )

        FreeTextButtonFingerprint.match().let { match ->
            val moveResult = match.instructionMatches.last()
            val resultRegister = moveResult.getInstruction<OneRegisterInstruction>().registerA
            match.method.addInstruction(moveResult.index + 1, "const/4 v$resultRegister, 0x0")
        }
    }
}
