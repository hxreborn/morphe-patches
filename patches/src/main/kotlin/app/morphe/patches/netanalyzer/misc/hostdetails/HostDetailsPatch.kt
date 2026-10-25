/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.netanalyzer.misc.hostdetails

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.netanalyzer.shared.ClickableModifierFingerprint
import app.morphe.patches.netanalyzer.shared.FEATURES_CLASS
import app.morphe.patches.netanalyzer.shared.PING_RESULT_ROW
import app.morphe.patches.netanalyzer.shared.PING_RESULT_ROW_PROBE
import app.morphe.patches.netanalyzer.shared.addFeatureActivity
import app.morphe.patches.netanalyzer.shared.composableParameters
import app.morphe.patches.netanalyzer.shared.patchesMenuPatch
import app.morphe.patches.netanalyzer.shared.rowChevronMethod
import app.morphe.patches.netanalyzer.shared.toolbarActionsPatch
import app.morphe.patches.netanalyzer.shared.probeFieldNames
import app.morphe.patches.netanalyzer.shared.propertyFields
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.patches.shared.misc.smali.addMethod
import app.morphe.patches.shared.misc.smali.descriptor
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.util.MethodUtil

private const val ROW_ON_CLICK = "onClick"
private const val CLICKABLE_ROW_METHOD = "hxClickableProbeRow"
private const val CLICKABLE_ENABLED = 1
private const val CLICKABLE_NO_LABEL = 0
private const val CLICKABLE_NO_DEFAULTS = 0
private const val CLICKABLE_ROW_LOCALS = 5
private const val CLICKABLE_ON_CLICK_PARAMETER = 3
private const val BACKGROUND_CALL_MATCH = 1

@Suppress("unused")
val hostDetailsPatch = bytecodePatch(
    name = "Host details",
    description = "Adds a details page for each ping reply and traceroute hop with its address, hostname, " +
        "round-trip times, TTL, AS and location.",
) {
    compatibleWith(AppCompatibilities.NETWORK_ANALYZER)
    availability(requireArm64)
    dependsOn(patchesMenuPatch, toolbarActionsPatch)

    execute {
        addFeatureActivity("ProbeDetailsActivity")

        val spacer = PingResultRowSpacerFingerprint.matchSingle()
        val spacerCall = spacer.instructionMatches.last()
        val composerRegister = spacerCall.getInstruction<FiveRegisterInstruction>().registerC
        spacer.method.replaceInstruction(
            spacerCall.index,
            "invoke-static { v$composerRegister }, ${descriptor(rowChevronMethod(spacer.classDef))}",
        )

        val row = PingResultRowBackgroundFingerprint.matchSingle()
        val rowMethod = row.originalMethod
        val rowParameters = row.composableParameters()
        val probeIndex = rowParameters.indexOf(PING_RESULT_ROW_PROBE)
        val onClickIndex = rowParameters.indexOf(ROW_ON_CLICK)
        if (probeIndex < 0 || onClickIndex < 0) {
            throw PatchException("$PING_RESULT_ROW takes $rowParameters, expected $PING_RESULT_ROW_PROBE and $ROW_ON_CLICK")
        }
        val probeType = rowMethod.parameterTypes[probeIndex].toString()
        val onClickType = rowMethod.parameterTypes[onClickIndex].toString()

        val background = row.instructionMatches[BACKGROUND_CALL_MATCH].instruction.getReference<MethodReference>()!!
        val modifierType = background.returnType
        if (background.parameterTypes.first().toString() != modifierType) {
            throw PatchException("The $PING_RESULT_ROW background call $background does not return its modifier")
        }

        val clickable = ClickableModifierFingerprint.matchSingle().originalMethod
        if (clickable.returnType != modifierType || clickable.parameterTypes[CLICKABLE_ON_CLICK_PARAMETER].toString() != onClickType) {
            throw PatchException("Clickable helper $clickable does not take $modifierType and $onClickType")
        }

        val fieldNames = probeFieldNames(classDefBy(probeType).propertyFields(), probeType)

        val clickableRow = row.classDef.addMethod(
            CLICKABLE_ROW_METHOD,
            listOf(modifierType, probeType),
            modifierType,
            AccessFlags.PRIVATE.value or AccessFlags.STATIC.value,
            CLICKABLE_ROW_LOCALS + 2,
            """
                const-class v0, $onClickType
                const-string v1, "$fieldNames"
                invoke-static { v0, p1, v1 }, $FEATURES_CLASS->onProbeClick(Ljava/lang/Class;Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
                move-result-object v3
                check-cast v3, $onClickType
                move-object v0, p0
                const/4 v1, $CLICKABLE_ENABLED
                const/4 v2, $CLICKABLE_NO_LABEL
                const/4 v4, $CLICKABLE_NO_DEFAULTS
                invoke-static/range { v0 .. v4 }, ${descriptor(clickable)}
                move-result-object v0
                return-object v0
            """,
        )

        val resultIndex = row.instructionMatches.last().index
        val modifierRegister = row.method.getInstruction<OneRegisterInstruction>(resultIndex).registerA
        val probeRegister = row.method.implementation!!.registerCount - MethodUtil.getParameterRegisterCount(row.method) + probeIndex
        val registers = row.method.getFreeRegisterProvider(resultIndex + 1, 2, modifierRegister, probeRegister)
        val modifierCopy = registers.getFreeRegister4Bit()
        val probeCopy = registers.getFreeRegister4Bit()
        row.method.addInstructions(
            resultIndex + 1,
            """
                move-object/from16 v$modifierCopy, v$modifierRegister
                move-object/from16 v$probeCopy, v$probeRegister
                invoke-static { v$modifierCopy, v$probeCopy }, ${descriptor(clickableRow)}
                move-result-object v$modifierCopy
                move-object/from16 v$modifierRegister, v$modifierCopy
            """,
        )
    }
}
