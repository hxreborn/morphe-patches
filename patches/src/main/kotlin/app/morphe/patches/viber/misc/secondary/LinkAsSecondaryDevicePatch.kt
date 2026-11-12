/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.misc.secondary

import app.morphe.patcher.Match
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.addInstructionsAtControlFlowLabel
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.viber.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.viber.misc.settings.patchesSettingsPatch
import app.morphe.patches.viber.shared.VIBER_SETTINGS_CLASS
import app.morphe.patches.viber.shared.addFilterCallAfter
import app.morphe.patches.viber.shared.markPatchApplied
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val SECONDARY_MODE_RESET_SITES = 4

private fun Match.filterBackupPermissions() {
    val permissionsStore = instructionMatches.last()
    val register = (permissionsStore.instruction as TwoRegisterInstruction).registerA
    method.addInstructionsAtControlFlowLabel(
        permissionsStore.index,
        """
            invoke-static/range { v$register .. v$register }, $VIBER_SETTINGS_CLASS->filterBackupPermissions([Ljava/lang/String;)[Ljava/lang/String;
            move-result-object v$register
        """,
    )
}

@Suppress("unused")
val linkAsSecondaryDevicePatch: BytecodePatch = bytecodePatch(
    name = "Link as secondary device",
    description = "Links to your main Viber phone.",
) {
    compatibleWith(AppCompatibilities.VIBER)
    dependsOn(spoofSignaturePatch, patchesSettingsPatch)

    execute {
        markPatchApplied("linkAsSecondaryDevice", linkAsSecondaryDevicePatch)
        val activateAsSecondaryCall = "$VIBER_SETTINGS_CLASS->activateAsSecondary(Z)Z"
        val secondaryDefault = SecondaryModeDefaultFingerprint.matchSingle()
        val (tabletCall, defaultTabletResult) = secondaryDefault.instructionMatches
        val tabletCheck = tabletCall.instruction.getReference<MethodReference>()!!
        secondaryDefault.method.addFilterCallAfter(defaultTabletResult.index, activateAsSecondaryCall)

        val setSecondaryMode = SetSecondaryModeFingerprint.matchSingle().originalMethod
        secondaryModeResetFingerprint(tabletCheck, setSecondaryMode)
            .matchAll(SECONDARY_MODE_RESET_SITES..SECONDARY_MODE_RESET_SITES)
            .forEach { match ->
                val (_, tabletResult) = match.instructionMatches
                match.method.addFilterCallAfter(tabletResult.index, activateAsSecondaryCall)
            }

        val (secondaryModeCall) = SecondaryPlatformNameFingerprint.matchSingle().instructionMatches
        val secondaryModeGetter = secondaryModeCall.instruction.getReference<MethodReference>()!!
        val backupAvailability = secondaryBackupAvailabilityFingerprint(secondaryModeGetter).matchSingle()
        backupAvailability.run {
            val (_, secondaryResult) = instructionMatches
            method.addFilterCallAfter(secondaryResult.index, "$VIBER_SETTINGS_CLASS->restrictBackupToPrimary(Z)Z")
        }
        backupAccountPermissionsFingerprint(backupAvailability.originalMethod.definingClass)
            .matchSingle().filterBackupPermissions()
        val (_, permissionCheck) = BackupAccountPermissionRequestFingerprint.matchSingle().instructionMatches
        val permissionHelper = permissionCheck.instruction.getReference<MethodReference>()!!
        backupAccountPermissionHelperFingerprint(permissionHelper.definingClass).matchSingle().filterBackupPermissions()
        RegistrationDeviceTypeFingerprint.matchSingle().run {
            val (_, _, tabletResult) = instructionMatches
            val register = (tabletResult.instruction as OneRegisterInstruction).registerA
            method.addInstructionsWithLabels(
                tabletResult.index + 1,
                """
                    if-nez v$register, :registration_device_type_resolved
                    invoke-static {}, $secondaryModeGetter
                    move-result v$register
                """,
                ExternalLabel("registration_device_type_resolved", method.getInstruction(tabletResult.index + 1)),
            )
        }
    }
}
