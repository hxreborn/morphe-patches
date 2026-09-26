/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonvpn.misc.restrictions

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.protonvpn.misc.settings.patchesSettingsPatch
import app.morphe.util.matchSingle

private const val FREE_ACCOUNT = "Lapp/hxreborn/extension/protonvpn/FreeAccount;"

internal object UserInfoUpdateFingerprint : Fingerprint(
    definingClass = "Lcom/protonvpn/android/auth/usecase/DefaultCurrentUserProvider\$1\$1\$3;",
    name = "emit",
    parameters = listOf(
        "Lcom/protonvpn/android/auth/usecase/PartialJointUserInfo;",
        "Lkotlin/coroutines/Continuation;",
    ),
    filters = listOf(methodCall(definingClass = "Lkotlinx/coroutines/flow/MutableStateFlow;", name = "setValue")),
)

internal object UserInfoInvalidateFingerprint : Fingerprint(
    definingClass = "Lcom/protonvpn/android/auth/usecase/DefaultCurrentUserProvider;",
    name = "invalidateCache",
    returnType = "V",
    parameters = emptyList(),
)

internal val freeAccountStatePatch = bytecodePatch {
    dependsOn(patchesSettingsPatch)

    execute {
        UserInfoUpdateFingerprint.matchSingle().run {
            method.addInstruction(
                instructionMatches.first().index,
                "invoke-static { p1 }, $FREE_ACCOUNT->onUserInfoChanged(Ljava/lang/Object;)V",
            )
        }
        UserInfoInvalidateFingerprint.matchSingle().method.addInstruction(
            0,
            "invoke-static { }, $FREE_ACCOUNT->onUserInfoInvalidated()V",
        )
    }
}
