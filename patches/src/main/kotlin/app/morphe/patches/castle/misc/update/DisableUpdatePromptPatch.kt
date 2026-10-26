/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.castle.misc.update

import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.secshell.editSecShellPayload
import app.morphe.patches.shared.misc.secshell.secShellPayloadPatch

private const val VERSION_CLASS = "Lcom/gxgx/daqiandy/bean/VersionBean;"

private const val ZERO_VERSION_CODE = """
    const/4 v0, 0x0
    invoke-static { v0 }, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;
    move-result-object v0
    return-object v0
"""

@Suppress("unused")
val disableUpdatePromptPatch = rawResourcePatch(
    name = "Disable update prompt",
    description = "Disables the new version dialog and the required update.",
) {
    compatibleWith(AppCompatibilities.CASTLE)

    dependsOn(secShellPayloadPatch)

    execute {
        editSecShellPayload {
            method(VERSION_CLASS, "getUpdateType").returnEarly(0)
            replaceBody(VERSION_CLASS, "getVersionCode", registers = 2, smali = ZERO_VERSION_CODE)
        }
    }
}
