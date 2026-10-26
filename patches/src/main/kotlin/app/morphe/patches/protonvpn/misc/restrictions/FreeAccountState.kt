/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonvpn.misc.restrictions

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.extensions.InstructionExtensions.instructionsOrNull
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.protonvpn.misc.anchors.ToStringFingerprint
import app.morphe.patches.protonvpn.misc.anchors.VpnUserIsFreeUserFingerprint
import app.morphe.patches.protonvpn.misc.anchors.setExtensionMember
import app.morphe.patches.protonvpn.misc.anchors.vpnUserType
import app.morphe.patches.protonvpn.misc.settings.patchesSettingsPatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

private const val FREE_ACCOUNT = "Lapp/hxreborn/extension/protonvpn/FreeAccount;"

private val stateFlowInvokes = listOf(Opcode.INVOKE_INTERFACE, Opcode.INVOKE_VIRTUAL)

private fun stateFlowSetValue(location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()) = methodCall(
    parameters = listOf("Ljava/lang/Object;"),
    returnType = "V",
    opcodes = stateFlowInvokes,
    location = location,
)

internal object UserInfoToStringFingerprint : ToStringFingerprint("PartialJointUserInt(user=")

private fun Method.receivesType(type: String) = parameterTypes.firstOrNull()?.toString() == type ||
    instructionsOrNull?.firstOrNull()?.let {
        it.opcode == Opcode.CHECK_CAST && it.getReference<TypeReference>()?.type == type
    } == true

private fun BytecodePatchContext.userInfoUpdateFingerprint(): Fingerprint {
    val userInfo = UserInfoToStringFingerprint.originalClassDef.type
    return Fingerprint(
        returnType = "Ljava/lang/Object;",
        parameters = listOf("L", "L"),
        filters = listOf(
            methodCall(definingClass = "Ljava/lang/Number;", name = "longValue"),
            opcode(Opcode.MOVE_RESULT_WIDE, MatchAfterImmediately()),
            stateFlowSetValue(),
        ),
        custom = { method, _ -> method.receivesType(userInfo) },
    )
}

private object UserInfoInvalidateFingerprint : Fingerprint(
    returnType = "V",
    parameters = emptyList(),
    filters = listOf(
        literal(0),
        stateFlowSetValue(MatchAfterImmediately()),
        methodCall(definingClass = "Ljava/lang/Number;", name = "longValue"),
        methodCall(definingClass = "Ljava/lang/Long;", name = "valueOf"),
        methodCall(
            parameters = listOf("Ljava/lang/Object;", "Ljava/lang/Object;"),
            returnType = "Z",
            opcodes = stateFlowInvokes,
        ),
    ),
)

internal val freeAccountStatePatch = bytecodePatch {
    dependsOn(patchesSettingsPatch)

    execute {
        val userInfo = UserInfoToStringFingerprint.originalClassDef
        setExtensionMember(
            "userInfoVpnUser",
            userInfo.fields.single {
                it.type == vpnUserType && !AccessFlags.STATIC.isSet(it.accessFlags)
            }.name,
        )
        setExtensionMember("vpnUserIsFreeUser", VpnUserIsFreeUserFingerprint.originalMethod.name)

        userInfoUpdateFingerprint().matchSingle().run {
            method.addInstruction(
                instructionMatches.last().index,
                "invoke-static { p1 }, $FREE_ACCOUNT->onUserInfoChanged(Ljava/lang/Object;)V",
            )
        }
        UserInfoInvalidateFingerprint.matchSingle().method.addInstruction(
            0,
            "invoke-static { }, $FREE_ACCOUNT->onUserInfoInvalidated()V",
        )
    }
}
