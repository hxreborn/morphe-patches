/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.lumo.misc.materialswitch

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.AccessFlags

internal object MaterialSwitchFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Z", "L", "Z", "L", "L", "I"),
    filters = listOf(
        methodCall(parameters = listOf("L", "Z", "Z", "L", "L", "L", "L", "I"), returnType = "V"),
    ),
)

internal fun androidViewFingerprint(modifier: String, composer: String) = Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("L", modifier, "L", composer, "I", "I"),
    custom = { method, _ -> method.parameterTypes[0] == method.parameterTypes[2] },
)
