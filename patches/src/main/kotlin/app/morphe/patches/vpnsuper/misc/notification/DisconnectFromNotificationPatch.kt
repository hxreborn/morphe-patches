/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.vpnsuper.misc.notification

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.smali.replaceStub
import app.morphe.patches.vpnsuper.misc.settings.markPatchApplied
import app.morphe.patches.vpnsuper.misc.settings.patchesSettingsPatch
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

private const val EXTENSION_CLASS = "Lapp/hxreborn/extension/vpnsuper/NotificationDisconnect;"
private const val CONNECTION_SERVICE_CLASS = "Lcom/superunlimited/feature/vpn/connection/presentation/VpnControlService;"

@Suppress("unused")
val disconnectFromNotificationPatch: BytecodePatch = bytecodePatch(
    name = "Disconnect from notification",
    description = "Skips opening the app when Disconnect is tapped in the notification. Doesn't work on Android TV.",
) {
    compatibleWith(AppCompatibilities.VPN_SUPER)
    dependsOn(patchesSettingsPatch)

    execute {
        markPatchApplied("disconnectFromNotification", disconnectFromNotificationPatch)
        StopActionStringsFingerprint.matchSingle()

        val disconnectAction = DisconnectActionFingerprint.matchSingle()
        val firstParameterType = disconnectAction.method.parameterTypes.firstOrNull()?.toString()
        if (firstParameterType != CONNECTION_SERVICE_CLASS) {
            throw PatchException(
                "Disconnect action first parameter type mismatch: expected $CONNECTION_SERVICE_CLASS, got $firstParameterType",
            )
        }
        val (_, _, _, getActivity) = disconnectAction.instructionMatches
        val getActivityCall = getActivity.instruction as FiveRegisterInstruction
        disconnectAction.method.replaceInstruction(
            getActivity.index,
            "invoke-static { v${getActivityCall.registerC}, v${getActivityCall.registerD}, " +
                "v${getActivityCall.registerE}, v${getActivityCall.registerF} }, " +
                "$EXTENSION_CLASS->disconnectAction(Landroid/content/Context;ILandroid/content/Intent;I)" +
                "Landroid/app/PendingIntent;",
        )

        mutableClassDefBy(EXTENSION_CLASS).apply {
            replaceStub(
                "connectionService",
                1,
                """
                    const-class v0, $CONNECTION_SERVICE_CLASS
                    return-object v0
                """,
            )
            methods.single { it.name == "stopConnectionAction" }.returnEarly(STOP_CONNECTION_ACTION)
        }
    }
}
