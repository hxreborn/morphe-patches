/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonmail.misc.layout

import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.protonmail.misc.settings.patchesSettingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.proton.markFeaturePatched
import app.morphe.patches.shared.misc.proton.markPatchMetadata
import app.morphe.patches.shared.misc.proton.returnVoidWhen

private const val SIDEBAR_ENTRIES_CLASS = "Lapp/hxreborn/extension/protonmail/SidebarEntries;"

@Suppress("unused")
val hideLayoutComponentsPatch: BytecodePatch = bytecodePatch(
    name = "Hide layout components",
    description = "Adds settings to hide the Subscription and Report a problem sidebar entries.",
) {
    compatibleWith(AppCompatibilities.PROTON_MAIL)
    dependsOn(patchesSettingsPatch)

    execute {
        markPatchMetadata("hideLayoutComponents", hideLayoutComponentsPatch)
        markFeaturePatched(SIDEBAR_ENTRIES_CLASS)

        SubscriptionEntryFingerprint.matchSingle().method
            .returnVoidWhen("$SIDEBAR_ENTRIES_CLASS->isSubscriptionHidden()Z")
        ReportAProblemEntryFingerprint.matchSingle().method
            .returnVoidWhen("$SIDEBAR_ENTRIES_CLASS->isReportAProblemHidden()Z")
    }
}
