/*
 * SPDX-FileCopyrightText: 2026 hoo-dles
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Additional terms under GPLv3 section 7:
 * - You must preserve reasonable legal notices and author attributions in this file.
 * - Modified versions must not misrepresent the origin of this file.
 *
 * Ported from hoo-dles/morphe-patches:
 * https://github.com/hoo-dles/morphe-patches/commit/8020932bf92fbbb978c489a888380e6625346be5
 * Commit 8020932bf92fbbb978c489a888380e6625346be5 (2026-07-30),
 * patches/src/main/kotlin/hoodles/morphe/patches/shared/misc/pairip/bytecode/BytecodePatch.kt
 * patches/src/main/kotlin/hoodles/morphe/patches/shared/misc/pairip/resources/ExtractDexPatch.kt
 * patches/src/main/kotlin/hoodles/morphe/patches/shared/misc/pairip/resources/PairipResourcesPatch.kt
 */
package app.morphe.patches.allinonecalculator.misc.pairip

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.inputStreamFromBundledResource
import app.morphe.util.returnEarly
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction10x
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11x
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21c
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableFieldReference
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableStringReference
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.function.Supplier

private const val PAIRIP_APPLICATION_CLASS = "Lcom/pairip/application/Application;"
private const val EXTENSION_CLASS = "Lapp/hxreborn/extension/allinonecalculator/PairipMethods;"
private const val STRING_TYPE = "Ljava/lang/String;"
private const val METHOD_TYPE = "Ljava/lang/reflect/Method;"

private val PAIRIP_ASSET_MAGIC = byteArrayOf(0x00, 0x49, 0x41, 0x50, 0x02)
private val DEX_MAGIC = byteArrayOf(0x64, 0x65, 0x78, 0x0a)

private val recoveredDexFiles = mutableListOf<ByteArray>()

private fun ByteArray.indexOfSequence(sequence: ByteArray): Int {
    outer@ for (start in 0..size - sequence.size) {
        for (offset in sequence.indices) {
            if (this[start + offset] != sequence[offset]) continue@outer
        }

        return start
    }

    return -1
}

private fun ByteArray.readLittleEndianInt(offset: Int) =
    (this[offset].toInt() and 0xFF) or
        ((this[offset + 1].toInt() and 0xFF) shl 8) or
        ((this[offset + 2].toInt() and 0xFF) shl 16) or
        ((this[offset + 3].toInt() and 0xFF) shl 24)

private val extractRecoveredDexPatch = rawResourcePatch {
    execute {
        recoveredDexFiles.clear()

        get("assets").listFiles { file -> file.isFile }?.forEach { asset ->
            val contents = asset.readBytes()
            if (contents.size < PAIRIP_ASSET_MAGIC.size) return@forEach
            if (!contents.copyOf(PAIRIP_ASSET_MAGIC.size).contentEquals(PAIRIP_ASSET_MAGIC)) return@forEach

            val dexOffset = contents.indexOfSequence(DEX_MAGIC)
            if (dexOffset < 0) return@forEach

            val dexSize = contents.readLittleEndianInt(dexOffset + 0x20)
            recoveredDexFiles += contents.copyOfRange(dexOffset, dexOffset + dexSize)
        }

        check(recoveredDexFiles.isNotEmpty()) { "No dex file embedded in the pairip assets" }
    }
}

private val mergeRecoveredDexPatch = bytecodePatch {
    dependsOn(extractRecoveredDexPatch)

    setExtensionInputStream(Supplier { ByteArrayInputStream(recoveredDexFiles.single()) })
}

private val PAIRIP_HOOKED_LIBRARIES = mapOf(
    "libflutter" to "libflutter.so",
    "libfastdev" to "libfastdev_quickjs_runtime.so",
)

private fun ByteArray.applyDelta(delta: ByteArray): ByteArray {
    val buffer = ByteBuffer.wrap(delta)

    val magic = ByteArray(5).also(buffer::get)
    check(magic.contentEquals("MDLT\u0001".toByteArray(Charsets.ISO_8859_1))) { "Not a delta file" }

    val sourceDigest = ByteArray(32).also(buffer::get)
    val targetDigest = ByteArray(32).also(buffer::get)
    check(sha256().contentEquals(sourceDigest)) { "Library does not match the delta it is patched with" }

    val target = ByteArray(buffer.long.toInt())
    copyInto(target, endIndex = minOf(size, target.size))

    repeat(buffer.int) {
        val offset = buffer.long.toInt()
        buffer.get(target, offset, buffer.int)
    }

    check(target.sha256().contentEquals(targetDigest)) { "Patched library does not match the expected result" }

    return target
}

private fun ByteArray.sha256() = MessageDigest.getInstance("SHA-256").digest(this)

private val removePairipResourcesPatch = rawResourcePatch {
    dependsOn(mergeRecoveredDexPatch)

    execute {
        // Restore the stock Flutter engine and QuickJS runtime, which pairip rewrote to call its
        // virtual machine for two hundred functions it stripped out.
        var restoredLibraries = 0
        get("lib").listFiles { file -> file.isDirectory }?.forEach { architecture ->
            PAIRIP_HOOKED_LIBRARIES.forEach { (name, fileName) ->
                val library = architecture.resolve(fileName)
                if (!library.exists()) return@forEach

                val delta = inputStreamFromBundledResource(
                    "pairip",
                    "delta-${architecture.name}-$name.bin",
                ) ?: throw IllegalStateException("No $fileName delta for ${architecture.name}")

                library.writeBytes(library.readBytes().applyDelta(delta.use { it.readBytes() }))
                restoredLibraries++
            }

            architecture.resolve("libpairipcore.so").delete()
        }
        check(restoredLibraries > 0) { "No pairip patched library found under lib/" }

        get("assets").listFiles { file -> file.isFile }?.forEach { asset ->
            val header = ByteArray(PAIRIP_ASSET_MAGIC.size)
            val isVirtualizedCode = asset.inputStream().use { stream ->
                stream.read(header) == header.size && header.contentEquals(PAIRIP_ASSET_MAGIC)
            }

            if (isVirtualizedCode) asset.delete()
        }
    }
}

