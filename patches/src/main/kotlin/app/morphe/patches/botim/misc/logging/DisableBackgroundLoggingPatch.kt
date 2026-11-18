/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.botim.misc.logging

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.manifest.disableApplicationComponents

private val loggingComponents = setOf(
    "com.alipay.tianyan.mobilesdk.TianyanLoggingService",
    "com.alipay.mobile.common.logging.process.LogServiceInToolsProcess",
    "com.alipay.mobile.common.logging.process.LogServiceInPushProcess",
)

@Suppress("unused")
val disableBackgroundLoggingPatch = resourcePatch(
    name = "Disable background logging",
    description = "Stops the logging service and the extra background process the app starts at launch.",
    default = false,
) {
    compatibleWith(AppCompatibilities.BOTIM)

    execute {
        document("AndroidManifest.xml").use { it.disableApplicationComponents(loggingComponents) }
    }
}
