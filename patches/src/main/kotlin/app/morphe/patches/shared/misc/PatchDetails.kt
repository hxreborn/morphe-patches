/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc

import app.morphe.patcher.patch.Patch
import app.morphe.patcher.patch.mutableClassDefBy
import app.morphe.patcher.util.returnEarly
import com.android.tools.smali.util.StringUtils.escapeString

internal fun returnTrueFrom(extensionClass: String, methodName: String) =
    mutableClassDefBy(extensionClass).methods.single { it.name == methodName }.returnEarly(true)

internal fun fillPatchDetails(
    extensionClass: String,
    patch: Patch,
    nameMethod: String,
    descriptionMethod: String?,
) {
    val methods = mutableClassDefBy(extensionClass).methods
    methods.single { it.name == nameMethod }.returnEarly(escapeString(checkNotNull(patch.name)))
    if (descriptionMethod != null) {
        methods.single { it.name == descriptionMethod }.returnEarly(escapeString(checkNotNull(patch.description)))
    }
}

internal fun fillPatchMetadata(
    extensionClass: String,
    methodName: String,
    patch: Patch,
    withDescription: Boolean = true,
) = fillPatchDetails(
    extensionClass,
    patch,
    methodName + "Name",
    (methodName + "Description").takeIf { withDescription },
)

internal fun fillAppliedPatch(
    extensionClass: String,
    methodName: String,
    patch: Patch,
    withDescription: Boolean = true,
) {
    returnTrueFrom(extensionClass, methodName)
    fillPatchMetadata(extensionClass, methodName, patch, withDescription)
}
