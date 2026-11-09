/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonvpn.misc.navigationbar

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.anyInstruction
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.util.getReference
import app.morphe.patches.protonvpn.misc.anchors.resourceField
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal object MainBottomBarFingerprint : Fingerprint(
    strings = listOf("mainBottomBar"),
    filters = listOf(
        methodCall(
            returnType = "V",
            parameters = listOf("L", "Z", "Z", "L", "Ljava/util/Set;", "L", "L", "I", "I"),
        ),
    ),
)

internal fun mainScaffoldFingerprint(bottomBarType: String) = Fingerprint(
    filters = listOf(methodCall(
        parameters = listOf("L", "L", "L", "L", "L", "I", "J", "J", "L", "L", "L", "I", "I"),
        returnType = "V",
    )),
    custom = { method, _ ->
        method.implementation?.instructions?.any {
            it.getReference<MethodReference>()?.let { reference ->
                reference.definingClass == bottomBarType && reference.name == "<init>"
            } == true
        } == true
    },
)

internal fun navigationBarFingerprint(bar: MethodReference) = Fingerprint(
    definingClass = bar.definingClass,
    name = bar.name,
    parameters = bar.parameterTypes.map(CharSequence::toString),
    filters = listOf(
        anyInstruction(
            methodCall(returnType = "V", parameters = listOf("L", "J", "J", "F", "L", "L", "L", "I")),
            methodCall(returnType = "V", parameters = listOf("L", "J", "J", "F", "L", "L", "L", "I", "I")),
        ),
    ),
)

internal fun navigationInsetsContentFingerprint(holder: String, insetType: String, modifierType: String) = Fingerprint(
    filters = listOf(methodCall(parameters = listOf(modifierType, insetType), returnType = modifierType)),
    custom = { method, _ ->
        method.implementation?.instructions?.any {
            it.getReference<FieldReference>()?.let { field ->
                field.definingClass == holder && field.type == insetType
            } == true
        } == true
    },
)

private const val INSET_CONSTRUCTOR_WINDOW = 3

internal fun navigationInsetSidesFingerprint(bar: MethodReference, constructor: MethodReference) = Fingerprint(
    definingClass = bar.definingClass,
    name = bar.name,
    parameters = bar.parameterTypes.map(CharSequence::toString),
    filters = listOf(
        fieldAccess(opcode = com.android.tools.smali.dexlib2.Opcode.SGET, type = "I"),
        methodCall(
            definingClass = constructor.definingClass,
            name = constructor.name,
            parameters = constructor.parameterTypes.map(CharSequence::toString),
            location = MatchAfterWithin(INSET_CONSTRUCTOR_WINDOW),
        ),
    ),
)

internal fun navigationIndicatorFingerprint(bar: MethodReference) = Fingerprint(
    definingClass = bar.definingClass,
    name = bar.name,
    parameters = bar.parameterTypes.map(CharSequence::toString),
    filters = listOf(
        methodCall(parameters = listOf("J", "F"), returnType = "J"),
        opcode(Opcode.MOVE_RESULT_WIDE, MatchAfterImmediately()),
        methodCall(parameters = listOf("J", "J"), returnType = "J", location = MatchAfterImmediately()),
        opcode(Opcode.MOVE_RESULT_WIDE, MatchAfterImmediately()),
    ),
)

internal object PaddingValuesFingerprint : Fingerprint(strings = listOf("PaddingValues(start="))

internal fun mainContentPaddingFingerprint(navType: String, modifierType: String, paddingType: String) = Fingerprint(
    strings = listOf("paddingValues"),
    filters = listOf(
        methodCall(parameters = listOf(modifierType, paddingType), returnType = modifierType),
        opcode(Opcode.MOVE_RESULT_OBJECT, MatchAfterImmediately()),
        methodCall(definingClass = navType, parameters = listOf(modifierType, "L", "I"), returnType = "V"),
    ),
)

internal fun settingsScreenFingerprint(modifier: String) = Fingerprint(filters = listOf(
    resourceField(ResourceType.STRING, "settings_title"),
    methodCall(parameters = listOf(modifier, "I", "L", "L", "L", "L", "L", "L", "I", "I"), returnType = "V"),
))

internal fun settingsScrollFingerprint(type: String, modifier: String, padding: String) = Fingerprint(
    definingClass = type,
    filters = listOf(
        methodCall(parameters = listOf(modifier, padding), returnType = modifier),
        opcode(Opcode.MOVE_RESULT_OBJECT, MatchAfterImmediately()),
        methodCall(parameters = listOf("I", "L", "I"), returnType = "L", location = MatchAfterImmediately()),
        opcode(Opcode.MOVE_RESULT_OBJECT, MatchAfterImmediately()),
        methodCall(parameters = listOf(modifier, "L"), returnType = modifier, location = MatchAfterImmediately()),
        opcode(Opcode.MOVE_RESULT_OBJECT, MatchAfterImmediately()),
    ),
)

internal fun profileButtonPaddingFingerprint(modifier: String) = Fingerprint(
    filters = listOf(
        resourceField(ResourceType.STRING, "profiles_button_create_profile"),
        methodCall(parameters = listOf(modifier, "F", "F", "F", "F", "I"), returnType = modifier),
        opcode(Opcode.MOVE_RESULT_OBJECT, MatchAfterImmediately()),
    ),
)

internal fun countriesListFingerprint(modifier: String, padding: String) = Fingerprint(
    strings = listOf("items", "onItemOpen", "onItemClick", "onOpenInfo", "navigateToUpsell"),
    filters = listOf(methodCall(parameters = listOf(modifier, "L", padding, "L", "L", "L", "Z", "L", "L", "L", "I", "I"), returnType = "V")),
)

internal fun recentsListFingerprint(list: MethodReference) = Fingerprint(
    strings = listOf("item IDs"),
    filters = listOf(methodCall(definingClass = list.definingClass, name = list.name, parameters = list.parameterTypes.map(CharSequence::toString))),
)
