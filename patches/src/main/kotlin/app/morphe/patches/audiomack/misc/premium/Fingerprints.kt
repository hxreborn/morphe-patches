/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.audiomack.misc.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val PREMIUM_STATE_PUBLISH_WINDOW = 3
private const val LYRICS_CONFIGURATION_CONSTRUCTOR_WINDOW = 3
private const val LYRICS_CONFIGURATION_READ_WINDOW = 2

internal object LyricsConfigurationFingerprint : Fingerprint(
    name = "<init>",
    filters = listOf(
        string("lyrics"),
        methodCall(
            name = "<init>",
            parameters = listOf("Ljava/lang/String;", "Ljava/lang/String;"),
            location = MatchAfterWithin(LYRICS_CONFIGURATION_CONSTRUCTOR_WINDOW),
        ),
        fieldAccess(opcode = Opcode.IPUT_OBJECT, location = MatchAfterImmediately()),
    ),
)

internal fun lyricsAvailabilityFingerprint(configurationField: FieldReference) = Fingerprint(
    filters = listOf(
        fieldAccess(
            definingClass = configurationField.definingClass,
            name = configurationField.name,
            type = configurationField.type,
            opcode = Opcode.IGET_OBJECT,
        ),
        methodCall(
            parameters = listOf(configurationField.type),
            returnType = "Ljava/lang/String;",
            location = MatchAfterWithin(LYRICS_CONFIGURATION_READ_WINDOW),
        ),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
    ),
)

internal object PremiumRepositoryFingerprint : Fingerprint(
    parameters = emptyList(),
    strings = listOf("in_app_purchase_mode"),
)

internal object PremiumStatusFingerprint : Fingerprint(
    classFingerprint = PremiumRepositoryFingerprint,
    parameters = listOf("Z", "Ljava/lang/Long;"),
    returnType = "Z",
)

internal object PremiumStateFingerprint : Fingerprint(
    classFingerprint = PremiumRepositoryFingerprint,
    parameters = emptyList(),
    returnType = "Z",
    filters = listOf(
        fieldAccess(opcode = Opcode.IGET_OBJECT),
        methodCall(definingClass = "Ljava/lang/Boolean;", name = "booleanValue"),
    ),
)

internal fun premiumStatePublisherFingerprint(stateField: FieldReference) = Fingerprint(
    filters = listOf(
        fieldAccess(definingClass = stateField.definingClass, name = stateField.name, type = stateField.type),
        methodCall(
            definingClass = stateField.type,
            parameters = listOf("Ljava/lang/Object;", "Ljava/lang/Object;"),
            returnType = "Z",
            location = MatchAfterWithin(PREMIUM_STATE_PUBLISH_WINDOW),
        ),
    ),
)
