/*
 * Copyright (C) 2026 Hoo-dles
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from hoo-dles/morphe-patches:
 * https://github.com/hoo-dles/morphe-patches/commit/3ad54cf13090739041b9f74c64c95e9994a0d980
 * Commit 3ad54cf13090739041b9f74c64c95e9994a0d980 (2026-07-27),
 * patches/src/main/kotlin/hoodles/morphe/patches/protonvpn/customdns/Fingerprints.kt
 */
package app.morphe.patches.protonvpn.misc.customdns

import app.morphe.patcher.Fingerprint
import app.morphe.patches.all.misc.resources.ResourceType
import app.morphe.patches.protonvpn.misc.anchors.resourceField
import app.morphe.patches.protonvpn.misc.restrictions.RestrictionGuardFingerprint

internal object CustomDnsViewStateFingerprint : Fingerprint(
    name = "<init>",
    parameters = listOf("Z", "Ljava/util/List;", "L", "Z", "Z"),
    filters = listOf(resourceField(ResourceType.STRING, "settings_custom_dns_title")),
)

internal object CustomDnsRestrictionFingerprint : RestrictionGuardFingerprint("getCustomDns")
