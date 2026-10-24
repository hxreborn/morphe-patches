/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.secshell

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.ResourcePatchContext
import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import java.security.MessageDigest
import java.util.Base64

private const val PAYLOAD_JAR = "assets/classes0.jar"
private const val SHELL_DEX = "classes.dex"
private const val EXTENSION_DEX = "extensions/extension.mpe"
private const val JAR_MANIFEST = "META-INF/MANIFEST.MF"

internal interface SecShellContainer {
    fun dexEntries(): List<String>

    fun read(name: String): ByteArray

    fun withEntries(replacements: Map<String, ByteArray>): ByteArray
}

internal class SecShellPayload(private val container: SecShellContainer) {
    private val editors = LinkedHashMap<String, SecShellDex>()

    fun dexContaining(classType: String) = singleDex(classType) { it.hasClass(classType) }

    fun replaceBody(classType: String, name: String, registers: Int, smali: String) =
        editor(dexContaining(classType)).replaceBody(classType, name, registers, smali)

    fun method(classType: String, name: String) = editor(dexContaining(classType)).method(classType, name)

    fun methods(classType: String) = editor(dexContaining(classType)).methods(classType)

    fun methods(classType: String, name: String) = editor(dexContaining(classType)).methods(classType, name)

    fun prependToMethodUsingString(string: String, firstParameter: String, smali: String): String {
        val method = methodUsingString(string, firstParameter)
        method.addInstructions(0, smali)
        return method.definingClass
    }

    fun methodUsingString(string: String, firstParameter: String? = null): MutableMethod {
        val matches = methodsUsingString(string, firstParameter)
        return matches.singleOrNull()
            ?: throw PatchException("Expected one method using \"$string\", found ${matches.size}")
    }

    fun methodsUsingString(string: String, firstParameter: String? = null) =
        editorUsing(string).methodsUsingString(string, firstParameter)

    fun instanceFields(classType: String) = editor(dexContaining(classType)).instanceFields(classType)

    fun referencesInMethodUsing(string: String) = editorUsing(string).referencesInMethodUsing(string)

    fun addExtensionPackage(packageType: String, besideClass: String) {
        val extension = SecShellPayload::class.java.classLoader.getResourceAsStream(EXTENSION_DEX)
            ?.use { DexBackedDexFile.fromInputStream(Opcodes.getDefault(), it.buffered()) }
            ?: throw PatchException("$EXTENSION_DEX is missing from the patch bundle")
        val packageClasses = extension.classes.filter { it.type.startsWith(packageType) }
        if (packageClasses.isEmpty()) throw PatchException("$EXTENSION_DEX has no class in $packageType")

        val editor = editor(dexContaining(besideClass))
        classesReachableFrom(packageClasses, extension.classes.associateBy { it.type }).forEach(editor::addClass)
    }

    fun sealed(): ByteArray =
        container.withEntries(editors.filterValues { it.isEdited }.mapValues { it.value.toBytes() })

    private fun singleDex(subject: String, holds: (SecShellDex) -> Boolean): String {
        val owners = container.dexEntries().filter { holds(editor(it)) }
        return owners.singleOrNull()
            ?: throw PatchException("Expected $subject in one payload dex, found ${owners.size}")
    }

    private fun editorUsing(string: String) = editor(singleDex("\"$string\"") { it.hasString(string) })

    private fun editor(entry: String) = editors.getOrPut(entry) { SecShellDex(container.read(entry)) }
}

private fun classesReachableFrom(roots: List<ClassDef>, extensionClasses: Map<String, ClassDef>): Collection<ClassDef> {
    val reached = LinkedHashMap<String, ClassDef>()
    val pending = ArrayDeque(roots)
    while (pending.isNotEmpty()) {
        val classDef = pending.removeFirst()
        if (reached.putIfAbsent(classDef.type, classDef) != null) continue
        classDef.referencedTypes()
            .map { it.trimStart('[') }
            .mapNotNullTo(pending) { extensionClasses[it] }
    }
    return reached.values
}

private fun ClassDef.referencedTypes(): Sequence<String> = sequence {
    superclass?.let { yield(it) }
    yieldAll(interfaces)
    fields.forEach { yield(it.type) }
    methods.forEach { method ->
        yieldAll(method.parameterTypes.map { it.toString() })
        yield(method.returnType)
        method.implementation?.instructions?.forEach { instruction ->
            when (val reference = (instruction as? ReferenceInstruction)?.reference) {
                is TypeReference -> yield(reference.type)
                is FieldReference -> {
                    yield(reference.definingClass)
                    yield(reference.type)
                }
                is MethodReference -> {
                    yield(reference.definingClass)
                    yieldAll(reference.parameterTypes.map { it.toString() })
                    yield(reference.returnType)
                }
            }
        }
    }
}

private class OpenPayload(val entryName: String, val payload: SecShellPayload)

private var openPayload: OpenPayload? = null

internal val secShellPayloadPatch = rawResourcePatch {
    execute {
        val jar = get(PAYLOAD_JAR)
        val entryName = if (jar.exists()) PAYLOAD_JAR else SHELL_DEX
        val bytes = get(entryName).readBytes()
        val container = when {
            jar.exists() -> SecShellJar.of(bytes)
            SecShellEmbeddedDex.isEmbedded(bytes) -> SecShellEmbeddedDex(bytes)
            else -> throw PatchException("Neither $PAYLOAD_JAR nor a payload in $SHELL_DEX, this is not a SecShell build")
        }
        openPayload = OpenPayload(entryName, SecShellPayload(container))
    }

    finalize {
        val open = openPayload ?: return@finalize
        openPayload = null
        val sealed = open.payload.sealed()
        get(open.entryName).writeBytes(sealed)
        updateManifestDigest(open.entryName, sealed)
    }
}

internal fun editSecShellPayload(block: SecShellPayload.() -> Unit) {
    val open = openPayload ?: throw PatchException("The SecShell payload is not open, depend on secShellPayloadPatch")
    open.payload.block()
}

private fun ResourcePatchContext.updateManifestDigest(entryName: String, bytes: ByteArray) {
    val manifest = get(JAR_MANIFEST)
    if (!manifest.exists()) return

    val text = manifest.readText()
    val entryDigest = Regex("\\r?\\nName: ${Regex.escape(entryName)}\\r?\\n([A-Za-z0-9-]+)-Digest: ([A-Za-z0-9+/=]+)")
    val match = entryDigest.find(text) ?: throw PatchException("$JAR_MANIFEST has no digest for $entryName")
    val algorithm = match.groupValues[1].replace("SHA1", "SHA-1")
    val digest = Base64.getEncoder().encodeToString(MessageDigest.getInstance(algorithm).digest(bytes))
    manifest.writeText(text.replaceRange(match.groups[2]!!.range, digest))
}
