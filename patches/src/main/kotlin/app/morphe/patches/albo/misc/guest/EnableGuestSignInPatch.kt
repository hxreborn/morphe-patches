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
        pattern = "30f05f3811f05f38300a508a1f825cea000000540000009404002036403f40f900f448f9504b40f9".hexToByteArray(),
        mask = "ffffffffffffffffffffffffffffffff1f0000ff000000fc1f00f8ffffffffffffffffffffffffff".hexToByteArray(),
        replacement = NOP,
    ),
    BranchSite(
        name = "guest button availability check",
        pattern = ("640340f900000094e10300aa62034091420040f90000009400002037" +
            "a0835ff800002037e00316aa00000014").hexToByteArray(),
        mask = ("ff03c0ff000000fcffffffffff03c0ffff03c0ff000000fc1f00f8ff" +
            "ffffffff1f00f8ffffffffff000000fc").hexToByteArray(),
        replacement = NOP,
    ),
    BranchSite(
        name = "guest captcha token check",
        pattern = ("a0035ff801f040b821801c8b62034091420040f90000009400002037" +
            "a0035ff8013041b821801c8b00000094e10300aa").hexToByteArray(),
        mask = ("ffffffffffffffffffffffffff03c0ffff03c0ff000000fc1f00f8ff" +
            "ffffffffffffffffffffffff000000fcffffffff").hexToByteArray(),
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
