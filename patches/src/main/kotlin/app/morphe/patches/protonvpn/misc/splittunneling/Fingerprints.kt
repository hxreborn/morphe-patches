/*
 * Copyright (C) 2026 Hoo-dles
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from hoo-dles/morphe-patches:
 * https://github.com/hoo-dles/morphe-patches/commit/3ad54cf13090739041b9f74c64c95e9994a0d980
 * Commit 3ad54cf13090739041b9f74c64c95e9994a0d980 (2026-07-27),
 * patches/src/main/kotlin/hoodles/morphe/patches/protonvpn/splittunneling/Fingerprints.kt
 */
package app.morphe.patches.protonvpn.misc.splittunneling

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.methodCall
import app.morphe.patcher.resource.ResourceType
import app.morphe.patches.protonvpn.misc.anchors.resourceField
import app.morphe.patches.protonvpn.misc.restrictions.restrictionGuardFingerprint
import com.android.tools.smali.dexlib2.Opcode

internal object SplitTunnelingViewStateFingerprint : Fingerprint(
    name = "<init>",
    filters = listOf(resourceField(ResourceType.STRING, "settings_split_tunneling_title")),
)

internal fun splitTunnelingRestrictionFingerprint() = restrictionGuardFingerprint("getSplitTunneling")

private const val SPLIT_TUNNELING_SETTINGS = "Lcom/protonvpn/android/settings/data/SplitTunnelingSettings;"

internal const val INCLUDED_APPS_GETTER = "getIncludedApps"
internal const val EXCLUDED_APPS_GETTER = "getExcludedApps"

internal object AppsSplitTunnelingFingerprint : Fingerprint(
    returnType = "V",
    filters = listOf(
        methodCall(definingClass = SPLIT_TUNNELING_SETTINGS, name = INCLUDED_APPS_GETTER),
        methodCall(definingClass = SPLIT_TUNNELING_SETTINGS, name = EXCLUDED_APPS_GETTER),
    ),
    strings = listOf("myPackageName"),
)

private const val SUMMARY_TO_ICON_DISTANCE = 30
private const val SETTINGS_ITEM_CALL_DISTANCE = 8

internal object IncludedAppsRowFingerprint : Fingerprint(
    returnType = "Ljava/lang/Object;",
    filters = listOf(
        methodCall(
            opcode = Opcode.INVOKE_STATIC,
            returnType = "Ljava/lang/String;",
            parameters = listOf("Ljava/util/List;", "L"),
        ),
        resourceField(
            ResourceType.DRAWABLE,
            "ic_proton_mobile",
            location = MatchAfterWithin(SUMMARY_TO_ICON_DISTANCE),
        ),
        methodCall(
            opcode = Opcode.INVOKE_STATIC_RANGE,
            returnType = "V",
            parameters = listOf("L", "I", "Ljava/lang/String;", "L", "L", "Z", "Z", "Z", "L", "L", "I", "I"),
            location = MatchAfterWithin(SETTINGS_ITEM_CALL_DISTANCE),
        ),
    ),
)
