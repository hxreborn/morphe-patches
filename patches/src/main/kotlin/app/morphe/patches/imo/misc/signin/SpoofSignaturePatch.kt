/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.imo.misc.signin

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.patches.shared.misc.signature.stockSigningCertificate
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest

private const val SECURITY_LIBRARY = "lib/arm64-v8a/libanti.so"

private const val BL_MASK = 0xfc000000.toInt()
private const val BL = 0x94000000.toInt()

private val CERTIFICATE_DIGEST_FUNCTION = intArrayOf(
    0xd10143ff.toInt(), 0xa9027bfd.toInt(), 0xf9001bf5.toInt(), 0xa9044ff4.toInt(),
    0x910083fd.toInt(), 0xd53bd055.toInt(), 0xaa0803f3.toInt(), 0xaa0003f4.toInt(),
    0xf94016a8.toInt(), 0xf81f83a8.toInt(), 0xaa1303e8.toInt(), BL,
    0x39400268, 0xf9400669.toInt(), 0xd341fd0a.toInt(), 0x7200011f,
    0x9a890148.toInt(), 0xb5000188.toInt(), 0x910003e8.toInt(), 0xaa1403e0.toInt(),
    BL, 0x39400268,
)

private val RETURN_STOCK_DIGEST = intArrayOf(
    0x10000109,
    0x5280040a,
    0x3900010a,
    0xa9402d2a.toInt(),
    0xf800110a.toInt(),
    0xf800910b.toInt(),
    0x3900451f,
    0xd65f03c0.toInt(),
)

private fun ByteBuffer.matchesAt(offset: Int) = CERTIFICATE_DIGEST_FUNCTION.withIndex().all { (index, word) ->
    val actual = getInt(offset + index * Int.SIZE_BYTES)
    if (word == BL) actual and BL_MASK == BL else actual == word
}

@Suppress("unused")
val spoofSignaturePatch = rawResourcePatch(
    name = "Spoof signature",
    description = "Restores phone verification by call when signing in.",
) {
    compatibleWith(AppCompatibilities.IMO)
    availability(requireArm64)

    execute {
        val library = get(SECURITY_LIBRARY, true)
        if (!library.exists()) throw PatchException("$SECURITY_LIBRARY not found")

        val bytes = library.readBytes()
        val words = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val patternSize = CERTIFICATE_DIGEST_FUNCTION.size * Int.SIZE_BYTES
        val sites = (0..bytes.size - patternSize step Int.SIZE_BYTES).filter(words::matchesAt)
        val site = sites.singleOrNull()
            ?: throw PatchException("Expected one certificate digest function in $SECURITY_LIBRARY, found ${sites.size}")

        val stockDigest = MessageDigest.getInstance("MD5").digest(packageMetadata.stockSigningCertificate().encoded)
        RETURN_STOCK_DIGEST.forEachIndexed { index, word -> words.putInt(site + index * Int.SIZE_BYTES, word) }
        words.position(site + RETURN_STOCK_DIGEST.size * Int.SIZE_BYTES)
        words.put(stockDigest)
        library.writeBytes(bytes)
    }
}
