/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonvpn.misc.connectionpreferences

import app.morphe.patcher.Match
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.p0Register
import app.morphe.patches.protonvpn.misc.anchors.VpnUserIsFreeUserFingerprint
import app.morphe.patches.protonvpn.misc.anchors.propertyGetter
import app.morphe.patches.protonvpn.misc.anchors.vpnUserType
import app.morphe.patches.protonvpn.misc.profiles.VpnCountryToStringFingerprint
import app.morphe.patches.protonvpn.misc.restrictions.SERVER
import app.morphe.patches.protonvpn.misc.restrictions.clearConstructorProperty
import app.morphe.patches.protonvpn.misc.restrictions.clearFreeUserCheck
import app.morphe.patches.protonvpn.misc.restrictions.clearFreeUserCheckInLambdaOf
import app.morphe.patches.protonvpn.misc.restrictions.keepUserSetting
import app.morphe.patches.protonvpn.misc.settings.patchesSettingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.proton.markPatchApplied
import app.morphe.patches.shared.misc.smali.descriptor
import app.morphe.patches.shared.misc.smali.replaceStub
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.BuilderOffsetInstruction
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction20t
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXCLUDED_LOCATIONS_CLASS = "Lapp/hxreborn/extension/protonvpn/ExcludedLocations;"
private const val VPN_USER_ARGUMENT_OFFSET = 2
private const val EXCLUDED_LOCATIONS_ARGUMENT_OFFSET = 5
private const val MAX_INVOKE_REGISTER = 15

@Suppress("unused")
val unlockConnectionPreferencesPatch: BytecodePatch = bytecodePatch(
    name = "Unlock connection preferences",
    description = "Unlocks the default connection, recent connections and excluded locations on free plans.",
) {
    compatibleWith(AppCompatibilities.PROTON_VPN)
    dependsOn(patchesSettingsPatch)

    execute {
        markPatchApplied("unlockConnectionPreferences", unlockConnectionPreferencesPatch)
        clearConstructorProperty(ConnectionPreferencesToStringFingerprint, "isFreeUser")
        keepUserSetting(defaultConnectionRestrictionFingerprint())
        clearFreeUserCheck(recentsRecordingFingerprint())
        clearFreeUserCheckInLambdaOf(RecentsListViewStateFlowFingerprint)
        clearLastFreeUserCheckBefore(defaultConnectionRecentsFingerprint().matchSingle())
        clearFreeUserCheck(quickConnectIntentFingerprint())
        clearFreeUserCheck(addExcludedLocationFingerprint())
        clearFreeUserCheck(removeExcludedLocationFingerprint())
        clearFreeUserCheck(excludedLocationsFlowFingerprint())
        excludeLocationsFromRandomServers()
        excludeLocationsFromFreeUserConnections()
        val connectionCardLabel = connectionCardLabelFingerprint()
        if (connectionCardLabel.matchOrNull() != null) {
            clearFreeUserCheck(connectionCardLabel)
        } else {
            clearLastFreeUserCheckBefore(inlinedConnectionCardFingerprint().matchSingle())
        }

        val recentIdLookup = defaultConnectionSettingFingerprint().matchOrNull()
        if (recentIdLookup != null) {
            val (_, recentIdLookupCall) = recentIdLookup.instructionMatches
            recentIdLookup.alwaysTakeLastIfEqz { it.target.location.index == recentIdLookupCall.index }
        } else {
            inlinedDefaultConnectionSettingFingerprint().matchSingle().run {
                val recentCheck = instructionMatches.first().index
                alwaysTakeLastIfEqz { it.location.index < recentCheck && it.target.location.index <= recentCheck }
            }
        }
    }
}

