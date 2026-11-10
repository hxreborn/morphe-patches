/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.launchbox.misc.premium

import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.dotnet.RETURN_TRUE
import app.morphe.patches.shared.misc.dotnet.editManagedAssembly
import app.morphe.patches.shared.misc.dotnet.returnStaticField

private const val LICENSE_ASSEMBLY = "Unbroken.LaunchBox.MAUI.dll"
private const val LICENSE_NAMESPACE = "Unbroken.LaunchBox.MAUI"
private const val LICENSE_TYPE = "Calibration"
private const val IS_LICENSED_GETTER = "get_Calibrated"
private const val UPDATES_EXPIRY_GETTER = "get_EDate"

@Suppress("unused")
val unlockPremiumPatch = rawResourcePatch(
    name = "Unlock premium",
    description = "Removes the 100-game limit and the license prompts.",
) {
    compatibleWith(AppCompatibilities.LAUNCHBOX)

    execute {
        editManagedAssembly(LICENSE_ASSEMBLY) {
            replaceMethodBody(LICENSE_NAMESPACE, LICENSE_TYPE, IS_LICENSED_GETTER, RETURN_TRUE)

            val dateTimeMaxValue = fieldReferenceToken("System", "DateTime", "MaxValue")
            replaceMethodBody(LICENSE_NAMESPACE, LICENSE_TYPE, UPDATES_EXPIRY_GETTER, returnStaticField(dateTimeMaxValue))
        }
    }
}
