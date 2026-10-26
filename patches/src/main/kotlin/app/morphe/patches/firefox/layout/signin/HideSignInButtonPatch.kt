/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from fork-maintainers/iceraven-browser (MPL-2.0):
 * https://github.com/fork-maintainers/iceraven-browser/commit/56c08e8000c4e16c88010ddf2e991016ddb50a8c
 * Commit 56c08e8000c4e16c88010ddf2e991016ddb50a8c (2026-10-20),
 * app/src/main/java/org/mozilla/fenix/settings/account/AccountUiView.kt
 */
package app.morphe.patches.firefox.layout.signin

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.firefox.misc.ui.PATCH_SETTINGS
import app.morphe.patches.firefox.shared.listInSheet
import app.morphe.patches.firefox.shared.readPreferenceAsFalse
import app.morphe.patches.firefox.shared.whenEnabled
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.smali.addStaticMethod
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.util.ReferenceUtil

private const val HIDE_SIGN_IN = "$PATCH_SETTINGS->hideSignIn()Z"

private const val COMPOSER = "Landroidx/compose/runtime/Composer;"
private const val CONTEXT_KT = "Lorg/mozilla/fenix/ext/ContextKt;"
private const val COMPONENTS = "Lorg/mozilla/fenix/components/Components;"
private const val BACKGROUND_SERVICES = "Lorg/mozilla/fenix/components/BackgroundServices;"
private const val ACCOUNT_MANAGER = "Lmozilla/components/service/fxa/manager/FxaAccountManager;"
private const val ACCOUNT_STATE = "Lmozilla/components/service/fxa/manager/AccountState"

