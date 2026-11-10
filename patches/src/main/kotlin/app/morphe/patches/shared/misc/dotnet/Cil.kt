/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.dotnet

import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val LDC_I4_1: Byte = 0x17
private const val LDSFLD: Byte = 0x7e
private const val RET: Byte = 0x2a

internal val RETURN_TRUE = byteArrayOf(LDC_I4_1, RET)

internal fun returnStaticField(fieldToken: Int): ByteArray =
    ByteBuffer.allocate(Byte.SIZE_BYTES + Int.SIZE_BYTES + Byte.SIZE_BYTES)
        .order(ByteOrder.LITTLE_ENDIAN)
        .put(LDSFLD)
        .putInt(fieldToken)
        .put(RET)
        .array()
