/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonmail.misc.theme

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.all.misc.resources.resourceMappingPatch
import app.morphe.patches.protonmail.misc.theme.webview.CachedMessageBodyFingerprint
import app.morphe.patches.protonmail.misc.theme.webview.ComposerCssFingerprint
import app.morphe.patches.protonmail.misc.theme.webview.InlineMessageBodyFingerprint
import app.morphe.patches.protonmail.misc.theme.webview.webSettingsThemePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.proton.markFeaturePatched
import app.morphe.patches.shared.misc.proton.AMOLED_THEME_CLASS
import app.morphe.patches.shared.misc.proton.transformCoreDarkBackgrounds
import app.morphe.patches.shared.misc.proton.injectColorTransformCall
import app.morphe.util.matchSingle
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.indexOfFirstLiteralInstructionOrThrow
import app.morphe.util.indexOfFirstInstructionReversed
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import java.lang.Long.toHexString

private const val WEB_CONTENT_BACKGROUND_CLASS = "Lapp/hxreborn/extension/protonmail/WebContentBackground;"
private const val SIDEBAR_COLORS_REGISTER_OFFSET = 18
private const val SIDEBAR_INTERACTION_PRESSED = 1
private const val SIDEBAR_SEPARATOR = 2
private const val COLOR_PACK_SHIFT = 32
private const val LOAD_DATA_WITH_BASE_URL =
    "Landroid/webkit/WebView;->loadDataWithBaseURL(Ljava/lang/String;Ljava/lang/String;" +
        "Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V"

private fun MutableMethod.injectBackgroundTransformCall(index: Int) =
    injectColorTransformCall(index, "$AMOLED_THEME_CLASS->transformBackground(J)J")

private const val TRANSFORM_PACKED_BACKGROUND = "$AMOLED_THEME_CLASS->transformPackedBackground(J)J"
private const val TRANSFORM_PACKED_SURFACE = "$AMOLED_THEME_CLASS->transformPackedSurface(J)J"

private val CONTACTS_COLOR_TRANSFORMS = listOf(
    ContactListScreenBackgroundFingerprint to TRANSFORM_PACKED_BACKGROUND,
    ContactListTopBarBackgroundFingerprint to TRANSFORM_PACKED_BACKGROUND,
    ContactSearchScreenBackgroundFingerprint to TRANSFORM_PACKED_BACKGROUND,
    ContactSearchTopBarBackgroundFingerprint to TRANSFORM_PACKED_BACKGROUND,
    ContactSearchFieldBackgroundFingerprint to TRANSFORM_PACKED_BACKGROUND,
    ContactCardSurfaceFingerprint to TRANSFORM_PACKED_SURFACE,
    ContactSwipeBoxSurfaceFingerprint to TRANSFORM_PACKED_SURFACE,
)

private fun Instruction.constructsProtonColors(): Boolean {
    if (opcode != Opcode.INVOKE_DIRECT_RANGE) return false
    val reference = (this as? ReferenceInstruction)?.reference as? MethodReference ?: return false

    return reference.name == "<init>" &&
        reference.parameterTypes.map(CharSequence::toString) == PROTON_COLORS_PARAMETERS
}

private fun MutableMethod.constructsDarkColors(invokeIndex: Int): Boolean {
    val flagRegister = getInstruction<RegisterRangeInstruction>(invokeIndex).startRegister + 1
    val flagIndex = indexOfFirstInstructionReversed(invokeIndex) {
        this is OneRegisterInstruction && registerA == flagRegister
    }

    return flagIndex >= 0 && (getInstruction(flagIndex) as? WideLiteralInstruction)?.wideLiteral == 1L
}

private fun MutableMethod.indexOfDarkColorsOrThrow() =
    implementation!!.instructions.withIndex()
        .filter { (_, instruction) -> instruction.constructsProtonColors() }
        .singleOrNull { (index, _) -> constructsDarkColors(index) }
        ?.index
        ?: throw PatchException("Could not find the dark color scheme")

