/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.publicvideos.promotions

import app.morphe.patcher.extensions.InstructionExtensions.removeInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val AD_BOOKING_TOOL_ORDINAL = 4
private const val VIP_PACKAGE_TOOL_ORDINAL = 5
private const val TOOL_ITEM_PARAMETER_COUNT = 8

@Suppress("unused")
val removePromotionsPatch = bytecodePatch(
    name = "Remove promotions",
    description = "Removes advertiser banners under posts and the VIP package and advertising offers " +
        "on the profile.",
) {
    compatibleWith(AppCompatibilities.PUBLIC)

    execute {
        BottomBarCtaImageFingerprint.matchSingle().method.returnEarly()

        val toolType = ProfileHeaderToolsFingerprint.matchSingle().method.implementation!!.instructions
            .firstNotNullOf { instruction ->
                val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
                reference?.takeIf {
                    it.name == "<init>" &&
                        it.parameterTypes.size == TOOL_ITEM_PARAMETER_COUNT &&
                        it.parameterTypes[1] == "I"
                }?.parameterTypes?.first()?.toString()
            }
        val toolFields = classDefBy(toolType).staticFields.filter { it.type == toolType }

        listOf(AD_BOOKING_TOOL_ORDINAL, VIP_PACKAGE_TOOL_ORDINAL)
            .map { profileToolAddedFingerprint(toolType, toolFields[it].name).matchSingle() }
            .sortedByDescending { it.instructionMatches.last().index }
            .forEach { it.method.removeInstruction(it.instructionMatches.last().index) }
    }
}
