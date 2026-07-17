/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Additional terms under GPLv3 section 7:
 * - You must preserve reasonable legal notices and author attributions in this file.
 * - Modified versions must not misrepresent the origin of this file.
 *
 * Ported from hxreborn/revanced-patches:
 * https://gitlab.com/hxreborn/revanced-patches/-/commit/ea27afb265862656079dea62697b42032a4704a3
 * Commit ea27afb265862656079dea62697b42032a4704a3 (2026-05-17),
 * patches/src/main/kotlin/app/revanced/patches/projectivy/misc/premium/UnlockPremiumPatch.kt
 */
package app.morphe.patches.projectivy.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.returnEarly
import com.android.tools.smali.dexlib2.AccessFlags

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks premium features.",
) {
    compatibleWith("com.spocky.projengmenu")

    execute {
        val methods = PtApplicationMarkerFingerprint.classDef.methods

        methods.single {
            AccessFlags.STATIC.isSet(it.accessFlags) &&
                it.returnType == "Z" &&
                it.parameterTypes.isEmpty()
        }.returnEarly(true)

        // Force the billing state setter to mark premium as active.
        methods.singleOrNull {
            it.parameterTypes == listOf("Lcom/spocky/projengmenu/PTApplication;", "Z")
        }?.addInstruction(0, "const/4 p1, 0x0")
    }
}
