/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.candycrushsaga.shared

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.resourcePatch
import java.security.MessageDigest
import org.w3c.dom.Element

private const val GAME_LIBRARY = "lib/arm64-v8a/libcandycrushsaga.so"
private const val GAME_LIBRARY_SHA256 = "f658c3ae188b11f10d5547590e1a0172cf95c3255f1e4f5576841acf68fb487e"
private const val HASH_BUFFER_SIZE = 65536

internal val candyCrushNativePatch = resourcePatch {
    execute {
        val gameLibrary = get(GAME_LIBRARY)
        if (!gameLibrary.isFile) throw PatchException("Missing Candy Crush native library")
        val digest = MessageDigest.getInstance("SHA-256")
        gameLibrary.inputStream().use { input ->
            val buffer = ByteArray(HASH_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        val hash = digest.digest().joinToString("") { "%02x".format(it) }
        if (hash != GAME_LIBRARY_SHA256) {
            throw PatchException("Unsupported Candy Crush native library: SHA-256 $hash, expected $GAME_LIBRARY_SHA256")
        }
        document("AndroidManifest.xml").use { manifest ->
            val application = manifest.getElementsByTagName("application").item(0) as? Element
                ?: throw PatchException("Candy Crush manifest has no application element")
            application.setAttributeNS(
                "http://schemas.android.com/apk/res/android",
                "android:extractNativeLibs",
                "true",
            )
        }
    }
}

