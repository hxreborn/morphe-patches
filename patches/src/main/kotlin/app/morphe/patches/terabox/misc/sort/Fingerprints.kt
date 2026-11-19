/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.terabox.misc.sort

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

internal const val CLOUD_FILE_CLASS = "Lcom/dubox/drive/cloudfile/io/model/CloudFile;"

private const val VIDEO_PREVIEW_PACKAGE = "Lcom/dubox/drive/ui/preview/video/"

private const val FILE_LIST_ADAPTER_CLASS = "Lcom/dubox/drive/files/ui/cloudfile/fileListV2/FileListRecyclerAdapterV2;"

internal object SortTypeOrderFingerprint : Fingerprint(
    returnType = "Ljava/lang/String;",
    parameters = emptyList(),
    filters = listOf(
        string("SORT_BY_TIME_DESC"),
        string("SORT_BY_TIME_ASC"),
    ),
)

internal object FileRowBindFingerprint : Fingerprint(
    definingClass = FILE_LIST_ADAPTER_CLASS,
    returnType = "V",
    parameters = listOf("L", CLOUD_FILE_CLASS, "I", "Z"),
    filters = listOf(
        fieldAccess(definingClass = CLOUD_FILE_CLASS, name = "size", opcode = Opcode.IGET_WIDE),
        fieldAccess(definingClass = CLOUD_FILE_CLASS, name = "serverMTime", opcode = Opcode.IGET_WIDE),
    ),
)

internal object FileRowChangeFingerprint : Fingerprint(
    filters = listOf(
        fieldAccess(definingClass = CLOUD_FILE_CLASS, name = "serverMTime", opcode = Opcode.IGET_WIDE),
        fieldAccess(definingClass = CLOUD_FILE_CLASS, name = "serverMTime", opcode = Opcode.IGET_WIDE, location = MatchAfterImmediately()),
        opcode(Opcode.CMP_LONG, MatchAfterImmediately()),
        opcode(Opcode.IF_NEZ, MatchAfterImmediately()),
    ),
    custom = { _, classDef -> classDef.type.startsWith(FILE_LIST_ADAPTER_CLASS.removeSuffix(";") + "$") },
)

internal object PlaylistTimeComparatorFingerprint : Fingerprint(
    name = "compare",
    returnType = "I",
    filters = listOf(
        fieldAccess(definingClass = CLOUD_FILE_CLASS, name = "serverMTime", opcode = Opcode.IGET_WIDE),
        fieldAccess(definingClass = CLOUD_FILE_CLASS, name = "serverMTime", opcode = Opcode.IGET_WIDE),
        methodCall(definingClass = "Lkotlin/comparisons/ComparisonsKt;", name = "compareValues"),
    ),
    custom = { _, classDef -> classDef.type.startsWith(VIDEO_PREVIEW_PACKAGE) },
)
