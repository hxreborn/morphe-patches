/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.ultimea.misc.equalizer

import app.morphe.patches.ultimea.misc.settings.NativeMethods
import app.morphe.patches.ultimea.misc.settings.replaceWithPlatformMethod
import java.io.File
import org.junit.jupiter.api.Assumptions.assumeTrue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NativeEqualizerNavigationTest {
    @Test
    fun replaceTileCallbackWithOnlyPlatformInvocationOnStockVersions() {
        val stocks = System.getenv("ULTIMEA_NATIVE_STOCKS")
        assumeTrue(stocks != null, "Set ULTIMEA_NATIVE_STOCKS to native stock libraries")
        for (path in stocks!!.split(File.pathSeparator)) {
            val bytes = File(path).readBytes()
            val original = NativeMethods(bytes)
            val callback = original.equalizerTileCallback()
            val route = callback.loads("/device-eq").single()
            val log = original.single("type", "log")
            val dispatch = original.firstCallAfter(log.loads("log").last(), log.end)
            val target = original.callTarget(dispatch)
            val edited = bytes.copyOf()
            edited.replaceWithPlatformMethod(callback, route, original)
            val changed = NativeMethods(edited)
            val tile = changed.equalizerTileCallback()
            assertEquals(callback.start, tile.start)
            assertEquals(target, changed.callTarget(changed.calls(tile).single()))
            assertTrue(bytes.indices.filter { bytes[it] != edited[it] }.all { it in callback.start until callback.end })
            println("Verified $path callback=0x${tile.start.toString(16)} routeLoads=${tile.loads("/device-eq").size} platformCalls=${changed.calls(tile).size}")
        }
    }
}
