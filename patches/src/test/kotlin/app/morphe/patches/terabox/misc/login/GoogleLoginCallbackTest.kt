/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.terabox.misc.login

import org.junit.jupiter.api.Assumptions.assumeTrue
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class GoogleLoginCallbackTest {
    private val node = System.getenv("PATH").orEmpty().split(File.pathSeparator)
        .map { File(it, "node") }.firstOrNull { it.canExecute() }

    private fun assertScenario(scenario: String) {
        val executable = node
        assumeTrue(executable != null, "node not on PATH")
        val fixture = File(requireNotNull(javaClass.getResource("/terabox/google-login-callback.cjs")).toURI())
        val source = File("../extensions/extension/src/main/java/app/hxreborn/extension/terabox/GoogleLoginChromeClient.java")
        val process = ProcessBuilder(requireNotNull(executable).absolutePath, fixture.absolutePath, source.absolutePath, scenario)
            .redirectErrorStream(true).start()
        val completed = process.waitFor(NODE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        if (!completed) process.destroyForcibly()
        assertTrue(completed, "Google login callback scenario timed out: $scenario")
        val output = process.inputStream.bufferedReader().use { it.readText() }
        assertEquals(0, process.exitValue(), output)
    }

    @Test
    fun deliverCredentialOnceWhenOpenerArrivesFirst() = assertScenario("opener-first")

    @Test
    fun deliverCredentialOnceWhenPopupArrivesFirst() = assertScenario("popup-first")

    @Test
    fun preserveCallerConfigurationAcrossInitialization() = assertScenario("initialize-twice")

    @Test
    fun allowRetryAfterServerRejectsCredential() = assertScenario("server-rejection")

    @Test
    fun allowRetryAfterTransportFails() = assertScenario("transport-failure")

    @Test
    fun allowDifferentCredentialAfterDelivery() = assertScenario("different-credential")

    private companion object {
        const val NODE_TIMEOUT_SECONDS = 15L
    }
}
