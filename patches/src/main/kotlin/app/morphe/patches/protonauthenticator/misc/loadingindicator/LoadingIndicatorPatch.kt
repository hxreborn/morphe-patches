/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonauthenticator.misc.loadingindicator

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.protonauthenticator.misc.settings.patchesSettingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.proton.drawPullRefreshAsLoadingIndicator
import app.morphe.patches.shared.misc.proton.resizeLoadingIndicatorContainer
import app.morphe.patches.shared.misc.smali.addStaticMethod
import app.morphe.patches.shared.misc.smali.descriptor
import app.morphe.patches.shared.misc.smali.replaceStub
import app.morphe.util.getReference
import app.morphe.util.matchSingle
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val ANDROID_VIEW_PARAMETERS = 6
private const val COLOR_TYPE = "J"
private const val CONTAINER_AND_INDICATOR_COLOR_REGISTERS = 4
private const val TONE_WITH_MAX_CHROMA_REGISTERS = 5
private const val TONE_WITH_MAX_CHROMA_STUB_REGISTERS = 3
private const val LOADING_INDICATOR_CONTAINER_CLASS =
    "Lapp/hxreborn/extension/protonauthenticator/LoadingIndicatorContainer;"

@Suppress("unused")
val loadingIndicatorPatch = bytecodePatch(
    name = "Material 3 loading indicator",
    description = "Shows the pull to refresh indicator as the Material 3 Expressive shape-morphing loading indicator.",
) {
    compatibleWith(AppCompatibilities.PROTON_AUTHENTICATOR)
    dependsOn(patchesSettingsPatch)

    execute {
        drawPullRefreshAsLoadingIndicator(PullToRefreshContentFingerprint, ANDROID_VIEW_PARAMETERS) { producerType, producer, progress, _ ->
            val produce = classDefBy(producerType).methods.single { it.returnType == "F" && it.parameterTypes.isEmpty() }
            """
                invoke-interface { $producer }, ${descriptor(produce)}
                move-result $progress
            """
        }
        PullToRefreshIndicatorSizeFingerprint.matchSingle().let { match ->
            resizeLoadingIndicatorContainer(match.method, match.instructionMatches.last())
        }

        PullToRefreshIndicatorColorsFingerprint.matchSingle().let { match ->
            val indicatorCall = match.instructionMatches.last()
            val indicatorInvoke = indicatorCall.instruction as? RegisterRangeInstruction
                ?: throw PatchException("Expected a range invoke of the pull to refresh indicator in ${descriptor(match.method)}")
            val parameters = indicatorInvoke.getReference<MethodReference>()!!.parameterTypes.map(CharSequence::toString)
            val containerParameter = parameters.indexOf(COLOR_TYPE)
            if (containerParameter < 0 || parameters.getOrNull(containerParameter + 1) != COLOR_TYPE) {
                throw PatchException("Expected container and indicator colors in the pull to refresh indicator call")
            }
            val containerColorRegister = indicatorInvoke.startRegister + 1 + containerParameter
            val lastColorRegister = containerColorRegister + CONTAINER_AND_INDICATOR_COLOR_REGISTERS - 1
            match.method.addInstructions(
                indicatorCall.index,
                """
                    invoke-static/range { v$containerColorRegister .. v$lastColorRegister }, $LOADING_INDICATOR_CONTAINER_CLASS->containerColor(JJ)J
                    move-result-wide v$containerColorRegister
                """,
            )
        }

        ColorRoleToneFingerprint.matchSingle().let { match ->
            val hctFromArgb = match.instructionMatches.first().instruction.getReference<MethodReference>()!!
            val hctToArgb = match.instructionMatches.last().instruction.getReference<MethodReference>()!!
            val hctType = hctFromArgb.returnType
            val hctFromComponents = hctFromComponentsFingerprint(hctType).matchSingle().originalMethod
            val hctHue = hctComponentGetterFingerprint(hctType, "getHue").matchSingle().originalMethod
            val hctChroma = hctComponentGetterFingerprint(hctType, "getChroma").matchSingle().originalMethod
            val toneWithMaxChroma = mutableClassDefBy(match.classDef.type).addStaticMethod(
                "toneWithMaxChroma",
                listOf("I", "F", "F"),
                "I",
                TONE_WITH_MAX_CHROMA_REGISTERS,
                """
                    invoke-static { p0 }, ${descriptor(hctFromArgb)}
                    move-result-object v0
                    invoke-virtual { v0 }, ${descriptor(hctHue)}
                    move-result v1
                    invoke-virtual { v0 }, ${descriptor(hctChroma)}
                    move-result v0
                    invoke-static { v0, p1 }, Ljava/lang/Math;->min(FF)F
                    move-result v0
                    invoke-static { v1, v0, p2 }, ${descriptor(hctFromComponents)}
                    move-result-object v0
                    invoke-virtual { v0 }, ${descriptor(hctToArgb)}
                    move-result v0
                    return v0
                """,
            )
            mutableClassDefBy(LOADING_INDICATOR_CONTAINER_CLASS).replaceStub(
                "toneWithMaxChroma",
                TONE_WITH_MAX_CHROMA_STUB_REGISTERS,
                """
                    invoke-static { p0, p1, p2 }, $toneWithMaxChroma
                    move-result p0
                    return p0
                """,
            )
        }
    }
}
