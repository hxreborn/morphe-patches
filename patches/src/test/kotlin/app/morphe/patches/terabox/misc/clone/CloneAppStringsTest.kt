/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.terabox.misc.clone

import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.iface.value.StringEncodedValue
import com.android.tools.smali.dexlib2.immutable.ImmutableClassDef
import com.android.tools.smali.dexlib2.immutable.ImmutableField
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.instruction.ImmutableInstruction10x
import com.android.tools.smali.dexlib2.immutable.instruction.ImmutableInstruction21c
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableStringReference
import com.android.tools.smali.dexlib2.immutable.value.ImmutableStringEncodedValue
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore
import com.android.tools.smali.dexlib2.writer.pool.DexPool
import java.nio.ByteBuffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

internal class CloneAppStringsTest {
    private val originalPackageName = "com.dubox.drive"
    private val clonePackageName = "$originalPackageName.clone1"
    private val originalAuthority = "$originalPackageName.cloudimage"
    private val renames = mapOf(originalPackageName to clonePackageName, originalAuthority to "$clonePackageName.cloudimage")
    private val classDescriptor = "Lexample/Constants;"

    private fun classWithStrings(vararg values: String, methodString: String? = null): ClassDef = ImmutableClassDef(
        classDescriptor,
        AccessFlags.PUBLIC.value,
        "Ljava/lang/Object;",
        emptyList(),
        null,
        emptyList(),
        values.mapIndexed { index, value ->
            ImmutableField(
                classDescriptor,
                "value$index",
                "Ljava/lang/String;",
                AccessFlags.STATIC.value,
                ImmutableStringEncodedValue(value),
                emptyList(),
                emptySet(),
            )
        },
        methodString?.let { value ->
            listOf(
                ImmutableMethod(
                    classDescriptor,
                    "read",
                    emptyList(),
                    "V",
                    AccessFlags.STATIC.value,
                    emptySet(),
                    emptySet(),
                    ImmutableMethodImplementation(
                        1,
                        listOf(
                            ImmutableInstruction21c(Opcode.CONST_STRING, 0, ImmutableStringReference(value)),
                            ImmutableInstruction10x(Opcode.RETURN_VOID),
                        ),
                        emptyList(),
                        emptyList(),
                    ),
                ),
            )
        }.orEmpty(),
    )

    private fun ClassDef.roundTrip(): ClassDef {
        val pool = DexPool(Opcodes.getDefault())
        pool.internClass(this)
        val bytes = MemoryDataStore().also(pool::writeTo).data
        return DexBackedDexFile(Opcodes.getDefault(), ByteBuffer.wrap(bytes)).classes.single()
    }

    @Test
    fun `rewrite encoded package and authorities without method strings`() {
        val source = classWithStrings(originalPackageName, originalAuthority, "content://$originalAuthority/local", "$originalPackageName.ui.MainActivity")
        val mutable = MutableClass(source)

        source.renameCloneStrings(renames) { mutable }

        val values = mutable.roundTrip().staticFields.associate { it.name to (it.initialValue as StringEncodedValue).value }
        assertEquals(
            mapOf(
                "value0" to clonePackageName,
                "value1" to "$clonePackageName.cloudimage",
                "value2" to "content://$clonePackageName.cloudimage/local",
                "value3" to "$originalPackageName.ui.MainActivity",
            ),
            values,
        )
        assertEquals(originalPackageName, (source.staticFields.first().initialValue as StringEncodedValue).value)
    }

    @Test
    fun `rewrite encoded fields alongside method strings`() {
        val source = classWithStrings(originalPackageName, methodString = originalAuthority)
        val mutable = MutableClass(source)

        source.renameCloneStrings(renames) { mutable }

        val result = mutable.roundTrip()
        assertEquals(clonePackageName, (result.staticFields.single().initialValue as StringEncodedValue).value)
        val instruction = result.methods.single().implementation!!.instructions.first()
        assertEquals("$clonePackageName.cloudimage", instruction.getReference<StringReference>()!!.string)
    }

    @Test
    fun `leave unrelated classes immutable`() {
        val source = classWithStrings("content://other.provider/local", "$originalPackageName.ui.MainActivity")
        var requestedMutableClass = false

        source.renameCloneStrings(renames) {
            requestedMutableClass = true
            MutableClass(source)
        }

        assertFalse(requestedMutableClass)
    }
}
