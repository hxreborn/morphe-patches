/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.botim.misc.signin

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.signature.spoofSignature
import app.morphe.patches.shared.misc.extension.sharedSignatureExtensionPatch

private const val APPLICATION_CLASS = "Lim/thebot/messenger/bot/BotApplication;"

@Suppress("unused")
val spoofSignaturePatch = bytecodePatch(
    name = "Spoof signature",
    description = "Lets a new phone number receive the verification code and sign in.",
) {
    dependsOn(sharedSignatureExtensionPatch)
    compatibleWith(AppCompatibilities.BOTIM)

    execute {
        spoofSignature(APPLICATION_CLASS)
    }
}
