/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.aliexpress.misc.signin

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.listDecodableFiles
import app.morphe.patcher.patch.patch
import app.morphe.patcher.patch.resourceFile
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

internal const val SIGNING_ARCHIVE_NAME = "hx_aliexpress_security.apk"

internal val signingDataPatch = patch {
    execute {
        val signaturePath = listDecodableFiles("META-INF/*.RSA").singleOrNull()
            ?: throw PatchException("Expected one AliExpress signing certificate entry")
        val securityDataPath = "res/drawable/yw_1222.jpg"
        val securityData = resourceFile(securityDataPath)
        if (!securityData.isFile) {
            throw PatchException("AliExpress signing data not found")
        }
        ZipOutputStream(resourceFile("assets/$SIGNING_ARCHIVE_NAME", false).outputStream()).use { archive ->
            for (path in listOf(signaturePath, securityDataPath)) {
                archive.putNextEntry(ZipEntry(path).apply { time = 0 })
                resourceFile(path).inputStream().use { it.copyTo(archive) }
                archive.closeEntry()
            }
        }
    }
}
