/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonauthenticator.misc.loadingindicator

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import app.morphe.patches.shared.misc.proton.pullRefreshContentFingerprint
import app.morphe.patches.shared.misc.proton.sizeModifierCall
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val PullToRefreshContentFingerprint = pullRefreshContentFingerprint(
    "Landroidx/compose/material3/ProgressIndicatorKt;",
    "Landroidx/compose/material3/pulltorefresh/PullToRefreshKt;",
)

internal object PullToRefreshIndicatorSizeFingerprint : Fingerprint(
    filters = listOf(
        methodCall(
            definingClass = "Landroidx/compose/material3/pulltorefresh/PullToRefreshKt;",
            name = "getSpinnerContainerSize",
            returnType = "F",
        ),
        opcode(Opcode.MOVE_RESULT, MatchAfterImmediately()),
        sizeModifierCall(MatchAfterImmediately()),
    ),
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

internal object ColorRoleToneFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.STATIC),
    parameters = listOf("I", "I"),
    returnType = "I",
    filters = listOf(
        methodCall(parameters = listOf("I")),
        opcode(Opcode.INT_TO_FLOAT, MatchAfterWithin(1)),
        methodCall(parameters = listOf("F"), returnType = "V", location = MatchAfterImmediately()),
        methodCall(parameters = emptyList(), returnType = "I", location = MatchAfterImmediately()),
    ),
)

internal fun hctFromComponentsFingerprint(hctType: String) = Fingerprint(
    definingClass = hctType,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    parameters = listOf("F", "F", "F"),
    returnType = hctType,
)

internal fun hctComponentGetterFingerprint(hctType: String, name: String) = Fingerprint(
    definingClass = hctType,
    name = name,
    parameters = emptyList(),
    returnType = "F",
)
