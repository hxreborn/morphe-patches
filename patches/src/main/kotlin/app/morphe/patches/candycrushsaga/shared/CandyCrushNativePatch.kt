/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.candycrushsaga.shared

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import java.security.MessageDigest
import org.w3c.dom.Element

private const val HASH_BUFFER_SIZE = 65536

internal const val SODA_LIBRARY_WITH_EXTRA_MOVES_SHA256 = "187479e4681064a198aa0a67b5af3456c45fe44765e65aee962ecfd7da789e44"

internal class CandyCrushNativeBuild(val libraryEntry: String, val sha256Hex: String, val resourceDirectory: String)

internal fun isCandyCrushSodaSaga(packageName: String) =
    packageName == AppCompatibilities.CANDY_CRUSH_SODA_SAGA.packageName

internal fun candyCrushNativeBuild(packageName: String): CandyCrushNativeBuild = when (packageName) {
    AppCompatibilities.CANDY_CRUSH_SAGA.packageName -> CandyCrushNativeBuild(
        "lib/arm64-v8a/libcandycrushsaga.so",
        "f658c3ae188b11f10d5547590e1a0172cf95c3255f1e4f5576841acf68fb487e",
        "candycrushsaga",
    )
    AppCompatibilities.CANDY_CRUSH_SODA_SAGA.packageName -> CandyCrushNativeBuild(
        "lib/arm64-v8a/libstritz.so",
        "1fd641f801e1e3ed690172ab37034cef2876d4df812aab9700a0c39ec09a5a23",
        "candycrushsodasaga",
    )
    else -> throw PatchException("Unsupported Candy Crush package: $packageName")
}

internal val candyCrushNativePatch = resourcePatch {
    execute {
        val build = candyCrushNativeBuild(packageMetadata.packageName)
        val gameLibrary = get(build.libraryEntry)
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
        if (hash != build.sha256Hex) {
            throw PatchException("Unsupported Candy Crush native library: SHA-256 $hash, expected ${build.sha256Hex}")
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

