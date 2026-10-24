/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.pairip

import app.morphe.patches.shared.misc.smali.assembleDex
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

internal class HoistedFieldsTest {
    private val dexFile = assembleDex(
        listOf(
            """
                .class public Lsample/Holder;
                .super Ljava/lang/Object;

                .method public static run()V
                    .registers 1
                    const-string v0, "https://example.test/endpoint"
                    return-void
                .end method
            """.trimIndent(),
        ),
    )

    @Test
    fun `reads the string a hash row names from the app's dex`() {
        // given
        val rows = listOf("hash\tsample.Holder\ta\t${prefix("https://example.test/endpoint")}")

        // when
        val fields = resolveHoistedFields("table.tsv", rows, listOf(dexFile))

        // then
        assertEquals("https://example.test/endpoint", fields.single().value)
        assertEquals("Ljava/lang/String;", fields.single().type)
    }

    @Test
    fun `keeps text rows as written`() {
        val rows = listOf("string\tsample.Holder\ta\tfirst\\nsecond", "method\tsample.Holder\tb\tx.Y.z()")

        val fields = resolveHoistedFields("table.tsv", rows, emptyList())

        assertEquals(listOf("first\nsecond", "x.Y.z()"), fields.map { it.value })
    }

    @Test
    fun `marks an assigned row as set by the app with its field type`() {
        val rows = listOf("assigned\tsample.Holder\ta\tLjava/lang/reflect/Method;")

        val field = resolveHoistedFields("table.tsv", rows, emptyList()).single()

        assertTrue(field.assignedByApp)
        assertEquals("Ljava/lang/reflect/Method;", field.type)
    }

    @Test
    fun `rejects a hash that no string in the app's dex matches`() {
        // given
        val rows = listOf("hash\tsample.Holder\ta\t${prefix("not in the dex")}")

        // when
        val failure = assertFailsWith<IllegalStateException> { resolveHoistedFields("table.tsv", rows, listOf(dexFile)) }

        // then
        assertTrue("0 strings" in failure.message.orEmpty())
    }

    @Test
    fun `rejects an unknown row encoding`() {
        assertFailsWith<IllegalStateException> { resolveHoistedFields("table.tsv", listOf("other\tsample.Holder\ta\tx"), emptyList()) }
    }

    private fun prefix(value: String) =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8)).take(8).joinToString("") { "%02x".format(it) }
}
