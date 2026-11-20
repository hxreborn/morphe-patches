/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.terabox.misc.sort

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.terabox.misc.settings.ApplicationCreateFingerprint
import app.morphe.patches.terabox.misc.settings.markPatchApplied
import app.morphe.patches.terabox.misc.settings.patchesSettingsPatch
import app.morphe.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.BuilderOffsetInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ThreeRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import app.morphe.patches.shared.misc.extension.teraboxSortByOriginalDateExtensionPatch

private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/hxreborn/extension/terabox/OriginalDateSort;"
private const val UTILS_CLASS = "Lapp/morphe/extension/shared/Utils;"
private const val BUILT_IN_SORT_ORDER_RETURNS = 9
private const val FILE_ROW_CHANGE_CALLBACKS = 2

@Suppress("unused")
val sortByOriginalDatePatch: BytecodePatch = bytecodePatch(
    name = "Sort by original date",
    description = "Sorts and displays files by their original modification dates.",
) {
    dependsOn(teraboxSortByOriginalDateExtensionPatch)
    compatibleWith(AppCompatibilities.TERABOX)
    dependsOn(patchesSettingsPatch)

    execute {
        markPatchApplied("sortByOriginalDate", sortByOriginalDatePatch)

        ApplicationCreateFingerprint.matchSingle().method.apply {
            val contextSet = indexOfFirstInstructionOrThrow {
                val reference = (this as? ReferenceInstruction)?.reference as? MethodReference
                reference?.definingClass == UTILS_CLASS && reference.name == "setContext"
            }
            addInstruction(contextSet + 1, "invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->loadProcessSetting()V")
        }

        SortTypeOrderFingerprint.matchSingle().apply {
            val (defaultOrderName) = instructionMatches
            val builtInReturns = method.instructions.withIndex()
                .filter { (index, instruction) -> index > defaultOrderName.index && instruction.opcode == Opcode.RETURN_OBJECT }
                .map { it.index }
            if (builtInReturns.size != BUILT_IN_SORT_ORDER_RETURNS) {
                throw PatchException("Expected $BUILT_IN_SORT_ORDER_RETURNS built-in sort order returns, found ${builtInReturns.size}")
            }
            builtInReturns.asReversed().forEach { index ->
                val register = method.getInstruction<OneRegisterInstruction>(index).registerA
                method.addInstructions(
                    index,
                    """
                        invoke-static/range { v$register .. v$register }, $EXTENSION_CLASS_DESCRIPTOR->timeSortOrder(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$register
                    """,
                )
            }
        }

        FileRowBindFingerprint.matchSingle().apply {
            val (_, uploadTimeRead) = instructionMatches
            method.useOriginalTimeAfter(uploadTimeRead.index)
        }

        PlaylistTimeComparatorFingerprint.matchSingle().apply {
            val (firstUploadTimeRead, secondUploadTimeRead) = instructionMatches
            method.useOriginalTimeAfter(secondUploadTimeRead.index)
            method.useOriginalTimeAfter(firstUploadTimeRead.index)
        }

        val rowChanges = FileRowChangeFingerprint.matchAll(FILE_ROW_CHANGE_CALLBACKS..FILE_ROW_CHANGE_CALLBACKS)
        rowChanges.forEach { match ->
            val (oldTimeRead, newTimeRead, timeCompare, timeChanged) = match.instructionMatches
            match.method.apply {
                val oldTime = getInstruction<TwoRegisterInstruction>(oldTimeRead.index)
                val newTime = getInstruction<TwoRegisterInstruction>(newTimeRead.index)
                val compareRegister = getInstruction<ThreeRegisterInstruction>(timeCompare.index).registerA
                val changedTarget = getInstruction<BuilderOffsetInstruction>(timeChanged.index).target.location.instruction!!
                val afterTimeCompare = timeChanged.index + 1
                addInstructionsWithLabels(
                    afterTimeCompare,
                    """
                        invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->sortsByOriginalDate()Z
                        move-result v$compareRegister
                        if-eqz v$compareRegister, :stock_compare
                        iget-wide v${oldTime.registerA}, v${oldTime.registerB}, $CLOUD_FILE_CLASS->localMTime:J
                        iget-wide v${newTime.registerA}, v${newTime.registerB}, $CLOUD_FILE_CLASS->localMTime:J
                        cmp-long v$compareRegister, v${oldTime.registerA}, v${newTime.registerA}
                        if-nez v$compareRegister, :original_time_changed
                    """,
                    ExternalLabel("stock_compare", getInstruction(afterTimeCompare)),
                    ExternalLabel("original_time_changed", changedTarget),
                )
            }
        }
    }
}

private fun MutableMethod.useOriginalTimeAfter(uploadTimeReadIndex: Int) {
    val uploadTimeRead = getInstruction<TwoRegisterInstruction>(uploadTimeReadIndex)
    val timeRegister = uploadTimeRead.registerA
    val fileRegister = uploadTimeRead.registerB
    val afterRead = uploadTimeReadIndex + 1
    val usesOriginalTimeRegister = getFreeRegisterProvider(afterRead, 1, timeRegister, timeRegister + 1, fileRegister).getFreeRegister()
    addInstructionsWithLabels(
        afterRead,
        """
            iget-wide v$timeRegister, v$fileRegister, $CLOUD_FILE_CLASS->localMTime:J
            invoke-static/range { v$timeRegister .. v${timeRegister + 1} }, $EXTENSION_CLASS_DESCRIPTOR->usesOriginalTime(J)Z
            move-result v$usesOriginalTimeRegister
            if-nez v$usesOriginalTimeRegister, :original_time
            iget-wide v$timeRegister, v$fileRegister, $CLOUD_FILE_CLASS->serverMTime:J
        """,
        ExternalLabel("original_time", getInstruction(afterRead)),
    )
}
