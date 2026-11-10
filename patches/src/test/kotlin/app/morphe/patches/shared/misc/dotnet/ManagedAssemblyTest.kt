/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.dotnet

import app.morphe.patcher.patch.PatchException
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

private const val NAMESPACE = "Sample.Licensing"
private const val TYPE = "License"
private const val IS_VALID_BODY = 592
private const val EXPIRY_BODY = 600
private const val DATE_TIME_MAX_VALUE_TOKEN = 0x0a00000e

class ManagedAssemblyTest {
    @Test
    fun replaceGetterWithTinyBody() {
        val stock = fixture()
        val assembly = ManagedAssembly(stock.copyOf())

        assembly.replaceMethodBody(NAMESPACE, TYPE, "get_IsValid", RETURN_TRUE)

        assertContentEquals(byteArrayOf(0x0a, 0x17, 0x2a), assembly.image.copyOfRange(IS_VALID_BODY, IS_VALID_BODY + 3))
        assertEquals(3, stock.indices.count { stock[it] != assembly.image[it] })
    }

    @Test
    fun resolveFieldReferenceToken() {
        val assembly = ManagedAssembly(fixture())

        val token = assembly.fieldReferenceToken("System", "DateTime", "MaxValue")
        assembly.replaceMethodBody(NAMESPACE, TYPE, "get_Expiry", returnStaticField(token))

        assertEquals(DATE_TIME_MAX_VALUE_TOKEN, token)
        assertContentEquals(
            byteArrayOf(0x1a, 0x7e, 0x0e, 0x00, 0x00, 0x0a, 0x2a),
            assembly.image.copyOfRange(EXPIRY_BODY, EXPIRY_BODY + 7),
        )
    }

    @Test
    fun rejectMissingMethod() {
        assertFailsWith<PatchException> { ManagedAssembly(fixture()).replaceMethodBody(NAMESPACE, TYPE, "get_Owner", RETURN_TRUE) }
    }

    @Test
    fun rejectCodeLargerThanBody() {
        assertFailsWith<PatchException> {
            ManagedAssembly(fixture()).replaceMethodBody(NAMESPACE, TYPE, "get_IsValid", ByteArray(8))
        }
    }

    @Test
    fun rejectMissingFieldReference() {
        assertFailsWith<PatchException> { ManagedAssembly(fixture()).fieldReferenceToken("System", "DateTime", "MinValue") }
    }

    private fun fixture() = checkNotNull(javaClass.getResourceAsStream("/dotnet/license.dll")).use { it.readBytes() }
}
