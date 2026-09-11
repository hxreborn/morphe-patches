/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.ijiami

import app.morphe.patcher.patch.PatchException
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
    )

    fun forLibexec(libexec: ByteArray): NativeProfile {
        val digest = MessageDigest.getInstance("SHA-256").digest(libexec)
            .joinToString("") { "%02x".format(it) }
        return all.firstOrNull { it.libexecSha256 == digest }
            ?: throw PatchException("Unsupported Ijiami native packer build: libexec.so sha-256 $digest")
    }
}
