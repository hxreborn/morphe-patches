/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Additional terms under GPLv3 section 7:
 * - You must preserve reasonable legal notices and author attributions in this file.
 * - Modified versions must not misrepresent the origin of this file.
 *
 * Ported from hxreborn/revanced-patches:
 * https://gitlab.com/hxreborn/revanced-patches/-/commit/7cbb1133f9a4328f5df41162c96438cc36527125
 * Commit 7cbb1133f9a4328f5df41162c96438cc36527125 (2026-05-07),
 * patches/src/main/kotlin/app/revanced/patches/showly/misc/premium/Fingerprints.kt
 */
package app.morphe.patches.showly.misc.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.opcode
import app.morphe.patches.all.misc.resources.ResourceType
import app.morphe.patches.all.misc.resources.resourceLiteral
import com.android.tools.smali.dexlib2.Opcode

private const val QONVERSION_INTERNAL = "Lcom/qonversion/android/sdk/internal/QonversionInternal;"

internal object PremiumRepoConstructorFingerprint : Fingerprint(
    strings = listOf("KEY_PREMIUM"),
)

internal object QonversionCheckEntitlementsFingerprint : Fingerprint(
    definingClass = QONVERSION_INTERNAL,
    name = "checkEntitlements",
)

internal object TraktLoginPaywallNavigationFingerprint : Fingerprint(
    filters = listOf(
        resourceLiteral(ResourceType.ID, "actionTraktLoginToPaywall"),
        opcode(Opcode.INVOKE_STATIC),
    ),
)
