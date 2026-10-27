/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.photoeditorpro.diagnostics

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.photoeditorpro.misc.fix.platform.AiRequestInterceptorFingerprint
import app.morphe.patches.photoeditorpro.misc.fix.platform.spoofIosPlatformPatch
import app.morphe.patches.photoeditorpro.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.photoeditorpro.shared.EXTENSION_PACKAGE
import app.morphe.patches.photoeditorpro.shared.markPatchInstalled
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.smali.addMethod
import app.morphe.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val AI_TRACE_CLASS = "$EXTENSION_PACKAGE/AiTrace;"
private const val AI_TRACE_INTERCEPTOR_CLASS = "$EXTENSION_PACKAGE/AiTraceInterceptor;"
private const val OBJECT = "Ljava/lang/Object;"

@Suppress("unused")
val traceAiRequestsPatch = bytecodePatch(
    name = "Inspect AI requests",
    description = "Shows the network calls an AI tool makes, such as HTTP requests and " +
        "Firebase uploads, and keeps a log.",
) {
    compatibleWith(AppCompatibilities.PHOTO_EDITOR_PRO)
    dependsOn(spoofIosPlatformPatch, spoofSignaturePatch)
    extendWith("extensions/extension.mpe")

    execute {
        val appInterceptor = AiRequestInterceptorFingerprint.matchSingle()
        val interceptorClass = appInterceptor.classDef.type
        val interceptorInterface = appInterceptor.classDef.interfaces.singleOrNull()
            ?: throw PatchException("$interceptorClass does not implement a single interface")
        val interceptMethod = appInterceptor.method

        mutableClassDefBy(AI_TRACE_INTERCEPTOR_CLASS).apply {
            interfaces.add(interceptorInterface)
            addMethod(
                interceptMethod.name,
                interceptMethod.parameterTypes.map(CharSequence::toString),
                interceptMethod.returnType,
                AccessFlags.PUBLIC.value or AccessFlags.FINAL.value,
                3,
                """
                    invoke-virtual { p0, p1 }, $AI_TRACE_INTERCEPTOR_CLASS->intercept($OBJECT)$OBJECT
                    move-result-object v0
                    check-cast v0, ${interceptMethod.returnType}
                    return-object v0
                """,
            )
        }

        requestClientInterceptorFingerprint(interceptorClass).matchSingle().let { match ->
            match.method.apply {
                val newIndex = match.instructionMatches[0].index
                val register = getInstruction<OneRegisterInstruction>(newIndex).registerA
                val constructorIndex = indexOfFirstInstructionOrThrow(newIndex) {
                    opcode == Opcode.INVOKE_DIRECT && (this as FiveRegisterInstruction).registerC == register
                }

                addInstructions(
                    constructorIndex + 1,
                    """
                        invoke-static { v$register }, $AI_TRACE_INTERCEPTOR_CLASS->wrap($OBJECT)$OBJECT
                        move-result-object v$register
                        check-cast v$register, $interceptorInterface
                    """,
                )
            }
        }

        StoragePutFileFingerprint.matchSingle().method.addInstructions(
            0,
            "invoke-static { p0 }, $AI_TRACE_CLASS->upload($OBJECT)V",
        )

        markPatchInstalled("pep_log_endpoints")
        markPatchInstalled("pep_overlay_position")
        markPatchInstalled("pep_overlay_linger")
    }
}
