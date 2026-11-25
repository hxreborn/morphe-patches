/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.terabox.misc.clone

import app.morphe.patcher.Patcher
import app.morphe.patcher.PatcherConfig
import app.morphe.patcher.patch.patch
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
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

internal class CloneAppStringsTest {
    private val originalPackageName = "com.dubox.drive"
    private val clonePackageName = "$originalPackageName.clone1"
    private val originalAuthority = "$originalPackageName.cloudimage"
    private val renames = mapOf(originalPackageName to clonePackageName, originalAuthority to "$clonePackageName.cloudimage")
    private val classDescriptor = "Lexample/Constants;"

    private fun classWithStrings(vararg values: String, methodStrings: List<String> = emptyList()): ClassDef = ImmutableClassDef(
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
        methodStrings.takeIf { it.isNotEmpty() }?.let { strings ->
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
                        strings.map { ImmutableInstruction21c(Opcode.CONST_STRING, 0, ImmutableStringReference(it)) } +
                            ImmutableInstruction10x(Opcode.RETURN_VOID),
                        emptyList(),
                        emptyList(),
                    ),
                ),
            )
        }.orEmpty(),
    )

    private fun ClassDef.dexBytes(): ByteArray {
        val pool = DexPool(Opcodes.getDefault())
        pool.internClass(this)
        return MemoryDataStore().also(pool::writeTo).data
    }

    @Test
    fun `rewrite encoded package and authorities without method strings`() {
        val source = classWithStrings(originalPackageName, originalAuthority, "content://$originalAuthority/local", "$originalPackageName.ui.MainActivity")
        val mutable = MutableClass(source)

        source.renameCloneStaticStrings(renames) { mutable }

        val result = DexBackedDexFile(Opcodes.getDefault(), ByteBuffer.wrap(mutable.dexBytes())).classes.single()
        val values = result.staticFields.associate { it.name to (it.initialValue as StringEncodedValue).value }
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
        val unrelatedUri = "content://$originalAuthority.other/local"
        val source = classWithStrings(
            originalPackageName,
            methodStrings = listOf(originalAuthority, "content://$originalAuthority/local", unrelatedUri),
        )
        val directory = Files.createTempDirectory("clone-app-strings").toFile()
        try {
            val apk = directory.resolve("fixture.apk")
            ZipOutputStream(apk.outputStream()).use { zip ->
                zip.putNextEntry(ZipEntry("AndroidManifest.xml"))
                checkNotNull(javaClass.getResourceAsStream("/metadata/AndroidManifest.axml")).use { it.copyTo(zip) }
                zip.putNextEntry(ZipEntry("classes.dex"))
                zip.write(source.dexBytes())
            }
            Patcher(PatcherConfig(apk, directory.resolve("work"))).use { patcher ->
                patcher += setOf(patch(name = "Rewrite fixture strings") { finalize { renameCloneDexStrings(renames) } })
                runBlocking { patcher().collect { assertNull(it.exception) } }
                val result = patcher.get().dexFiles.flatMap { dex ->
                    dex.stream.use { DexBackedDexFile.fromInputStream(Opcodes.getDefault(), it.buffered()).classes }
                }.single()
                val instructions = result.methods.single().implementation!!.instructions
                assertEquals(clonePackageName, (result.staticFields.single().initialValue as StringEncodedValue).value)
                assertEquals(
                    listOf("$clonePackageName.cloudimage", "content://$clonePackageName.cloudimage/local", unrelatedUri),
                    instructions.mapNotNull { it.getReference<StringReference>()?.string },
                )
            }
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun `leave unrelated classes immutable`() {
        val source = classWithStrings("content://other.provider/local", "$originalPackageName.ui.MainActivity")
        var requestedMutableClass = false

        source.renameCloneStaticStrings(renames) {
            requestedMutableClass = true
            MutableClass(source)
        }

        assertFalse(requestedMutableClass)
    }
}
