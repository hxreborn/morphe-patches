/*
 * Copyright (C) 2026 hoo-dles
 * Copyright (C) 2026 hxreborn
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
package app.morphe.patches.shared.misc.pairip

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.DEX_FILE_SIZE_OFFSET
import app.morphe.patches.shared.DEX_HEADER_SIZE
import app.morphe.patches.shared.DEX_MAGIC
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.smali.descriptor
import app.morphe.patches.shared.readInt
import app.morphe.util.inputStreamFromBundledResource
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction10x
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11x
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21c
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableFieldReference
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableStringReference
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.function.Supplier

private const val PAIRIP_APPLICATION_CLASS = "Lcom/pairip/application/Application;"
private const val EXTENSION_CLASS = "Lapp/hxreborn/extension/shared/PairipMethods;"
private const val STRING_TYPE = "Ljava/lang/String;"
private const val METHOD_TYPE = "Ljava/lang/reflect/Method;"
private const val HASHED_ENCODING = "hash"
private const val ASSIGNED_ENCODING = "assigned"
private const val HEX_RADIX = 16
private val HOISTED_TYPES = setOf(STRING_TYPE, METHOD_TYPE)

private val PAIRIP_ASSET_MAGIC = byteArrayOf(0x00, 0x49, 0x41, 0x50, 0x02)
private const val KEY_DIGEST_BYTES = 8

private val recoveredDexFiles = mutableListOf<ByteArray>()
private val virtualizedCodeAssets = mutableListOf<File>()
private var hoistedFields = emptyList<HoistedField>()
private var isPairipPacked = false

private fun ByteArray.indexOfSequence(sequence: ByteArray): Int {
    outer@ for (start in 0..size - sequence.size) {
        for (offset in sequence.indices) {
            if (this[start + offset] != sequence[offset]) continue@outer
        }

        return start
    }

    return -1
}

private fun File.isVirtualizedCode(): Boolean {
    val header = ByteArray(PAIRIP_ASSET_MAGIC.size)
    return inputStream().use { it.read(header) == header.size } && header.contentEquals(PAIRIP_ASSET_MAGIC)
}

private fun pairipBuildLines(packageName: String, versionCode: String, extension: String): List<String> {
    val name = "$packageName-$versionCode.$extension"
    return inputStreamFromBundledResource("pairip", name)
        ?.bufferedReader()?.useLines { lines -> lines.filter { it.isNotEmpty() }.toList() }
        ?: throw PatchException("Missing pairip/$name")
}

internal fun recoverEmbeddedDex(asset: ByteArray): ByteArray? {
    if (asset.size < PAIRIP_ASSET_MAGIC.size) return null
    if (!asset.copyOf(PAIRIP_ASSET_MAGIC.size).contentEquals(PAIRIP_ASSET_MAGIC)) return null

    val dexOffset = asset.indexOfSequence(DEX_MAGIC)
    if (dexOffset < 0) return null

    val available = asset.size - dexOffset
    check(available >= DEX_HEADER_SIZE) { "Embedded dex header is truncated" }

    val dexSize = asset.readInt(dexOffset + DEX_FILE_SIZE_OFFSET)
    check(dexSize in DEX_HEADER_SIZE..available) {
        "Embedded dex declares $dexSize bytes but the asset holds only $available from the dex offset"
    }

    return asset.copyOfRange(dexOffset, dexOffset + dexSize)
}

private val extractRecoveredDexPatch = rawResourcePatch {
    execute {
        recoveredDexFiles.clear()
        virtualizedCodeAssets.clear()
        hoistedFields = emptyList()
        isPairipPacked = get("lib").listFiles { file -> file.isDirectory }
            ?.any { architecture -> architecture.resolve("libpairipcore.so").exists() } == true
        if (!isPairipPacked) return@execute

        get("assets").listFiles { file -> file.isFile && file.isVirtualizedCode() }?.forEach { asset ->
            virtualizedCodeAssets += asset
            recoverEmbeddedDex(asset.readBytes())?.let { recoveredDexFiles += it }
        }

        check(virtualizedCodeAssets.isNotEmpty()) { "No pairip VM assets found" }

        val appDexFiles = generateSequence(1) { it + 1 }
            .map { get(if (it == 1) "classes.dex" else "classes$it.dex") }
            .takeWhile { it.exists() }
            .map { it.readBytes() }
            .toList()
        hoistedFields = resolveHoistedFields(
            "${packageMetadata.packageName}-${packageMetadata.versionCode}.tsv",
            pairipBuildLines(packageMetadata.packageName, packageMetadata.versionCode, "tsv"),
            appDexFiles,
        )
    }
}

private val mergeRecoveredDexPatch = bytecodePatch {
    dependsOn(extractRecoveredDexPatch)

    extendWithAll {
        recoveredDexFiles.map { dex -> Supplier { ByteArrayInputStream(dex) } }
    }
}

private val LIBRARIES_LEFT_WRAPPED = setOf(
    "libapplovin-native-crash-reporter.so",
    "libconceal.so",
    "libparams.so",
    "libtraffic_interface1.so",
)

private class WrappedLibrary(val file: File, val wrapped: ByteArray, val keyName: String, val key: ByteArray?)

private fun ByteArray.sha256() = MessageDigest.getInstance("SHA-256").digest(this)

private val removePairipResourcesPatch = rawResourcePatch {
    dependsOn(mergeRecoveredDexPatch)

    execute {
        if (!isPairipPacked) return@execute

        var removedRuntimes = 0
        get("lib").listFiles { file -> file.isDirectory }?.forEach { architecture ->
            val libraries = architecture.listFiles { file ->
                file.name.endsWith(".so") && file.name !in LIBRARIES_LEFT_WRAPPED
            }.orEmpty().mapNotNull { file ->
                val wrapped = file.readBytes()
                if (!wrapped.isPairipWrappedLibrary()) return@mapNotNull null
                val digestPrefix = wrapped.sha256().take(KEY_DIGEST_BYTES).joinToString("") { "%02x".format(it) }
                val keyName = "key-${architecture.name}-${file.name.removeSuffix(".so")}-$digestPrefix.bin"
                WrappedLibrary(file, wrapped, keyName, inputStreamFromBundledResource("pairip", keyName)?.use { it.readBytes() })
            }
            val unkeyed = libraries.filter { it.key == null }
            val hasRuntime = architecture.resolve("libpairipcore.so").exists()

            if (unkeyed.isNotEmpty()) {
                if (architecture.name == "arm64-v8a" || unkeyed.size < libraries.size) {
                    throw PatchException("Missing pairip/${unkeyed.first().keyName} for ${architecture.name}/${unkeyed.first().file.name}")
                }
                architecture.deleteRecursively()
                if (hasRuntime) removedRuntimes++
                return@forEach
            }

            libraries.forEach { library ->
                library.file.writeBytes(unwrapNativeLibrary(library.wrapped, checkNotNull(library.key)))
            }

            if (hasRuntime) {
                architecture.resolve("libpairipcore.so").delete()
                removedRuntimes++
            }
        }
        check(removedRuntimes > 0) { "No libpairipcore.so found under lib/" }

        virtualizedCodeAssets.forEach(File::delete)
    }
}

internal class HoistedField(
    val type: String,
    val className: String,
    val fieldName: String,
    val value: String,
    val assignedByApp: Boolean = false,
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

private fun findDexStrings(dexFiles: List<ByteArray>, hashes: Set<Long>): Map<Long, Set<String>> {
    val found = mutableMapOf<Long, MutableSet<String>>()
    if (hashes.isEmpty()) return found
    val digest = MessageDigest.getInstance("SHA-256")
    dexFiles.forEach { dexFile ->
        DexBackedDexFile(Opcodes.getDefault(), ByteBuffer.wrap(dexFile)).stringReferences.forEach { reference ->
            val prefix = ByteBuffer.wrap(digest.digest(reference.string.toByteArray(Charsets.UTF_8))).long
            if (prefix in hashes) found.getOrPut(prefix) { mutableSetOf() } += reference.string
        }
    }
    return found
}

internal fun resolveHoistedFields(tableName: String, rows: List<String>, dexFiles: List<ByteArray>): List<HoistedField> {
    val columns = rows.map { it.split('\t', limit = 4) }
    val dexStrings = findDexStrings(dexFiles, columns.filter { it[0] == HASHED_ENCODING }.map { it[3].toULong(HEX_RADIX).toLong() }.toSet())

    return columns.map { (encoding, className, fieldName, value) ->
        when (encoding) {
            "string" -> HoistedField(STRING_TYPE, className, fieldName, value.unescape())
            "method" -> HoistedField(METHOD_TYPE, className, fieldName, value.unescape())
            ASSIGNED_ENCODING -> HoistedField(value, className, fieldName, "", assignedByApp = true)
            HASHED_ENCODING -> {
                val matches = dexStrings[value.toULong(HEX_RADIX).toLong()].orEmpty()
                check(matches.size == 1) {
                    "$className.$fieldName: ${matches.size} strings in the app's dex match hash $value in pairip/$tableName"
                }
                HoistedField(STRING_TYPE, className, fieldName, matches.single())
            }
            else -> throw IllegalStateException("Unknown encoding $encoding in pairip/$tableName")
        }
    }
}

val removePairipVirtualizationPatch = bytecodePatch {
    compatibleWith(
        AppCompatibilities.ALL_IN_ONE_CALCULATOR,
        AppCompatibilities.AUDIOLAB,
        AppCompatibilities.BANGLA_CALENDAR,
        AppCompatibilities.BETTERSLEEP,
        AppCompatibilities.CATZY,
        AppCompatibilities.CONNECTED2,
        AppCompatibilities.DEVCHECK,
        AppCompatibilities.GLASS_WIDGETS,
        AppCompatibilities.HINDU_CALENDAR,
        AppCompatibilities.INTERNET_SPEED_METER_LITE,
        AppCompatibilities.LINGODEER,
        AppCompatibilities.LUMO,
        AppCompatibilities.MUSICOLET,
        AppCompatibilities.MUSICOLET_CAST_PLUGIN,
        AppCompatibilities.POE,
        AppCompatibilities.TRANSPARENT_CLOCK,
        AppCompatibilities.USA_VPN,
        AppCompatibilities.VLLO,
        AppCompatibilities.VPN_INDIA,
        AppCompatibilities.YUKA,
    )

    dependsOn(removePairipResourcesPatch)
    extendWith("extensions/extension.mpe")

    execute {
        if (!isPairipPacked) return@execute

        VMRunnerStaticInitializerFingerprint.method.returnEarly()
        VMRunnerInvokeFingerprint.method.returnEarly(null)

        check(
            mutableClassDefBy(PAIRIP_APPLICATION_CLASS)
                .virtualMethods.removeIf { it.name == "attachBaseContext" }
        ) { "attachBaseContext not found on $PAIRIP_APPLICATION_CLASS" }

        val tableName = "${packageMetadata.packageName}-${packageMetadata.versionCode}.tsv"
        val table = hoistedFields.groupBy { it.className.toClassDescriptor() }

        val holders = mutableSetOf<String>()
        val virtualizedMethods = mutableSetOf<String>()
        classDefForEach { classDef ->
            classDef.virtualizedMethods().mapTo(virtualizedMethods, ::descriptor)
            val onlyStaticFields = classDef.methods.none() &&
                classDef.fields.all { AccessFlags.STATIC.isSet(it.accessFlags) }
            if (onlyStaticFields && classDef.staticFields.any { it.initialValue == null && it.type in HOISTED_TYPES }) {
                holders += classDef.type
            }
        }
        check(table.keys.containsAll(holders)) { "Holder classes missing from $tableName: ${holders - table.keys}" }

        val listedMethods = pairipBuildLines(packageMetadata.packageName, packageMetadata.versionCode, "stubs").toSet()
        if (virtualizedMethods != listedMethods) {
            throw PatchException(
                "Virtualized methods differ from ${tableName.removeSuffix(".tsv")}.stubs: " +
                    "unlisted ${virtualizedMethods - listedMethods}, not virtualized ${listedMethods - virtualizedMethods}",
            )
        }

        table.forEach { (classDescriptor, records) ->
            val classDef = mutableClassDefBy(classDescriptor)

            val staticFields = classDef.staticFields.map { it.name }.toSet()
            val hoistedFields = classDef.staticFields
                .filter { it.initialValue == null && it.type in HOISTED_TYPES }
                .associate { it.name to it.type }
            val recordedFields = records.associate { it.fieldName to it.type }
            check(recordedFields.size == records.size) { "$classDescriptor has duplicate rows in $tableName" }
            check(recordedFields.keys.containsAll(hoistedFields.keys) && staticFields.containsAll(recordedFields.keys)) {
                "$classDescriptor does not match $tableName: " +
                    "not in the table ${hoistedFields.keys - recordedFields.keys}, not in the app ${recordedFields.keys - staticFields}"
            }
            check(hoistedFields.all { (name, type) -> recordedFields[name] == type }) {
                "$classDescriptor field kinds differ from $tableName"
            }

            val fields = records.filter { it.fieldName in hoistedFields && !it.assignedByApp }
            if (fields.isEmpty()) return@forEach

            check(classDef.directMethods.none { it.name == "<clinit>" }) {
                "$classDescriptor already has a static initializer"
            }

            val implementation = MutableMethodImplementation(4)
            fields.forEach { field ->
                if (field.type == METHOD_TYPE) {
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
