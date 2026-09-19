/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.vpnify.misc.fix.signature

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.all.misc.hex.hexPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.signature.stockSigningCertificate
import app.morphe.util.matchSingle
import app.morphe.util.returnEarly
import java.security.MessageDigest
import java.util.HexFormat

private const val ARM64 = "lib/arm64-v8a/libnative-lib.so"
private const val ARM32 = "lib/armeabi-v7a/libnative-lib.so"
private const val X86_64 = "lib/x86_64/libnative-lib.so"
private const val X86 = "lib/x86/libnative-lib.so"

private const val DIGEST_PREFIX_LENGTH = 10

private val disableNativeSignatureCheckPatch = hexPatch(ignoreMissingTargetFiles = true, block = {
    "1f 05 00 71 e8 07 9f 1a" asPatternTo "1f 05 00 71 08 00 80 52" inFile ARM64
    "70 1e 18 bf 01 20" asPatternTo "00 20 00 bf 00 bf" inFile ARM32
    "80 bd 98 dd ff ff 01 0f 95 c0" asPatternTo "31 c0 90 90 90 90 90 90 90 90" inFile X86_64
    "80 bd 5c de ff ff 01 0f 95 c0" asPatternTo "31 c0 90 90 90 90 90 90 90 90" inFile X86
})

val spoofSignaturePatch = bytecodePatch {
    compatibleWith(AppCompatibilities.VPNIFY)
    dependsOn(disableNativeSignatureCheckPatch)

    execute {
        val stockDigestPrefix = HexFormat.of().formatHex(
            MessageDigest.getInstance("SHA-1")
                .digest(packageMetadata.stockSigningCertificate().encoded),
        ).take(DIGEST_PREFIX_LENGTH)

        CertificateDigestFingerprint.matchSingle().method.returnEarly(stockDigestPrefix)
    }
}