private fun MutableMethod.setSidebarPressedAndSeparatorColors() {
    val invokeIndex = indexOfDarkColorsOrThrow()
    val instruction = getInstruction<RegisterRangeInstruction>(invokeIndex)
    val sidebarColorsRegister = instruction.startRegister + instruction.registerCount - SIDEBAR_COLORS_REGISTER_OFFSET
    val packed = toHexString(SIDEBAR_PRESSED_AND_SEPARATOR_COLOR shl COLOR_PACK_SHIFT)

    addInstructions(
        invokeIndex,
        listOf(SIDEBAR_INTERACTION_PRESSED, SIDEBAR_SEPARATOR).joinToString("\n") { role ->
            "const-wide v${sidebarColorsRegister + role * 2}, 0x${packed}L"
        },
    )
}

private fun MutableMethod.replaceCachedMessageBackground() {
    val index = indexOfFirstInstructionOrThrow {
        opcode == Opcode.CHECK_CAST &&
            (this as? ReferenceInstruction)?.reference?.toString() == "Ljava/io/InputStream;"
    }
    val register = getInstruction<OneRegisterInstruction>(index).registerA

    addInstructions(
        index + 1,
        """
            invoke-static/range {v$register .. v$register}, $WEB_CONTENT_BACKGROUND_CLASS->replaceBackground(Ljava/io/InputStream;)Ljava/io/InputStream;
            move-result-object v$register
        """,
    )
}

private fun MutableMethod.replaceInlineMessageBackground() {
    val index = indexOfFirstInstructionOrThrow {
        (this as? ReferenceInstruction)?.reference?.toString() == LOAD_DATA_WITH_BASE_URL
    }
    val register = getInstruction<RegisterRangeInstruction>(index).startRegister + 2

    addInstructions(
        index,
        """
            invoke-static/range {v$register .. v$register}, $WEB_CONTENT_BACKGROUND_CLASS->replaceBackground(Ljava/lang/String;)Ljava/lang/String;
            move-result-object v$register
        """,
    )
}

@Suppress("unused")
val amoledThemePatch = bytecodePatch(
    name = "AMOLED dark theme",
    description = "Replaces the dark theme background with pure black.",
) {
    compatibleWith(AppCompatibilities.PROTON_MAIL)
    dependsOn(resourceMappingPatch, webSettingsThemePatch)

    execute {
        DarkPaletteFingerprint.matchSingle().method.apply {
            DARK_BACKGROUND_COLORS.forEach { color ->
                injectBackgroundTransformCall(indexOfFirstLiteralInstructionOrThrow(color))
            }
        }

        transformCoreDarkBackgrounds()

        ColorSchemeFingerprint.matchSingle().method.setSidebarPressedAndSeparatorColors()

        UpsellingDarkBackgroundFingerprint.instructionMatchesOrNull?.first()?.index?.let { index ->
            UpsellingDarkBackgroundFingerprint.method.injectBackgroundTransformCall(index)
        }

        CONTACTS_COLOR_TRANSFORMS.forEach { (fingerprint, transform) ->
            val match = fingerprint.matchSingle()
            match.method.injectColorTransformCall(match.instructionMatches.last().index, transform)
        }

        markFeaturePatched(AMOLED_THEME_CLASS)

        CachedMessageBodyFingerprint.matchSingle().method.replaceCachedMessageBackground()
        InlineMessageBodyFingerprint.matchSingle().method.replaceInlineMessageBackground()
        ComposerCssFingerprint.matchSingle().let { match ->
            val inputStreamResult = match.instructionMatches.last()
            val register = inputStreamResult.getInstruction<OneRegisterInstruction>().registerA
            match.method.addInstructions(
                inputStreamResult.index + 1,
                """
                    invoke-static/range { v$register .. v$register }, $WEB_CONTENT_BACKGROUND_CLASS->replaceBackground(Ljava/io/InputStream;)Ljava/io/InputStream;
                    move-result-object v$register
                """,
            )
        }
    }
}
