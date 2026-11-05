/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.patreon.layout.search

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getReference
import app.morphe.patches.patreon.misc.settings.markFeaturePatched
import app.morphe.patches.patreon.misc.settings.patchesSettingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.smali.replaceMethod
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/hxreborn/extension/patreon/SearchSuggestions;"
private const val ITEMS_GETTER_REGISTERS = 3

@Suppress("unused")
val hideSearchSuggestionsPatch = bytecodePatch(
    name = "Hide search suggestions",
    description = "Removes suggested creators and topics from Explore.",
) {
    compatibleWith(AppCompatibilities.PATREON)
    dependsOn(patchesSettingsPatch)
    extendWith("extensions/extension.mpe")

    execute {
        markFeaturePatched(EXTENSION_CLASS_DESCRIPTOR)
        val itemsGetter = ExploreSectionItemsFingerprint.matchAll(1..1).single()
        val sectionTypeGetter = ExploreSectionTypeFingerprint.matchAll(1..1).single()
        val displayTypeGetter = ExploreSectionDisplayTypeFingerprint.matchAll(1..1).single()
        val (itemsRead) = itemsGetter.instructionMatches
        val (sectionTypeRead) = sectionTypeGetter.instructionMatches
        val (displayTypeRead) = displayTypeGetter.instructionMatches
        val itemsField = itemsRead.instruction.getReference<FieldReference>()!!
        val sectionTypeField = sectionTypeRead.instruction.getReference<FieldReference>()!!
        val displayTypeField = displayTypeRead.instruction.getReference<FieldReference>()!!
        mutableClassDefBy(itemsGetter.method.definingClass).replaceMethod(
            itemsGetter.method.name,
            ITEMS_GETTER_REGISTERS,
            """
                iget-object v0, p0, $sectionTypeField
                iget-object v1, p0, $displayTypeField
                iget-object p0, p0, $itemsField
                invoke-static { p0, v0, v1 }, $EXTENSION_CLASS_DESCRIPTOR->filterItems(Ljava/util/List;Ljava/lang/Enum;Ljava/lang/Enum;)Ljava/util/List;
                move-result-object p0
                return-object p0
            """,
        )
    }
}
