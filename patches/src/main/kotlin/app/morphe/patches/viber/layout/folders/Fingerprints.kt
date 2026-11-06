/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.layout.folders

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

internal object ChatFoldersPayloadFingerprint : Fingerprint(
    name = "toString",
    filters = listOf(string("ChatFoldersFeaturePayload(maxFolders=")),
)

internal object MaxFoldersFingerprint : Fingerprint(
    classFingerprint = ChatFoldersPayloadFingerprint,
    returnType = "I",
    parameters = listOf(),
    filters = listOf(fieldAccess(opcode = Opcode.IGET, type = "I")),
    custom = { method, _ -> method.name != "hashCode" },
)
