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
import app.morphe.patches.shared.misc.smali.parameterRegister

private const val APPLICATION_CLASS = "Lcom/taobao/idlefish/TaoBaoApplication;"

val spoofSignaturePatch = bytecodePatch {
    compatibleWith(AppCompatibilities.IDLEFISH)

    dependsOn(signingDataPatch)

    extendWith("extensions/extension.mpe")

    execute {
        spoofSignature(APPLICATION_CLASS)
        val initializer = SecurityContextInitializerFingerprint.matchSingle().method
        val contextRegister = initializer.parameterRegister(0)
        initializer.addInstructions(
            0,
            """
                invoke-static/range { $contextRegister .. $contextRegister }, Lapp/hxreborn/extension/shared/SecurityContext;->wrap(Landroid/content/Context;)Landroid/content/Context;
                move-result-object $contextRegister
            """,
        )
    }
}
