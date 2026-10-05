/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.keepa.misc.accounts

import org.junit.jupiter.api.Assumptions.assumeTrue
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

internal class MultipleAccountsAnchorsTest {

    private val STOCK_MANAGE_CONSTS = 104

    private val stockDir: File? = System.getenv("KEEPA_STOCK_DIR")?.let(::File)

    private val node: File? = System.getenv("PATH").orEmpty().split(File.pathSeparator)
        .map { File(it, "node") }.firstOrNull { it.canExecute() }

    private fun resource(name: String): File =
        File(MultipleAccountsAnchorsTest::class.java.getResource("/keepa/$name")!!.toURI())

    private fun stockFile(name: String): File {
        assumeTrue(stockDir != null, "KEEPA_STOCK_DIR not set")
        return File(stockDir, name)
    }

    private fun stock(name: String) = stockFile(name).readText()

    private fun runNode(vararg args: String): Pair<Int, String> {
        assumeTrue(node != null, "node not on PATH")
        val process = ProcessBuilder(node!!.absolutePath, *args).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        return process.waitFor() to output
    }

    private fun temp(text: String): String =
        File.createTempFile("keepa-edited", ".mjs").apply { deleteOnExit(); writeText(text) }.absolutePath

    private fun editedBundle() = applyNetworkEdits(stock("bundle.mjs"), resource("accounts.js").readText())
    private fun editedSettings() = applySettingsEdits(stock("src_app_features_settings_settings_component_ts.mjs"))
    private fun editedManage() = applyManageEdits(stock("src_app_features_manage_manage_routes_ts.mjs"))

    @Test
    fun editedFilesPassNodeSyntaxCheck() {
        for (edited in listOf(editedBundle(), editedSettings(), editedManage())) {
            val (exit, output) = runNode("--check", temp(edited))
            assertEquals(0, exit, "node --check failed: $output")
        }
    }

    @Test
    fun manageConstsCountMatchesStock() {
        val manage = stock("src_app_features_manage_manage_routes_ts.mjs")
        assertEquals(STOCK_MANAGE_CONSTS, constsCount(manage, "ManageComponent"))
    }

    @Test
    fun editedTemplatesKeepIvySlotsConsistent() {
        val vendor = stockFile("vendor.mjs").absolutePath
        val checker = resource("ivy-slots.js").absolutePath
        for (edited in listOf(editedSettings(), editedManage())) {
            val (exit, output) = runNode(checker, vendor, temp(edited))
            assertEquals(0, exit, output)
        }
        val misnumbered = editedSettings().replace("s.eq3(13,hxAccountsRow", "s.eq3(12,hxAccountsRow")
        assertNotEquals(0, runNode(checker, vendor, temp(misnumbered)).first, "slot checker accepted a misnumbered binding")
    }

    @Test
    fun runtimeRoutesAcrossAccounts() {
        val (exit, output) = runNode(resource("accounts-harness.js").absolutePath, resource("accounts.js").absolutePath)
        assertEquals(0, exit, output)
    }
}
