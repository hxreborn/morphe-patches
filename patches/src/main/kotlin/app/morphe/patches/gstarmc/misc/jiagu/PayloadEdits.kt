/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.gstarmc.misc.jiagu

import app.morphe.patcher.patch.ResourcePatchContext
import app.morphe.patches.shared.misc.jiagu.JiaguCipher
import app.morphe.patches.shared.misc.jiagu.JiaguPayloadDex
import app.morphe.patches.shared.misc.jiagu.PayloadDexEditor
import app.morphe.patches.shared.misc.jiagu.asJiaguDex

internal fun ResourcePatchContext.editPayloadDexes(edit: (PayloadDexEditor) -> Unit): Int {
    val classes = get("classes.dex")
    val packed = classes.readBytes().asJiaguDex()
    val cipher = JiaguCipher(JiaguProfiles.forConfig(packed.config).key)

    var modifiedDexes = 0
    val parts = packed.parts().map { part ->
        val payload = JiaguPayloadDex(cipher.crypt(part.encrypted))
        val editor = PayloadDexEditor(payload.dex)

        edit(editor)
        if (editor.isModified) {
            modifiedDexes++
            part.withEncrypted(cipher.crypt(payload.reframe(editor.buildDex())))
        } else {
            part
        }
    }

    classes.writeBytes(packed.withParts(parts))
    return modifiedDexes
}
