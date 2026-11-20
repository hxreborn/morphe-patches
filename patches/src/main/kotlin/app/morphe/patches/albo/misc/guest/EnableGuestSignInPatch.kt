/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.albo.misc.guest

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.dart.DART_AOT_LIBRARY
import app.morphe.patches.shared.misc.dart.dartAotLibrary
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.patches.shared.replaceMasked

private const val BRANCH_OFFSET = 24

private val NOP = "1f2003d5".hexToByteArray()
private val BRANCH_OVER_TOKEN_CHECK = "0a000014".hexToByteArray()

private class BranchSite(
    val name: String,
    val pattern: ByteArray,
    val mask: ByteArray,
    val replacement: ByteArray,
)

private val GUEST_SIGN_IN_SITES = listOf(
    BranchSite(
        name = "guest button visibility check",
        pattern = ("30f05f3811f05f38300a508a1f825cea400000545832229484042036a0835ff8" +
            "60002037e00316aa").hexToByteArray(),
        mask = ("ffffffffffffffffffffffffffffffff1f0000ff000000fc1f00f8ffff0fe0ff" +
            "1f00f8ffffffffff").hexToByteArray(),
        replacement = NOP,
    ),
    BranchSite(
        name = "guest captcha token check",
        pattern = ("a08357f801f040b821801c8b62374491423c47f908e9d39740012037" +
            "a08357f8013041b821801c8b70010094e10300aa").hexToByteArray(),
        mask = ("ff0fe0ffffffffffffffffffff0380ffff03c0ff000000fc1f00f8ff" +
            "ff0fe0ffffffffffffffffff000000fcffffffff").hexToByteArray(),
        replacement = BRANCH_OVER_TOKEN_CHECK,
    ),
)

@Suppress("unused")
val enableGuestSignInPatch = resourcePatch(
    name = "Enable guest sign-in",
    description = "Enables guest sign-in without a Google account.",
) {
    compatibleWith(AppCompatibilities.ALBO)
    availability(requireArm64)
    dependsOn(removePairipProtectionPatch)

    execute {
        val library = dartAotLibrary()

        GUEST_SIGN_IN_SITES.forEach { site ->
            if (!library.replaceMasked(site.pattern, site.mask, mapOf(BRANCH_OFFSET to site.replacement))) {
                throw PatchException("Could not find the ${site.name} in $DART_AOT_LIBRARY")
            }
        }
    }
}