private class HoistedField(
    val isMethod: Boolean,
    val className: String,
    val fieldName: String,
    val value: String,
)

private fun String.unescape() = buildString {
    var index = 0
    while (index < this@unescape.length) {
        val character = this@unescape[index++]
        if (character != '\\' || index == this@unescape.length) {
            append(character)
            continue
        }

        when (val escaped = this@unescape[index++]) {
            'n' -> append('\n')
            'r' -> append('\r')
            't' -> append('\t')
            else -> append(escaped)
        }
    }
}

private fun String.toClassDescriptor() = "L${replace('.', '/')};"

private fun readHoistedFields(): List<HoistedField> {
    val stream = inputStreamFromBundledResource("pairip", "all.in.one.calculator.tsv")
        ?: throw IllegalStateException("Missing pairip/all.in.one.calculator.tsv")

    return stream.bufferedReader().useLines { lines ->
        lines.filter { it.isNotEmpty() }.map { line ->
            val (kind, className, fieldName, value) = line.split('\t', limit = 4)

            HoistedField(kind == "method", className, fieldName, value.unescape())
        }.toList()
    }
}

private operator fun <T> List<T>.component4() = this[3]

@Suppress("unused")
val removePairipProtectionPatch = bytecodePatch(
    name = "Remove pairip protection",
    description = "Removes the Play Integrity protection that crashes a patched install on " +
        "startup and sends it to Google Play. Background receivers the protection virtualized " +
        "stop running.",
) {
    compatibleWith(AppCompatibilities.ALL_IN_ONE_CALCULATOR)

    dependsOn(removePairipResourcesPatch)
    extendWith("extensions/extension.mpe")

    execute {
        VMRunnerStaticInitializerFingerprint.method.returnEarly()
        VMRunnerInvokeFingerprint.method.returnEarly(null)

        check(
            mutableClassDefBy(PAIRIP_APPLICATION_CLASS)
                .virtualMethods.removeIf { it.name == "attachBaseContext" }
        ) { "attachBaseContext not found on $PAIRIP_APPLICATION_CLASS" }

        readHoistedFields().groupBy { it.className }.forEach { (className, records) ->
            val classDescriptor = className.toClassDescriptor()
            val classDef = mutableClassDefBy(classDescriptor)

            // The virtual machine only filled fields the dex leaves without a value.
            val fields = records.filter { record ->
                classDef.staticFields.any { it.name == record.fieldName && it.initialValue == null }
            }
            if (fields.isEmpty()) return@forEach

            check(classDef.directMethods.none { it.name == "<clinit>" }) {
                "$classDescriptor already has a static initializer"
            }

            val implementation = MutableMethodImplementation(4)
            fields.forEach { field ->
                if (field.isMethod) {
                    implementation.addMethodBinding(classDescriptor, field)
                } else {
                    implementation.addStringAssignment(classDescriptor, field)
                }
            }
            implementation.addInstruction(BuilderInstruction10x(Opcode.RETURN_VOID))

            classDef.directMethods.add(
                ImmutableMethod(
                    classDescriptor,
                    "<clinit>",
                    emptyList<ImmutableMethodParameter>(),
                    "V",
                    AccessFlags.STATIC.value or AccessFlags.CONSTRUCTOR.value,
                    null,
                    null,
                    implementation,
                ).toMutable(),
            )
        }
    }
}

private fun MutableMethodImplementation.addStringAssignment(
    classDescriptor: String,
    field: HoistedField,
) {
    addInstruction(BuilderInstruction21c(Opcode.CONST_STRING, 0, ImmutableStringReference(field.value)))
    addInstruction(
        BuilderInstruction21c(
            Opcode.SPUT_OBJECT,
            0,
            ImmutableFieldReference(classDescriptor, field.fieldName, STRING_TYPE),
        ),
    )
}

private fun MutableMethodImplementation.addMethodBinding(
    classDescriptor: String,
    field: HoistedField,
) {
    val signature = field.value.substringBefore('(').substringAfterLast(' ')
    val targetClass = signature.substringBeforeLast('.')
    val targetMethod = signature.substringAfterLast('.')
    val parameters = field.value.substringAfter('(').substringBeforeLast(')')

    addInstruction(BuilderInstruction21c(Opcode.CONST_STRING, 0, ImmutableStringReference(targetClass)))
    addInstruction(BuilderInstruction21c(Opcode.CONST_STRING, 1, ImmutableStringReference(targetMethod)))
    addInstruction(BuilderInstruction21c(Opcode.CONST_STRING, 2, ImmutableStringReference(parameters)))
    addInstruction(
        BuilderInstruction35c(
            Opcode.INVOKE_STATIC,
            3,
            0,
            1,
            2,
            0,
            0,
            ImmutableMethodReference(
                EXTENSION_CLASS,
                "find",
                listOf(STRING_TYPE, STRING_TYPE, STRING_TYPE),
                METHOD_TYPE,
            ),
        ),
    )
    addInstruction(BuilderInstruction11x(Opcode.MOVE_RESULT_OBJECT, 3))
    addInstruction(
        BuilderInstruction21c(
            Opcode.SPUT_OBJECT,
            3,
            ImmutableFieldReference(classDescriptor, field.fieldName, METHOD_TYPE),
        ),
    )
}
