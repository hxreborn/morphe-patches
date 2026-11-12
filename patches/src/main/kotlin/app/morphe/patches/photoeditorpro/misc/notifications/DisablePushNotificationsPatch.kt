/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.photoeditorpro.misc.notifications

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.photoeditorpro.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.photoeditorpro.shared.EXTENSION_PACKAGE
import app.morphe.patches.photoeditorpro.shared.PATCH_APPLICATION_CLASS
import app.morphe.patches.photoeditorpro.shared.markPatchInstalled
import app.morphe.patches.photoeditorpro.shared.markPatchMetadata
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.analytics.putApplicationMetaData

private const val PUSH_NOTIFICATIONS_CLASS = "$EXTENSION_PACKAGE/PushNotifications;"

private val disableNotificationDelegationPatch = resourcePatch {
    execute {
        document("AndroidManifest.xml").use { document ->
            document.putApplicationMetaData("firebase_messaging_notification_delegation_enabled", "false")
        }
    }
}

@Suppress("unused")
val disablePushNotificationsPatch: BytecodePatch = bytecodePatch(
    name = "Disable push notifications",
    description = "Blocks push notifications and the prompt asking to allow them.",
) {
    compatibleWith(AppCompatibilities.PHOTO_EDITOR_PRO)
    dependsOn(spoofSignaturePatch, disableNotificationDelegationPatch)
    extendWith("extensions/extension.mpe")

    execute {
        markPatchMetadata("disablePushNotifications", disablePushNotificationsPatch)

        FirebaseMessagingHandleIntentFingerprint.method.returnEarly()

        NotificationPermissionPromptFingerprint.matchSingle().method.addInstructions(
            0,
            """
                sget-object v0, Ljava/lang/Boolean;->FALSE:Ljava/lang/Boolean;
                return-object v0
            """,
        )

        mutableClassDefBy(PATCH_APPLICATION_CLASS)
            .methods.single { it.name == "onCreate" }
            .addInstruction(
                0,
                "invoke-static { p0 }, $PUSH_NOTIFICATIONS_CLASS->clearPlayServicesDelegate(Landroid/content/Context;)V",
            )

        markPatchInstalled("pep_disable_push")
    }
}
