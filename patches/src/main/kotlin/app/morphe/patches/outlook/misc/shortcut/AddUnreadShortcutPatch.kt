/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.outlook.misc.shortcut

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.Patch
import app.morphe.patcher.patch.mutableClassDefBy
import app.morphe.patcher.patch.patch
import app.morphe.patcher.resource.ResourceType
import app.morphe.patches.outlook.misc.fix.signin.fixSignInPatch
import app.morphe.patches.outlook.misc.settings.OUTLOOK_PATCH_SETTINGS_CLASS
import app.morphe.patches.outlook.misc.settings.markPatchApplied
import app.morphe.patches.outlook.misc.settings.patchesSettingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.extension.outlookAppliedPatchesExtensionPatch
import app.morphe.patches.shared.misc.extension.outlookUnreadShortcutExtensionPatch
import app.morphe.patches.shared.misc.smali.replaceResourceIdStubs
import app.morphe.patches.shared.misc.smali.replaceStub
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

private const val UNREAD_SHORTCUT_CLASS = "Lapp/hxreborn/extension/outlook/shortcut/UnreadShortcut;"

@Suppress("unused")
val addUnreadShortcutPatch: Patch = patch(
    name = "Add unread shortcut",
    description = "Adds an Unread shortcut to the app icon's long-press menu that opens unread mail from all " +
        "accounts and folders. Launchers with four shortcut slots show it in place of View calendar.",
) {
    dependsOn(outlookAppliedPatchesExtensionPatch)
    compatibleWith(AppCompatibilities.OUTLOOK)

    dependsOn(fixSignInPatch, patchesSettingsPatch, outlookUnreadShortcutExtensionPatch)

    execute {
        markPatchApplied("addUnreadShortcut", addUnreadShortcutPatch)
        SearchDeepLinkParserFingerprint.originalMethod

        mutableClassDefBy(UNREAD_SHORTCUT_CLASS).apply {
            replaceResourceIdStubs(ResourceType.DRAWABLE, mapOf("iconId" to "ic_fluent_mail_unread_24_regular"))
            replaceResourceIdStubs(ResourceType.STRING, mapOf("labelId" to "unread"))
            replaceResourceIdStubs(ResourceType.COLOR, mapOf("colorId" to "brand_outlook"))
        }
        mutableClassDefBy(OUTLOOK_PATCH_SETTINGS_CLASS).replaceStub(
            "updateUnreadShortcut",
            1,
            """
                invoke-static {p0}, $UNREAD_SHORTCUT_CLASS->update(Landroid/content/Context;)V
                return-void
            """,
        )

        val directShareShortcuts = DirectShareShortcutsFingerprint.match()
        val (setDynamicShortcutsCall) = directShareShortcuts.instructionMatches
        directShareShortcuts.method.apply {
            val call = getInstruction<FiveRegisterInstruction>(setDynamicShortcutsCall.index)
            replaceInstruction(
                setDynamicShortcutsCall.index,
                "invoke-static {v${call.registerC}, v${call.registerD}}, " +
                    "$UNREAD_SHORTCUT_CLASS->setDynamicShortcuts(Landroid/content/pm/ShortcutManager;Ljava/util/List;)Z",
            )
        }
    }
}
