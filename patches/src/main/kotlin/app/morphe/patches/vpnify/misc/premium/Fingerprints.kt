/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.vpnify.misc.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.opcode
import app.morphe.patches.all.misc.resources.ResourceType
import app.morphe.patches.all.misc.resources.resourceLiteral
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object SubscriptionActiveFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "Z",
    parameters = emptyList(),
    strings = listOf("subscription_active"),
)

internal object AutoRenewingSubscriptionFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "Z",
    parameters = emptyList(),
    strings = listOf("is_auto_renewing_subscription"),
)

internal object PremiumActivatedTitleFingerprint : Fingerprint(
    filters = listOf(
        opcode(Opcode.INSTANCE_OF),
        resourceLiteral(
            ResourceType.STRING,
            "premium_activated_title",
            location = InstructionLocation.MatchAfterWithin(2),
        ),
    ),
)

internal fun premiumActivatedCallbackFingerprint(premiumActivatedType: String) = Fingerprint(
    filters = listOf(
        fieldAccess(type = premiumActivatedType, opcode = Opcode.SGET_OBJECT),
        opcode(Opcode.INVOKE_INTERFACE, InstructionLocation.MatchAfterImmediately()),
    ),
)
