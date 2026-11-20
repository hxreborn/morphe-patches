/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches

import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.Reference
import java.io.File
import java.nio.ByteBuffer

internal object ExtensionPayloads {
    private val directory = File(
        checkNotNull(ExtensionPayloads::class.java.getResource("/extensions")) {
            "extensions is not on the test classpath"
        }.toURI(),
    )

    val resourceNames: List<String> by lazy {
        directory.walkTopDown()
            .filter { it.isFile && it.extension == "mpe" }
            .map { "extensions/" + it.relativeTo(directory).invariantSeparatorsPath }
            .sorted()
            .toList()
    }

    fun bytes(resourceName: String): ByteArray = directory.resolve(resourceName.removePrefix("extensions/")).readBytes()

    fun classes(resourceName: String): List<ClassDef> =
        DexBackedDexFile(Opcodes.getDefault(), ByteBuffer.wrap(bytes(resourceName))).classes.toList()

    fun referencedTypes(resourceName: String): Set<String> =
        DexBackedDexFile(Opcodes.getDefault(), ByteBuffer.wrap(bytes(resourceName))).typeSection
            .map { it.trimStart('[') }
            .toSet()

    private const val OBJECT_TYPE = "Ljava/lang/Object;"

    private const val CONSTRUCTOR_NAME = "<init>"

    private val objectMethodNames = setOf(
        "equals", "hashCode", "toString", "getClass", "clone", "finalize", "notify", "notifyAll", "wait",
    )

    fun memberReferences(resourceName: String): List<Reference> =
        DexBackedDexFile(Opcodes.getDefault(), ByteBuffer.wrap(bytes(resourceName))).let { dex ->
            dex.methodSection.toList() + dex.fieldSection.toList()
        }

    fun unresolvedMembers(references: Iterable<Reference>, defined: Map<String, ClassDef>): List<String> =
        references.mapNotNull { reference ->
            val owner = when (reference) {
                is MethodReference -> reference.definingClass
                is FieldReference -> reference.definingClass
                else -> return@mapNotNull null
            }
            if (owner !in defined || resolves(reference, owner, defined, mutableSetOf())) null else reference.toString()
        }

    private fun resolves(reference: Reference, owner: String, defined: Map<String, ClassDef>, visited: MutableSet<String>): Boolean {
        val classDef = defined[owner] ?: return owner != OBJECT_TYPE || reference is MethodReference && reference.name in objectMethodNames
        if (!visited.add(owner)) return false
        if (classDef.declares(reference)) return true
        if (reference is MethodReference && reference.name == CONSTRUCTOR_NAME) return false
        val supertypes = listOfNotNull(classDef.superclass) + classDef.interfaces
        return supertypes.any { resolves(reference, it, defined, visited) }
    }

    private fun ClassDef.declares(reference: Reference) = when (reference) {
        is MethodReference -> methods.any {
            it.name == reference.name && it.returnType == reference.returnType &&
                it.parameterTypes.map(CharSequence::toString) == reference.parameterTypes.map(CharSequence::toString)
        }
        is FieldReference -> fields.any { it.name == reference.name && it.type == reference.type }
        else -> true
    }

    val methodsByClass: Map<String, Set<String>> by lazy {
        resourceNames.flatMap(::classes).associate { classDef ->
            classDef.type to classDef.methods
                .map { "${it.name}(${it.parameterTypes.joinToString("")})${it.returnType}" }
                .toSet()
        }
    }
}
