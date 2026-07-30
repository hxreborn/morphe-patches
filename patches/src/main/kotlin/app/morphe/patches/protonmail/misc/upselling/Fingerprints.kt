/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonmail.misc.upselling

import app.morphe.patcher.Fingerprint
import app.morphe.patches.all.misc.resources.ResourceType
import app.morphe.patches.all.misc.resources.resourceLiteral

private val SIDEBAR_UPSELLING_LABELS = listOf(
    "drawer_upgrade_plus",
    "drawer_upgrade_unlimited",
    "drawer_upgrade_plus_black_friday",
    "drawer_upgrade_plus_spring_sale",
    "drawer_upgrade_plus_summer_sale",
)

internal val sidebarUpsellingRowFingerprints = SIDEBAR_UPSELLING_LABELS.map { label ->
    Fingerprint(
        returnType = "V",
        filters = listOf(
            resourceLiteral(ResourceType.STRING, label, exceptionIfResourceNotFound = false),
        ),
    )
}

internal object UpgradeUpsellingButtonFingerprint : Fingerprint(
    returnType = "V",
    filters = listOf(
        resourceLiteral(ResourceType.STRING, "upselling_button_item_content_description"),
    ),
)
