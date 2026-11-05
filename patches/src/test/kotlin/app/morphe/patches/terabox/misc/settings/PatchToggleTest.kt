/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.terabox.misc.settings

import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.misc.smali.assembleDex
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.iface.instruction.OffsetInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore
import com.android.tools.smali.dexlib2.writer.pool.DexPool
import java.nio.ByteBuffer
import kotlin.test.Test
import kotlin.test.assertEquals

internal class PatchToggleTest {
    @Test
    fun `branch to intact stock method when toggle is off`() {
        val source = """
            .class public Lexample/Gate;
            .super Ljava/lang/Object;
            .method public static allowed(I)Z
                .registers 2
                const/4 v0, 0x0
                if-eqz p0, :done
                const/4 v0, 0x1
                :done
                return v0
            .end method
        """.trimIndent()
        val original = DexBackedDexFile(Opcodes.getDefault(), ByteBuffer.wrap(assembleDex(listOf(source))))
            .classes.single()
        val mutable = MutableClass(original)
        mutable.methods.single().overrideWhenEnabled(PREMIUM_ENABLED) { returnEarly(true) }
        val pool = DexPool(Opcodes.getDefault())
        pool.internClass(mutable)
        val bytes = MemoryDataStore().also(pool::writeTo).data
        val method = DexBackedDexFile(Opcodes.getDefault(), ByteBuffer.wrap(bytes)).classes.single().methods.single()
        val instructions = method.implementation!!.instructions.toList()
        val call = (instructions.first() as ReferenceInstruction).reference as MethodReference
        assertEquals(TERABOX_PATCH_SETTINGS_CLASS, call.definingClass)
        assertEquals("isPremiumUnlocked", call.name)
        val branchIndex = instructions.indexOfFirst { it.opcode == Opcode.IF_EQZ }
        val branch = instructions[branchIndex] as OffsetInstruction
        val branchAddress = instructions.take(branchIndex).sumOf { it.codeUnits }
        val targetAddress = branchAddress + branch.codeOffset
        val originalIndex = instructions.indices.single { index ->
            instructions.take(index).sumOf { it.codeUnits } == targetAddress
        }
        assertEquals(
            original.methods.single().implementation!!.instructions.map { it.opcode },
            instructions.drop(originalIndex).map { it.opcode },
        )
        assertEquals(0, (instructions[originalIndex] as OneRegisterInstruction).registerA)
    }
}
