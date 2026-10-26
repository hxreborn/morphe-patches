/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.netanalyzer.misc.devicenames

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.netanalyzer.shared.FEATURES_CLASS
import app.morphe.patches.netanalyzer.shared.addFeatureActivity
import app.morphe.patches.netanalyzer.shared.lanScanObserverPatch
import app.morphe.patches.netanalyzer.shared.registerAtLaunch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.patches.shared.misc.smali.toJavaClassName
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val REGISTRATION_SEPARATOR = ";"

@Suppress("unused")
val customDeviceNamesPatch = bytecodePatch(
    name = "Custom device names",
    description = "Adds custom names for Wi-Fi networks and LAN devices, shown in the Wi-Fi and LAN lists.",
) {
    compatibleWith(AppCompatibilities.NETWORK_ANALYZER)
    availability(requireArm64)
    dependsOn(lanScanObserverPatch)

    execute {
        addFeatureActivity("LanCustomNameActivity")

        val deviceName = LanDeviceNameFingerprint.matchSingle()
        val nameMap = deviceName.instructionMatches.first().instruction.getReference<FieldReference>()!!
        val registration = listOf(nameMap.definingClass.toJavaClassName(), nameMap.name)
            .joinToString(REGISTRATION_SEPARATOR)

        val method = deviceName.method
        val nameRegister = method.getFreeRegisterProvider(0, 1).getFreeRegister4Bit()
        method.addInstructionsWithLabels(
            0,
            """
                invoke-static { p0 }, $FEATURES_CLASS->lanDeviceName(Ljava/lang/Object;)Ljava/lang/String;
                move-result-object v$nameRegister
                if-eqz v$nameRegister, :original
                return-object v$nameRegister
            """,
            ExternalLabel("original", method.getInstruction(0)),
        )

        registerAtLaunch("$FEATURES_CLASS->registerDeviceNames(Ljava/lang/String;)V", registration)
    }
}
