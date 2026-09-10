/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.gstarmc.misc.jiagu

import app.morphe.patcher.patch.PatchException
import java.security.MessageDigest

internal class JiaguProfile(
    val version: String,
    val configSha256: String,
    private val keyHex: String,
    val stubResource: String,
) {
    val key get() = ByteArray(keyHex.length / 2) {
        keyHex.substring(it * 2, it * 2 + 2).toInt(16).toByte()
    }
}

internal object JiaguProfiles {
    private val all = listOf(
        JiaguProfile(
            "5.19.4",
            "7b165a77ac3d96ce42c7af60eaa4869c1a7a33ea775ff115bcce15fc50a3943d",
            "301e381006130516451900c33c11361b",
            "stub-5-19-4.dex",
        ),
        JiaguProfile(
            "5.19.6",
            "ecacf7cb2ace5e85c7a5a7b78213252cd4f0bb005c4db4de4fb560c9cfcfe72b",
            "e08ee8b3ebbfcdb9c5de83d4ce94f7ba",
            "stub-5-19-6.dex",
        ),
        JiaguProfile(
            "5.20.0",
            "c2b189a301089b672e6a67082ee0feb09123122aefd8aec3003f3122ddcd771a",
            "6a576bab7aeb79415d717d7f7f407e4f",
            "stub-5-20-0.dex",
        ),
        JiaguProfile(
            "5.21.0",
            "60292dbec3f56621a8ecc5c4e2f2a0bc97d4a49d41d5725659db26aaafe34721",
            "5aec47eb4f9a3994378d3f92499365f3",
            "stub-5-21-0.dex",
        ),
    )

    fun forConfig(config: ByteArray): JiaguProfile {
        val digest = MessageDigest.getInstance("SHA-256").digest(config)
            .joinToString("") { "%02x".format(it) }

        return all.firstOrNull { it.configSha256 == digest }
            ?: throw PatchException(
                "Unsupported Jiagu build: config sha-256 $digest, supported " +
                    all.joinToString { it.version },
            )
    }
}
