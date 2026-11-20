/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches

import app.morphe.patcher.patch.Patch
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class ExtensionLoadersTest {
    private val sourcesCopyingIntoNestedDex = setOf("app.morphe.patches.castle.misc.premium.UnlockPremiumPatchKt")

    private val payloadsByClass: Map<String, String> by lazy {
        ExtensionPayloads.resourceNames
            .flatMap { name -> ExtensionPayloads.classes(name).map { it.type.removePrefix("L").removeSuffix(";") to name } }
            .toMap()
    }

    private val fooLoaded = setOf("extensions/hxreborn/foo.mpe")

    private val fixtureClass = "app.morphe.patches.ExtensionLoaderFixturesKt"

    private val fixtureCoverage: ExtensionLoaderCoverage by lazy {
        ExtensionLoaderCoverage(
            PatchBytecode(listOf(File("build/classes/kotlin/test"))),
            mapOf("app/hxreborn/extension/foo/Bar" to "extensions/hxreborn/foo.mpe"),
        )
    }

    private fun fixture(field: String) = ExtensionLoaderCoverage.Member(fixtureClass, field)

    private fun closureOf(root: Patch): List<Patch> {
        val seen = LinkedHashSet<Patch>()
        val queue = ArrayDeque(listOf(root))
        while (queue.isNotEmpty()) {
            val patch = queue.removeFirst()
            if (seen.add(patch)) queue.addAll(patch.dependencies)
        }
        return seen.toList()
    }

    @Test
    fun `every selectable patch loads the payload of each extension class its dependencies call`() {
        val coverage = ExtensionLoaderCoverage(PatchBytecode(listOf(BundlePatches.classesDirectory)), payloadsByClass)
        val membersByPatch = BundlePatches.declared
            .associate { it.patch to ExtensionLoaderCoverage.Member(it.className, it.field) }
            .toMap(java.util.IdentityHashMap())
        val problems = mutableListOf<String>()
        var checked = 0
        for ((root, member) in membersByPatch) {
            if (root.name == null || member.className in sourcesCopyingIntoNestedDex) continue
            val closure = closureOf(root).mapNotNull(membersByPatch::get)
            if (closure.any { coverage.calledBy(it).isNotEmpty() }) checked++
            problems += coverage.problems("${member.className}#${member.field}", closure, BundlePatches.payloadsLoadedBy(root))
        }

        assertEquals(emptyList(), problems.distinct())
        assertTrue(checked >= 50, "the scan found too few selectable patches calling extension classes to trust: $checked")
    }

    @Test
    fun `reads a descriptor Patreon builds from a package constant`() {
        val coverage = ExtensionLoaderCoverage(PatchBytecode(listOf(BundlePatches.classesDirectory)), payloadsByClass)
        val member = ExtensionLoaderCoverage.Member(
            "app.morphe.patches.patreon.layout.launcher.HideHomeTabPatchKt",
            "hideHomeTabPatch",
        )

        assertTrue("app/hxreborn/extension/patreon/HomeTab" in coverage.calledBy(member))
    }

    @Test
    fun `fails an interpolated descriptor whose loader is missing`() {
        val problems = fixtureCoverage.problems("interpolated", listOf(fixture("fixtureInterpolatedPatch")), emptySet())

        assertEquals(1, problems.size)
        assertTrue(problems.single().contains("calls extensions/hxreborn/foo.mpe"))
    }

    @Test
    fun `passes an interpolated descriptor whose loader is present`() {
        val closure = listOf(fixture("fixtureInterpolatedPatch"), fixture("fixtureLoaderPatch"))

        val problems = fixtureCoverage.problems("interpolated", closure, fooLoaded)

        assertEquals(emptyList(), problems)
    }

    @Test
    fun `fails a patch whose sibling in the same file provides the loader`() {
        val problems = fixtureCoverage.problems("sibling", listOf(fixture("fixtureSiblingPatch")), emptySet())

        assertEquals(1, problems.size)
    }

    @Test
    fun `follows a helper function its patch calls`() {
        val problems = fixtureCoverage.problems("helper", listOf(fixture("fixtureHelperPatch")), emptySet())

        assertEquals(1, problems.size)
    }

    @Test
    fun `ignores a patch that names no extension class`() {
        val problems = fixtureCoverage.problems("quiet", listOf(fixture("fixtureQuietPatch")), emptySet())

        assertEquals(emptyList(), problems)
    }

    @Test
    fun `fails a class name a helper qualifies with the extension package when the loader is missing`() {
        val problems = fixtureCoverage.problems("qualified", listOf(fixture("fixtureHelperQualifiedPatch")), emptySet())

        assertEquals(1, problems.size)
        assertTrue(problems.single().contains("calls extensions/hxreborn/foo.mpe"))
    }

    @Test
    fun `passes a class name a helper qualifies with the extension package when the loader is present`() {
        val closure = listOf(fixture("fixtureHelperQualifiedPatch"))

        assertEquals(emptyList(), fixtureCoverage.problems("qualified", closure, fooLoaded))
    }

    @Test
    fun `resolves the activities NetworkAnalyzer registers through its menu helper`() {
        val coverage = ExtensionLoaderCoverage(PatchBytecode(listOf(BundlePatches.classesDirectory)), payloadsByClass)
        val member = ExtensionLoaderCoverage.Member(
            "app.morphe.patches.netanalyzer.misc.bonjour.BonjourBrowserPatchKt",
            "bonjourBrowserPatch",
        )

        val called = coverage.calledBy(member)

        assertTrue("app/hxreborn/extension/netanalyzer/BonjourActivity" in called, called.toString())
        assertTrue("app/hxreborn/extension/netanalyzer/BonjourServiceActivity" in called, called.toString())
    }

    @Test
    fun `fails a helper-qualified view no payload defines`() {
        val problems = fixtureCoverage.problems("unowned", listOf(fixture("fixtureUnownedViewPatch")), fooLoaded)

        assertTrue(problems.single().contains("foo/UnownedView") && problems.single().contains("no payload defines"), problems.toString())
    }

    @Test
    fun `fails a helper-qualified nested component no payload defines`() {
        val problems = fixtureCoverage.problems("unowned", listOf(fixture("fixtureUnownedNestedPatch")), fooLoaded)

        assertTrue(problems.single().contains("foo/Outer\$InnerActivity") && problems.single().contains("no payload defines"), problems.toString())
    }

    @Test
    fun `fails a helper-qualified component nested several levels deep`() {
        val problems = fixtureCoverage.problems("unowned", listOf(fixture("fixtureUnownedDeeplyNestedPatch")), fooLoaded)

        assertTrue(problems.single().contains("foo/Outer\$Middle\$InnerActivity") && problems.single().contains("no payload defines"), problems.toString())
    }

    @Test
    fun `fails a helper-qualified component whose class no payload defines`() {
        val withoutBonjour = payloadsByClass.filterKeys { !it.endsWith("/BonjourActivity") }
        val coverage = ExtensionLoaderCoverage(PatchBytecode(listOf(BundlePatches.classesDirectory)), withoutBonjour)
        val member = ExtensionLoaderCoverage.Member(
            "app.morphe.patches.netanalyzer.misc.bonjour.BonjourBrowserPatchKt",
            "bonjourBrowserPatch",
        )

        val problems = coverage.problems("bonjour", listOf(member), payloadsByClass.values.toSet())

        assertTrue(problems.any { it.contains("BonjourActivity") && it.contains("no payload defines") }, problems.toString())
    }

    @Test
    fun `fails a descriptor under the extension package that no payload defines`() {
        val problems = fixtureCoverage.problems("missing", listOf(fixture("fixtureMissingOwnerPatch")), emptySet())

        assertEquals(1, problems.size)
        assertTrue(problems.single().contains("no payload defines"))
    }

    @Test
    fun `fails a Morphe library descriptor that no payload defines`() {
        val problems = fixtureCoverage.problems("library", listOf(fixture("fixtureLibraryPatch")), emptySet())

        assertEquals(1, problems.size)
        assertTrue(problems.single().contains("no payload defines"))
    }
}
