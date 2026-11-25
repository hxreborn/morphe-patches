/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonpass.account

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resourceLiteral

internal object FreeAccountsLimitCheckFingerprint : Fingerprint(
    returnType = "Ljava/lang/Object;",
    filters = listOf(resourceLiteral(ResourceType.STRING, "auth_only_1_proton_account_failed")),
)
