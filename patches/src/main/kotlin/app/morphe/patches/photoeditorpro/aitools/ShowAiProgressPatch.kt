/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.photoeditorpro.aitools

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.photoeditorpro.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.photoeditorpro.shared.ENHANCE_TASK_CLASS
import app.morphe.patches.photoeditorpro.shared.EXTENSION_PACKAGE
import app.morphe.patches.photoeditorpro.shared.OK_HTTP_UTILS_CLASS
import app.morphe.patches.photoeditorpro.shared.REMOVAL_TASK_CLASS
import app.morphe.patches.photoeditorpro.shared.submitRequestMethod
import app.morphe.patches.photoeditorpro.shared.markPatchInstalled
import app.morphe.patches.photoeditorpro.shared.methodMatching
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.getFreeRegisterProvider
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstruction
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.indexOfFirstLiteralInstruction
import app.morphe.util.indexOfFirstLiteralInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

private const val PROGRESS_TRACE_CLASS = "$EXTENSION_PACKAGE/ProgressTrace;"

private const val LOADING_FRAGMENT_PACKAGE =
    "Lcom/camerasideas/collagemaker/activity/fragment/loading"
private const val REMOVAL_LOADING_FRAGMENT = "$LOADING_FRAGMENT_PACKAGE/RemovalLoadingFragment;"
private const val ENHANCE_LOADING_FRAGMENT = "$LOADING_FRAGMENT_PACKAGE/EnhanceLoadingFragment;"

private const val SET_DURATION =
    "Landroid/animation/ValueAnimator;->setDuration(J)Landroid/animation/ValueAnimator;"
private const val ADD_UPDATE_LISTENER = "addUpdateListener"
private const val SET_TEXT = "Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V"
private const val THREAD_SLEEP = "Ljava/lang/Thread;->sleep(J)V"
private const val PREDICT_PATH = "/predict/"

private const val RESULT_DELIVERED = 0x400L
private const val STOCK_PROGRESS_FLOOR = 50L

private val TRACED_TOOLS = mapOf(
    REMOVAL_TASK_CLASS to (REMOVAL_LOADING_FRAGMENT to "AI Remove"),
    ENHANCE_TASK_CLASS to (ENHANCE_LOADING_FRAGMENT to "Enhance"),
)

@Suppress("unused")
val showAiProgressPatch = bytecodePatch(
    name = "Show AI progress",
    description = "Reads the current stage off the real network activity instead of the " +
        "fake progress bar InShot ships.",
) {
    compatibleWith(AppCompatibilities.PHOTO_EDITOR_PRO)
    dependsOn(spoofSignaturePatch)
    extendWith("extensions/extension.mpe")

    execute {
        val submitName = submitRequestMethod().name

        TRACED_TOOLS.forEach { (taskClass, tool) ->
            val (fragmentClass, feature) = tool
            hookTask(taskClass, feature, submitName)
            hookLoadingFragment(fragmentClass)
        }

        markPatchInstalled("pep_show_ai_progress")
    }
}

private fun BytecodePatchContext.hookTask(
    taskClass: String,
    feature: String,
    submitName: String,
) {
    val task = mutableClassDefBy(taskClass)

    val runMethod = task.methodMatching("run") {
        it.name == "run" && it.parameterTypes.isEmpty() && it.returnType == "V"
    }
    val register = runMethod.getFreeRegisterProvider(0, 1).getFreeRegister4Bit()
    runMethod.addInstructions(
        0,
        """
            const-string v$register, "$feature"
            invoke-static { v$register }, $PROGRESS_TRACE_CLASS->beginUpload(Ljava/lang/String;)V
        """,
    )

    task.methodMatching("predict") {
        it.indexOfFirstInstruction {
            val reference = getReference<MethodReference>()
            reference?.definingClass == OK_HTTP_UTILS_CLASS && reference.name == submitName
        } >= 0 &&
            it.indexOfFirstInstruction {
                getReference<StringReference>()?.string?.contains(PREDICT_PATH) == true
            } >= 0
    }.addInstructions(0, "invoke-static { }, $PROGRESS_TRACE_CLASS->predict()V")

    task.methodMatching("polling") {
        it.indexOfFirstInstruction { getReference<MethodReference>()?.toString() == THREAD_SLEEP } >= 0
    }.addInstructions(0, "invoke-static { }, $PROGRESS_TRACE_CLASS->polling()V")

    task.methodMatching("result handler") {
        it.parameterTypes.firstOrNull()?.toString() == "I" && it.returnType == "V" &&
            it.indexOfFirstLiteralInstruction(RESULT_DELIVERED) >= 0
    }.addInstructions(0, "invoke-static { p1 }, $PROGRESS_TRACE_CLASS->finished(I)V")
}

private fun BytecodePatchContext.hookLoadingFragment(fragmentClass: String) {
    val fragment = mutableClassDefBy(fragmentClass)

    val progressMethod = fragment.methodMatching("progress animator") {
        it.parameterTypes.map(CharSequence::toString) == listOf("I") &&
            it.returnType == "V" &&
            it.indexOfFirstInstruction { getReference<MethodReference>()?.toString() == SET_DURATION } >= 0
    }

    val listenerType = progressMethod.run {
        val listenerIndex = indexOfFirstInstructionOrThrow {
            opcode == Opcode.INVOKE_VIRTUAL &&
                getReference<MethodReference>()?.name == ADD_UPDATE_LISTENER
        }
        val newIndex = (listenerIndex - 1 downTo 0).firstOrNull {
            getInstruction(it).opcode == Opcode.NEW_INSTANCE
        } ?: throw PatchException("No listener allocation before $ADD_UPDATE_LISTENER")

        getInstruction(newIndex).getReference<TypeReference>()!!.type
    }

    progressMethod.apply {
        val durationIndex = indexOfFirstInstructionOrThrow {
            getReference<MethodReference>()?.toString() == SET_DURATION
        }
        val durationRegister = getInstruction<Instruction35c>(durationIndex).registerD

        addInstructions(
            durationIndex,
            """
                invoke-static { v$durationRegister, v${durationRegister + 1} }, $PROGRESS_TRACE_CLASS->progressDurationMs(J)J
                move-result-wide v$durationRegister
            """,
        )

        val floorIndex = indexOfFirstLiteralInstructionOrThrow(STOCK_PROGRESS_FLOOR)
        val floorRegister = getInstruction<OneRegisterInstruction>(floorIndex).registerA
        replaceInstruction(floorIndex, "const/16 v$floorRegister, 0x0")

        addInstructions(
            0,
            """
                invoke-static { p1 }, $PROGRESS_TRACE_CLASS->progress(I)I
                move-result p1
            """,
        )
    }

    mutableClassDefBy(listenerType).methodMatching("label") {
        it.indexOfFirstInstruction { getReference<MethodReference>()?.toString() == SET_TEXT } >= 0
    }.apply {
        val index = indexOfFirstInstructionOrThrow {
            getReference<MethodReference>()?.toString() == SET_TEXT
        }
        val setText = getInstruction<Instruction35c>(index)
        val view = setText.registerC
        val register = setText.registerD

        addInstructions(
            index,
            """
                invoke-static { v$view, v$register }, $PROGRESS_TRACE_CLASS->label(Landroid/widget/TextView;Ljava/lang/CharSequence;)Ljava/lang/CharSequence;
                move-result-object v$register
            """,
        )
    }
}
