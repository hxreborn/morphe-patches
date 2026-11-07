/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.marks.misc.questionfilters

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val QUESTION_FORMAT_FILTER_CLASS = "Lapp/hxreborn/extension/marks/QuestionFormatFilter;"
private const val CHAPTER_QUESTIONS_CLASS = "Lcom/scoremarks/marks/data/models/marks_v_3_0/GetCPYQChapterQuestions;"
private const val CHAPTER_FILTERS_CLASS = "Lcom/scoremarks/marks/data/models/marks_v_3_0/GetSingleChapterFilterResponse;"
private const val PAGE_SIZE = 5000
private const val LIMIT_PARAMETER = 4
private const val OFFSET_PARAMETER = 5
private const val QUESTION_STRUCTURE_PARAMETER = 11

private fun RegisterRangeInstruction.parameterRegister(reference: MethodReference, parameter: Int): Int =
    startRegister + 1 + reference.parameterTypes.take(parameter).sumOf { if (it == "J" || it == "D") 2 else 1 }

@Suppress("unused")
val unlockQuestionFiltersPatch: BytecodePatch = bytecodePatch(
    name = "Unlock question filters",
    description = "Shows question formats using questions already available in the chapter.",
) {
    compatibleWith(AppCompatibilities.MARKS)
    extendWith("extensions/extension.mpe")

    execute {
        ChapterQuestionsRequestFingerprint.matchSingle().apply {
            val requestCall = instructionMatches.single().index
            val invocation = method.getInstruction<RegisterRangeInstruction>(requestCall)
            val reference = method.getInstruction(requestCall).getReference<MethodReference>()!!
            if (
                reference.parameterTypes.getOrNull(LIMIT_PARAMETER) != "Ljava/lang/Integer;" ||
                reference.parameterTypes.getOrNull(OFFSET_PARAMETER) != "Ljava/lang/Integer;" ||
                reference.parameterTypes.getOrNull(QUESTION_STRUCTURE_PARAMETER) != "Ljava/util/List;"
            ) {
                throw PatchException("MARKS chapter question parameters changed")
            }
            val limitRegister = invocation.parameterRegister(reference, LIMIT_PARAMETER)
            val offsetRegister = invocation.parameterRegister(reference, OFFSET_PARAMETER)
            val formatRegister = invocation.parameterRegister(reference, QUESTION_STRUCTURE_PARAMETER)
            method.addInstructionsWithLabels(
                requestCall,
                """
                    invoke-static/range {v$formatRegister .. v$formatRegister}, $QUESTION_FORMAT_FILTER_CLASS->select(Ljava/util/List;)Z
                    move-result v$formatRegister
                    if-eqz v$formatRegister, :keepPage
                    const/16 v$offsetRegister, $PAGE_SIZE
                    invoke-static/range {v$offsetRegister .. v$offsetRegister}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;
                    move-result-object v$limitRegister
                    const/16 v$offsetRegister, 0x0
                    :keepPage
                    const/16 v$formatRegister, 0x0
                """,
            )
        }
        GsonBuilderFingerprint.matchSingle().apply {
            val registrationCall = instructionMatches.last().index
            val builderRegister = method.getInstruction<FiveRegisterInstruction>(registrationCall).registerC
            val builderResultIndex = registrationCall + 1
            val builderResult = method.getInstruction<OneRegisterInstruction>(builderResultIndex)
            if (builderResult.opcode != Opcode.MOVE_RESULT_OBJECT || builderResult.registerA != builderRegister) {
                throw PatchException("MARKS Gson builder result changed")
            }
            val afterBuilderResult = builderResultIndex + 1
            val freeRegisters = method.getFreeRegisterProvider(afterBuilderResult, 3, builderRegister)
            val factoryRegister = freeRegisters.getFreeRegister4Bit()
            val questionsRegister = freeRegisters.getFreeRegister4Bit()
            val filtersRegister = freeRegisters.getFreeRegister4Bit()
            method.addInstructions(
                afterBuilderResult,
                """
                    new-instance v$factoryRegister, $QUESTION_FORMAT_FILTER_CLASS
                    const-class v$questionsRegister, $CHAPTER_QUESTIONS_CLASS
                    const-class v$filtersRegister, $CHAPTER_FILTERS_CLASS
                    invoke-direct {v$factoryRegister, v$questionsRegister, v$filtersRegister}, $QUESTION_FORMAT_FILTER_CLASS-><init>(Ljava/lang/Class;Ljava/lang/Class;)V
                    invoke-virtual {v$builderRegister, v$factoryRegister}, Lcom/google/gson/GsonBuilder;->registerTypeAdapterFactory(Lcom/google/gson/TypeAdapterFactory;)Lcom/google/gson/GsonBuilder;
                    move-result-object v$builderRegister
                """,
            )
        }
    }
}
