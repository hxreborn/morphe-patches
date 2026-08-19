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
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction
import java.io.File

private const val DARK_BACKGROUND_CSS = "--background-norm: #191927"
private const val BLACK_BACKGROUND_CSS = "--background-norm: #000000"
private const val EXTENSION_CLASS = "Lapp/hxreborn/extension/protonmail/AmoledTheme;"
private const val LOAD_DATA_WITH_BASE_URL =
    "Landroid/webkit/WebView;->loadDataWithBaseURL(Ljava/lang/String;Ljava/lang/String;" +
        "Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V"

private fun MutableMethod.replaceColorWithBlack(index: Int) {
    val register = getInstruction<OneRegisterInstruction>(index).registerA
    replaceInstruction(index, "const-wide v$register, 0xff000000L")
}

private fun File.replaceDarkBackgroundCss() = replaceAsciiInPlace(DARK_BACKGROUND_CSS, BLACK_BACKGROUND_CSS)

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

        UpsellingDarkBackgroundFingerprint.instructionMatchesOrNull?.first()?.index?.let { index ->
            UpsellingDarkBackgroundFingerprint.method.replaceColorWithBlack(index)
        }

        CachedMessageBodyFingerprint.method.replaceCachedMessageBackground()
        InlineMessageBodyFingerprint.method.replaceInlineMessageBackground()
    }
}
