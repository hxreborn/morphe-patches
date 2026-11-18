/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.gms

import app.morphe.patcher.Match
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.BuilderOffsetInstruction
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

private const val MAX_INVOKE_REGISTER = 15

private val GOTO_OPCODES = setOf(Opcode.GOTO, Opcode.GOTO_16, Opcode.GOTO_32)

private fun requireInlinedSignInSafe(
    match: Match,
    serverClientIdRegister: Int,
    activityRegister: Int,
    callbackRegister: Int,
) {
    val (clientIdCheck, activityLoad, activityCheck, _, callbackCall) = match.instructionMatches.map { it.index }
    val method = match.method
    if (method.getInstruction<TwoRegisterInstruction>(activityLoad).registerA != activityRegister) {
        throw PatchException("Credential sign-in activity register does not match the activity check")
    }
    val instructions = method.implementation!!.instructions
    fun returnsDirectly(call: Int): Boolean {
        val next = instructions.getOrNull(call + 1) as? BuilderOffsetInstruction
        return next?.opcode in GOTO_OPCODES &&
            next?.target?.location?.instruction?.opcode == Opcode.RETURN_VOID
    }
    val credentialRequestCall = match.instructionMatches.single {
        it.instruction.opcode == Opcode.INVOKE_VIRTUAL_RANGE
    }.index
    if (!returnsDirectly(callbackCall) || !returnsDirectly(credentialRequestCall)) {
        throw PatchException("Credential sign-in call does not return directly")
    }
    fun isOverwritten(register: Int, start: Int, end: Int) = instructions.subList(start + 1, end).any {
        it.opcode.setsRegister() && (it as OneRegisterInstruction).registerA.let { target ->
            target == register || it.opcode.setsWideRegister() && target + 1 == register
        }
    }
    if (isOverwritten(serverClientIdRegister, clientIdCheck, activityCheck) ||
        isOverwritten(callbackRegister, activityCheck, callbackCall)
    ) {
        throw PatchException("Credential sign-in argument register is overwritten")
    }
}

internal val getCredentialGmsCoreSignInPatch = bytecodePatch {
    extendWith("extensions/extension.mpe")

    dependsOn(gmsCoreSpoofedSignaturePatch, gmsCoreManifestPatch)

    execute {
        bindGmsCoreSignInTypes()
        renameCloudMessagingToGmsCore()

        val originalMatch = GetCredentialHandlerFingerprint.matchAllOrNull()?.let {
            GetCredentialHandlerFingerprint.matchSingle()
        }
        val match = originalMatch ?: InlinedGetCredentialHandlerFingerprint.matchSingle()
        val indices = match.instructionMatches.map { it.index }
        val (clientIdCheck, activityCheck, callbackCall) = if (originalMatch != null) {
            val (clientId, activity, _, _, callback) = indices
            Triple(clientId, activity, callback)
        } else {
            val (clientId, _, activity, _, callback) = indices
            Triple(clientId, activity, callback)
        }
        val method = match.method
        val serverClientIdRegister = method.getInstruction<FiveRegisterInstruction>(clientIdCheck).registerC
        val activityRegister = method.getInstruction<OneRegisterInstruction>(activityCheck).registerA
        val callbackRegister = method.getInstruction<FiveRegisterInstruction>(callbackCall).registerC
        if (listOf(activityRegister, serverClientIdRegister, callbackRegister).any { it > MAX_INVOKE_REGISTER }) {
            throw PatchException("Credential sign-in registers exceed the invoke limit")
        }
        if (originalMatch == null) {
            requireInlinedSignInSafe(match, serverClientIdRegister, activityRegister, callbackRegister)
        }

        method.addInstructions(
            activityCheck,
            """
                invoke-static { v$activityRegister, v$serverClientIdRegister, v$callbackRegister }, $GMS_CORE_SIGN_IN_CLASS->getCredential(Landroid/app/Activity;Ljava/lang/String;Ljava/lang/Object;)V
                return-void
            """,
        )
    }

    finalize {
        redirectGmsPackageToGmsCore()
    }
}

@Suppress("unused")
val getCredentialGmsCoreSupportPatch = bytecodePatch(
    name = "GmsCore support",
    description = "Signs in through GmsCore instead of Google Play Services. " +
        "Requires GmsCore to be installed.",
) {
    compatibleWith(AppCompatibilities.JUJO, AppCompatibilities.MEMONEET, AppCompatibilities.STORYWISE)

    dependsOn(getCredentialGmsCoreSignInPatch)
}
