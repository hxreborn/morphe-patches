/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.classdojo.misc.reports

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patches.all.misc.hermes.hermesPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import app.morphe.patches.shared.misc.extension.classdojoUnlockProgressReportsExtensionPatch

private const val EXTENSION_CLASS = "Lapp/hxreborn/extension/classdojo/ReportBundle;"

private const val REPORT_CONTENT_BY_SUBSCRIPTION =
    "B0 1C 0E 5E 0B 02 09 6E 0B 03 00 0B 45 11 0B 17 3E 78 04 0B 6F 0B 13 00 11 0B AE 19 5E 11 02 08 6E 11 03 00 11 45 12 11 18 3F 78 04 11 6F 0B 13 00 12 11"
private const val DETAILED_REPORT_CONTENT =
    "B0 1C 0E 5E 0B 02 08 6E 0B 03 00 0B 45 11 0B 17 3F 78 04 0B 6F 0B 13 00 11 0B AE 19 5E 11 02 08 6E 11 03 00 11 45 12 11 18 3F 78 04 11 6F 0B 13 00 12 11"

@Suppress("unused")
val unlockProgressReportsPatch = patch(
    name = "Unlock progress reports",
    description = "Unlocks detailed progress statistics. Date and points filters keep their existing limits. " +
        "Report limits can return after in-app updates.",
) {
    dependsOn(classdojoUnlockProgressReportsExtensionPatch)
    compatibleWith(AppCompatibilities.CLASSDOJO)
    dependsOn(
        hermesPatch {
            setOf(REPORT_CONTENT_BY_SUBSCRIPTION to DETAILED_REPORT_CONTENT)
        },
    )

    execute {
        DownloadedBundleFingerprint.match().apply {
            val (_, _, logBundle) = instructionMatches
            val pathRegister = method.getInstruction<FiveRegisterInstruction>(logBundle.index).registerC
            val registers = method.getFreeRegisterProvider(logBundle.index, 3, pathRegister)
            val contextRegister = registers.getFreeRegister4Bit()
            val originalRegister = registers.getFreeRegister4Bit()
            val replacementRegister = registers.getFreeRegister4Bit()
            method.addInstructions(
                logBundle.index,
                """
                    invoke-virtual { p0 }, ${method.definingClass}->getContext()Landroid/content/Context;
                    move-result-object v$contextRegister
                    const-string v$originalRegister, "$REPORT_CONTENT_BY_SUBSCRIPTION"
                    const-string v$replacementRegister, "$DETAILED_REPORT_CONTENT"
                    invoke-static { v$contextRegister, v$pathRegister, v$originalRegister, v$replacementRegister }, $EXTENSION_CLASS->prepare(Landroid/content/Context;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
                    move-result-object v$pathRegister
                """,
            )
        }
    }
}
