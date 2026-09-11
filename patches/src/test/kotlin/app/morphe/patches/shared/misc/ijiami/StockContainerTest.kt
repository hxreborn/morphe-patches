/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.ijiami

import org.junit.jupiter.api.Assumptions.assumeTrue
import java.io.File
import java.security.MessageDigest
import java.util.zip.Adler32
import java.util.zip.ZipFile
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class StockContainerTest {
    private val configured: String? = System.getenv("IJIAMI_STOCK_APK")
    private val corpus = configured
        ?.split(File.pathSeparatorChar)
        ?.filter { it.isNotBlank() }
        ?.map(::File)
        .orEmpty()

    private fun eachStock(action: (String, ByteArray) -> Unit) {
        assumeTrue(configured != null, "IJIAMI_STOCK_APK is not set")
        assertTrue(corpus.isNotEmpty(), "IJIAMI_STOCK_APK is set but lists no paths")
        corpus.forEach { apk ->
            assertTrue(apk.isFile, "configured stock APK does not exist: $apk")
            val classes = ZipFile(apk).use { it.getInputStream(it.getEntry("classes.dex")).readBytes() }
            action(apk.name, classes)
        }
    }

    @Test
    fun `reads a stock container back byte for byte`() = eachStock { name, original ->
        val container = IjiamiContainer.of(original)
        Fixtures.assertBytesEqual(original, container.rebuild(container.decrypt()), "round trip $name")
    }

    @Test
    fun `every payload dex verifies itself once the head is recovered`() = eachStock { name, classes ->
        val container = IjiamiContainer.of(classes)
        val payload = container.decrypt()
        val dexes = PayloadDex.split(payload)

        assertTrue(dexes.size > 1, "$name: expected a multi dex payload, got ${dexes.size}")

        val verified = dexes.map { dex ->
            val bytes = payload.copyOfRange(dex.start, dex.start + dex.size)
            val signature = MessageDigest.getInstance("SHA-1").digest(bytes.copyOfRange(32, bytes.size))
            val checksum = Adler32().apply { update(bytes, 12, bytes.size - 12) }.value.toInt()
            signature.contentEquals(bytes.copyOfRange(12, 32)) && checksum == bytes.readInt(8)
        }

        assertEquals(
            List(dexes.size - 1) { true } + false,
            verified,
            "$name: unexpected set of payload dex files failing their own integrity fields",
        )
    }

    @Test
    fun `recovers exactly the head block of a stock container`() = eachStock { name, classes ->
        val container = IjiamiContainer.of(classes)
        val payload = container.decrypt()
        val substituted = payload.copyOf()

        PayloadDex.split(payload)

        val repaired = payload.indices.filter { payload[it] != substituted[it] }
        if (repaired.isNotEmpty()) {
            assertEquals(0, repaired.first(), "$name: recovery started past the first byte")
            assertTrue(
                repaired.last() < OPAQUE_BLOCK_SIZE,
                "$name: recovery reached byte ${repaired.last()}, past the block the packer encrypts twice",
            )
        }
    }

    @Test
    fun `rebuilds a stock container with the packer signature check disarmed`() = eachStock { name, classes ->
        val container = IjiamiContainer.of(classes)
        val payload = container.decrypt()

        IjiamiPayload(PayloadDex.split(payload), container.opaqueRanges)
            .method("Lcom/ijiami/residconfusion/ConfusionUtils;", "check")
            .returnEarly()

        val rebuilt = container.rebuild(payload)
        assertEquals(rebuilt.size, rebuilt.readInt(32), "$name")

        val signature = MessageDigest.getInstance("SHA-1").digest(rebuilt.copyOfRange(32, rebuilt.size))
        assertContentEquals(signature, rebuilt.copyOfRange(12, 32), "$name")
    }
}
