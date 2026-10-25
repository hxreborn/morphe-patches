/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.cubasisle3.misc.telemetry

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities

@Suppress("unused")
val disableTelemetryPatch = bytecodePatch(
    name = "Disable telemetry",
    description = "Stops crash reports, sessions and configuration requests from reaching Bugsnag.",
) {
    compatibleWith(AppCompatibilities.CUBASIS_LE_3)

    execute {
        val start = BugsnagStartFingerprint.matchSingle()
        val startIndex = start.instructionMatches.first().index

        start.method.apply {
            val registers = getFreeRegisterProvider(startIndex, 3)
            val configuration = registers.getFreeRegister4Bit()
            val scratch = registers.getFreeRegister4Bit()
            val endpoints = registers.getFreeRegister4Bit()

            removeInstruction(startIndex)
            addInstructions(
                startIndex,
                """
                    invoke-static { p0 }, Lcom/bugsnag/android/Configuration;->load(Landroid/content/Context;)Lcom/bugsnag/android/Configuration;
                    move-result-object v$configuration
                    invoke-static {}, Ljava/util/Collections;->emptySet()Ljava/util/Set;
                    move-result-object v$scratch
                    invoke-virtual { v$configuration, v$scratch }, Lcom/bugsnag/android/Configuration;->setEnabledReleaseStages(Ljava/util/Set;)V
                    new-instance v$endpoints, Lcom/bugsnag/android/EndpointConfiguration;
                    const-string v$scratch, ""
                    invoke-direct { v$endpoints, v$scratch, v$scratch }, Lcom/bugsnag/android/EndpointConfiguration;-><init>(Ljava/lang/String;Ljava/lang/String;)V
                    invoke-virtual { v$configuration, v$endpoints }, Lcom/bugsnag/android/Configuration;->setEndpoints(Lcom/bugsnag/android/EndpointConfiguration;)V
                    invoke-static { p0, v$configuration }, Lcom/bugsnag/android/Bugsnag;->start(Landroid/content/Context;Lcom/bugsnag/android/Configuration;)Lcom/bugsnag/android/Client;
                """,
            )
        }
    }
}
