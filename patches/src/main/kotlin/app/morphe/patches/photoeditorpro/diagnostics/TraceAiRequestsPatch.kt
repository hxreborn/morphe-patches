/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.photoeditorpro.diagnostics

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.photoeditorpro.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.photoeditorpro.shared.EXTENSION_PACKAGE
import app.morphe.patches.photoeditorpro.shared.OK_HTTP_UTILS_CLASS
import app.morphe.patches.photoeditorpro.shared.markPatchInstalled
import app.morphe.patches.photoeditorpro.shared.methodMatching
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.matchSingle
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val AI_TRACE_CLASS = "$EXTENSION_PACKAGE/AiTrace;"
private const val OK_HTTP_BUILDER_CLASS = "Lokhttp3/OkHttpClient\$Builder;"
private const val OK_HTTP_BUILD = "$OK_HTTP_BUILDER_CLASS->build()Lokhttp3/OkHttpClient;"

@Suppress("unused")
val traceAiRequestsPatch = bytecodePatch(
    name = "Inspect AI requests",
    description = "Shows the network calls an AI tool makes, such as HTTP requests and " +
        "Firebase uploads, and keeps a log, so you can watch your photo fly to China or the US.",
) {
    compatibleWith(AppCompatibilities.PHOTO_EDITOR_PRO)
    dependsOn(spoofSignaturePatch)
    extendWith("extensions/extension.mpe")

    execute {
        mutableClassDefBy(OK_HTTP_UTILS_CLASS)
            .methodMatching("constructor") { it.name == "<init>" }
            .apply {
                val buildIndex = indexOfFirstInstructionOrThrow {
                    getReference<MethodReference>()?.toString() == OK_HTTP_BUILD
                }
                val builder = getInstruction<Instruction35c>(buildIndex).registerC

                addInstructions(
                    buildIndex,
                    "invoke-static { v$builder }, " +
                        "$AI_TRACE_CLASS->install($OK_HTTP_BUILDER_CLASS)V",
                )
            }

        StoragePutFileFingerprint.matchSingle().method.addInstructions(
            0,
            "invoke-static { p0 }, $AI_TRACE_CLASS->upload(Ljava/lang/Object;)V",
        )

        markPatchInstalled("pep_log_endpoints")
        markPatchInstalled("pep_overlay_position")
        markPatchInstalled("pep_overlay_linger")
    }
}
