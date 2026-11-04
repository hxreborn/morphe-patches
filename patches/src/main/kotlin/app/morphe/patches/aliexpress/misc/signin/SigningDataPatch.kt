/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.aliexpress.misc.signin

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.rawResourcePatch
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

internal const val SIGNING_ARCHIVE_NAME = "hx_aliexpress_security.apk"

internal val signingDataPatch = rawResourcePatch {
    execute {
        val signaturePath = listDecodableFiles("META-INF/*.RSA").singleOrNull()
            ?: throw PatchException("Expected one AliExpress signing certificate entry")
        val securityDataPath = "res/drawable/yw_1222.jpg"
        val securityData = get(securityDataPath)
        if (!securityData.isFile) {
            throw PatchException("AliExpress signing data not found")
        }
        ZipOutputStream(get("assets/$SIGNING_ARCHIVE_NAME", false).outputStream()).use { archive ->
            for (path in listOf(signaturePath, securityDataPath)) {
                archive.putNextEntry(ZipEntry(path).apply { time = 0 })
                get(path).inputStream().use { it.copyTo(archive) }
                archive.closeEntry()
            }
        }
    }
}
