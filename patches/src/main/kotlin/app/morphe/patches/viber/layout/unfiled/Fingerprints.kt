/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.layout.unfiled

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal object FolderEntityFingerprint : Fingerprint(
    strings = listOf("FolderEntity(id="),
)

internal object FolderTabEntityFingerprint : Fingerprint(
    strings = listOf("FolderWithUnreadCount(entity="),
)

internal object FolderTabsFingerprint : Fingerprint(
    returnType = "Ljava/lang/Object;",
    filters = listOf(
        string("9ea5e582-4ea7-41a7-9124-f4566dc01d88"),
        methodCall(definingClass = "Ljava/util/ArrayList;", name = "isEmpty"),
    ),
)

internal object FolderSelectionFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("L", "I"),
    strings = listOf("9ea5e582-4ea7-41a7-9124-f4566dc01d88"),
)

internal object FolderQueryFingerprint : Fingerprint(
    returnType = "Ljava/lang/String;",
    strings = listOf("[conversations].[_id] in (select conversation_id from folders_to_chats where folder_id = '"),
)

internal object EmptyFolderViewFingerprint : Fingerprint(
    strings = listOf("titleView"),
)

internal fun folderConstructorFingerprint(folderType: String) = Fingerprint(
    definingClass = folderType,
    name = "<init>",
    parameters = listOf("Ljava/lang/String;", "Ljava/lang/String;", "I", "I", "J", "Ljava/lang/String;"),
)

internal fun folderTabConstructorFingerprint(tabType: String, folderType: String) = Fingerprint(
    definingClass = tabType,
    name = "<init>",
    parameters = listOf(folderType, "I"),
    filters = listOf(methodCall(definingClass = folderType, parameters = listOf(), returnType = "Ljava/lang/String;")),
)

internal object UnreadFolderFingerprint : Fingerprint(
    returnType = "Z",
    filters = listOf(
        fieldAccess(type = "Ljava/lang/String;", opcode = Opcode.IGET_OBJECT),
        string("9ea5e582-4ea7-41a7-9124-f4566dc01d88"),
    ),
)

internal fun loadParamsFingerprint(folderIdField: FieldReference) = Fingerprint(
    returnType = "V",
    filters = listOf(
        fieldAccess(reference = folderIdField, opcode = Opcode.IGET_OBJECT),
        string("KEY_LOAD_PARAMS"),
        methodCall(
            parameters = listOf("Ljava/lang/Object;", "Ljava/lang/String;"),
            returnType = "V",
            location = MatchAfterImmediately(),
        ),
    ),
)

internal fun folderQueryDispatcherFingerprint(queryMethod: MethodReference) = Fingerprint(
    returnType = "Ljava/lang/String;",
    parameters = listOf("L", "Z"),
    filters = listOf(methodCall(reference = queryMethod)),
)
