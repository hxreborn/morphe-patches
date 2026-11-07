/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonmail.shared

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.misc.smali.descriptor
import app.morphe.patches.shared.misc.smali.replaceStub
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val UNIFFI_CALLS_CLASS = "Lapp/hxreborn/extension/protonmail/UniffiCalls;"
private const val KOTLIN_RESULTS_CLASS = "Lapp/hxreborn/extension/protonmail/KotlinResults;"
private const val THROWABLE_TYPE = "Ljava/lang/Throwable;"
private const val BUNDLED_COROUTINE_SINGLETONS = "Lkotlin/coroutines/intrinsics/CoroutineSingletons;"
private const val BUNDLED_RESULT_FAILURE = "Lkotlin/Result\$Failure;"

internal val uniffiCallsPatch = bytecodePatch {
    extendWith("extensions/extension.mpe")

    execute {
        with(
            IoDispatcherFingerprint.matchAll(1..2)
                .map { it.classDef }
                .single { "Ljava/util/concurrent/Executor;" in it.interfaces },
        ) {
            val dispatcherInstanceField = fields.single {
                it.type == type && AccessFlags.STATIC.isSet(it.accessFlags)
            }

            mutableClassDefBy(UNIFFI_CALLS_CLASS).methods
                .single { it.name == "ioDispatcher" }
                .addInstructions(
                    0,
                    """
                        sget-object v0, $type->${dispatcherInstanceField.name}:${dispatcherInstanceField.type}
                        return-object v0
                    """,
                )
        }

        val (_, suspendedField) = CoroutineSuspendedFingerprint.matchAll(1..2)
            .single { it.classDef.type != BUNDLED_COROUTINE_SINGLETONS }
            .instructionMatches
        val suspended = suspendedField.instruction.getReference<FieldReference>()!!
        val failure = ResultFailureFingerprint.matchAll(1..2)
            .single { it.classDef.type != BUNDLED_RESULT_FAILURE }
            .classDef
        val failureCause = failure.fields.single { it.type == THROWABLE_TYPE }

        with(mutableClassDefBy(KOTLIN_RESULTS_CLASS)) {
            replaceStub(
                "isCoroutineSuspended",
                2,
                """
                    sget-object v0, ${descriptor(suspended)}
                    if-ne p0, v0, :other
                    const/4 v0, 0x1
                    return v0
                    :other
                    const/4 v0, 0x0
                    return v0
                """,
            )
            replaceStub(
                "isFailure",
                2,
                """
                    instance-of v0, p0, ${failure.type}
                    return v0
                """,
            )
            replaceStub(
                "failureCause",
                1,
                """
                    check-cast p0, ${failure.type}
                    iget-object p0, p0, ${descriptor(failureCause)}
                    return-object p0
                """,
            )
        }
    }
}