@Suppress("unused")
val hideSignInButtonPatch = bytecodePatch(
    name = "Hide sign-in button",
    description = "Hides Sign in entries from Settings, the menu, Synced tabs, sharing, Bookmarks and " +
        "Passwords settings.",
    default = false,
) {
    compatibleWith(AppCompatibilities.FIREFOX)

    extendWith("extensions/extension.mpe")

    execute {
        listInSheet("hideSignIn")

        AccountUiStateFingerprint.matchSingle().let { match ->
            val method = match.method
            val signIn = match.instructionMatches.last().getInstruction<OneRegisterInstruction>().registerA
            val hide = match.classDef.addStaticMethod(
                "hideSignIn",
                listOf(PREFERENCE, "Z"),
                "V",
                3,
                """
                    invoke-static { }, $HIDE_SIGN_IN
                    move-result v0
                    if-eqz v0, :show
                    const/4 p1, 0x0
                    :show
                    invoke-virtual { p0, p1 }, $PREFERENCE->setVisible(Z)V
                    return-void
                """,
            )
            val visibilityCalls = method.instructions.withIndex().filter { (_, instruction) ->
                instruction.opcode == Opcode.INVOKE_VIRTUAL &&
                    instruction.getReference<MethodReference>()?.name == "setVisible" &&
                    (instruction as FiveRegisterInstruction).registerC == signIn
            }
            if (visibilityCalls.isEmpty()) throw PatchException("No setVisible call on the sign-in preference")
            visibilityCalls.forEach { (index, instruction) ->
                val visible = (instruction as FiveRegisterInstruction).registerD
                method.replaceInstruction(index, "invoke-static { v$signIn, v$visible }, $hide")
            }
            val addressSync = AddressSyncVisibilityFingerprint.matchSingle()
            val visibilityCall = addressSync.instructionMatches.last()
            val arguments = visibilityCall.getInstruction<FiveRegisterInstruction>()
            addressSync.method.replaceInstruction(
                visibilityCall.index,
                "invoke-static { v${arguments.registerC}, v${arguments.registerD} }, $hide",
            )
        }

        MenuAccountRowFingerprint.matchSingle().let { match ->
            val call = match.instructionMatches.single()
            val range = call.getInstruction<RegisterRangeInstruction>()
            val reference = call.instruction.getReference<MethodReference>()!!
            val original = ReferenceUtil.getMethodDescriptor(reference)
            val accountRow = match.classDef.addStaticMethod(
                "accountRow",
                reference.parameterTypes.map(CharSequence::toString),
                "V",
                reference.parameterTypes.size + 1,
                """
                    invoke-static { }, $HIDE_SIGN_IN
                    move-result v0
                    if-eqz v0, :show
                    instance-of v0, p1, ${ACCOUNT_STATE}${'$'}NotAuthenticated;
                    if-nez v0, :hide
                    instance-of v0, p1, ${ACCOUNT_STATE}${'$'}Unknown;
                    if-eqz v0, :show
                    :hide
                    const v0, 0x6e7a3b40
                    invoke-interface { p4, v0 }, $COMPOSER->startReplaceGroup(I)V
                    invoke-interface { p4 }, $COMPOSER->endReplaceGroup()V
                    return-void
                    :show
                    invoke-static/range { p0 .. p5 }, $original
                    return-void
                """,
            )
            val first = range.startRegister
            match.method.replaceInstruction(
                call.index,
                "invoke-static/range { v$first .. v${first + range.registerCount - 1} }, $accountRow",
            )
        }

        SyncedTabsSignInPageFingerprint.matchSingle().let { match ->
            val method = match.method
            val (description, cta, endNode) = match.instructionMatches
            val text = description.getInstruction<OneRegisterInstruction>().registerA
            val end = method.getInstruction(endNode.index)
            val ctaScratch = method.getFreeRegisterProvider(cta.index, 1).getFreeRegister()
            method.addInstructionsWithLabels(
                cta.index,
                """
                    invoke-static { }, $HIDE_SIGN_IN
                    move-result v$ctaScratch
                    if-nez v$ctaScratch, :end
                """,
                ExternalLabel("end", end),
            )
            method.whenEnabled(HIDE_SIGN_IN, description.index + 1, exclude = text) {
                "sget v$text, $R_STRING->synced_tabs_no_tabs:I"
            }
        }

        ShareSignInOptionFingerprint.matchAll().forEach { match ->
            val moveResult = match.instructionMatches.last()
            val list = moveResult.getInstruction<OneRegisterInstruction>().registerA
            match.method.whenEnabled(HIDE_SIGN_IN, moveResult.index + 1, exclude = list) {
                """
                    invoke-static { }, Ljava/util/Collections;->emptyList()Ljava/util/List;
                    move-result-object v$list
                """
            }
        }

        BookmarksSignInButtonFingerprint.matchSingle().method.let { method ->
            val showSignIn = "p${method.parameterTypes.size - 1}"
            method.whenEnabled(HIDE_SIGN_IN) { "const/16 $showSignIn, 0x0" }
        }

        SyncPreferenceLoggedOffFingerprint.matchSingle().let { match ->
            val title = match.instructionMatches.last()
            val preference = title.getInstruction<FiveRegisterInstruction>().registerC
            val hide = match.classDef.addStaticMethod(
                "hideSignInRow",
                listOf(PREFERENCE),
                "V",
                2,
                """
                    invoke-static { }, $HIDE_SIGN_IN
                    move-result v0
                    if-eqz v0, :show
                    const/4 v0, 0x0
                    invoke-virtual { p0, v0 }, $PREFERENCE->setVisible(Z)V
                    :show
                    return-void
                """,
            )
            match.method.addInstruction(title.index + 1, "invoke-static { v$preference }, $hide")
        }

        readPreferenceAsFalse("pref_key_setup_checklist_complete", HIDE_SIGN_IN)

        SendToDevicesChooserActionFingerprint.matchSingle().method.whenEnabled(HIDE_SIGN_IN) { (account) ->
            """
                invoke-static/range { p1 .. p1 }, $CONTEXT_KT->getComponents(Landroid/content/Context;)$COMPONENTS
                move-result-object $account
                invoke-virtual/range { $account .. $account }, $COMPONENTS->getBackgroundServices()$BACKGROUND_SERVICES
                move-result-object $account
                invoke-virtual/range { $account .. $account }, $BACKGROUND_SERVICES->getAccountManager()$ACCOUNT_MANAGER
                move-result-object $account
                invoke-virtual/range { $account .. $account }, $ACCOUNT_MANAGER->authenticatedAccount()Lmozilla/components/service/fxa/FirefoxAccount;
                move-result-object $account
                if-nez $account, :hx_stock
                const/16 $account, 0x0
                return-object $account
            """
        }
    }
}
