/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.terabox.misc.backup

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.StringComparisonType.STARTS_WITH
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.checkCast
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

private const val COMPACT_TITLE_STATE_WINDOW = 2

internal object BackupProviderFingerprint : Fingerprint(
    strings = listOf("state = 106 AND extra_info IN ("),
)

internal object BackupCountQueryFingerprint : Fingerprint(
    classFingerprint = BackupProviderFingerprint,
    filters = listOf(string("state = 104 OR state = 100")),
)

internal object BackupUiStateFingerprint : Fingerprint(
    strings = listOf("BackupModuleUiState(displayState="),
)

internal fun pendingBackupCountFingerprint(stateType: String) = Fingerprint(
    definingClass = stateType,
    returnType = "I",
    parameters = emptyList(),
    filters = listOf(
        fieldAccess(type = "I", opcode = Opcode.IGET),
        opcode(Opcode.RETURN, MatchAfterImmediately()),
    ),
)

internal object CompactTitleFingerprint : Fingerprint(
    filters = listOf(string("com.dubox.drive.photo.collection.components.compactTitle (BackupModuleContent.kt:", STARTS_WITH)),
)

internal fun compactBackupModuleFingerprint(stateType: String, titleMethod: String, displayStateType: String) = Fingerprint(
    filters = listOf(
        string("com.dubox.drive.photo.collection.components.CompactBackupModule (BackupModuleContent.kt:", STARTS_WITH),
        methodCall(definingClass = stateType, returnType = displayStateType),
        methodCall(titleMethod, location = MatchAfterWithin(COMPACT_TITLE_STATE_WINDOW)),
        opcode(Opcode.MOVE_RESULT_OBJECT, MatchAfterImmediately()),
    ),
)

internal fun nativeBackupTitleFingerprint(stateType: String) = Fingerprint(
    strings = listOf("album_backup_status_title_show"),
    filters = listOf(
        checkCast(stateType),
        methodCall("Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V"),
    ),
)

internal fun renderBackupTitleFingerprint(fragmentType: String, titleMethod: String, stateType: String, displayStateType: String) = Fingerprint(
    definingClass = fragmentType,
    parameters = listOf("F"),
    filters = listOf(
        checkCast(stateType),
        methodCall(definingClass = stateType, returnType = displayStateType),
        fieldAccess(type = "Ljava/lang/Integer;", opcode = Opcode.IGET_OBJECT),
        methodCall(titleMethod),
    ),
)

internal object HomeTransferMonitorFingerprint : Fingerprint(
    strings = listOf("TransferNum-backup"),
)

internal fun homeBackupQueryFingerprint(monitorType: String, queryMethod: String) = Fingerprint(
    definingClass = monitorType,
    filters = listOf(methodCall(queryMethod)),
)

internal fun homeBackupCountDispatchFingerprint(monitorType: String) = Fingerprint(
    definingClass = monitorType,
    returnType = "V",
    filters = listOf(
        methodCall("Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;"),
        methodCall("Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;"),
        methodCall("Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;"),
        methodCall("Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;"),
        methodCall("Lkotlin/jvm/functions/Function4;->invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"),
    ),
)
