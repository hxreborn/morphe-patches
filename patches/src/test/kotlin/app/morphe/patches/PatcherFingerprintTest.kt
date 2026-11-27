/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.Patcher
import app.morphe.patcher.PatcherConfig
import app.morphe.patcher.StringComparisonType.STARTS_WITH
import app.morphe.patcher.literal
import app.morphe.patcher.patch.Patch
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.patch
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.string
import app.morphe.patcher.util.smali.toInstructions
import app.morphe.patches.protonvpn.misc.anchors.resourceField
import app.morphe.patches.protonvpn.misc.anchors.resourceFieldsPatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.immutable.ImmutableClassDef
import com.android.tools.smali.dexlib2.immutable.ImmutableField
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.value.ImmutableIntEncodedValue
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore
import com.android.tools.smali.dexlib2.writer.pool.DexPool
import com.reandroid.arsc.chunk.TableBlock
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val FIXTURE_CLASS = "Lfixture/Matcher;"
private const val FIXTURE_RESOURCE = "spinner"

internal class PatcherFingerprintTest {
    @Test
    fun `validate exact strings prefix matching and retry`() {
        val prefix = "fixture-marker"
        val firstMethod = fixtureMethod(
            "first",
            """
                const-string v0, "$prefix first"
                const/4 v0, 0x1
                nop
                const/4 v0, 0x1
                const/4 v0, 0x2
                return-void
            """,
        )
        val secondMethod = fixtureMethod("second", "const-string v0, \"$prefix second\"\nreturn-void")
        val dex = fixtureDex(listOf(firstMethod, secondMethod))
        val checkMatches = patch(name = "Check fixture matches") {
            execute {
                val exact = Fingerprint(strings = listOf(prefix))
                val prefixed = Fingerprint(filters = listOf(string(prefix, STARTS_WITH)))
                val consecutive = Fingerprint(filters = listOf(literal(1), literal(2, location = MatchAfterImmediately())))

                assertNull(exact.matchOrNull())
                assertEquals(setOf("first", "second"), prefixed.matchAll(2..2).map { it.method.name }.toSet())
                assertEquals(listOf(3, 4), consecutive.match().instructionMatches.map { it.index })
                val failure = assertFailsWith<PatchException> { prefixed.match() }
                assertTrue(failure.message.orEmpty().contains("Expected a single match but found 2 matches"))
            }
        }

        runFixture(checkMatches, dex)
    }

    @Test
    fun `refresh resource field and literal matches for the next APK`() {
        val fingerprint = Fingerprint(filters = listOf(resourceField(ResourceType.ID, FIXTURE_RESOURCE)))
        val results = mutableListOf<Set<String>>()
        val checkMatches = patch(name = "Check fixture resource") {
            dependsOn(resourceFieldsPatch)
            execute {
                results += fingerprint.matchAll(2..2).map { it.method.name }.toSet()
            }
        }

        val resourceIds = mutableSetOf<Int>()
        for (extraEntry in listOf(false, true)) {
            val table = TableBlock()
            val pkg = table.newPackage(0x7f, "fixture.app")
            if (extraEntry) pkg.getOrCreate("", "id", "placeholder").setDataAsBoolean(false)
            val entry = pkg.getOrCreate("", "id", FIXTURE_RESOURCE)
            entry.setDataAsBoolean(false)
            table.refresh()
            val id = entry.resourceId
            assertTrue(resourceIds.add(id), "Fixture APKs must use different resource IDs")
            val field = ImmutableField(
                FIXTURE_CLASS, "spinner", "I", AccessFlags.STATIC.value,
                ImmutableIntEncodedValue(id), emptyList(), emptySet(),
            )
            val dex = fixtureDex(
                listOf(
                    fixtureMethod("byField", "sget v0, $FIXTURE_CLASS->spinner:I\nreturn-void"),
                    fixtureMethod("byLiteral", "const v0, $id\nreturn-void"),
                ),
                listOf(field),
            )
            runFixture(checkMatches, dex, table.bytes)
        }

        assertEquals(List(2) { setOf("byField", "byLiteral") }, results)
    }

    private fun fixtureMethod(name: String, smali: String) = ImmutableMethod(
        FIXTURE_CLASS, name, emptyList(), "V",
        AccessFlags.PUBLIC.value or AccessFlags.STATIC.value,
        null, null, ImmutableMethodImplementation(1, smali.trimIndent().toInstructions(), null, null),
    )

    private fun fixtureDex(methods: List<ImmutableMethod>, fields: List<ImmutableField> = emptyList()): ByteArray {
        val pool = DexPool(Opcodes.getDefault())
        pool.internClass(
            ImmutableClassDef(
                FIXTURE_CLASS, AccessFlags.PUBLIC.value, "Ljava/lang/Object;",
                emptyList(), null, null, fields, methods,
            ),
        )
        return MemoryDataStore().also(pool::writeTo).data
    }

    private fun runFixture(patch: Patch, dex: ByteArray, resources: ByteArray? = null) {
        val directory = Files.createTempDirectory("patcher-fingerprint").toFile()
        try {
            val apk = directory.resolve("fixture.apk")
            ZipOutputStream(apk.outputStream()).use { zip ->
                zip.putNextEntry(ZipEntry("AndroidManifest.xml"))
                checkNotNull(javaClass.getResourceAsStream("/metadata/AndroidManifest.axml")).use { it.copyTo(zip) }
                zip.putNextEntry(ZipEntry("classes.dex"))
                zip.write(dex)
                if (resources != null) {
                    zip.putNextEntry(ZipEntry("resources.arsc"))
                    zip.write(resources)
                }
            }
            Patcher(PatcherConfig(apk, directory.resolve("work"), targetPatcherVersion = "2.5.0", validate = true))
                .use { patcher ->
                    patcher += setOf(patch)
                    runBlocking { patcher().collect { it.exception?.let { failure -> throw failure } } }
                }
        } finally {
            directory.deleteRecursively()
        }
    }
}
