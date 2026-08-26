/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.photoeditorpro.diagnostics

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

private const val URI = "Landroid/net/Uri;"
private const val STRING = "Ljava/lang/String;"
private const val FILE_INPUT_STREAM = "Ljava/io/FileInputStream;"

private fun Iterable<CharSequence>.types() = map(CharSequence::toString)

internal object StoragePutFileFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf(URI),
    custom = { method, classDef ->
        method.returnType != "V" &&
            classDef.methods.any {
                it.parameterTypes.types() == listOf(STRING) && it.returnType == classDef.type
            } &&
            classDef.methods.any { it.parameterTypes.types() == listOf(FILE_INPUT_STREAM) }
    },
)
