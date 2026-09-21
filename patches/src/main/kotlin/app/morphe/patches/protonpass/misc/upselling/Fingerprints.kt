/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonpass.misc.upselling

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.fieldAccess
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

private const val UPGRADE_AVAILABLE_FIELD = "isUpgradeAvailable"
private const val HOME_NAVIGATION_CLASS = "Lproton/android/pass/features/home/HomeNavigation"
private const val ONBOARDING_ROUTE = "$HOME_NAVIGATION_CLASS\$OnBoarding;"
private const val UPSELL_ONBOARDING_ROUTE = "$HOME_NAVIGATION_CLASS\$UpsellV2AndOnboarding;"

internal object UpgradeInfoConstructorFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    returnType = "V",
    parameters = listOf("Z", "Z", "L", "I", "I", "I"),
    filters = listOf(
        fieldAccess(name = UPGRADE_AVAILABLE_FIELD, type = "Z", opcode = Opcode.IPUT_BOOLEAN),
    ),
)

internal object PlanLimitReachedFingerprint : Fingerprint(
    returnType = "Z",
    parameters = listOf("L", "I"),
    filters = listOf(
        fieldAccess(name = UPGRADE_AVAILABLE_FIELD, type = "Z", opcode = Opcode.IGET_BOOLEAN),
    ),
)

internal object OnboardingRouteFingerprint : Fingerprint(
    filters = listOf(
        fieldAccess(type = UPSELL_ONBOARDING_ROUTE, opcode = Opcode.SGET_OBJECT),
        fieldAccess(type = ONBOARDING_ROUTE, opcode = Opcode.SGET_OBJECT, location = MatchAfterWithin(3)),
    ),
)
