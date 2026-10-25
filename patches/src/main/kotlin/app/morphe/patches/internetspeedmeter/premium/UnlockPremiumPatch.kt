/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.internetspeedmeter.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.internetspeedmeter.ads.removeAdsPatch
import app.morphe.patches.internetspeedmeter.misc.protection.removeAppProtectionPatch
import app.morphe.patches.internetspeedmeter.misc.settings.removeSettingsRow
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.adoptChild
import app.morphe.util.getNode
import app.morphe.util.insertFirst
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val EXTENSION_PACKAGE = "Lapp/hxreborn/extension/internetspeedmeter"
private const val SETTINGS_CLASS_DESCRIPTOR = "$EXTENSION_PACKAGE/PremiumSettings;"
private const val NOTIFICATION_CLASS_DESCRIPTOR = "$EXTENSION_PACKAGE/NotificationSettings;"
private const val SMART_NOTIFICATION_CLASS_DESCRIPTOR = "$EXTENSION_PACKAGE/SmartNotification;"

private const val OVERVIEW_ACTIVITY = "app.hxreborn.extension.internetspeedmeter.QuickOverviewActivity"
private const val NETWORK_RECEIVER = "app.hxreborn.extension.internetspeedmeter.NetworkAvailableReceiver"
private const val USAGE_STATS_PERMISSION = "android.permission.PACKAGE_USAGE_STATS"

private val addPremiumComponentsPatch = resourcePatch {
    execute {
        document("AndroidManifest.xml").use { document ->
            val manifest = document.documentElement
            manifest.insertFirst(
                document.createElement("uses-permission").apply {
                    setAttribute("android:name", USAGE_STATS_PERMISSION)
                },
            )
            manifest.insertFirst(
                document.createElement("queries").apply {
                    adoptChild("intent") {
                        adoptChild("action") { setAttribute("android:name", "android.intent.action.MAIN") }
                    }
                },
            )

            document.getNode("application").apply {
                adoptChild("activity") {
                    setAttribute("android:name", OVERVIEW_ACTIVITY)
                    setAttribute("android:exported", "false")
                    setAttribute("android:excludeFromRecents", "true")
                    setAttribute("android:launchMode", "singleInstance")
                    setAttribute("android:noHistory", "true")
                    setAttribute("android:theme", "@android:style/Theme.Material.Light.Dialog.NoActionBar")
                }
                adoptChild("receiver") {
                    setAttribute("android:name", NETWORK_RECEIVER)
                    setAttribute("android:exported", "false")
                }
            }
        }
    }
}

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks the notification dialog, smart notifications, themes, notification accent and " +
        "the starting hour of the day.",
) {
    compatibleWith(AppCompatibilities.INTERNET_SPEED_METER_LITE)

    dependsOn(removeAppProtectionPatch, removeAdsPatch, addPremiumComponentsPatch, sessionCounterPatch)

    execute {
        PremiumRowClickFingerprint.method.apply {
            val buyDialogIndex = PremiumRowClickFingerprint.instructionMatches.first().index
            val rowViewRegister = getInstruction<OneRegisterInstruction>(buyDialogIndex).registerA
            addInstructionsWithLabels(
                buyDialogIndex,
                """
                    move-object/from16 v$rowViewRegister, p2
                    invoke-static { v$rowViewRegister }, $SETTINGS_CLASS_DESCRIPTOR->onRowClick(Landroid/view/View;)Z
                    move-result v$rowViewRegister
                    if-eqz v$rowViewRegister, :buy_dialog
                    return-void
                """,
                ExternalLabel("buy_dialog", getInstruction(buyDialogIndex)),
            )
        }

        SettingsRowViewFingerprint.method.apply {
            instructions.filter { it.opcode == Opcode.RETURN_OBJECT }.reversed().forEach { returnInstruction ->
                val returnIndex = returnInstruction.location.index
                val rowRegister = (returnInstruction as OneRegisterInstruction).registerA
                replaceInstruction(
                    returnIndex,
                    "invoke-static { v$rowRegister }, $SETTINGS_CLASS_DESCRIPTOR->bindRow(Landroid/view/View;)V",
                )
                addInstruction(returnIndex + 1, "return-object v$rowRegister")
            }
        }

        BuildNotificationFingerprint.method.apply {
            val buildIndex = BuildNotificationFingerprint.instructionMatches.single().index
            val builderRegister = getInstruction<FiveRegisterInstruction>(buildIndex).registerC
            addInstruction(
                buildIndex,
                "invoke-static { p0, v$builderRegister }, $NOTIFICATION_CLASS_DESCRIPTOR->" +
                    "applyAccentAndTapAction(Landroid/content/Context;Landroid/app/Notification\$Builder;)V",
            )
        }

        ServiceStartCommandFingerprint.method.apply {
            val stopRegister = getFreeRegisterProvider(0, 1).getFreeRegister4Bit()
            addInstructionsWithLabels(
                0,
                """
                    invoke-static { p0 }, $SMART_NOTIFICATION_CLASS_DESCRIPTOR->stopWhenOffline(Landroid/app/Service;)Z
                    move-result v$stopRegister
                    if-eqz v$stopRegister, :start
                    const/4 v$stopRegister, 0x2
                    return v$stopRegister
                """,
                ExternalLabel("start", getInstruction(0)),
            )
        }

        removeSettingsRow("buy_app_title")
    }
}
