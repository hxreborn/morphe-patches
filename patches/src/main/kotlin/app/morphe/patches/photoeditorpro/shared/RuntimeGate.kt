/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.photoeditorpro.shared

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.util.getFreeRegisterProvider

internal const val STRING = "Ljava/lang/String;"
internal const val EXTENSION_PACKAGE = "Lapp/hxreborn/extension/photoeditorpro"
internal const val PATCH_GATES_CLASS = "$EXTENSION_PACKAGE/PatchGates;"
internal const val PATCH_PANEL_CLASS = "$EXTENSION_PACKAGE/PatchPanel;"
internal const val OK_HTTP_UTILS_CLASS =
    "Lcom/camerasideas/collagemaker/network/OkHttpUtils;"
internal const val REMOVAL_TASK_CLASS =
    "Lcom/camerasideas/collagemaker/model/removal/ImageRemovalGCTask;"
internal const val ENHANCE_TASK_CLASS =
    "Lcom/camerasideas/collagemaker/model/enhancer/ImageEnhanceGCTask;"

internal val AI_TASK_CLASSES = listOf(
    REMOVAL_TASK_CLASS,
    ENHANCE_TASK_CLASS,
    "Lcom/camerasideas/collagemaker/model/sketch/ImageSketchGCTask;",
    "Lcom/camerasideas/collagemaker/model/segmentation/ImageSegmentationTask;",
    "Lcom/camerasideas/collagemaker/activity/fragment/imagefragment/cutout/" +
        "ImageAiCutoutGCTask;",
)

internal fun MutableClass.methodMatching(role: String, predicate: (MutableMethod) -> Boolean): MutableMethod =
    methods.singleOrNull(predicate)
        ?: throw PatchException("No single $role method in $type")

internal fun BytecodePatchContext.submitRequestMethod(): MutableMethod =
    mutableClassDefBy(OK_HTTP_UTILS_CLASS).methodMatching("submit request") { method ->
        val parameters = method.parameterTypes.map(CharSequence::toString)
        method.returnType == "V" && parameters.size == 3 &&
            parameters[0] == STRING && parameters[2].startsWith("[")
    }

internal fun MutableMethod.returnTrueWhileEnabled(gateMethod: String) {
    val register = getFreeRegisterProvider(0, 1).getFreeRegister4Bit()

    addInstructionsWithLabels(
        0,
        """
            invoke-static { }, $PATCH_GATES_CLASS->$gateMethod()Z
            move-result v$register
            if-eqz v$register, :stock
            const/4 v$register, 0x1
            return v$register
            :stock
            nop
        """,
    )
}

internal fun BytecodePatchContext.markPatchInstalled(settingKey: String) {
    mutableClassDefBy(PATCH_PANEL_CLASS)
        .methods.single { it.name == "<clinit>" }
        .apply {
            val end = implementation!!.instructions.count() - 1
            val register = getFreeRegisterProvider(end, 1).getFreeRegister4Bit()

            addInstructions(
                end,
                """
                    const-string v$register, "$settingKey"
                    invoke-static { v$register }, $PATCH_PANEL_CLASS->markInstalled(Ljava/lang/String;)V
                """,
            )
        }
}
