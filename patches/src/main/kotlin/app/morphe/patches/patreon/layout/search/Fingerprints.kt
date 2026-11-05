/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.patreon.layout.search

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import com.android.tools.smali.dexlib2.Opcode

private object ExploreSectionToStringFingerprint : Fingerprint(
    strings = listOf("ExploreSectionVO(id="),
)

internal object ExploreSectionItemsFingerprint : Fingerprint(
    classFingerprint = ExploreSectionToStringFingerprint,
    parameters = listOf(),
    returnType = "Ljava/util/List;",
    filters = listOf(fieldAccess(opcode = Opcode.IGET_OBJECT, type = "Ljava/util/List;")),
)

internal object ExploreSectionTypeFingerprint : Fingerprint(
    classFingerprint = ExploreSectionToStringFingerprint,
    parameters = listOf(),
    returnType = "Lcom/patreon/android/database/model/objects/ExploreSectionType;",
    filters = listOf(
        fieldAccess(opcode = Opcode.IGET_OBJECT, type = "Lcom/patreon/android/database/model/objects/ExploreSectionType;"),
    ),
)

internal object ExploreSectionDisplayTypeFingerprint : Fingerprint(
    classFingerprint = ExploreSectionToStringFingerprint,
    parameters = listOf(),
    returnType = "Lcom/patreon/android/database/model/objects/ExploreSectionDisplayType;",
    filters = listOf(
        fieldAccess(opcode = Opcode.IGET_OBJECT, type = "Lcom/patreon/android/database/model/objects/ExploreSectionDisplayType;"),
    ),
)
