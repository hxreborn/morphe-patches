/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonvpn.misc.restrictions

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.protonvpn.misc.anchors.ToStringFingerprint
import app.morphe.patches.protonvpn.misc.anchors.VpnUserIsFreeUserFingerprint
import app.morphe.patches.protonvpn.misc.anchors.setExtensionMember
import app.morphe.patches.protonvpn.misc.anchors.vpnUserType
import app.morphe.patches.protonvpn.misc.settings.patchesSettingsPatch
import app.morphe.util.getReference
import app.morphe.util.matchSingle
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val FREE_ACCOUNT = "Lapp/hxreborn/extension/protonvpn/FreeAccount;"

private fun stateFlowSetValue() = methodCall(
    name = "setValue",
    parameters = listOf("Ljava/lang/Object;"),
    returnType = "V",
    opcode = Opcode.INVOKE_INTERFACE,
)

internal object UserInfoToStringFingerprint : ToStringFingerprint("PartialJointUserInt(user=")

private fun BytecodePatchContext.userInfoUpdateFingerprint() = Fingerprint(
    returnType = "Ljava/lang/Object;",
    parameters = listOf(UserInfoToStringFingerprint.originalClassDef.type, "L"),
    filters = listOf(
        methodCall(definingClass = "Ljava/lang/Number;", name = "longValue"),
        opcode(Opcode.MOVE_RESULT_WIDE, MatchAfterImmediately()),
        stateFlowSetValue(),
    ),
)

private fun userInfoInvalidateFingerprint(userProvider: String) = Fingerprint(
    definingClass = userProvider,
    returnType = "V",
    parameters = emptyList(),
    filters = listOf(
        stateFlowSetValue(),
        methodCall(definingClass = "Ljava/lang/Long;", name = "valueOf"),
        methodCall(
            parameters = listOf("Ljava/lang/Object;", "Ljava/lang/Object;"),
            returnType = "Z",
            opcode = Opcode.INVOKE_INTERFACE,
        ),
    ),
)

internal val freeAccountStatePatch = bytecodePatch {
    dependsOn(patchesSettingsPatch)

    execute {
        val userInfo = UserInfoToStringFingerprint.originalClassDef
        setExtensionMember(
            "userInfoVpnUser",
            userInfo.methods.first {
                it.returnType == vpnUserType && it.parameterTypes.isEmpty() && !AccessFlags.STATIC.isSet(it.accessFlags)
            }.name,
        )
        setExtensionMember("vpnUserIsFreeUser", VpnUserIsFreeUserFingerprint.originalMethod.name)

        val userProvider = userInfoUpdateFingerprint().matchSingle().run {
            method.addInstruction(
                instructionMatches.last().index,
                "invoke-static { p1 }, $FREE_ACCOUNT->onUserInfoChanged(Ljava/lang/Object;)V",
            )
            method.implementation!!.instructions.firstNotNullOf { instruction ->
                instruction.getReference<MethodReference>()?.takeIf {
                    instruction.opcode == Opcode.INVOKE_STATIC && it.parameterTypes.singleOrNull()?.toString() == it.definingClass
                }?.definingClass
            }
        }
        userInfoInvalidateFingerprint(userProvider).matchSingle().method.addInstruction(
            0,
            "invoke-static { }, $FREE_ACCOUNT->onUserInfoInvalidated()V",
        )
    }
}
