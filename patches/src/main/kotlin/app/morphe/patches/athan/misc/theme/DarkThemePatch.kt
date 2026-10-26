/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.athan.misc.theme

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.athan.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.asSequence
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import org.w3c.dom.Document
import org.w3c.dom.Element

private const val FORCE_DARK_ALLOWED = "android:forceDarkAllowed"

private const val APPCOMPAT_LIGHT_BASE_THEME = "Base.V7.Theme.AppCompat.Light"

private const val MODE_NIGHT_AUTO = 0

private const val MODE_NIGHT_FOLLOW_SYSTEM = -1

private val styleFiles = listOf("res/values/styles.xml", "res/values-v29/styles.xml")

private fun Document.styles() = getElementsByTagName("style").asSequence().filterIsInstance<Element>()

private val allowForceDarkPatch = resourcePatch {
    execute {
        document(styleFiles.first()).use { styles ->
            val baseTheme = styles.styles().singleOrNull { it.getAttribute("name") == APPCOMPAT_LIGHT_BASE_THEME }
                ?: throw PatchException("$APPCOMPAT_LIGHT_BASE_THEME not found in ${styleFiles.first()}")
            baseTheme.appendChild(
                styles.createElement("item").also {
                    it.setAttribute("name", FORCE_DARK_ALLOWED)
                    it.textContent = "true"
                },
            )
        }

        styleFiles.forEach { path ->
            document(path).use { styles ->
                styles.styles()
                    .flatMap { it.childNodes.asSequence().filterIsInstance<Element>() }
                    .filter { it.getAttribute("name") == FORCE_DARK_ALLOWED }
                    .forEach { it.textContent = "true" }
            }
        }
    }
}

@Suppress("unused")
val darkThemePatch = bytecodePatch(
    name = "Dark theme",
    description = "Shows the app in dark colors when the system dark theme is on.",
) {
    compatibleWith(AppCompatibilities.ATHAN)

    dependsOn(spoofSignaturePatch, allowForceDarkPatch)

    execute {
        SetApplicationNightModeFingerprint.matchAll().forEach { match ->
            val invoke = match.instructionMatches.first()
            val mode = invoke.getInstruction<FiveRegisterInstruction>().registerD
            match.method.addInstruction(invoke.index, "const/4 v$mode, $MODE_NIGHT_AUTO")
        }

        DefaultNightModeFingerprint.matchSingle().let { match ->
            val nightMode = match.instructionMatches[1]
            val register = nightMode.getInstruction<OneRegisterInstruction>().registerA
            match.method.replaceInstruction(nightMode.index, "const/4 v$register, $MODE_NIGHT_FOLLOW_SYSTEM")
        }

        ComposeDisallowForceDarkFingerprint.matchSingle().method.returnEarly()
    }
}
