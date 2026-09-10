/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.ijiami

import app.morphe.patcher.patch.PatchException
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith

internal class SubstitutionRecoveryTest {
    private fun encrypt(plain: ByteArray) =
        ByteArray(plain.size) { Fixtures.substitution[plain[it].toInt() and 0xFF] }

    @Test
    fun `recovers the table from an encrypted payload`() {
        val cipher = encrypt(Fixtures.recoverablePayload())

        val recovered = SubstitutionRecovery.recover(cipher)

        assertContentEquals(Fixtures.substitution, recovered.substitution)
        assertContentEquals(Fixtures.inverse, recovered.inverse)
    }

    @Test
    fun `rejects a payload it cannot bootstrap from`() {
        val cipher = encrypt(Fixtures.payload(copies = 1))

        assertFailsWith<PatchException> { SubstitutionRecovery.recover(cipher) }
    }

    @Test
    fun `rejects a payload no permutation can verify`() {
        val cipher = encrypt(Fixtures.recoverablePayload())
        cipher[cipher.size / 2] = (cipher[cipher.size / 2] + 1).toByte()

        assertFailsWith<PatchException> { SubstitutionRecovery.recover(cipher) }
    }
}
