/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.graphmessenger.misc.fix.signature

import app.morphe.patcher.patch.PatchException
import app.morphe.patches.netanalyzer.shared.Arm64
import app.morphe.patches.readera.shared.MaskedPattern
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val ANY_WORD = "?? ?? ?? ??"
private const val V1_CERTIFICATE_LOOKUP_OFFSET = 0x34

private const val MOV_X0_X21 = 0xaa1503e0.toInt()
private const val MOV_X0_X22 = 0xaa1603e0.toInt()
private const val MOV_X1_X22 = 0xaa1603e1.toInt()
private const val MOV_X1_X24 = 0xaa1803e1.toInt()
private const val MOV_X22_X0 = 0xaa0003f6.toInt()
private const val CLEAR_ARCHIVE_SLOT = 0xf90027ff.toInt()
private const val CLEAR_V1_CERTIFICATE_SLOT = 0xf90017ff.toInt()
private const val ADR_X8_DIGEST = 0x100000a8
private const val LDP_Q0_Q1_X8 = 0xad400500.toInt()
private const val STP_Q0_Q1_X0 = 0xad000400.toInt()
private const val STP_Q0_Q1_X19 = 0xad000660.toInt()
private const val STP_Q0_Q1_X22 = 0xad0006c0.toInt()

private const val V2_CERTIFICATE_DIGEST_PREFIX = "e0 83 01 91 $ANY_WORD e0 83 01 91"
private const val V1_CERTIFICATE_LOOKUP =
    "$ANY_WORD e1 83 01 91 e0 03 13 aa $ANY_WORD e1 43 00 91"

internal object NativeCertificateChecks {
    const val LIBRARY = "lib/arm64-v8a/libtmessages.49.so"

    private val stringTableKeyDerivation = MaskedPattern(
        "stringTableKeyDerivation",
        "$V2_CERTIFICATE_DIGEST_PREFIX e1 03 16 aa e2 03 18 aa $V1_CERTIFICATE_LOOKUP " +
            "e0 03 17 aa f3 23 00 f9 ff ff 00 a9 $ANY_WORD e0 27 00 f9 $ANY_WORD e1 0b 40 f9 e2 23 00 91 " +
            "f8 03 00 aa $ANY_WORD $ANY_WORD f9 03 00 aa e0 17 00 f9 $ANY_WORD $ANY_WORD e0 03 08 aa " +
            "$ANY_WORD $ANY_WORD $ANY_WORD e0 03 1b aa $ANY_WORD $ANY_WORD $ANY_WORD $ANY_WORD $ANY_WORD " +
            "fa 07 40 f9 $ANY_WORD 00 04 80 52 $ANY_WORD $ANY_WORD f6 03 00 aa e0 83 01 91 $ANY_WORD " +
            "e0 83 01 91 e1 03 19 aa e2 03 1a aa $ANY_WORD e1 83 01 91 e0 03 16 aa $ANY_WORD",
    )

    private val certificateComparison = MaskedPattern(
        "certificateComparison",
        "$V2_CERTIFICATE_DIGEST_PREFIX e1 03 15 aa e2 03 16 aa $V1_CERTIFICATE_LOOKUP " +
            "e0 03 14 aa f3 23 00 f9 ff ff 00 a9 $ANY_WORD e0 27 00 f9 $ANY_WORD e1 0b 40 f9 e2 23 00 91 " +
            "$ANY_WORD $ANY_WORD f4 03 00 aa e0 17 00 f9 $ANY_WORD $ANY_WORD e0 03 08 aa " +
            "$ANY_WORD $ANY_WORD $ANY_WORD e0 03 16 aa $ANY_WORD $ANY_WORD $ANY_WORD $ANY_WORD $ANY_WORD " +
            "e1 07 40 f9 $ANY_WORD e0 03 14 aa $ANY_WORD",
    )

    fun spoofCertificateDigest(library: ByteArray, stockCertificateDigest: ByteArray) {
        val code = ByteBuffer.wrap(library).order(ByteOrder.LITTLE_ENDIAN)
        val keyDerivation = stringTableKeyDerivation.singleIn(library)
        val comparison = certificateComparison.singleIn(library)

        val digestCall = comparison + certificateComparison.size - Arm64.INSTRUCTION_BYTES
        if (!Arm64.isBranchLink(code.getInt(digestCall))) {
            throw PatchException("${certificateComparison.name}: expected a digest call at 0x${digestCall.toString(16)}")
        }
        val digestFunction = Arm64.branchLinkTarget(code.getInt(digestCall), digestCall)

        code.replaceV1CertificateLookup(
            blockStart = keyDerivation,
            resumeAt = keyDerivation + stringTableKeyDerivation.size,
            beforeDigestCall = listOf(MOV_X0_X22, MOV_X1_X24),
            afterDigestCall = listOf(MOV_X22_X0),
            storeV1Digest = STP_Q0_Q1_X22,
            digestFunction = digestFunction,
            stockCertificateDigest = stockCertificateDigest,
        )
        code.replaceV1CertificateLookup(
            blockStart = comparison,
            resumeAt = comparison + certificateComparison.size,
            beforeDigestCall = listOf(CLEAR_ARCHIVE_SLOT, CLEAR_V1_CERTIFICATE_SLOT, MOV_X0_X21, MOV_X1_X22),
            afterDigestCall = emptyList(),
            storeV1Digest = STP_Q0_Q1_X0,
            digestFunction = digestFunction,
            stockCertificateDigest = stockCertificateDigest,
        )
    }

    private fun ByteBuffer.replaceV1CertificateLookup(
        blockStart: Int,
        resumeAt: Int,
        beforeDigestCall: List<Int>,
        afterDigestCall: List<Int>,
        storeV1Digest: Int,
        digestFunction: Int,
        stockCertificateDigest: ByteArray,
    ) {
        var at = blockStart + V1_CERTIFICATE_LOOKUP_OFFSET
        fun emit(instruction: Int) {
            putInt(at, instruction)
            at += Arm64.INSTRUCTION_BYTES
        }

        beforeDigestCall.forEach(::emit)
        emit(Arm64.branchLink((digestFunction - at) / Arm64.INSTRUCTION_BYTES))
        afterDigestCall.forEach(::emit)
        listOf(ADR_X8_DIGEST, LDP_Q0_Q1_X8, storeV1Digest, STP_Q0_Q1_X19).forEach(::emit)
        emit(Arm64.branch((resumeAt - at) / Arm64.INSTRUCTION_BYTES))
        position(at)
        put(stockCertificateDigest)
        if (position() > resumeAt) throw PatchException("V1 certificate lookup replacement overruns its block")
    }
}
