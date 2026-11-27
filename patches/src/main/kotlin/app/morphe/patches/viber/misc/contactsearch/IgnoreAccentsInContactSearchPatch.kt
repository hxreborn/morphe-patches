/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.misc.contactsearch

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.Patch
import app.morphe.patcher.patch.mutableClassDefBy
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.addPatchInterface
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.extension.viberIgnoreAccentsInContactSearchExtensionPatch
import app.morphe.patches.viber.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.viber.misc.settings.patchesSettingsPatch
import app.morphe.patches.viber.shared.markPatchApplied

private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/hxreborn/extension/viber/ContactSearch;"

private const val SQLITE_SCALAR_FUNCTION_INTERFACE = "Lorg/sqlite/database/util/UnaryOperator;"

@Suppress("unused")
val ignoreAccentsInContactSearchPatch: Patch = patch(
    name = "Ignore accents in contact search",
    description = "Matches accented contact names when the search has no accents.",
) {
    dependsOn(viberIgnoreAccentsInContactSearchExtensionPatch)
    compatibleWith(AppCompatibilities.VIBER)
    dependsOn(spoofSignaturePatch, patchesSettingsPatch)

    execute {
        markPatchApplied("ignoreAccentsInContactSearch", ignoreAccentsInContactSearchPatch)

        with(mutableClassDefBy(EXTENSION_CLASS_DESCRIPTOR)) {
            addPatchInterface(SQLITE_SCALAR_FUNCTION_INTERFACE, "apply", methods.single { it.name == "foldAccents" })
        }

        SQLiteDatabaseOpenInnerFingerprint.method.addInstructions(
            0,
            """
                iget-object v0, p0, $SQLITE_DATABASE_CLASS->mConfigurationLocked:$SQLITE_CONFIGURATION_CLASS
                iget-object v0, v0, $SQLITE_CONFIGURATION_CLASS->customScalarFunctions:Landroid/util/ArrayMap;
                invoke-static { v0 }, $EXTENSION_CLASS_DESCRIPTOR->addFoldAccentsFunction(Ljava/util/Map;)V
            """,
        )

        ContactNameLikeClauseFingerprint.method.addInstructions(
            0,
            """
                invoke-static { p1 }, $EXTENSION_CLASS_DESCRIPTOR->searchedColumn(Ljava/lang/String;)Ljava/lang/String;
                move-result-object p1
                invoke-static { p2 }, $EXTENSION_CLASS_DESCRIPTOR->searchedText(Ljava/lang/String;)Ljava/lang/String;
                move-result-object p2
            """,
        )
    }
}
