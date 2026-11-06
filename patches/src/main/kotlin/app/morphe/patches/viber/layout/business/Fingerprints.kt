/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.layout.business

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.string
import app.morphe.patches.viber.shared.preferenceFilters

internal object ChatSelectionFingerprint : Fingerprint(
    returnType = "Ljava/lang/String;",
    parameters = listOf(),
    filters = listOf(
        string("[conversations].[deleted] = 0 and "),
        string("[conversations].[grouping_key] = 'customers_inbox_'||?"),
    ),
)

internal object BusinessInboxIntroPreferenceFingerprint : Fingerprint(
    filters = preferenceFilters("pref_business_chat_inbox_show_intro_dialog_ftue"),
)

internal object BusinessInboxTooltipPreferenceFingerprint : Fingerprint(
    filters = preferenceFilters("pref_business_chat_inbox_show_tooltip_ftue"),
)
