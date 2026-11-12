/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.idlefish.misc.fix.signature

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.resourcePatch
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

private const val SIGNING_ARCHIVE_NAME = "hx_security.apk"
private const val SECURITY_DATA_PATH = "res/drawable/yw_1222.jpg"

internal val signingDataPatch = resourcePatch {
    execute {
        val signaturePaths = listDecodableFiles("META-INF/*.RSA")
        val signaturePath = signaturePaths.singleOrNull()
            ?: throw PatchException("Expected one META-INF/*.RSA signing certificate entry, found ${signaturePaths.size}")
        if (!get(SECURITY_DATA_PATH).isFile) {
            throw PatchException("Signing data not found: $SECURITY_DATA_PATH")
        }
        val securityLibraries = listApkEntries("lib/").filter { it.substringAfterLast('/').startsWith("libsg") }

        ZipOutputStream(get("assets/$SIGNING_ARCHIVE_NAME", false).outputStream()).use { archive ->
            for (path in listOf(signaturePath, SECURITY_DATA_PATH) + securityLibraries) {
                archive.putNextEntry(ZipEntry(path).apply { time = 0 })
                get(path).inputStream().use { it.copyTo(archive) }
                archive.closeEntry()
            }
        }
    }
}
