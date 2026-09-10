/*
 * SPDX-FileCopyrightText: 2026 hxreborn
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
            "5.19.6",
            "ecacf7cb2ace5e85c7a5a7b78213252cd4f0bb005c4db4de4fb560c9cfcfe72b",
            "e08ee8b3ebbfcdb9c5de83d4ce94f7ba",
            "stub-5-19-6.dex",
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
