/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.terabox.misc.backup

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.smali.descriptor
import app.morphe.patches.shared.misc.smali.replaceStub
import app.morphe.patches.terabox.misc.settings.markPatchApplied
import app.morphe.patches.terabox.misc.settings.patchesSettingsPatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod

private const val EXTENSION_CLASS = "Lapp/hxreborn/extension/terabox/BackupCount;"

@Suppress("unused")
val showBackupCountPatch: BytecodePatch = bytecodePatch(
    name = "Show backup count",
    description = "Shows remaining files on Home and Photos, including paused backups.",
) {
    compatibleWith(AppCompatibilities.TERABOX)
    dependsOn(patchesSettingsPatch)

    execute {
        markPatchApplied("showBackupCount", showBackupCountPatch)
        val backupQuery = BackupCountQueryFingerprint.matchSingle()
        val pendingQuery = ImmutableMethod.of(backupQuery.method).toMutable().apply {
            setName("patch_queryPendingBackupFiles")
        }
        backupQuery.classDef.methods.add(pendingQuery)
        backupQuery.apply {
            val (selection) = instructionMatches
            val register = (selection.instruction as OneRegisterInstruction).registerA
            pendingQuery.replaceInstruction(selection.index, "invoke-static {}, $EXTENSION_CLASS->selection()Ljava/lang/String;")
            pendingQuery.addInstructions(
                selection.index + 1,
                """
                    move-result-object v$register
                """,
            )
        }
        val monitorType = HomeTransferMonitorFingerprint.matchSingle().classDef.type
        val homeQuery = homeBackupQueryFingerprint(monitorType, descriptor(backupQuery.method)).matchSingle()
        val homePendingQuery = ImmutableMethod.of(homeQuery.method).toMutable().apply {
            setName("patch_queryPendingBackupFiles")
            setAccessFlags(AccessFlags.PUBLIC.value or AccessFlags.STATIC.value)
        }
        homeQuery.classDef.methods.add(homePendingQuery)
        val (queryCall) = homeQuery.instructionMatches
        val receiver = (queryCall.instruction as FiveRegisterInstruction).registerC
        homePendingQuery.replaceInstruction(queryCall.index, "invoke-virtual { v$receiver }, ${descriptor(pendingQuery)}")
        mutableClassDefBy(EXTENSION_CLASS).replaceStub(
            "queryPendingFiles",
            1,
            """
                invoke-static {}, ${descriptor(homePendingQuery)}
                move-result-object v0
                return-object v0
            """,
        )
        homeBackupCountDispatchFingerprint(monitorType).matchAll(2..2).forEach { match ->
            val (_, _, backupCount) = match.instructionMatches
            val register = (backupCount.instruction as FiveRegisterInstruction).registerC
            match.method.addInstructions(
                backupCount.index,
                """
                    invoke-static { v$register }, $EXTENSION_CLASS->withRemainingCount(I)I
                    move-result v$register
                """,
            )
        }
        val stateType = BackupUiStateFingerprint.matchSingle().classDef.type
        val pendingCount = descriptor(pendingBackupCountFingerprint(stateType).matchSingle().method)
        val compactTitle = CompactTitleFingerprint.matchSingle().method
        compactBackupModuleFingerprint(stateType, descriptor(compactTitle), compactTitle.parameterTypes.first().toString())
            .matchSingle().apply {
                val (_, stateRead, _, titleRead) = instructionMatches
                val stateRegister = (stateRead.instruction as FiveRegisterInstruction).registerC
                val titleRegister = (titleRead.instruction as OneRegisterInstruction).registerA
                method.apply {
                    val countRegister = getFreeRegisterProvider(titleRead.index + 1, 1).getFreeRegister4Bit()
                    addInstructions(
                        titleRead.index + 1,
                        """
                            invoke-virtual { v$stateRegister }, $pendingCount
                            move-result v$countRegister
                            invoke-static { v$titleRegister, v$countRegister }, $EXTENSION_CLASS->withRemaining(Ljava/lang/String;I)Ljava/lang/String;
                            move-result-object v$titleRegister
                        """,
                    )
                }
            }
        val nativeTitle = nativeBackupTitleFingerprint(stateType).matchSingle()
        nativeTitle.apply {
            val (stateCast, titleWrite) = instructionMatches
            val stateRegister = (stateCast.instruction as OneRegisterInstruction).registerA
            val titleRegister = (titleWrite.instruction as FiveRegisterInstruction).registerD
            method.apply {
                val countRegister = getFreeRegisterProvider(titleWrite.index, 1).getFreeRegister4Bit()
                addInstructionsWithLabels(
                    titleWrite.index,
                    """
                        const/4 v$countRegister, 0x0
                        if-eqz v$stateRegister, :format_title
                        invoke-virtual { v$stateRegister }, $pendingCount
                        move-result v$countRegister
                        :format_title
                        invoke-static { v$titleRegister, v$countRegister }, $EXTENSION_CLASS->withRemaining(Ljava/lang/String;I)Ljava/lang/String;
                        move-result-object v$titleRegister
                    """,
                )
            }
        }
        renderBackupTitleFingerprint(nativeTitle.classDef.type, descriptor(nativeTitle.method), stateType, compactTitle.parameterTypes.first().toString()).matchSingle().apply {
            val (_, stateRead, cachedStatus, _) = instructionMatches
            val stateRegister = (stateRead.instruction as FiveRegisterInstruction).registerC
            val statusField = cachedStatus.instruction.getReference<FieldReference>()!!
            method.apply {
                val register = getFreeRegisterProvider(stateRead.index, 1).getFreeRegister4Bit()
                addInstructionsWithLabels(
                    stateRead.index,
                    """
                        invoke-virtual { v$stateRegister }, $pendingCount
                        move-result v$register
                        invoke-static { p0, v$register }, $EXTENSION_CLASS->shouldRefreshTitle(Ljava/lang/Object;I)Z
                        move-result v$register
                        if-eqz v$register, :original
                        const/4 v$register, 0x0
                        iput-object v$register, p0, ${descriptor(statusField)}
                    """,
                    ExternalLabel("original", getInstruction(stateRead.index)),
                )
            }
        }
    }
}
