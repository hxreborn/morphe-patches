/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.mymoveset.misc.updates

import app.morphe.patcher.patch.resourcePatch
import app.morphe.util.findElementByAttributeValueOrThrow

private const val UPDATES_ENABLED = "expo.modules.updates.ENABLED"

internal val disableOtaUpdatesPatch = resourcePatch {
    execute {
        document("AndroidManifest.xml").use { document ->
            document.getElementsByTagName("meta-data")
                .findElementByAttributeValueOrThrow("android:name", UPDATES_ENABLED)
                .setAttribute("android:value", "false")
        }
    }
}
