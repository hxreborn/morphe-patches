/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.vpnsuper.misc.notification

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.smali.descriptor
import app.morphe.patches.shared.misc.smali.replaceStub
import app.morphe.patches.vpnsuper.misc.settings.markPatchApplied
import app.morphe.patches.vpnsuper.misc.settings.patchesSettingsPatch
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import org.w3c.dom.Element

private const val EXTENSION_CLASS = "Lapp/hxreborn/extension/vpnsuper/NotificationDisconnect;"
private const val CONNECTION_SERVICE_CLASS = "Lcom/superunlimited/feature/vpn/connection/presentation/VpnControlService;"
private const val DISCONNECT_RECEIVER_CLASS_NAME = "app.hxreborn.extension.vpnsuper.NotificationDisconnectReceiver"

private val notificationDisconnectReceiverPatch = resourcePatch {
    execute {
        document("AndroidManifest.xml").use { document ->
            (document.getElementsByTagName("application").item(0) as Element).appendChild(
                document.createElement("receiver").apply {
                    setAttribute("android:name", DISCONNECT_RECEIVER_CLASS_NAME)
                    setAttribute("android:exported", "false")
                },
            )
        }
    }
}

@Suppress("unused")
val disconnectFromNotificationPatch: BytecodePatch = bytecodePatch(
    name = "Disconnect from notification",
    description = "Skips opening the app when Disconnect is tapped in the notification. Doesn't work on Android TV.",
) {
    compatibleWith(AppCompatibilities.VPN_SUPER)
    dependsOn(patchesSettingsPatch, notificationDisconnectReceiverPatch)

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

        val (lazyFieldRead, lazyValueCall, flagWrite) =
            ManualDisconnectFlagWriteFingerprint.matchSingle().instructionMatches
        val flagHolderLazy = lazyFieldRead.getInstruction<ReferenceInstruction>().getReference<FieldReference>()
            ?: throw PatchException("Manual disconnect flag holder field not found in $WIDGET_ACTION_RECEIVER_CLASS")
        val lazyValueGetter = lazyValueCall.getInstruction<ReferenceInstruction>().getReference<MethodReference>()
            ?: throw PatchException("Lazy value getter not found in $WIDGET_ACTION_RECEIVER_CLASS")
        val flagSetter = flagWrite.getInstruction<ReferenceInstruction>().getReference<MethodReference>()
            ?: throw PatchException("Manual disconnect flag setter not found in $WIDGET_ACTION_RECEIVER_CLASS")

        mutableClassDefBy(EXTENSION_CLASS).apply {
            replaceStub(
                "markUserDisconnected",
                2,
                """
                    new-instance v0, $WIDGET_ACTION_RECEIVER_CLASS
                    invoke-direct { v0 }, $WIDGET_ACTION_RECEIVER_CLASS-><init>()V
                    iget-object v0, v0, ${descriptor(flagHolderLazy)}
                    invoke-interface { v0 }, ${descriptor(lazyValueGetter)}
                    move-result-object v0
                    check-cast v0, ${flagSetter.definingClass}
                    const/4 v1, 0x1
                    invoke-virtual { v0, v1 }, ${descriptor(flagSetter)}
                    return-void
                """,
            )
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
