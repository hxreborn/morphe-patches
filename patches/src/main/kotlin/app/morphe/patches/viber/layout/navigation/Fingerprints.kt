/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.layout.navigation

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchBeforeAnywhere
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resourceLiteral
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val STOCK_MINIMUM_HOME_TAB_COUNT = 3L

internal object BuildHomeTabsFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        methodCall(
            opcode = Opcode.INVOKE_STATIC,
            parameters = listOf("Ljava/util/ArrayList;", "I", "L"),
            returnType = "Z",
        ),
        string("Home tabs count("),
    ),
)

internal fun tabVisibilityFingerprint(tabVisibilityMethod: MethodReference) = Fingerprint(
    definingClass = tabVisibilityMethod.definingClass,
    name = tabVisibilityMethod.name,
    returnType = tabVisibilityMethod.returnType,
    parameters = tabVisibilityMethod.parameterTypes.map(CharSequence::toString),
)

internal fun homeTabCountRangeFingerprint(tabBuilderType: String) = Fingerprint(
    definingClass = tabBuilderType,
    name = "<clinit>",
    filters = listOf(
        literal(STOCK_MINIMUM_HOME_TAB_COUNT),
        methodCall(definingClass = "Lkotlin/ranges/IntRange;", name = "<init>", parameters = listOf("I", "I")),
    ),
)

internal object InflateHomeTabsFingerprint : Fingerprint(
    filters = listOf(
        resourceLiteral(ResourceType.LAYOUT, "view_bottom_navigation_item"),
        literal(STOCK_MINIMUM_HOME_TAB_COUNT, location = MatchBeforeAnywhere()),
        opcode(Opcode.IF_GT, location = MatchAfterImmediately()),
    ),
)
