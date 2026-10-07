/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches

import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import java.io.File
import java.nio.ByteBuffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class ExtensionDescriptorsTest {
    private val classPrefix = "Lapp/hxreborn/extension/"

    private val classConstant = Regex("""(?:const\s+)?val\s+(\w+)\s*=\s*"(L[\w/$]+;)"""")

    private val constantReference = Regex("""[$]\{?(\w+)\}?""")

    private val extensionCall = Regex(
        """(?:[$]\{?(\w+)\}?|($classPrefix[\w/$]+;))->([\w<>$]+)\(([^)"\s]*)\)(\[*(?:[VZBSCIJFD]|L[\w/$]+;))""",
    )

    private val extensionMethods: Map<String, Set<String>> by lazy {
        val bytes = checkNotNull(javaClass.getResourceAsStream("/extensions/extension.mpe")) {
            "extensions/extension.mpe is not on the test classpath"
        }.use { it.readBytes() }

        DexBackedDexFile(Opcodes.getDefault(), ByteBuffer.wrap(bytes)).classes.associate { classDef ->
            classDef.type to classDef.methods
                .map { "${it.name}(${it.parameterTypes.joinToString("")})${it.returnType}" }
                .toSet()
        }
    }

    private val sources by lazy {
        File("src/main/kotlin").walkTopDown().filter { it.extension == "kt" }.associateWith { it.readText() }
    }

    private val sharedConstants by lazy {
        sources.values
            .flatMap { text -> classConstant.findAll(text).map { it.groupValues[1] to it.groupValues[2] }.toList() }
            .groupBy({ it.first }, { it.second })
            .mapValues { it.value.distinct() }
            .filterValues { it.size == 1 }
            .mapValues { it.value.single() }
    }

    private data class ScanResult(val calls: Int, val problems: List<String>)

    private fun scan(text: String): ScanResult {
        val local = classConstant.findAll(text).associate { it.groupValues[1] to it.groupValues[2] }
        var calls = 0
        val problems = mutableListOf<String>()

        extensionCall.findAll(text).forEach { match ->
            val (name, literal, method, rawParameters, returnType) = match.destructured
            val owner = literal.ifEmpty { local[name] ?: sharedConstants[name] }
            if (owner == null || !owner.startsWith(classPrefix)) return@forEach

            val parameters = constantReference.replace(rawParameters) {
                local[it.groupValues[1]] ?: sharedConstants[it.groupValues[1]] ?: it.value
            }
            calls++

            val signature = "$method($parameters)$returnType"
            val methods = extensionMethods[owner]
            when {
                methods == null -> problems += "$owner is not in the extension dex ($signature)"
                signature !in methods -> problems += "$owner has no $signature"
            }
        }

        return ScanResult(calls, problems)
    }

    @Test
    fun `every extension call a patch injects exists in the extension dex`() {
        val results = sources.mapValues { scan(it.value) }

        val problems = results.flatMap { (file, scanned) -> scanned.problems.map { "${file.path}: $it" } }
        assertEquals(emptyList(), problems)
        assertTrue(results.values.sumOf { it.calls } >= 50, "the scan found too few calls to trust")
    }

    @Test
    fun `reports a call whose parameters do not match the extension`() {
        val real = extensionMethods.entries.first { (type, methods) ->
            type.startsWith("Lapp/hxreborn/extension/") && methods.any { it.startsWith("grantEntitlement(") }
        }

        val scanned = scan("""invoke-static { v0 }, ${real.key}->grantEntitlement(Ljava/lang/Object;)V""")

        assertEquals(1, scanned.calls)
        assertEquals(1, scanned.problems.size)
    }

    @Test
    fun `reports a call to a class the extension does not contain`() {
        val scanned = scan("""invoke-static { v0 }, Lapp/hxreborn/extension/missing/Nowhere;->run()V""")

        assertEquals(1, scanned.problems.size)
    }
}
