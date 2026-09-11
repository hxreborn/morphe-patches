/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.ijiami

import kotlin.test.Test
import kotlin.test.assertContentEquals

internal class PayloadRecoveryTest {
    private fun payload() =
        Fixtures.sampleDex + Fixtures.sampleDex + Fixtures.sampleDex + Fixtures.recoverableDex()

    private fun editMiddle(classes: ByteArray, name: String, edit: PayloadMethods.() -> Unit): ByteArray {
        val container = IjiamiContainer.of(classes)
        val payload = container.decrypt()
        val dexes = PayloadDex.split(payload)
        PayloadMethods(dexes[1].bodiesOf(Fixtures.CLASS, name)).edit()
        IjiamiPayload(dexes, container.opaqueRanges).seal()
        return container.rebuild(payload)
    }

    @Test
    fun `a middle dex edit survives a second production recovery`() {
        val first = editMiddle(Fixtures.container(payload()), "flag") { returnEarly(true) }
        val second = editMiddle(first, "run") { returnEarly() }

        val payload = IjiamiContainer.of(second).decrypt()
        val middle = PayloadDex.split(payload)[1]
        val flag = middle.bodiesOf(Fixtures.CLASS, "flag").single()
        val run = middle.bodiesOf(Fixtures.CLASS, "run").single()

        assertContentEquals(
            byteArrayOf(0x12, 0x10, 0x0f, 0x00),
            payload.copyOfRange(flag.range.first + 16, flag.range.first + 20),
        )
        assertContentEquals(
            byteArrayOf(0x0e, 0x00),
            payload.copyOfRange(run.range.first + 16, run.range.first + 18),
        )
    }
}
