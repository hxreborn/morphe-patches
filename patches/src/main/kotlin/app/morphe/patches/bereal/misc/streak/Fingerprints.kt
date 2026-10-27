/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.bereal.misc.streak

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.newInstance
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

internal object StreakRecoveryOfferEligibleToStringFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    parameters = emptyList(),
    strings = listOf("Eligible(offerId="),
)

internal object StreakSaveOfferEligibleToStringFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    parameters = emptyList(),
    strings = listOf("Eligible(currentStreak="),
)

internal fun eligibilityCheckFingerprint(eligibleType: String) = Fingerprint(
    returnType = "Ljava/lang/Object;",
    parameters = listOf("L"),
    filters = listOf(newInstance(eligibleType)),
)

internal fun notEligibleSingletonFingerprint(eligibleInterfaces: List<String>) = Fingerprint(
    name = "<clinit>",
    interfaces = eligibleInterfaces,
    filters = listOf(fieldAccess(definingClass = "this", opcode = Opcode.SPUT_OBJECT)),
)

internal fun notEligibleReadFingerprint(notEligible: FieldReference) = Fingerprint(
    filters = listOf(fieldAccess(reference = notEligible, opcode = Opcode.SGET_OBJECT)),
)
