/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.netanalyzer.shared

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.util.findMutableMethodOf
import app.morphe.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.util.MethodUtil

private const val PROBE_PROPERTIES =
    "address,hostname,location,delays,delay,pingable,seqNumber,isDestination,ttl,inFlight,time"

internal fun probeFieldNames(probeFields: Map<String, String>, probeType: String) =
    PROBE_PROPERTIES.split(',').joinToString(",") { property ->
        "$property=${probeFields[property] ?: throw PatchException("$probeType has no $property property")}"
    }

private const val STRING_TYPE = "Ljava/lang/String;"
private const val OBJECT_TYPE = "Ljava/lang/Object;"
private const val METHOD_SEPARATOR = ","
private const val REGISTRATION_SEPARATOR = ";"

context(context: BytecodePatchContext)
private fun recordStarts(start: MethodReference, recorder: String): String = with(context) {
    val method = mutableClassDefBy(start.definingClass).findMutableMethodOf(start)
    val stop = method.implementation!!.instructions.first { instruction ->
        instruction.opcode == Opcode.INVOKE_STATIC && instruction.getReference<MethodReference>()!!.let {
            it.definingClass == start.definingClass && it.parameterTypes.isEmpty() && it.returnType == "V"
        }
    }.getReference<MethodReference>()!!
    val parameters = start.parameterTypes.joinToString("") { type ->
        if (type.startsWith("L") && type != STRING_TYPE) OBJECT_TYPE else type.toString()
    }
    if (!recorder.contains("($parameters)")) throw PatchException("$recorder does not take the parameters of $start")
    val lastParameter = MethodUtil.getParameterRegisterCount(method) - 1
    method.addInstructions(0, "invoke-static/range { p0 .. p$lastParameter }, $recorder")
    listOf(start.name, stop.name).joinToString(METHOD_SEPARATOR)
}

internal val hostToolsPatch = bytecodePatch {
    dependsOn(patchesMenuPatch)

    execute {
        val starts = ToolStartFingerprint.matchSingle()
        val tracerouteStart = starts.instructionMatches.first().instruction.getReference<MethodReference>()!!
        val pingStart = starts.instructionMatches.last().instruction.getReference<MethodReference>()!!
        val tracerouteTool = tracerouteStart.definingClass
        val pingTool = pingStart.definingClass
        val runMethods = listOf(
            recordStarts(pingStart, "$FEATURES_CLASS->recordPingStart(Ljava/lang/String;IIZLjava/lang/Object;IZZ)V"),
            recordStarts(tracerouteStart, "$FEATURES_CLASS->recordTracerouteStart(Ljava/lang/String;IIZZ)V"),
        ).joinToString(METHOD_SEPARATOR)

        val row = PingResultRowFingerprint.matchSingle()
        val probeIndex = row.composableParameters().indexOf(PING_RESULT_ROW_PROBE)
        if (probeIndex < 0) throw PatchException("$PING_RESULT_ROW takes no $PING_RESULT_ROW_PROBE parameter")
        val probeType = row.originalMethod.parameterTypes[probeIndex].toString()
        val fieldNames = probeFieldNames(classDefBy(probeType).propertyFields(), probeType)

        registerAtLaunch(
            "$FEATURES_CLASS->registerHostTools(Ljava/lang/Class;Ljava/lang/Class;Ljava/lang/String;)V",
            "$fieldNames$REGISTRATION_SEPARATOR$runMethods",
            listOf(pingTool, tracerouteTool),
        )
    }
}
