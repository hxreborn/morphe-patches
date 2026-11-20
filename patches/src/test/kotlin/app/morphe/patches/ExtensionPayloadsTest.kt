/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches

import app.morphe.patcher.patch.BytecodePatch
import com.android.tools.smali.dexlib2.immutable.ImmutableClassDef
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.util.MethodUtil
import app.morphe.patches.shared.misc.pairip.conditionalSharedPairipExtensionPatch
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val CASTLE_NESTED_DEX_SOURCE = "src/main/kotlin/app/morphe/patches/castle/misc/premium/UnlockPremiumPatch.kt"

internal class ExtensionPayloadsTest {
    private val dexMagic = "dex\n".toByteArray(Charsets.ISO_8859_1).toList()

    private val forbiddenPrefixes = listOf("Lkotlin/", "Lorg/jetbrains/", "Lorg/intellij/")

    @Test
    fun `every payload is a raw dex`() {
        assertTrue(ExtensionPayloads.resourceNames.isNotEmpty())
        val notDex = ExtensionPayloads.resourceNames.filter {
            ExtensionPayloads.bytes(it).take(dexMagic.size) != dexMagic
        }

        assertEquals(emptyList(), notDex)
    }

    @Test
    fun `no class is defined in two payloads`() {
        val owners = ExtensionPayloads.resourceNames
            .flatMap { name -> ExtensionPayloads.classes(name).map { it.type to name } }
            .groupBy({ it.first }, { it.second })
            .filterValues { it.size > 1 }

        assertEquals(emptyMap(), owners)
    }

    private val restoredKotlinPrefix = "extensions/hxreborn/kotlin-"

    private val restoredKotlinTypes = mapOf(
        "extensions/hxreborn/kotlin-uuid.mpe" to setOf("Lkotlin/uuid/Uuid;", "Lkotlin/uuid/Uuid\$Companion;", "Lkotlin/uuid/UuidSerialized;"),
    )

    @Test
    fun `only the Catzy restore payload defines Kotlin classes and it holds the types Catzy references`() {
        val kotlinPayloads = ExtensionPayloads.resourceNames.filter { it.startsWith(restoredKotlinPrefix) }
        val foreign = kotlinPayloads.flatMap { name ->
            ExtensionPayloads.classes(name).filter { !it.type.startsWith("Lkotlin/") && !it.type.endsWith("/R;") }
                .map { "$name defines ${it.type}" }
        }
        val missing = restoredKotlinTypes.flatMap { (name, types) ->
            types.filter { type -> ExtensionPayloads.classes(name).none { it.type == type } }.map { "$name lacks $it" }
        }

        assertEquals(emptyList(), foreign)
        assertEquals(emptyList(), missing)
        assertEquals(restoredKotlinTypes.keys, kotlinPayloads.toSet())
    }

    private val uuidSerializationMethods = mapOf(
        "Lkotlin/uuid/Uuid;" to setOf("writeReplace()Ljava/lang/Object;", "readObject(Ljava/io/ObjectInputStream;)V"),
        "Lkotlin/uuid/UuidSerialized;" to setOf(
            "<init>()V",
            "writeExternal(Ljava/io/ObjectOutput;)V",
            "readExternal(Ljava/io/ObjectInput;)V",
            "readResolve()Ljava/lang/Object;",
        ),
    )

    @Test
    fun `Catzy UUID payload keeps the stdlib Java serialization members`() {
        val classes = ExtensionPayloads.classes("extensions/hxreborn/kotlin-uuid.mpe").associateBy { it.type }
        val missingMethods = uuidSerializationMethods.flatMap { (type, methods) ->
            methods - ExtensionPayloads.methodsByClass[type].orEmpty()
        }
        val serialVersionUid = classes["Lkotlin/uuid/UuidSerialized;"]?.staticFields?.map { it.name }.orEmpty()

        assertEquals(emptyList(), missingMethods)
        assertEquals(listOf("Ljava/io/Serializable;"), classes["Lkotlin/uuid/Uuid;"]?.interfaces?.filter { it.startsWith("Ljava/io/") })
        assertEquals(listOf("Ljava/io/Externalizable;"), classes["Lkotlin/uuid/UuidSerialized;"]?.interfaces)
        assertTrue("serialVersionUID" in serialVersionUid, "UuidSerialized lacks serialVersionUID")
    }

