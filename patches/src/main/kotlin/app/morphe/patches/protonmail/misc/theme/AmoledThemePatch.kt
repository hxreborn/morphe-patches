/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonmail.misc.theme

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.protonmail.shared.RUST_CORE
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.replaceAsciiInPlace
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.indexOfFirstInstructionReversed
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import java.io.File
import java.lang.Long.toHexString

private const val DARK_BACKGROUND_CSS = "--background-norm: #191927"
private const val BLACK_BACKGROUND_CSS = "--background-norm: #000000"
private const val SIDEBAR_BLOCK_REGISTER_OFFSET = 18
private const val SIDEBAR_INTERACTION_PRESSED = 1
private const val SIDEBAR_SEPARATOR = 2
private const val COLOR_PACK_SHIFT = 32
private const val EXTENSION_CLASS = "Lapp/hxreborn/extension/protonmail/AmoledTheme;"
private const val LOAD_DATA_WITH_BASE_URL =
    "Landroid/webkit/WebView;->loadDataWithBaseURL(Ljava/lang/String;Ljava/lang/String;" +
        "Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V"

private fun MutableMethod.replaceColorWithBlack(index: Int) {
    val register = getInstruction<OneRegisterInstruction>(index).registerA
    replaceInstruction(index, "const-wide v$register, 0xff000000L")
}

private fun File.replaceDarkBackgroundCss() = replaceAsciiInPlace(DARK_BACKGROUND_CSS, BLACK_BACKGROUND_CSS)

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

private fun MutableMethod.restoreSidebarStructure() {
    val invokeIndex = indexOfDarkColorsOrThrow()
    val instruction = getInstruction<RegisterRangeInstruction>(invokeIndex)
    val block = instruction.startRegister + instruction.registerCount - SIDEBAR_BLOCK_REGISTER_OFFSET
    val packed = toHexString(SIDEBAR_STRUCTURE_COLOR shl COLOR_PACK_SHIFT)

    addInstructions(
        invokeIndex,
        listOf(SIDEBAR_INTERACTION_PRESSED, SIDEBAR_SEPARATOR).joinToString("\n") { role ->
            "const-wide v${block + role * 2}, 0x${packed}L"
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
            invoke-static {v$register}, $EXTENSION_CLASS->replaceBackground(Ljava/io/InputStream;)Ljava/io/InputStream;
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
            invoke-static {v$register}, $EXTENSION_CLASS->replaceBackground(Ljava/lang/String;)Ljava/lang/String;
            move-result-object v$register
        """,
    )
}

private val webViewBackgroundPatch = resourcePatch {
    execute {
        if (!get("res/raw/css_reset_with_custom_props.css").replaceDarkBackgroundCss()) {
            throw PatchException("Could not find the composer background")
        }

        val nativeCores = get("lib").walk().filter { it.name == RUST_CORE }
        if (nativeCores.count { it.replaceDarkBackgroundCss() } == 0) {
            throw PatchException("Could not find the message body background")
        }
    }
}

@Suppress("unused")
val amoledThemePatch = bytecodePatch(
    name = "AMOLED dark theme",
    description = "Replaces the dark theme background with pure black.",
) {
    compatibleWith(AppCompatibilities.PROTON_MAIL)
    dependsOn(webViewBackgroundPatch)
    extendWith("extensions/extension.mpe")

    execute {
        DarkPaletteFingerprint.method.apply {
            DARK_BACKGROUND_COLORS.forEach { color ->
                replaceColorWithBlack(
                    indexOfFirstInstructionOrThrow {
                        (this as? WideLiteralInstruction)?.wideLiteral == color
                    },
                )
            }
        }

        ColorSchemeFingerprint.method.restoreSidebarStructure()

        UpsellingDarkBackgroundFingerprint.instructionMatchesOrNull?.first()?.index?.let { index ->
            UpsellingDarkBackgroundFingerprint.method.replaceColorWithBlack(index)
        }

        CachedMessageBodyFingerprint.method.replaceCachedMessageBackground()
        InlineMessageBodyFingerprint.method.replaceInlineMessageBackground()
    }
}
