/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.internetspeedmeter.misc.settings

import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions

internal fun removeSettingsRow(titleName: String) {
    val (_, storeRow) = settingsRowFingerprint(titleName).match().instructionMatches
    val row = storeRow.fieldReference

    val add = settingsRowAddFingerprint(row).match()
    val (loadRow, addRow) = add.instructionMatches
    add.method.removeInstructions(loadRow.index, addRow.index - loadRow.index + 1)
}