    @Test
    fun `every payload loader has a patch that depends on it unless the nested DEX reads the payload`() {
        val nestedReads = File(CASTLE_NESTED_DEX_SOURCE).readText()
            .let { Regex(""""(extensions/hxreborn/[\w-]+\.mpe)"""").findAll(it).map { match -> match.groupValues[1] }.toSet() }
        val declared = BundlePatches.declared
        val loaders = declared.flatMap { entry ->
            (entry.patch as? BytecodePatch)?.let(BundlePatches::payloadsLoadedDirectlyBy).orEmpty().map { it to entry.patch }
        }
        val dependedOn = declared.flatMap { entry -> entry.patch.dependencies }.toSet()
        val orphans = loaders.filter { (resource, patch) -> patch !in dependedOn && resource !in nestedReads }.map { it.first }

        assertEquals(emptyList(), orphans)
    }

    @Test
    fun `no payload defines Kotlin or annotation classes`() {
        val definitions = ExtensionPayloads.resourceNames.filterNot { it.startsWith(restoredKotlinPrefix) }.flatMap { name ->
            ExtensionPayloads.classes(name)
                .filter { classDef -> forbiddenPrefixes.any(classDef.type::startsWith) }
                .map { "$name defines ${it.type}" }
        }

        assertEquals(emptyList(), definitions)
    }

    private val exportedResource = Regex("""extension\s*\{\s*name\s*=\s*"(extensions/hxreborn/[\w/-]+\.mpe)"""")

    private val moduleResources: Map<String, String> by lazy {
        File("../extensions").walkTopDown()
            .onEnter { it.name != "build" && it.name != "node_modules" }
            .filter { it.isFile && it.name == "build.gradle.kts" }
            .mapNotNull { script ->
                exportedResource.find(script.readText())?.let {
                    script.parentFile.relativeTo(File("../extensions")).invariantSeparatorsPath to it.groupValues[1]
                }
            }
            .toMap()
    }

    @Test
    fun `every exporting module has exactly one resource and every resource one module`() {
        val modulesByResource = moduleResources.entries.groupBy({ it.value }, { it.key })

        assertEquals(emptyMap(), modulesByResource.filterValues { it.size > 1 })
        assertEquals(modulesByResource.keys.sorted(), ExtensionPayloads.resourceNames.sorted())
    }

    @Test
    fun `every resource has exactly one loader patch`() {
        val loadersByResource = BundlePatches.declared
            .flatMap { declared ->
                (declared.patch as? BytecodePatch)?.let(BundlePatches::payloadsLoadedDirectlyBy).orEmpty()
                    .map { it to declared.patch }
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, patches) -> patches.toSet().size }

        assertEquals(ExtensionPayloads.resourceNames.associateWith { 1 }, loadersByResource)
    }

    private val syntheticSuffix = Regex("""(?:-IA|[$]-CC|[$][$]ExternalSynthetic.*)$""")

    @Test
    fun `every payload dex holds exactly the classes its list names`() {
        val problems = mutableListOf<String>()
        for ((directory, resource) in moduleResources) {
            val classList = File("../extensions/$directory/classes.list")
            if (!classList.isFile) continue
            val removedList = File("../extensions/$directory/removed.list")
            val removed = if (removedList.isFile) removedList.readLines().filter { it.isNotBlank() }.toSet() else emptySet()
            val listed = classList.readLines().filter { it.isNotBlank() && it !in removed }.toSet()
            val packaged = ExtensionPayloads.classes(resource)
                .map { it.type.removePrefix("L").removeSuffix(";").replace(syntheticSuffix, "") }
                .filterNot { it.endsWith("/R") }
                .toSet()
            if (packaged != listed) {
                problems += "$resource: listed but not packaged ${listed - packaged}, packaged but not listed ${packaged - listed}"
            }
        }

        assertEquals(emptyList(), problems)
        assertTrue(moduleResources.size > 50)
    }

    private val extensionTypePrefixes = listOf("Lapp/hxreborn/extension/", "Lapp/morphe/extension/")

    private val pairipMethodsAreUsed = Class.forName("app.morphe.patches.shared.misc.pairip.RemovePairipVirtualizationPatchKt")
        .getDeclaredField("pairipMethodsAreUsed")
        .apply { isAccessible = true }

    private fun <T> withPairipMethodsUsed(used: Boolean, block: () -> T): T {
        val original = pairipMethodsAreUsed.getBoolean(null)
        pairipMethodsAreUsed.setBoolean(null, used)
        try {
            return block()
        } finally {
            pairipMethodsAreUsed.setBoolean(null, original)
        }
    }

    private fun unresolvedExtensionReferences(): List<String> {
        val definedByPayload = ExtensionPayloads.resourceNames.associateWith { name ->
            ExtensionPayloads.classes(name).map { it.type }.toSet()
        }
        return BundlePatches.declared.filter { it.patch.name != null }.flatMap { declared ->
            val loaded = BundlePatches.payloadsLoadedBy(declared.patch)
            val defined = loaded.flatMap { definedByPayload.getValue(it) }.toSet()
            loaded.flatMap { payload ->
                ExtensionPayloads.referencedTypes(payload)
                    .filter { type -> extensionTypePrefixes.any(type::startsWith) && type !in defined }
                    .map { type -> "${declared.className}#${declared.field} loads $payload, which references $type" }
            }
        }.distinct()
    }

    @Test
    fun `every payload a patch loads finds the extension classes it references in that patch's payloads`() {
        val selectablePatchesLoadingPayloads = BundlePatches.declared
            .count { it.patch.name != null && BundlePatches.payloadsLoadedBy(it.patch).isNotEmpty() }

        assertEquals(emptyList(), withPairipMethodsUsed(true, ::unresolvedExtensionReferences))
        assertEquals(emptyList(), withPairipMethodsUsed(false, ::unresolvedExtensionReferences))
        assertTrue(selectablePatchesLoadingPayloads >= 100, "only $selectablePatchesLoadingPayloads patches load a payload")
    }

    @Test
    fun `Castle nested DEX inputs define every extension class the Castle package reaches`() {
        val inputs = Regex(""""(extensions/hxreborn/[\w-]+\.mpe)"""").findAll(File(CASTLE_NESTED_DEX_SOURCE).readText())
            .map { it.groupValues[1] }
            .toSet()
        val defined = inputs.flatMap { name -> ExtensionPayloads.classes(name).map { it.type } }.toSet()
        val unresolved = inputs.flatMap { name ->
            ExtensionPayloads.referencedTypes(name)
                .filter { type -> extensionTypePrefixes.any(type::startsWith) && type !in defined }
                .map { "$name references $it" }
        }

        assertEquals(
            setOf(
                "extensions/hxreborn/castle-device-guid.mpe",
                "extensions/hxreborn/castle-full-hd-playlist.mpe",
                "extensions/hxreborn/shared-streams.mpe",
            ),
            inputs,
        )
        assertTrue(defined.any { it.startsWith("Lapp/hxreborn/extension/castle/") }, "no Castle class in $inputs")
        assertEquals(emptyList(), unresolved)
    }

    private fun unresolvedExtensionMembers(): List<String> {
        val classesByPayload = ExtensionPayloads.resourceNames.associateWith { ExtensionPayloads.classes(it) }
        return BundlePatches.declared.filter { it.patch.name != null }.flatMap { declared ->
            val loaded = BundlePatches.payloadsLoadedBy(declared.patch)
            val defined = loaded.flatMap { classesByPayload.getValue(it) }.associateBy { it.type }
            loaded.flatMap { payload ->
                ExtensionPayloads.unresolvedMembers(ExtensionPayloads.memberReferences(payload), defined)
                    .map { "${declared.className}#${declared.field} loads $payload, which references missing $it" }
            }
        }.distinct()
    }

    @Test
    fun `every member a loaded payload references exists in that patch's payloads`() {
        assertEquals(emptyList(), withPairipMethodsUsed(true, ::unresolvedExtensionMembers))
        assertEquals(emptyList(), withPairipMethodsUsed(false, ::unresolvedExtensionMembers))
    }

    @Test
    fun `member check reports a called method its class no longer declares`() {
        val nightModeType = "Lapp/hxreborn/extension/shared/NightMode;"
        val nightMode = ExtensionPayloads.classes("extensions/hxreborn/shared-night-mode.mpe").single { it.type == nightModeType }
        val references = ExtensionPayloads.memberReferences("extensions/hxreborn/iiec-amoled-dark-theme.mpe")
        val called = references.filterIsInstance<MethodReference>().first { it.definingClass == nightModeType }
        val withoutCalledMethod = ImmutableClassDef(
            nightMode.type, nightMode.accessFlags, nightMode.superclass, nightMode.interfaces, nightMode.sourceFile,
            nightMode.annotations, nightMode.fields, nightMode.methods.filterNot { MethodUtil.methodSignaturesMatch(it, called) },
        )

        assertEquals(emptyList(), ExtensionPayloads.unresolvedMembers(references, mapOf(nightModeType to nightMode)))
        assertEquals(
            listOf(called.toString()),
            ExtensionPayloads.unresolvedMembers(references, mapOf(nightModeType to withoutCalledMethod)),
        )
    }

    @Test
    fun `member check reports a called constructor its class no longer declares`() {
        val adapterType = "Lapp/hxreborn/extension/shared/ActivityCallbacksAdapter;"
        val adapter = ExtensionPayloads.classes("extensions/hxreborn/shared-activity-callbacks-adapter.mpe")
            .single { it.type == adapterType }
        val (references, called) = ExtensionPayloads.resourceNames.filter { it.startsWith("extensions/hxreborn/bereal-") }
            .firstNotNullOf { name ->
                val references = ExtensionPayloads.memberReferences(name)
                references.filterIsInstance<MethodReference>()
                    .firstOrNull { it.definingClass == adapterType && it.name == "<init>" }
                    ?.let { references to it }
            }
        val withoutConstructor = ImmutableClassDef(
            adapter.type, adapter.accessFlags, adapter.superclass, adapter.interfaces, adapter.sourceFile,
            adapter.annotations, adapter.fields, adapter.methods.filterNot { MethodUtil.methodSignaturesMatch(it, called) },
        )

        assertEquals(emptyList(), ExtensionPayloads.unresolvedMembers(references, mapOf(adapterType to adapter)))
        assertEquals(
            listOf(called.toString()),
            ExtensionPayloads.unresolvedMembers(references, mapOf(adapterType to withoutConstructor)),
        )
    }

    @Test
    fun `pairip payload loads only while the app calls pairip methods`() {
        val patch = conditionalSharedPairipExtensionPatch

        assertEquals(setOf("extensions/hxreborn/shared-pairip.mpe"), withPairipMethodsUsed(true) { BundlePatches.payloadsLoadedDirectlyBy(patch) })
        assertEquals(emptySet(), withPairipMethodsUsed(false) { BundlePatches.payloadsLoadedDirectlyBy(patch) })
    }
}
