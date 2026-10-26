/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.ijiami

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.ResourcePatchContext
import app.morphe.patches.shared.DEX_FILE_SIZE_OFFSET
import app.morphe.patches.shared.DEX_MAP_OFF_OFFSET
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val OVERLAY_CLASSES_ASSET = "assets/hx.classes"
private const val OVERLAY_CODE_ASSET = "assets/hx.code"
private const val RECORD_HEADER_SIZE = 16

internal fun ResourcePatchContext.appendCodeOverlay(bodies: List<MethodBody>) {
    if (bodies.isEmpty()) return

    val classesAsset = get(OVERLAY_CLASSES_ASSET, copy = false)
    val classNames = (if (classesAsset.exists()) classesAsset.readLines() else emptyList()) +
        bodies.map { it.classDescriptor.removePrefix("L").removeSuffix(";").replace('/', '.') }
    classesAsset.writeText(classNames.filter { it.isNotEmpty() }.distinct().joinToString("\n"))

    val codeAsset = get(OVERLAY_CODE_ASSET, copy = false)
    val records = ByteArrayOutputStream()
    if (codeAsset.exists()) records.write(codeAsset.readBytes())
    bodies.forEach { records.write(it.overlayRecord()) }
    codeAsset.writeBytes(records.toByteArray())
}

private fun MethodBody.overlayRecord(): ByteArray {
    val range = editedRange ?: throw PatchException("No edit to record for $description")
    val code = payloadBytes(range)
    return ByteBuffer.allocate(RECORD_HEADER_SIZE + code.size)
        .order(ByteOrder.LITTLE_ENDIAN)
        .putInt(dex.int(DEX_FILE_SIZE_OFFSET))
        .putInt(dex.int(DEX_MAP_OFF_OFFSET))
        .putInt(range.first - dex.start)
        .putInt(code.size)
        .put(code)
        .array()
}
