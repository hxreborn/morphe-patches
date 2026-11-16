/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.quranify.misc.integrity

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

private const val ANDROID_API_BASE_URL = "https://api-android.quranify.pro/api/"

private val OBFUSCATION_KEY =
    "4fa27118e3059c6b2df74482b13960ce075aab21d4963f8812ef576dc04ba973".chunked(2)
        .map { it.toInt(16).toByte() }

private fun String.obfuscate() = encodeToByteArray()
    .mapIndexed { index, byte -> byte.toInt() xor OBFUSCATION_KEY[index % OBFUSCATION_KEY.size].toInt() }
    .joinToString("") { "%02x".format(it and 0xff) }

internal object ApiEndpointsFingerprint : Fingerprint(
    strings = listOf("appBaseUrl", "apiHost", "mediaHost"),
    filters = listOf(
        string(ANDROID_API_BASE_URL.obfuscate()),
        methodCall(
            definingClass = "Lcom/mchutov/core/security/Obfuscated;",
            name = "decode",
            location = MatchAfterImmediately(),
        ),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
    ),
)

internal object FetchSessionTokenFingerprint : Fingerprint(
    strings = listOf(
        "check-integrity returned no session_token",
        "Device not recognized by Google Play",
    ),
)
