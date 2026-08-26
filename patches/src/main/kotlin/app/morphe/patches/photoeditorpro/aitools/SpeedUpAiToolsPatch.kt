/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.photoeditorpro.aitools

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.photoeditorpro.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.photoeditorpro.shared.AI_TASK_CLASSES
import app.morphe.patches.photoeditorpro.shared.PATCH_GATES_CLASS
import app.morphe.patches.photoeditorpro.shared.methodMatching
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstruction
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.indexOfFirstLiteralInstruction
import app.morphe.util.matchSingle
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val THREAD_SLEEP = "Ljava/lang/Thread;->sleep(J)V"
private const val STOCK_CHUNK_BYTES = 262144L
private val PLAUSIBLE_POLL_BUDGET = 1L..10_000L

@Suppress("unused")
val speedUpAiToolsPatch = bytecodePatch(
    name = "Speed up AI tools",
    description = "Shortens the AI tool wait by polling for the result more often and " +
        "uploading the photo in larger chunks.",
) {
    compatibleWith(AppCompatibilities.PHOTO_EDITOR_PRO)
    dependsOn(spoofSignaturePatch)
    extendWith("extensions/extension.mpe")

    execute {
        AI_TASK_CLASSES.forEach { task ->
            val pollMethod = mutableClassDefBy(task).methodMatching("poll") { method ->
                method.indexOfFirstInstruction {
                    opcode == Opcode.INVOKE_STATIC &&
                        getReference<MethodReference>()?.toString() == THREAD_SLEEP
                } >= 0
            }

            pollMethod.apply {
                val counterIndex = indexOfFirstInstructionOrThrow {
                    opcode == Opcode.IGET && getReference<FieldReference>()?.type == "I"
                }
                val compareIndex = indexOfFirstInstructionOrThrow(counterIndex) {
                    opcode == Opcode.IF_LE || opcode == Opcode.IF_GT
                }
                val budgetRegister =
                    getInstruction<TwoRegisterInstruction>(compareIndex).registerB
                val budgetIndex = (compareIndex - 1 downTo counterIndex).firstOrNull {
                    val instruction = getInstruction(it)
                    instruction is WideLiteralInstruction &&
                        instruction is OneRegisterInstruction &&
                        instruction.registerA == budgetRegister
                } ?: throw PatchException("No poll budget literal in $task")

                val budgetLiteral = getInstruction<WideLiteralInstruction>(budgetIndex).wideLiteral
                if (budgetLiteral !in PLAUSIBLE_POLL_BUDGET) {
                    throw PatchException(
                        "Poll budget literal $budgetLiteral out of expected range in $task",
                    )
                }

                addInstructions(
                    budgetIndex + 1,
                    """
                        invoke-static { v$budgetRegister }, $PATCH_GATES_CLASS->pollBudget(I)I
                        move-result v$budgetRegister
                    """,
                )

                val sleepIndex = indexOfFirstInstructionOrThrow {
                    opcode == Opcode.INVOKE_STATIC &&
                        getReference<MethodReference>()?.toString() == THREAD_SLEEP
                }
                val register = getInstruction<Instruction35c>(sleepIndex).registerC

                addInstructions(
                    sleepIndex,
                    """
                        invoke-static { v$register, v${register + 1} }, $PATCH_GATES_CLASS->pollIntervalMs(J)J
                        move-result-wide v$register
                    """,
                )
            }
        }

        val uploadTask = mutableClassDefBy(UploadChunkSizeFingerprint.matchSingle().classDef.type)
        val chunkSites = uploadTask.methods
            .filter { it.name == "<init>" }
            .mapNotNull { constructor ->
                val index = constructor.indexOfFirstLiteralInstruction(STOCK_CHUNK_BYTES)
                if (index < 0) null else constructor to index
            }

        if (chunkSites.isEmpty()) {
            throw PatchException("No upload chunk size literal in ${uploadTask.type}")
        }

        chunkSites.forEach { (constructor, index) ->
            constructor.apply {
                val register = getInstruction<OneRegisterInstruction>(index).registerA

                addInstructions(
                    index + 1,
                    """
                        invoke-static { v$register }, $PATCH_GATES_CLASS->uploadChunkBytes(I)I
                        move-result v$register
                    """,
                )
            }
        }
    }
}
