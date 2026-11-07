/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.layout.startfolder

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.anyInstruction
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

internal fun startFolderTabsFingerprint(folderType: String, tabType: String) = Fingerprint(
    returnType = "Ljava/lang/Object;",
    filters = listOf(
        anyInstruction(
            methodCall(definingClass = tabType, parameters = listOf(), returnType = folderType),
            fieldAccess(definingClass = tabType, type = folderType, opcode = Opcode.IGET_OBJECT),
        ),
        string("9ea5e582-4ea7-41a7-9124-f4566dc01d88"),
        fieldAccess(type = "I", opcode = Opcode.IGET),
        methodCall(
            definingClass = "Lkotlin/collections/CollectionsKt;",
            name = "getOrNull",
            parameters = listOf("Ljava/util/List;", "I"),
            location = MatchAfterImmediately(),
        ),
    ),
)

internal fun resetFolderFingerprint(controllerType: String, folderType: String) = Fingerprint(
    definingClass = controllerType,
    parameters = listOf("Ljava/lang/String;"),
    returnType = "V",
    filters = listOf(
        methodCall(definingClass = folderType, parameters = listOf(), returnType = "I"),
        opcode(Opcode.MOVE_RESULT, MatchAfterImmediately()),
    ),
)
