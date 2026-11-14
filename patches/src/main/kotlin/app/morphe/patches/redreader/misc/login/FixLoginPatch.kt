/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.redreader.misc.login

import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patches.shared.compat.AppCompatibilities

private const val REDDIT_CLIENT_ID = "yH0aTnJEt6qUgGn835B4vg"

@Suppress("unused")
val fixLoginPatch = rawResourcePatch(
    name = "Fix login",
    description = "Enables Reddit login without entering a client ID.",
) {
    compatibleWith(AppCompatibilities.REDREADER, AppCompatibilities.REDREADER_ALPHA)

    execute {
        val authenticationFile = get("assets/reddit_auth.txt")
        if (!authenticationFile.exists()) {
            authenticationFile.parentFile.mkdirs()
            authenticationFile.writeText("\"$REDDIT_CLIENT_ID\"\n")
        }
    }
}
