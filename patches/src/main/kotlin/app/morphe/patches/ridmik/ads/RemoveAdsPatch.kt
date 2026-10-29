/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.ridmik.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.doRecursively
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import java.io.File
import org.w3c.dom.Element

private const val AD_SLOT_ID = "@id/adsParent"

private val collapseAdSlotsPatch = resourcePatch {
    execute {
        val layouts = decode("res/layout*")
        val slotLayouts = layouts.filter { path -> AD_SLOT_ID in get(path).readText() }
        if (slotLayouts.isEmpty()) throw PatchException("No layout contains $AD_SLOT_ID")

        val slotLayoutReferences = mutableSetOf<String>()
        slotLayouts.forEach { path ->
            document(path).use { document ->
                if (document.documentElement.getAttribute("android:id") == AD_SLOT_ID) {
                    slotLayoutReferences += "@layout/${File(path).nameWithoutExtension}"
                }
                document.documentElement.hideMatching { it.getAttribute("android:id") == AD_SLOT_ID }
            }
        }

        layouts.filter { path -> slotLayoutReferences.any { it in get(path).readText() } }.forEach { path ->
            document(path).use { document ->
                document.documentElement.hideMatching {
                    it.tagName == "include" && it.getAttribute("layout") in slotLayoutReferences
                }
            }
        }
    }
}

private fun Element.hideMatching(predicate: (Element) -> Boolean) = doRecursively { node ->
    if (node is Element && predicate(node)) node.setAttribute("android:visibility", "gone")
}

@Suppress("unused")
val removeAdsPatch = bytecodePatch(
    name = "Remove ads",
    description = "Removes banner, full-screen and app-open ads from the keyboard and the app, " +
        "and sponsored results in GIF search.",
) {
    compatibleWith(AppCompatibilities.RIDMIK_KEYBOARD)

    dependsOn(collapseAdSlotsPatch)

    execute {
        KeyboardPremiumStatusFingerprint.matchSingle().method.returnEarly(true)
        AppPremiumStatusFingerprint.matchSingle().method.returnEarly(true)
        EmojiSearchAdLoadFingerprint.matchSingle().method.returnEarly()
        StoreAdRowFingerprint.matchSingle().method.returnEarly()

        GifAdsEnabledFingerprint.matchAll(2..2).forEach { match ->
            val resultIndex = match.instructionMatches.last().index
            val resultRegister = match.method.getInstruction<OneRegisterInstruction>(resultIndex).registerA
            match.method.addInstruction(resultIndex + 1, "const/4 v$resultRegister, 0x0")
        }
    }
}
