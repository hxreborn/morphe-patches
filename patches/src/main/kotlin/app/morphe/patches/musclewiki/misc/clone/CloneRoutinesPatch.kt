/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.musclewiki.misc.clone

import app.morphe.patcher.patch.patch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.dart.dartAotLibrary
import app.morphe.patches.shared.misc.requireArm64

@Suppress("unused")
val cloneRoutinesPatch = patch(
    name = "Clone routines",
    description = "Add Duplicate to the routine list and routine detail menus.",
) {
    compatibleWith(AppCompatibilities.MUSCLEWIKI)
    availability(requireArm64)
    execute {
        val library = dartAotLibrary()
        val bytes = library.readBytes()
        val targets = RoutineCloneTargets(bytes)
        val resetIdentities = bytes.addRoutineCloneIdentityReset(targets)
        bytes.addRoutineCloneMenu(targets)
        bytes.addRoutineCloneSelection(targets)
        bytes.addRoutineCloneEditor(targets, resetIdentities)
        bytes.addRoutineCloneSave(targets, resetIdentities)
        library.writeBytes(bytes)
    }
}
