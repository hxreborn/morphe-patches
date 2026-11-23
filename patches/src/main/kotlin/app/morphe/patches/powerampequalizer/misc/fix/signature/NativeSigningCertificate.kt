/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.powerampequalizer.misc.fix.signature

import app.morphe.patcher.patch.PatchException
import app.morphe.patches.shared.misc.arm64.ANY_WORD
import app.morphe.patches.shared.misc.arm64.Arm64
import app.morphe.patches.shared.misc.arm64.MaskedPattern
import app.morphe.patches.shared.misc.pairip.ElfImage
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val X0 = 0
private const val X1 = 1
private const val X2 = 2
private const val X9 = 9
private const val X21 = 21
private const val X22 = 22
private const val X23 = 23
private const val MINIMUM_CERTIFICATE_BUFFER = 0x400
private const val CERTIFICATE_ALIGNMENT = 16L
private const val COPY_SOURCE_PAGE = 0
private const val COPY_SOURCE_PAGE_OFFSET = 1
private const val COPY_CERTIFICATE_LENGTH = 2
private const val COPY_ALLOCATION_SIZE = 3
private const val COPY_MEMORY_COPY_CALL = 13
private const val COPY_PADDING_LENGTH_CHECK = 14
private const val COPY_PADDING_LENGTH_BRANCH = 15
private const val FALLBACK_ALLOCATION_SIZE = 2
private const val FALLBACK_LENGTH_PAGE = 6
private const val FALLBACK_CERTIFICATE_LENGTH = 7
private const val FALLBACK_LENGTH_STORE = 9
private const val FALLBACK_SOURCE = 10
private const val FALLBACK_TAIL_CALL = 12

internal object NativeSigningCertificate {
    const val LIBRARY = "lib/arm64-v8a/libpeqcore.so"

    private val certificateCopy = MaskedPattern(
        "certificateCopy",
        "bf 02 10 f1 08 80 80 52 b7 82 88 9a e0 03 17 aa ?? ?? ?? 94 $ANY_WORD ?? ?? 00 f9 $ANY_WORD " +
            "15 ?? ?? f9 ?? ?? ?? b4 f8 03 00 aa e1 03 16 aa e2 03 15 aa ?? ?? ?? 94 bf fe 0f f1 88 00 00 54 " +
            "e1 02 15 cb 00 03 15 8b ?? ?? ?? 94",
    )

    private val randomCertificateFallback = MaskedPattern(
        "randomCertificateFallback",
        "fd 7b bf a9 fd 03 00 91 00 80 80 52 ?? ?? ?? 94 ?? ?? ?? b4 $ANY_WORD $ANY_WORD 0a 80 80 52 " +
            "?? ?? ?? f9 2a ?? ?? f9 01 80 80 52 fd 7b c1 a8 ?? ?? ?? 14",
    )

    fun withStockCertificate(library: ByteArray, certificate: ByteArray): ByteArray {
        val elf = ElfImage(library)
        val appended = elf.appendToWritableSegment(certificate, CERTIFICATE_ALIGNMENT)
        val patched = appended.library
        val code = ByteBuffer.wrap(patched).order(ByteOrder.LITTLE_ENDIAN)
        val copy = certificateCopy.singleIn(patched)
        val fallback = randomCertificateFallback.singleIn(patched)
        val bufferSize = maxOf(certificate.size, MINIMUM_CERTIFICATE_BUFFER)
        fun offsetOf(block: Int, instruction: Int) = block + instruction * Arm64.INSTRUCTION_BYTES

        code.putInt(offsetOf(copy, COPY_SOURCE_PAGE), Arm64.adrp(X22, elf.address(offsetOf(copy, COPY_SOURCE_PAGE)), appended.address))
        code.putInt(offsetOf(copy, COPY_SOURCE_PAGE_OFFSET), Arm64.addPageOffset(X22, X22, appended.address))
        code.putInt(offsetOf(copy, COPY_CERTIFICATE_LENGTH), Arm64.moveWideZero(X21, certificate.size))
        code.putInt(offsetOf(copy, COPY_ALLOCATION_SIZE), Arm64.moveWideZero(X0, bufferSize))
        code.putInt(offsetOf(copy, COPY_PADDING_LENGTH_CHECK), Arm64.moveWideZero(X23, bufferSize))
        code.putInt(offsetOf(copy, COPY_PADDING_LENGTH_BRANCH), Arm64.NOP)

        val lengthPageAt = offsetOf(fallback, FALLBACK_LENGTH_PAGE)
        val lengthPage = Arm64.adrpPage(code.getInt(lengthPageAt), elf.address(lengthPageAt))
        if (lengthPage != Arm64.page(appended.address)) {
            throw PatchException("Certificate address 0x${appended.address.toString(16)} is not on the fallback length page 0x${lengthPage.toString(16)}")
        }
        val memoryCopyCallAt = offsetOf(copy, COPY_MEMORY_COPY_CALL)
        val memoryCopy = Arm64.branchLinkTarget(code.getInt(memoryCopyCallAt), memoryCopyCallAt)
        val tailCallAt = offsetOf(fallback, FALLBACK_TAIL_CALL)
        val lengthStoreAt = offsetOf(fallback, FALLBACK_LENGTH_STORE)

        code.putInt(offsetOf(fallback, FALLBACK_ALLOCATION_SIZE), Arm64.moveWideZero(X0, bufferSize))
        code.putInt(offsetOf(fallback, FALLBACK_CERTIFICATE_LENGTH), Arm64.moveWideZero(X2, certificate.size))
        code.putInt(lengthStoreAt, Arm64.withTransferRegister(code.getInt(lengthStoreAt), X2))
        code.putInt(offsetOf(fallback, FALLBACK_SOURCE), Arm64.addPageOffset(X1, X9, appended.address))
        code.putInt(tailCallAt, Arm64.branch((memoryCopy - tailCallAt) / Arm64.INSTRUCTION_BYTES))
        return patched
    }
}
