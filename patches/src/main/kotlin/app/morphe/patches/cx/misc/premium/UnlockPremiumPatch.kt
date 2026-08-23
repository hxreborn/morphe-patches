/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.cx.misc.premium

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.matchSingle
import app.morphe.util.returnEarly

private const val STRING_CLASS = "Ljava/lang/String;"
private const val LICENSE_CHECK_COUNT = 2

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks premium and removes ads.",
) {
    compatibleWith(AppCompatibilities.CX_FILE_EXPLORER)

    execute {
        val licenseChecks = LicenseHolderFingerprint.matchSingle().classDef.methods.filter {
            it.returnType == "Z" &&
                it.parameterTypes.map(CharSequence::toString) == listOf(STRING_CLASS)
        }

        if (licenseChecks.size != LICENSE_CHECK_COUNT) {
            throw PatchException(
                "Expected $LICENSE_CHECK_COUNT licence checks, found ${licenseChecks.size}",
            )
        }

        licenseChecks.forEach { it.returnEarly(true) }
    }
}
