/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.netanalyzer.shared

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.matchSingle
import com.android.tools.smali.dexlib2.util.MethodUtil

private const val MENU_RUNNING = "running"
private const val MENU_SCAN_INFO = "exportScanInfo"
private const val MENU_ON_START = "onStart"
private const val MENU_ON_STOP = "onStop"
private const val BOOLEAN_TYPE = "Z"

internal val lanScanObserverPatch = bytecodePatch {
    dependsOn(patchesMenuPatch)

    execute {
        val menu = LanScanMenuFingerprint.matchSingle()
        val parameters = menu.composableParameters()
        val observed = listOf(MENU_RUNNING, MENU_SCAN_INFO, MENU_ON_START, MENU_ON_STOP)
        val indices = observed.map(parameters::indexOf)
        if (indices.any { it < 0 }) {
            throw PatchException("$LAN_SCAN_MENU takes $parameters, expected $observed")
        }
        val (runningIndex, scanInfoIndex, onStartIndex, onStopIndex) = indices
        val method = menu.method
        if (method.parameterTypes.take(indices.max() + 1).any { it.toString() == "J" || it.toString() == "D" }) {
            throw PatchException("$LAN_SCAN_MENU has a wide parameter before one of $observed")
        }
        if (method.parameterTypes[runningIndex].toString() != BOOLEAN_TYPE) {
            throw PatchException("$LAN_SCAN_MENU $MENU_RUNNING is ${method.parameterTypes[runningIndex]}, expected $BOOLEAN_TYPE")
        }
        val firstParameter = method.implementation!!.registerCount - MethodUtil.getParameterRegisterCount(method)
        val registers = method.getFreeRegisterProvider(0, observed.size)
        val runningCopy = registers.getFreeRegister4Bit()
        val scanInfoCopy = registers.getFreeRegister4Bit()
        val onStartCopy = registers.getFreeRegister4Bit()
        val onStopCopy = registers.getFreeRegister4Bit()
        method.addInstructions(
            0,
            """
                move/from16 v$runningCopy, v${firstParameter + runningIndex}
                move-object/from16 v$scanInfoCopy, v${firstParameter + scanInfoIndex}
                move-object/from16 v$onStartCopy, v${firstParameter + onStartIndex}
                move-object/from16 v$onStopCopy, v${firstParameter + onStopIndex}
                invoke-static { v$runningCopy, v$scanInfoCopy, v$onStartCopy, v$onStopCopy }, $FEATURES_CLASS->observeLanScan(ZLjava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V
            """,
        )

        val networkScan = networkScanItemFingerprint(method.parameterTypes[scanInfoIndex].toString())
            .matchSingle().originalMethod
        registerAtLaunch(
            "$FEATURES_CLASS->registerNetworkScanItem(Ljava/lang/Class;Ljava/lang/String;)V",
            networkScan.name,
            listOf(networkScan.definingClass),
        )
    }
}
