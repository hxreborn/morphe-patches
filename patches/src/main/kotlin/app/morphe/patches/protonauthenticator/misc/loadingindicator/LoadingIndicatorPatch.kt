/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonauthenticator.misc.loadingindicator

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.protonauthenticator.misc.settings.patchesSettingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.proton.drawPullRefreshAsLoadingIndicator
import app.morphe.patches.shared.misc.smali.descriptor
import app.morphe.patches.shared.misc.smali.replaceStub
import app.morphe.util.getReference
import app.morphe.util.matchSingle
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val ANDROID_VIEW_PARAMETERS = 6
private const val COLOR_TYPE = "J"
private const val CONTAINER_AND_INDICATOR_COLOR_REGISTERS = 4
private const val COLOR_ROLE_COUNT = 4
private const val ACCENT_CONTAINER_ROLE_PARAMETER = 3
private const val ACCENT_CONTAINER_STUB_REGISTERS = 3
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

        val getColorRoles = ColorRolesFingerprint.matchSingle().method
        mutableClassDefBy(LOADING_INDICATOR_CONTAINER_CLASS).replaceStub(
            "accentContainer",
            ACCENT_CONTAINER_STUB_REGISTERS,
            """
                invoke-static { p0, p1 }, ${descriptor(getColorRoles)}
                move-result-object v0
                invoke-virtual { v0 }, ${accentContainerGetter(getColorRoles.returnType)}
                move-result v0
                return v0
            """,
        )
    }
}

private fun BytecodePatchContext.accentContainerGetter(colorRolesType: String): String {
    val colorRolesClass = classDefBy(colorRolesType)
    val constructor = colorRolesClass.methods.single { it.name == "<init>" && it.parameterTypes.size == COLOR_ROLE_COUNT }
    val implementation = constructor.implementation!!
    val accentContainerRegister = implementation.registerCount - (constructor.parameterTypes.size + 1) + ACCENT_CONTAINER_ROLE_PARAMETER
    val accentContainerField = implementation.instructions.single {
        it.opcode == Opcode.IPUT && (it as TwoRegisterInstruction).registerA == accentContainerRegister
    }.getReference<FieldReference>()!!
    val getter = colorRoleGetterFingerprint(accentContainerField).matchSingle().originalMethod
    return descriptor(getter)
}
