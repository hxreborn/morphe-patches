/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.ultimea.misc.distractions

import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.dart.dartAotLibrary
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.patches.shared.readInt
import app.morphe.patches.ultimea.misc.settings.nativeMethods
import app.morphe.patches.ultimea.misc.settings.markPatchApplied
import app.morphe.patches.ultimea.misc.settings.patchesSettingsPatch
import app.morphe.patches.ultimea.misc.settings.writeNativeEdits

private const val INSTRUCTION_BYTES = 4
private const val HOME_ACTION_ROW_PROPERTIES = 3
private const val MOVE_IMMEDIATE_MASK = -32
private const val MOVE_SIX = 0xd28000c0.toInt()
private const val MOVE_TWO = 0xd2800040.toInt()
private const val REGISTER_MASK = 31
private const val GENERAL_FIRST_LOAD = 20
private const val GENERAL_THIRD_LOAD = 40
private const val GENERAL_LENGTH = 92
private const val LOAD_MENU_ROW = 0xf85d03a0.toInt()
private const val LOAD_APP_SETTINGS_ROW = 0xf85f83a0.toInt()
private const val MOVE_SIX_X0 = 0xd28000c0.toInt()
private const val CONFIG_VALUE = 24
private const val COMMUNITY_OBSERVED = 60
private const val DEVICE_CHAT_OBSERVED = 84
private const val SUPPORT_CHAT_OBSERVED = 92
private const val BOOLEAN_BRANCH_MASK = 0xfff8001f.toInt()
private const val BOOLEAN_TRUE_BRANCH = 0x37200000
private val RETURN_FRAME = "ef031daafd79c1a8c0035fd6".hexToByteArray()
private const val FALSE_X1 = 0x9100c2c1.toInt()

private fun Int.bytes() = ByteArray(INSTRUCTION_BYTES) { (this ushr (it * Byte.SIZE_BITS)).toByte() }

@Suppress("unused")
val removeDistractionsPatch: BytecodePatch = bytecodePatch(
    name = "Remove distractions",
    description = "Removes community, message and support entries.",
) {
    compatibleWith(AppCompatibilities.ULTIMEA)
    availability(requireArm64)
    dependsOn(patchesSettingsPatch, resourcePatch {
        execute {
            val library = dartAotLibrary().readBytes()
            val home = nativeMethods.single("homepage_nodevice_mydevice", "#161616")
            val homeLength = (home.start until home.end step INSTRUCTION_BYTES).filter { offset ->
                val instruction = library.readInt(offset)
                val register = instruction and REGISTER_MASK
                instruction and MOVE_IMMEDIATE_MASK == MOVE_SIX &&
                    library.readInt(offset + INSTRUCTION_BYTES) == (0xb800b020.toInt() or register)
            }.singleOrNull { offset ->
                val parentAllocation = nativeMethods.firstCallAfter(offset, home.end)
                val parentConfigurationEnd = nativeMethods.firstCallAfter(parentAllocation, home.end)
                nativeMethods.poolLoads(home).count {
                    it > parentAllocation && it < parentConfigurationEnd
                } == HOME_ACTION_ROW_PROPERTIES
            } ?: throw PatchException("Expected one Home actions child list with Row configuration")
            val settings = nativeMethods.single("qaa_title", "contact_us", "app_settings") {
                "language_english" !in it.stringLoads.values
            }
            val rows = (settings.loads("app_settings").single() until settings.end step INSTRUCTION_BYTES).single {
                library.readInt(it) == 0xaa1603e1.toInt() && library.readInt(it + INSTRUCTION_BYTES) == 0xd2800142.toInt()
            }
            val communityFunction = nativeMethods.single("app_ultimea_community") {
                it.stringLoads.size == 1
            }
            val community = communityFunction.start
            val empty = (community until communityFunction.end step INSTRUCTION_BYTES).last {
                library.readInt(it) == 0xd65f03c0.toInt()
            } - 4 * INSTRUCTION_BYTES
            val emptyReturn = nativeMethods.poolLoad(empty + INSTRUCTION_BYTES, 0).flatMap { it.bytes().asList() }.toByteArray() + RETURN_FRAME
            val configuration = nativeMethods.single("community_switch") { function ->
                (function.start until function.end step INSTRUCTION_BYTES).any {
                    library.readInt(it) == 0xb8007001.toInt()
                }
            }
            val config = (configuration.start until configuration.end step INSTRUCTION_BYTES).single {
                library.readInt(it) == 0xf85f83a1.toInt() && library.readInt(it + INSTRUCTION_BYTES) == 0xb8007001.toInt()
            } - CONFIG_VALUE
            val chat = nativeMethods.single("assets/icons/device_hasmsgchat.svg", "assets/icons/device_nomsgchat.svg").start
            val support = nativeMethods.single("assets/icons/customer_red.svg", "assets/icons/customer.svg").start
            listOf(community + COMMUNITY_OBSERVED, chat + DEVICE_CHAT_OBSERVED, support + SUPPORT_CHAT_OBSERVED).forEach {
                if (library.readInt(it) and BOOLEAN_BRANCH_MASK != BOOLEAN_TRUE_BRANCH) {
                    throw PatchException("Expected observed support visibility check")
                }
            }
            val expectedRows = mapOf(
                rows + GENERAL_FIRST_LOAD to 0xf85e83a0.toInt(),
                rows + GENERAL_THIRD_LOAD to LOAD_MENU_ROW,
                rows + GENERAL_LENGTH to 0xd2800140.toInt(),
                config + CONFIG_VALUE to 0xf85f83a1.toInt(),
            )
            expectedRows.forEach { (offset, instruction) ->
                if (library.readInt(offset) != instruction) throw PatchException("Unexpected Profile row construction")
            }
            writeNativeEdits("remove_distractions", library, listOf(
                homeLength to (MOVE_TWO or (library.readInt(homeLength) and REGISTER_MASK)).bytes(),
                rows + GENERAL_FIRST_LOAD to LOAD_MENU_ROW.bytes(),
                rows + GENERAL_THIRD_LOAD to LOAD_APP_SETTINGS_ROW.bytes(),
                rows + GENERAL_LENGTH to MOVE_SIX_X0.bytes(),
                community + COMMUNITY_OBSERVED to emptyReturn,
                config + CONFIG_VALUE to FALSE_X1.bytes(),
                chat + DEVICE_CHAT_OBSERVED to emptyReturn,
                support + SUPPORT_CHAT_OBSERVED to emptyReturn,
            ) + library.copyOf().removeCommunityNavigation())
        }
    })
    execute { markPatchApplied("removeDistractions", removeDistractionsPatch) }
}
