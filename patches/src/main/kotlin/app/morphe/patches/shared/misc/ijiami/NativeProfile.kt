/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.ijiami

import java.security.MessageDigest

internal class NativeProfile(val name: String, val libexecSha256: String, val resource: String)

internal object NativeProfiles {
    private val all = listOf(
        NativeProfile(
            "ffdfe477",
            "ffdfe4774dcd7243a546a76185b76ff6707b184f987f07068f370389288d9db8",
            "libhxpatch-ffdfe477.so",
        ),
        NativeProfile(
            "1f3fe693",
            "1f3fe693586b069264b80a47349c2aee29daacab4511ccc54d7d5d9023124dfd",
            "libhxpatch-1f3fe693.so",
        ),
        NativeProfile(
            "2d62d8b7",
            "2d62d8b7e4b9413462b5b21ed1a4a8acd4a15dfff5b1b443f4cf0d0c1c998ec8",
            "libhxpatch-2d62d8b7.so",
        ),
        NativeProfile(
            "ed71312c",
            "ed71312c4f1427b0ea9b8628b2aa4ec1cfa5a841a8ff425deeb2a41bba713dc2",
            "libhxpatch-ed71312c.so",
        ),
    )

    fun forLibexecOrNull(libexec: ByteArray): NativeProfile? {
        val digest = sha256(libexec)
        return all.firstOrNull { it.libexecSha256 == digest }
    }

    fun sha256(bytes: ByteArray) =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}
