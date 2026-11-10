/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.ultimea.misc.settings

import app.morphe.patcher.patch.ResourcePatchContext
import java.io.DataOutputStream

internal fun ResourcePatchContext.writeNativeEdits(
    feature: String,
    library: ByteArray,
    edits: List<Pair<Int, ByteArray>>,
) {
    val file = get("assets/hx_ultimea/$feature")
    file.parentFile.mkdirs()
    DataOutputStream(file.outputStream()).use { output ->
        output.writeInt(edits.size)
        edits.forEach { (offset, replacement) ->
            output.writeInt(offset)
            output.writeInt(replacement.size)
            output.write(library, offset, replacement.size)
            output.write(replacement)
        }
    }
}
