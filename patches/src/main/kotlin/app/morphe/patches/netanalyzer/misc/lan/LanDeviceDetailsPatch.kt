/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.netanalyzer.misc.lan

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.netanalyzer.shared.FEATURES_CLASS
import app.morphe.patches.netanalyzer.shared.addFeatureActivity
import app.morphe.patches.netanalyzer.shared.patchesMenuPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64
import com.android.tools.smali.dexlib2.AccessFlags

private const val DEVICE_CALLBACK_ARGUMENTS = 3

@Suppress("unused")
val lanDeviceDetailsPatch = bytecodePatch(
    name = "LAN device details",
    description = "Adds a details page for each device in the LAN scan with its names, Wake on LAN, " +
        "Bonjour and UPnP services and web interface, plus text and CSV export of the scan.",
) {
    compatibleWith(AppCompatibilities.NETWORK_ANALYZER)
    availability(requireArm64)
    dependsOn(patchesMenuPatch)

    execute {
        addFeatureActivity("LanDeviceActivity")
        addFeatureActivity("WakeOnLanActivity")
        addFeatureActivity("BonjourActivity")
        addFeatureActivity("BonjourServiceActivity")
        addFeatureActivity("UpnpActivity")
        addFeatureActivity("UpnpServicesActivity")

        val screen = LanScanScreenFingerprint.matchSingle().method
        val callbackIndex = screen.parameterTypes.indexOfFirst { type ->
            val definition = classDefByOrNull(type.toString())
            definition != null && AccessFlags.INTERFACE.isSet(definition.accessFlags) &&
                definition.methods.any { method ->
                    AccessFlags.ABSTRACT.isSet(method.accessFlags) &&
                        method.parameterTypes.size == DEVICE_CALLBACK_ARGUMENTS
                }
        }
        if (callbackIndex < 0) throw PatchException("$LAN_SCAN_SCREEN takes no device callback")
        val callbackType = screen.parameterTypes[callbackIndex]

        screen.addInstructions(
            0,
            """
                move-object/from16 v1, p$callbackIndex
                const-class v0, $callbackType
                invoke-static { v0, v1 }, $FEATURES_CLASS->wrapLanDeviceClick(Ljava/lang/Class;Ljava/lang/Object;)Ljava/lang/Object;
                move-result-object v1
                check-cast v1, $callbackType
                move-object/from16 p$callbackIndex, v1
            """,
        )
    }
}
