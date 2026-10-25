/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.albo.misc.imports

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.dart.DART_AOT_LIBRARY
import app.morphe.patches.shared.misc.dart.dartAotLibrary
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.patches.shared.replaceMasked

private val NOP = "1f2003d5".hexToByteArray()

private val LOAD_MAX_ENTITLEMENT_INTO_X0 = "7073409100f244f9106e45f9".hexToByteArray()

private class CreditSite(
    val name: String,
    val pattern: ByteArray,
    val mask: ByteArray,
    val replacementOffset: Int,
    val replacement: ByteArray,
)

private val BULK_IMPORT_CREDIT_SITES = listOf(
    CreditSite(
        name = "bulk import credits entitlement",
        pattern = ("a0831ef82800009470734091106e45f9e04101a9" +
            "705f409110ae45f97e734091de7345f9").hexToByteArray(),
        mask = ("ffffffff000000fcffffffffffffffffffffffff" +
            "ffffffffffffffffffffffffffffffff").hexToByteArray(),
        replacementOffset = 4,
        replacement = LOAD_MAX_ENTITLEMENT_INTO_X0,
    ),
    CreditSite(
        name = "bulk import credit deduction",
        pattern = ("227c419341000036227040f85f0000f16c000054e00316aa38559917a3835ef8" +
            "e20300aa611b4691214840f94ef45294e20300aae10316aa9f000094").hexToByteArray(),
        mask = ("ffffffff1f0000ffffffffffffffffff1f0000ffffffffff000000fcffffffff" +
            "ffffffffff03c0ffff03c0ff000000fcffffffffffffffff000000fc").hexToByteArray(),
        replacementOffset = 16,
        replacement = NOP,
    ),
)

@Suppress("unused")
val removeBulkImportLimitPatch = resourcePatch(
    name = "Remove bulk import limit",
    description = "Removes the credit limit on bulk imports. " +
        "Doesn't affect the weekly limit on single imports.",
) {
    compatibleWith(AppCompatibilities.ALBO)
    availability(requireArm64)
    dependsOn(removePairipProtectionPatch)

    execute {
        val library = dartAotLibrary()

        BULK_IMPORT_CREDIT_SITES.forEach { site ->
            if (!library.replaceMasked(site.pattern, site.mask, mapOf(site.replacementOffset to site.replacement))) {
                throw PatchException("Could not find the ${site.name} in $DART_AOT_LIBRARY")
            }
        }
    }
}
