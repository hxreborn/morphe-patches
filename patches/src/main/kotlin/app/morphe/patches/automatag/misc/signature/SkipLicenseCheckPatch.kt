/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.automatag.misc.signature

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.patcher.util.returnEarly
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod

internal val skipLicenseCheckPatch = bytecodePatch {
    execute {
        val match = IsOriginalFingerprint.matchSingle()
        val isOriginal = match.method

        match.classDef.directMethods.remove(isOriginal)
        match.classDef.directMethods.add(
            ImmutableMethod(
                isOriginal.definingClass,
                isOriginal.name,
                isOriginal.parameters,
                isOriginal.returnType,
                isOriginal.accessFlags and AccessFlags.NATIVE.value.inv(),
                null,
                null,
                MutableMethodImplementation(1),
            ).toMutable().apply { returnEarly(true) },
        )
    }
}
