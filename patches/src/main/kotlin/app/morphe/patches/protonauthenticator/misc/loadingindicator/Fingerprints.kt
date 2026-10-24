/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonauthenticator.misc.loadingindicator

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patcher.string
import app.morphe.patches.shared.misc.proton.pullRefreshContentFingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

internal val PullToRefreshContentFingerprint = pullRefreshContentFingerprint(
    "Landroidx/compose/material3/ProgressIndicatorKt;",
    "Landroidx/compose/material3/pulltorefresh/PullToRefreshKt;",
)

internal object PullToRefreshIndicatorColorsFingerprint : Fingerprint(
    filters = listOf(
        string("\$this\$PullToRefreshBox"),
        methodCall(
            definingClass = "Landroidx/compose/material3/pulltorefresh/PullToRefreshDefaults;",
            returnType = "V",
        ),
    ),
)

internal object ColorRolesFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    parameters = listOf("I", "Z"),
    filters = listOf(90L, 40L, 100L, 10L, 80L, 20L, 30L).map(::literal),
)

internal fun colorRoleGetterFingerprint(role: FieldReference) = Fingerprint(
    definingClass = role.definingClass,
    returnType = "I",
    parameters = emptyList(),
    filters = listOf(fieldAccess(role, Opcode.IGET)),
)
