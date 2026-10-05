/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.keepa.misc.offercount

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch

private const val RESTRICTED_FLAG = "isRestricted:!0"
private const val UNRESTRICTED_FLAG = "isRestricted:!1"
private const val OVERVIEW_ROW_BUILDER = "getListItemCsvTypeWithStats"

private val offerCountCsvTypes = mapOf("COUNT_NEW" to 11, "COUNT_USED" to 12)

private val overviewWarehouseRow = Regex(
    """[\w$]+\?[\w$]+:this\.getListItemCsvTypeWithStats\([\w$]+,9\),(?=ListItemUtil\.getAsin)""",
)

private val overviewRatingRow = Regex(
    """([\w$]+\?[\w$]+:ListItemUtil\.getCsvType\([\w$]+,)16(,[^()]*\),)""",
)

@Suppress("unused")
val showOfferCountsPatch = resourcePatch(
    name = "Show offer counts",
    description = "Shows the new and used offer counts in the product overview.",
) {
    compatibleWith(AppCompatibilities.KEEPA)
    dependsOn(removePairipProtectionPatch)

    execute {
        val bundle = get("assets/app/bundle.mjs")
        var bundleSource = bundle.readText()
        for ((name, csvType) in offerCountCsvTypes) {
            val declaration = Regex("""$name:\{index:$csvType,[^}]*\}""").findAll(bundleSource).singleOrNull()?.value
                ?: throw PatchException("Could not uniquely find the $name declaration.")
            if (!declaration.contains(RESTRICTED_FLAG)) {
                throw PatchException("Could not find the restricted flag in the $name declaration.")
            }
            bundleSource = bundleSource.replace(
                declaration,
                declaration.replace(RESTRICTED_FLAG, UNRESTRICTED_FLAG),
            )
        }
        bundle.writeText(bundleSource)

        val chunks = get("assets/app").listFiles { file -> file.extension == "mjs" }
            ?: throw PatchException("Could not read the application bundle directory.")

        val overviewChunks = chunks.mapNotNull { chunk ->
            val source = chunk.readText()
            if (source.contains(OVERVIEW_ROW_BUILDER)) chunk to source else null
        }
        if (overviewChunks.isEmpty()) {
            throw PatchException("Could not find the product overview.")
        }

        overviewChunks.forEach { (chunk, source) ->
            if (overviewWarehouseRow.findAll(source).count() != 1) {
                throw PatchException("Could not uniquely find the overview rows in ${chunk.name}.")
            }
            val ratingRow = overviewRatingRow.findAll(source).singleOrNull()
                ?: throw PatchException("Could not uniquely find the row template in ${chunk.name}.")

            val (rowPrefix, rowSuffix) = ratingRow.destructured
            val offerCountRows = offerCountCsvTypes.values.joinToString("") { csvType ->
                "$rowPrefix$csvType$rowSuffix"
            }
            chunk.writeText(source.replace(overviewWarehouseRow) { it.value + offerCountRows })
        }
    }
}