private fun BytecodePatchContext.excludeLocationsFromRandomServers() {
    ExcludedLocationsConstructorFingerprint.matchSingle().run {
        val (returnVoid) = instructionMatches
        requireThisInP0()
        method.addInstruction(
            returnVoid.index,
            "invoke-static { p0 }, $EXCLUDED_LOCATIONS_CLASS->onExcludedLocationsCreated(Ljava/lang/Object;)V",
        )
    }

    randomServerFingerprint().matchSingle().run {
        val (_, countriesResult) = instructionMatches
        val countries = countriesResult.getInstruction<OneRegisterInstruction>().registerA
        method.addInstructions(
            countriesResult.index + 1,
            """
                invoke-static { v$countries }, $EXCLUDED_LOCATIONS_CLASS->countriesWithoutExcludedServers(Ljava/util/List;)Ljava/util/List;
                move-result-object v$countries
            """,
        )
    }

    val country = VpnCountryToStringFingerprint.originalClassDef.type
    val countryConstructor = VpnCountryConstructorFingerprint.matchSingle().originalMethod
    val excludedLocations = ExcludedLocationsToStringFingerprint.originalClassDef.type
    mutableClassDefBy(EXCLUDED_LOCATIONS_CLASS).apply {
        replaceStub(
            "serverList",
            2,
            """
                check-cast p0, $country
                invoke-virtual { p0 }, ${descriptor(propertyGetter(VpnCountryToStringFingerprint, "serverList"))}
                move-result-object v0
                return-object v0
            """,
        )
        replaceStub(
            "isExcluded",
            3,
            """
                check-cast p0, $SERVER
                check-cast p1, $excludedLocations
                invoke-static { p0, p1 }, ${descriptor(serverIsExcludedFingerprint().matchSingle().originalMethod)}
                move-result v0
                return v0
            """,
        )
        replaceStub(
            "copyWithServers",
            4,
            """
                check-cast p0, $country
                invoke-virtual { p0 }, ${descriptor(propertyGetter(VpnCountryToStringFingerprint, "flag"))}
                move-result-object v1
                new-instance v0, $country
                invoke-direct { v0, v1, p1 }, ${descriptor(countryConstructor)}
                return-object v0
            """,
        )
    }
}

private fun BytecodePatchContext.excludeLocationsFromFreeUserConnections() {
    bestServerForConnectIntentCallFingerprint().matchSingle().run {
        val (bestServerCall, bestServerResult) = instructionMatches
        val arguments = bestServerCall.instruction as? RegisterRangeInstruction
            ?: throw PatchException("Best server call is not a range call in $method")
        val server = bestServerResult.getInstruction<OneRegisterInstruction>().registerA
        val vpnUser = arguments.startRegister + VPN_USER_ARGUMENT_OFFSET
        val excludedLocations = arguments.startRegister + EXCLUDED_LOCATIONS_ARGUMENT_OFFSET
        if (maxOf(server, vpnUser, excludedLocations) > MAX_INVOKE_REGISTER) {
            throw PatchException("Best server registers do not fit a non-range invoke in $method")
        }
        method.addInstructions(
            bestServerResult.index + 1,
            """
                invoke-static { v$server, v$vpnUser, v$excludedLocations }, $EXCLUDED_LOCATIONS_CLASS->serverUnlessExcludedForFreeUser(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
                move-result-object v$server
                check-cast v$server, $SERVER
            """,
        )
    }

    mutableClassDefBy(EXCLUDED_LOCATIONS_CLASS).replaceStub(
        "isFreeUser",
        2,
        """
            check-cast p0, $vpnUserType
            invoke-virtual { p0 }, ${descriptor(VpnUserIsFreeUserFingerprint.originalMethod)}
            move-result v0
            return v0
        """,
    )
}

private fun Match.requireThisInP0() {
    val thisRegister = method.p0Register
    val thisOverwrite = method.instructions.indexOfFirst { instruction ->
        instruction.opcode.setsRegister() && (instruction as OneRegisterInstruction).registerA.let { target ->
            target == thisRegister || instruction.opcode.setsWideRegister() && target + 1 == thisRegister
        }
    }
    if (thisOverwrite >= 0) {
        throw PatchException("p0 is overwritten at $thisOverwrite in constructor $method")
    }
}

private fun Match.alwaysTakeLastIfEqz(isTarget: (BuilderOffsetInstruction) -> Boolean) {
    val branch = method.instructions.filterIsInstance<BuilderOffsetInstruction>().last {
        it.opcode == Opcode.IF_EQZ && isTarget(it)
    }
    method.replaceInstruction(branch.location.index, BuilderInstruction20t(Opcode.GOTO_16, branch.target))
}

private fun BytecodePatchContext.clearLastFreeUserCheckBefore(anchor: Match) {
    val isFreeUser = VpnUserIsFreeUserFingerprint.originalMethod
    val instructions = anchor.method.instructions
    val callIndex = instructions.take(anchor.instructionMatches.last().index).indexOfLast {
        it.getReference<MethodReference>()?.let { reference ->
            reference.definingClass == isFreeUser.definingClass && reference.name == isFreeUser.name &&
                reference.parameterTypes == isFreeUser.parameterTypes
        } == true
    }
    if (callIndex < 0) throw PatchException("No free-user check before the anchor in ${anchor.method}")
    val register = (instructions[callIndex + 1] as OneRegisterInstruction).registerA
    anchor.method.addInstruction(callIndex + 2, "const/16 v$register, 0x0")
}
