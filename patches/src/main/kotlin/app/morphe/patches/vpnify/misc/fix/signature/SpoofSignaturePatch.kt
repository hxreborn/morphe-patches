/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.vpnify.misc.fix.signature

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patches.all.misc.hex.hexPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.signature.stockSigningCertificate
import app.morphe.util.matchSingle
import app.morphe.util.returnEarly
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.util.HexFormat

private const val ARM64 = "lib/arm64-v8a/libnative-lib.so"
private const val ARM32 = "lib/armeabi-v7a/libnative-lib.so"
private const val X86_64 = "lib/x86_64/libnative-lib.so"
private const val X86 = "lib/x86/libnative-lib.so"

private const val DIGEST_PREFIX_LENGTH = 10

private const val CMP_W8_1 = 0x7100051f
private const val CSET_NE = 0x1a9f07e0
private const val MOVZ_ZERO = 0x52800000
private const val REGISTER_MASK = 0x1f.inv()

private val disableNativeSignatureCheckPatch = hexPatch(ignoreMissingTargetFiles = true, block = {
    "70 1e 18 bf 01 20" asPatternTo "00 20 00 bf 00 bf" inFile ARM32
    "80 bd 98 dd ff ff 01 0f 95 c0" asPatternTo "31 c0 90 90 90 90 90 90 90 90" inFile X86_64
    "80 bd 5c de ff ff 01 0f 95 c0" asPatternTo "31 c0 90 90 90 90 90 90 90 90" inFile X86
})

private val disableArm64SignatureCheckPatch = rawResourcePatch {
    execute {
        val library = get(ARM64, true)
        if (!library.exists()) return@execute

        val bytes = library.readBytes()
        val words = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val sites = (0..bytes.size - 8 step 4).filter { offset ->
            words.getInt(offset) == CMP_W8_1 && words.getInt(offset + 4) and REGISTER_MASK == CSET_NE
        }
        val site = sites.singleOrNull()
            ?: throw PatchException("Expected one cmp w8, #1; cset ne in $ARM64, found ${sites.size}")

        val register = words.getInt(site + 4) and REGISTER_MASK.inv()
        words.putInt(site + 4, MOVZ_ZERO or register)
        library.writeBytes(bytes)
    }
}

val spoofSignaturePatch = bytecodePatch {
    compatibleWith(AppCompatibilities.VPNIFY)
    dependsOn(disableNativeSignatureCheckPatch, disableArm64SignatureCheckPatch)

    execute {
        val stockDigestPrefix = HexFormat.of().formatHex(
            MessageDigest.getInstance("SHA-1")
                .digest(packageMetadata.stockSigningCertificate().encoded),
        ).take(DIGEST_PREFIX_LENGTH)

        CertificateDigestFingerprint.matchSingle().method.returnEarly(stockDigestPrefix)
    }
}
