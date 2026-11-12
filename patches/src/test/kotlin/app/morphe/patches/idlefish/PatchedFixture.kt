/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.idlefish

import app.morphe.patcher.Patcher
import app.morphe.patcher.PatcherConfig
import app.morphe.patcher.patch.Patch
import app.morphe.patches.shared.misc.smali.assembleDex
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.util.ReferenceUtil
import java.nio.ByteBuffer
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.runBlocking
import kotlin.test.assertNull

private object FixtureResources

internal fun Method.body(): List<Instruction> = implementation!!.instructions.toList()

internal fun Instruction.describe(): String {
    val registers = when (this) {
        is FiveRegisterInstruction -> listOf(registerC, registerD, registerE, registerF, registerG).take(registerCount)
        is TwoRegisterInstruction -> listOf(registerA, registerB)
        is OneRegisterInstruction -> listOf(registerA)
        else -> emptyList()
    }
    val reference = (this as? ReferenceInstruction)?.let { ReferenceUtil.getReferenceString(it.reference) }
    return "${opcode.name} $registers $reference"
}

internal fun assembledClasses(smaliClasses: List<String>): List<ClassDef> =
    DexBackedDexFile(Opcodes.getDefault(), ByteBuffer.wrap(assembleDex(smaliClasses))).classes.toList()

internal fun patchedClasses(patch: Patch<*>, smaliClasses: List<String>): List<ClassDef> {
    val directory = Files.createTempDirectory("idlefish-fixture").toFile()
    try {
        val apk = directory.resolve("fixture.apk")
        ZipOutputStream(apk.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("AndroidManifest.xml"))
            checkNotNull(FixtureResources.javaClass.getResourceAsStream("/metadata/AndroidManifest.axml")).use {
                it.copyTo(zip)
            }
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("classes.dex"))
            zip.write(assembleDex(smaliClasses))
            zip.closeEntry()
        }
        Patcher(PatcherConfig(apk, directory.resolve("work"))).use { patcher ->
            patcher += setOf(patch)
            runBlocking { patcher().collect { assertNull(it.exception) } }
            return patcher.get().dexFiles.flatMap { output ->
                output.stream.use { DexBackedDexFile.fromInputStream(Opcodes.getDefault(), it.buffered()).classes }
            }
        }
    } finally {
        directory.deleteRecursively()
    }
}
