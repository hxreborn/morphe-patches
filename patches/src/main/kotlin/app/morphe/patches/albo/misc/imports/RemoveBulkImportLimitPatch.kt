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

private val LOAD_MAX_ENTITLEMENT_INTO_X0 = "701f4191006a47f910e247f9".hexToByteArray()

private val SET_IS_FROM_BULK_IMPORT = "c082009120b001b8".hexToByteArray()

private class ImportLimitSite(
    val name: String,
    val pattern: ByteArray,
    val mask: ByteArray,
    val replacementOffset: Int,
    val replacement: ByteArray,
)

private val IMPORT_LIMIT_SITES = listOf(
    ImportLimitSite(
        name = "bulk import credits entitlement",
        pattern = ("a0831ef828000094701f419110e247f9e04101a9" +
            "70ff4091109643f97e1f4191dee747f9").hexToByteArray(),
        mask = ("ffffffff000000fcffffffffffffffffffffffff" +
            "ffffffffffffffffffffffffffffffff").hexToByteArray(),
        replacementOffset = 4,
        replacement = LOAD_MAX_ENTITLEMENT_INTO_X0,
    ),
    ImportLimitSite(
        name = "bulk import credit deduction",
        pattern = "227c419341000036227040f85f0000f16c000054e00316aade0e9c14".hexToByteArray(),
        mask = "ffffffff1f00f8ffff0fe0ffffffffff1f0000ffffffffff000000fc".hexToByteArray(),
        replacementOffset = 16,
        replacement = NOP,
    ),
    ImportLimitSite(
        name = "shared link import request",
        pattern = ("1f4fff97a10b40f9e20316aa044fff97a10b40f9e20316aa07000094" +
            "a00b40f9ef031daafd79c1a8c0035fd6").hexToByteArray(),
        mask = ("000000fcffffffffffffffff000000fcffffffffffffffff000000fc" +
            "ffffffffffffffffffffffffffffffff").hexToByteArray(),
        replacementOffset = 8,
        replacement = SET_IS_FROM_BULK_IMPORT,
    ),
    ImportLimitSite(
        name = "link import request",
        pattern = ("77000094a10b40f9e20316aa5c000094a0035ff8027041b842801c8b" +
            "a10b40f946000094a0035ff802b041b8").hexToByteArray(),
        mask = ("000000fcffffffffffffffff000000fcffffffffffffffffffffffff" +
            "ffffffff000000fcffffffffffffffff").hexToByteArray(),
        replacementOffset = 8,
        replacement = SET_IS_FROM_BULK_IMPORT,
    ),
)

@Suppress("unused")
val removeBulkImportLimitPatch = resourcePatch(
    name = "Remove bulk import limit",
    description = "Removes the credit limit on bulk imports and the weekly limit on link imports.",
) {
    compatibleWith(AppCompatibilities.ALBO)
    availability(requireArm64)
    dependsOn(removePairipProtectionPatch)

    execute {
        val library = dartAotLibrary()

        IMPORT_LIMIT_SITES.forEach { site ->
            if (!library.replaceMasked(site.pattern, site.mask, mapOf(site.replacementOffset to site.replacement))) {
                throw PatchException("Could not find the ${site.name} in $DART_AOT_LIBRARY")
            }
        }
    }
}
