/*
 * Copyright (C) 2026 heval99
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from heval99/Heval-Morphe-Patches:
 * https://github.com/heval99/Heval-Morphe-Patches/commit/28097b43dbc8de99bb516a44f84708ae6cbb0f64
 * Commit 28097b43dbc8de99bb516a44f84708ae6cbb0f64 (2026-10-25),
 * patches/src/main/kotlin/app/template/patches/firefox/misc/promotions/HidePromotionalPromptsPatch.kt
 */
package app.morphe.patches.firefox.misc.promotions

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.InstructionLocation.MatchFirst
import app.morphe.patcher.checkCast
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

internal object NextMessageFingerprint : Fingerprint(
    definingClass = "Lmozilla/components/service/nimbus/messaging/NimbusMessagingStorage;",
    name = "getNextMessage",
    parameters = listOf("Ljava/lang/String;", "Ljava/util/List;"),
    filters = listOf(methodCall(definingClass = "Lmozilla/components/service/nimbus/messaging/Message;", name = "getSurface")),
)

internal const val WORKER_RESULT_SUCCESS = "Landroidx/work/ListenableWorker\$Result\$Success;"

internal object MessageNotificationWorkFingerprint : Fingerprint(
    name = "doWork",
    parameters = listOf("Lkotlin/coroutines/Continuation;"),
    filters = listOf(
        string("org.mozilla.fenix.message.tag"),
        methodCall(definingClass = WORKER_RESULT_SUCCESS, name = "<init>", parameters = listOf()),
    ),
)

internal object DefaultBrowserPromptFingerprint : Fingerprint(
    definingClass = "Lorg/mozilla/fenix/HomeActivity;",
    name = "onCreate",
    parameters = listOf("Landroid/os/Bundle;"),
    filters = listOf(
        methodCall(
            definingClass = "Lmozilla/components/support/utils/Browsers\$Companion;",
            name = "isDefaultBrowser",
            parameters = listOf("Landroid/content/Context;"),
        ),
        opcode(Opcode.MOVE_RESULT, MatchAfterImmediately()),
        literal(0x1d, location = MatchAfterWithin(4)),
        fieldAccess(
            opcode = Opcode.SGET_OBJECT,
            definingClass = "Lorg/mozilla/fenix/HomeActivity\$maybeShowSetAsDefaultBrowserPrompt\$1;",
            location = MatchAfterWithin(20),
        ),
    ),
)

private const val ONBOARDING_CARD_DATA = "Lorg/mozilla/fenix/nimbus/OnboardingCardData;"
internal const val ONBOARDING_CARD_TYPE = "Lorg/mozilla/fenix/nimbus/OnboardingCardType;"

internal object OnboardingCardFilterFingerprint : Fingerprint(
    definingClass = "Lorg/mozilla/fenix/onboarding/view/OnboardingMapperKt\$\$",
    name = "invoke",
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Ljava/lang/Object;"),
    filters = listOf(
        checkCast(ONBOARDING_CARD_DATA, MatchFirst()),
        fieldAccess(opcode = Opcode.IGET_OBJECT, definingClass = ONBOARDING_CARD_DATA, name = "cardType\$delegate"),
        checkCast(ONBOARDING_CARD_TYPE, MatchAfterWithin(3)),
        methodCall(definingClass = ONBOARDING_CARD_DATA, name = "getEnabled"),
    ),
)

internal object ContinuousOnboardingFingerprint : Fingerprint(
    name = "start",
    returnType = "V",
    parameters = listOf(),
    strings = listOf("Showing default-browser role request dialog.", "Showing sync card dialog."),
)

internal object ReviewPromptStateFingerprint : Fingerprint(
    definingClass = "Lorg/mozilla/fenix/reviewprompt/ShowReviewPromptBinding;",
    name = "onState",
    parameters = listOf("Lkotlinx/coroutines/flow/Flow;", "Lkotlin/coroutines/Continuation;"),
    filters = listOf(fieldAccess(opcode = Opcode.SGET_OBJECT, definingClass = "Lkotlin/Unit;", name = "INSTANCE")),
)
