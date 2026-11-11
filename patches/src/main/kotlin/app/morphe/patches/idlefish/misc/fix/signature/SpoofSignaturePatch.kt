/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.idlefish.misc.fix.signature

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.signature.spoofSignature

private const val APPLICATION_CLASS = "Lcom/taobao/idlefish/TaoBaoApplication;"

val spoofSignaturePatch = bytecodePatch {
    compatibleWith(AppCompatibilities.IDLEFISH)

    dependsOn(signingDataPatch)

    extendWith("extensions/extension.mpe")

    execute {
        spoofSignature(APPLICATION_CLASS)
        SecurityContextInitializerFingerprint.matchSingle().method.addInstructions(
            0,
            """
                invoke-static/range { p1 .. p1 }, Lapp/hxreborn/extension/shared/SecurityContext;->wrap(Landroid/content/Context;)Landroid/content/Context;
                move-result-object p1
            """,
        )
    }
}
