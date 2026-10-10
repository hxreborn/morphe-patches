/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.realmelink.session

import app.morphe.patcher.InstructionFilter
import app.morphe.patcher.Match
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.removeInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.generated.descriptor
import app.morphe.patches.shared.misc.generated.replaceStub
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.matchSingle
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.Reference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

private const val SESSION_RENEWAL_CLASS = "Lapp/hxreborn/extension/realmelink/SessionRenewal;"

private inline fun <reified T : Reference> Match.referenceOf(filter: InstructionFilter): T =
    instructionMatches.single { it.filter === filter }.getInstruction<ReferenceInstruction>().reference as? T
        ?: throw PatchException("Matched instruction does not reference a ${T::class.simpleName}")

@Suppress("unused")
val bypassSessionExpiryPatch = bytecodePatch(
    name = "Bypass session expiry",
    description = "Renews the session in the background instead of asking to sign in again. " +
        "The sign-in prompt appears only when renewal fails.",
) {
    compatibleWith(AppCompatibilities.REALME_LINK)

    extendWith("extensions/extension.mpe")

    execute {
        val linkAuth = LinkServerAuthFingerprint.matchSingle()
        val sessionAuth = linkAuth.referenceOf<MethodReference>(sessionAuthCall)
        val resultSuccess = linkAuth.referenceOf<MethodReference>(resultSuccessCall)
        val accountServiceGetter = linkAuth.referenceOf<MethodReference>(accountServiceCall)
        val linkTokenField = linkAuth.referenceOf<FieldReference>(linkTokenRead)
        val storeTokens = linkAuth.referenceOf<MethodReference>(storeTokensCall)
        val resultType = resultSuccess.definingClass
        if (linkTokenField.definingClass != resultType) {
            throw PatchException("Link token field does not belong to $resultType")
        }

        val heytapToken = AccountTokensToStringFingerprint.matchSingle()
            .referenceOf<FieldReference>(heytapTokenRead)
        val currentAccount = classDefBy(accountServiceGetter.returnType).methods.singleOrNull {
            it.returnType == heytapToken.definingClass && it.parameters.isEmpty()
        } ?: throw PatchException("No single account getter on ${accountServiceGetter.returnType}")

        mutableClassDefBy(SESSION_RENEWAL_CLASS).apply {
            replaceStub(
                "accountService",
                1,
                """
                    invoke-static { }, ${descriptor(accountServiceGetter)}
                    move-result-object v0
                    return-object v0
                """,
            )
            replaceStub(
                "heytapToken",
                2,
                """
                    check-cast p0, ${accountServiceGetter.returnType}
                    invoke-interface { p0 }, ${descriptor(currentAccount)}
                    move-result-object v0
                    if-eqz v0, :signed_out
                    iget-object v0, v0, ${descriptor(heytapToken)}
                    return-object v0
                    :signed_out
                    const/4 v0, 0x0
                    return-object v0
                """,
            )
            replaceStub(
                "exchange",
                3,
                """
                    const-string v0, ""
                    invoke-static { v0, p0 }, ${descriptor(sessionAuth)}
                    move-result-object v1
                    invoke-virtual { v1 }, Lio/reactivex/Observable;->blockingFirst()Ljava/lang/Object;
                    move-result-object v1
                    return-object v1
                """,
            )
            replaceStub(
                "isSuccess",
                2,
                """
                    check-cast p0, $resultType
                    invoke-virtual { p0 }, ${descriptor(resultSuccess)}
                    move-result v0
                    return v0
                """,
            )
            replaceStub(
                "linkToken",
                2,
                """
                    check-cast p0, $resultType
                    iget-object v0, p0, ${descriptor(linkTokenField)}
                    check-cast v0, Ljava/lang/String;
                    return-object v0
                """,
            )
            replaceStub(
                "storeTokens",
                3,
                """
                    check-cast p0, ${storeTokens.definingClass}
                    invoke-virtual { p0, p1, p2 }, ${descriptor(storeTokens)}
                    return-void
                """,
            )
        }

        ReLoginPromptFingerprint.matchSingle().method.apply {
            val resumedContinuation = getInstruction(
                indexOfFirstInstructionOrThrow(Opcode.INSTANCE_OF),
            ).getReference<TypeReference>()?.type
                ?: throw PatchException("Re-login prompt does not start with a continuation check")

            addInstructionsWithLabels(
                0,
                """
                    instance-of v0, p2, $resumedContinuation
                    if-nez v0, :prompt
                    invoke-static { }, $SESSION_RENEWAL_CLASS->renew()Z
                    move-result v0
                    if-eqz v0, :prompt
                    sget-object v0, Lkotlin/Unit;->INSTANCE:Lkotlin/Unit;
                    return-object v0
                """,
                ExternalLabel("prompt", getInstruction(0)),
            )
        }

        ReLoginToastFingerprint.matchSingle().apply {
            method.removeInstruction(instructionMatches.last().index)
        }
    }
}
