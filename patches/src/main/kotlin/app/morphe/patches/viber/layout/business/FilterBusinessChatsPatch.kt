/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.layout.business

import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.viber.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.viber.misc.settings.patchesSettingsPatch
import app.morphe.patches.viber.shared.addFilterCallAfter
import app.morphe.patches.viber.shared.markPatchApplied
import app.morphe.patches.viber.shared.redirectPreferences

private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/hxreborn/extension/viber/BusinessChats;"

@Suppress("unused")
val filterBusinessChatsPatch: BytecodePatch = bytecodePatch(
    name = "Filter business chats",
    description = "Filters selected business chats after reading and removes inbox discovery prompts. Shows unread chats and verification-code chats.",
) {
    compatibleWith(AppCompatibilities.VIBER)
    dependsOn(spoofSignaturePatch, patchesSettingsPatch)
    execute {
        markPatchApplied("filterBusinessChats", filterBusinessChatsPatch)
        ChatSelectionFingerprint.matchSingle().apply {
            val (selectionPrefix, _) = instructionMatches
            method.addFilterCallAfter(selectionPrefix.index, "$EXTENSION_CLASS_DESCRIPTOR->filterSelection(Ljava/lang/String;)Ljava/lang/String;")
        }
        listOf(BusinessInboxIntroPreferenceFingerprint, BusinessInboxTooltipPreferenceFingerprint)
            .map { it.matchSingle() }
            .redirectPreferences(
                "$EXTENSION_CLASS_DESCRIPTOR->preferenceKey(Ljava/lang/String;)Ljava/lang/String;",
                "$EXTENSION_CLASS_DESCRIPTOR->showPrompt(Z)Z",
            )
    }
}
