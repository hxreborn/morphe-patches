/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonpass.misc.navigationbar

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.anyInstruction
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resourceLiteral
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal const val COMPOSE_MODIFIER = "Landroidx/compose/ui/Modifier;"
internal const val LAZY_LIST_STATE = "Landroidx/compose/foundation/lazy/LazyListState;"
internal const val NESTED_SCROLL_CONNECTION = "Landroidx/compose/ui/input/nestedscroll/NestedScrollConnection;"

internal val scaffoldBottomPaddingCall = methodCall(
    definingClass = "Landroidx/compose/foundation/layout/PaddingValues;",
    name = "calculateBottomPadding-D9Ej5fM",
)
internal val contentPaddingCall = methodCall(name = "padding-qDBjuR0\$default", returnType = COMPOSE_MODIFIER)
internal val contentPaddingResult = opcode(Opcode.MOVE_RESULT_OBJECT, MatchAfterImmediately())
internal val contentSizeAnimationCall = methodCall(
    name = "animateContentSize\$default",
    returnType = COMPOSE_MODIFIER,
    location = MatchAfterWithin(2),
)
internal val contentSizeAnimationResult = opcode(Opcode.MOVE_RESULT_OBJECT, MatchAfterImmediately())

internal object AppContentPaddingFingerprint : Fingerprint(
    strings = listOf("PassAppContent-OfflineIndicator"),
    filters = listOf(
        scaffoldBottomPaddingCall,
        contentPaddingCall,
        contentPaddingResult,
        contentSizeAnimationCall,
        contentSizeAnimationResult,
    ),
)

internal object HomeBottomBarFingerprint : Fingerprint(
    returnType = "V",
    strings = listOf("BottomBarPadding"),
    filters = listOf(methodCall(name = "BottomNavigation-PEIptTM")),
)

internal object MainScaffoldFingerprint : Fingerprint(
    name = "PassAppContent",
    filters = listOf(methodCall(name = "Scaffold-27mzLpw")),
)

internal object AppSnackbarHostFingerprint : Fingerprint(
    filters = listOf(
        methodCall(name = "applyIf", returnType = COMPOSE_MODIFIER),
        opcode(Opcode.MOVE_RESULT_OBJECT, MatchAfterImmediately()),
        methodCall(name = "PassSnackbarHost", location = MatchAfterWithin(1)),
    ),
)

internal object NestedScrollModifierFingerprint : Fingerprint(
    name = "nestedScroll",
    isStatic = true,
    returnType = COMPOSE_MODIFIER,
    parameters = listOf(
        COMPOSE_MODIFIER,
        NESTED_SCROLL_CONNECTION,
        "Landroidx/compose/ui/input/nestedscroll/NestedScrollDispatcher;",
    ),
)

internal object OffsetModifierFingerprint : Fingerprint(
    name = "offset-VpY3zN4",
    isStatic = true,
    returnType = COMPOSE_MODIFIER,
    parameters = listOf(COMPOSE_MODIFIER, "F", "F"),
)

internal object VelocityConstructorFingerprint : Fingerprint(
    definingClass = "Landroidx/compose/ui/unit/Velocity;",
    name = "<init>",
    parameters = listOf("J"),
)

internal object LazyListApplyMeasureResultFingerprint : Fingerprint(
    definingClass = LAZY_LIST_STATE,
    returnType = "V",
    parameters = listOf("Landroidx/compose/foundation/lazy/LazyListMeasureResult;", "Z", "Z"),
)

internal object NavigationBarsPaddingFingerprint : Fingerprint(
    name = "navigationBarsPadding",
    parameters = listOf(COMPOSE_MODIFIER),
    returnType = COMPOSE_MODIFIER,
)

internal fun navigationInsetConversionFingerprint(bar: MethodReference) = Fingerprint(
    definingClass = bar.definingClass,
    name = bar.name,
    parameters = bar.parameterTypes.map(CharSequence::toString),
    filters = listOf(
        anyInstruction(
            methodCall(name = "toDp-u2uoSUM", returnType = "F"),
            methodCall(name = "calculateBottomPadding-D9Ej5fM", returnType = "F"),
        ),
        opcode(Opcode.MOVE_RESULT, MatchAfterImmediately()),
    ),
)

internal fun navigationInsetAnimationFingerprint(bar: MethodReference) = Fingerprint(
    definingClass = bar.definingClass,
    name = bar.name,
    parameters = bar.parameterTypes.map(CharSequence::toString),
    filters = listOf(methodCall(name = "animateDpAsState-AjpBEmI")),
)

internal fun scrollContentFingerprint(resourceName: String) = Fingerprint(
    filters = listOf(
        anyInstruction(
            methodCall(parameters = listOf(COMPOSE_MODIFIER, "Landroidx/compose/foundation/ScrollState;", "Z", "Z"), returnType = COMPOSE_MODIFIER),
            methodCall(parameters = listOf(COMPOSE_MODIFIER, "Landroidx/compose/foundation/ScrollState;", "I"), returnType = COMPOSE_MODIFIER),
        ),
        opcode(Opcode.MOVE_RESULT_OBJECT, MatchAfterImmediately()),
        resourceLiteral(ResourceType.STRING, resourceName),
    ),
)

internal object HomeItemsContentFingerprint : Fingerprint(
    strings = listOf("\$this\$AnimatedVisibility"),
    filters = listOf(methodCall(name = "LazyColumn", returnType = "V")),
    custom = { _, classDef ->
        classDef.methods.any { method ->
            method.name == "<init>" && method.parameterTypes.containsAll(listOf(
                LAZY_LIST_STATE,
                "Lkotlinx/collections/immutable/ImmutableList;",
                "Lkotlinx/collections/immutable/ImmutableSet;",
            ))
        }
    },
)

internal object HomePagedItemsContentFingerprint : Fingerprint(
    strings = listOf("homeLoading"),
    filters = listOf(
        fieldAccess(opcode = Opcode.IGET_OBJECT, type = LAZY_LIST_STATE),
        methodCall(name = "LazyColumn", returnType = "V"),
    ),
)

internal object HomeItemsWithPaddingImplFingerprint : Fingerprint(
    strings = listOf("homeLoading"),
    filters = listOf(methodCall(
        name = "LazyColumn",
        returnType = "V",
        parameters = listOf(COMPOSE_MODIFIER, LAZY_LIST_STATE, "Landroidx/compose/foundation/layout/PaddingValuesImpl;", "L", "L", "L", "Z", "L", "L", "I", "I"),
    )),
)

internal fun nativeBottomNavigationFingerprint(outer: MethodReference) = Fingerprint(
    definingClass = outer.definingClass,
    name = outer.name,
    parameters = outer.parameterTypes.map(CharSequence::toString),
    filters = listOf(methodCall(definingClass = outer.definingClass, name = "BottomNavigation-_UMDTes", returnType = "V")),
)
