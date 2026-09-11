/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.quranify.misc.integrity

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.matchSingle
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference

private const val ANDROID_API_HOST = "api-android.quranify.pro"
private const val WEB_API_HOST = "api-web.quranify.pro"

private val OBFUSCATION_KEY =
    "4fa27118e3059c6b2df74482b13960ce075aab21d4963f8812ef576dc04ba973".hexToBytes()

private fun String.hexToBytes() = chunked(2).map { it.toInt(16).toByte() }.toByteArray()

private fun ByteArray.xorWithKey() =
    mapIndexed { index, byte -> byte.toInt().xor(OBFUSCATION_KEY[index % OBFUSCATION_KEY.size].toInt()).toByte() }
        .toByteArray()

private fun String.deobfuscate() = String(hexToBytes().xorWithKey())

private fun String.obfuscate() = encodeToByteArray().xorWithKey().joinToString("") { "%02x".format(it) }

private val HEX_LITERAL = Regex("^(?:[0-9a-f]{2})+$")

val bypassIntegrityCheckPatch = bytecodePatch {
    compatibleWith(AppCompatibilities.QURANIFY)

    execute {
        val method = ApiEndpointsFingerprint.matchSingle().method

        val redirected = method.instructions.toList().count { instruction ->
            val encoded = ((instruction as? ReferenceInstruction)?.reference as? StringReference)?.string
            if (encoded == null || !HEX_LITERAL.matches(encoded)) return@count false

            val endpoint = runCatching { encoded.deobfuscate() }.getOrNull()
            if (endpoint == null || !endpoint.contains(ANDROID_API_HOST)) return@count false

            val register = (instruction as OneRegisterInstruction).registerA
            val replacement = endpoint.replace(ANDROID_API_HOST, WEB_API_HOST).obfuscate()
            method.replaceInstruction(
                method.instructions.indexOf(instruction),
                "const-string v$register, \"$replacement\"",
            )
            true
        }

        if (redirected == 0) {
            throw PatchException("Could not redirect $ANDROID_API_HOST to $WEB_API_HOST")
        }

        FetchSessionTokenFingerprint.matchSingle().method.addInstructions(
            0,
            """
                const-string v0, "morphe"
                return-object v0
            """,
        )
    }
}
