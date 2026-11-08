/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonpass.misc.navigationbar

import app.morphe.patcher.Match
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.util.getReference
import app.morphe.patches.shared.misc.smali.addStaticMethod
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.misc.proton.installMeasuredBottomInsets
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import app.morphe.patches.protonpass.misc.settings.patchesSettingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.proton.FLOATING_NAVIGATION_BAR_CLASS
import app.morphe.patches.shared.misc.proton.injectFloatingNavigationBar
import app.morphe.patches.shared.misc.proton.markFeaturePatched
import app.morphe.patches.shared.misc.proton.markPatchMetadata
import app.morphe.patches.shared.misc.smali.descriptor
import app.morphe.patches.shared.misc.smali.replaceStub
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
val floatingNavigationBarPatch: BytecodePatch = bytecodePatch(
    name = "Floating navigation bar",
    description = "Adds optional floating navigation with adjustable blur and glass.",
) {
    compatibleWith(AppCompatibilities.PROTON_PASS)
    dependsOn(patchesSettingsPatch)

    var contentPadding: Match? = null

    execute {
        contentPadding = AppContentPaddingFingerprint.matchSingle()
        markPatchMetadata("floatingNavigationBar", floatingNavigationBarPatch)
        markFeaturePatched(FLOATING_NAVIGATION_BAR_CLASS)
        val insets = NavigationBarsPaddingFingerprint.matchOrNull()?.method
        if (insets == null) {
            installMeasuredBottomInsets(COMPOSE_MODIFIER)
        } else {
            mutableClassDefBy(FLOATING_NAVIGATION_BAR_CLASS).replaceStub(
                "outerInsets",
                2,
                """
                    check-cast p0, $COMPOSE_MODIFIER
                    invoke-static { p0 }, ${descriptor(insets)}
                    move-result-object p0
                    return-object p0
                """,
            )
        }
        HomeBottomBarFingerprint.matchSingle().let { match ->
            val (navigationCall) = match.instructionMatches
            val outer = navigationCall.instruction.getReference<MethodReference>()!!
            val nativeNavigation = nativeBottomNavigationFingerprint(outer).matchSingle()
            val (nativeCall) = nativeNavigation.instructionMatches
            val native = nativeCall.instruction.getReference<MethodReference>()!!
            val parameters = native.parameterTypes.map(CharSequence::toString)
            check(parameters.count { it == "F" } == 1)
            val elevationRegister = parameters.take(parameters.indexOf("F")).sumOf { if (it == "J" || it == "D") 2 else 1 }
            val registers = parameters.sumOf { if (it == "J" || it == "D") 2 else 1 }
            val bridge = mutableClassDefBy(FLOATING_NAVIGATION_BAR_CLASS).addStaticMethod(
                "nativeBottomNavigation", parameters, "V", registers,
                """
                    invoke-static/range { p$elevationRegister .. p$elevationRegister }, $FLOATING_NAVIGATION_BAR_CLASS->elevation(F)F
                    move-result p$elevationRegister
                    invoke-static/range { p0 .. p${registers - 1} }, ${descriptor(native)}
                    return-void
                """,
            )
            val nativeRange = nativeCall.instruction as RegisterRangeInstruction
            nativeNavigation.method.replaceInstruction(
                nativeCall.index,
                "invoke-static/range { v${nativeRange.startRegister} .. v${nativeRange.startRegister + nativeRange.registerCount - 1} }, $bridge",
            )
            injectFloatingNavigationBar(match.method, navigationCall.index)
            if (insets == null) {
                val animation = navigationInsetAnimationFingerprint(match.method).matchSingle()
                val (animationCall) = animation.instructionMatches
                val register = animationCall.getInstruction<RegisterRangeInstruction>().startRegister
                match.method.addInstructions(
                    animationCall.index,
                    """
                        invoke-static { v$register }, $FLOATING_NAVIGATION_BAR_CLASS->itemBottomPadding(F)F
                        move-result v$register
                    """,
                )
            } else {
                val conversion = navigationInsetConversionFingerprint(match.method).matchSingle()
                val (_, paddingResult) = conversion.instructionMatches
                val register = paddingResult.getInstruction<OneRegisterInstruction>().registerA
                match.method.addInstructions(
                    paddingResult.index + 1,
                    """
                        invoke-static { v$register }, $FLOATING_NAVIGATION_BAR_CLASS->itemBottomPadding(F)F
                        move-result v$register
                    """,
                )
            }
        }
    }
    finalize {
        installFloatingContentPadding(checkNotNull(contentPadding))
    }

}
