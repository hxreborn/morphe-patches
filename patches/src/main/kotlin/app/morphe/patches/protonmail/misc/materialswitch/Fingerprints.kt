/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonmail.misc.materialswitch

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val MATERIAL3_PACKAGE = "Landroidx/compose/material3/"
private const val VIEW_INTEROP_PACKAGE = "Landroidx/compose/ui/viewinterop/"

internal object MaterialSwitchFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Z", "L", "L", "Z", "L", "L", "I"),
    filters = listOf(
        methodCall(
            parameters = listOf("L", "Z", "L", "Z", "L", "L"),
            returnType = "L",
            opcodes = listOf(Opcode.INVOKE_STATIC, Opcode.INVOKE_STATIC_RANGE),
        ),
    ),
    custom = { _, classDef -> classDef.type.startsWith(MATERIAL3_PACKAGE) },
)

internal fun switchWrapperFingerprint(materialSwitch: MethodReference) = Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Z", "L", "L", "Z", "L", "I", "I"),
    filters = listOf(methodCall(reference = materialSwitch)),
)

internal fun androidViewFingerprint(function: String, modifier: String, composer: String) = Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf(function, modifier, function, composer, "I", "I"),
    custom = { _, classDef -> classDef.type.startsWith(VIEW_INTEROP_PACKAGE) },
)
