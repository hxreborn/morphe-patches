/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.flights25.misc.premium

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.dart.DartSnapshot
import app.morphe.patches.shared.misc.dart.dartAotLibrary
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.patches.shared.misc.requireArm64

@Suppress("unused")
val calendarImportPatch = resourcePatch(
    name = "Remove calendar import limit",
    description = "Removes limit on single-flight calendar imports.",
) {
    compatibleWith(AppCompatibilities.FLIGHTS25)
    availability(requireArm64)
    dependsOn(removePairipProtectionPatch)

    execute {
        val library = dartAotLibrary()
        val bytes = library.readBytes()
        val snapshot = DartSnapshot(bytes)
        CalendarImport(bytes, snapshot.executableOffsets, snapshot.stringsByPoolOffset).removeLimit()
        library.writeBytes(bytes)
    }
}
