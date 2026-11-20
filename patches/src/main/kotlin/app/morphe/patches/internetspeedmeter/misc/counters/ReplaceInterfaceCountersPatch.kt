/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.internetspeedmeter.misc.counters

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter
import app.morphe.patches.shared.misc.extension.internetspeedmeterInterfaceCountersExtensionPatch

private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/hxreborn/extension/internetspeedmeter/InterfaceCounters;"
private const val COUNTERS_LIBRARY = "libtraffic_interface1.so"

private val REPLACEMENTS = mapOf(
    "getIfaceRxByte" to "getIfaceRxBytes",
    "getIfaceTxByte" to "getIfaceTxBytes",
    "getMobileRxByte" to "getMobileRxBytes",
    "getMobileTxByte" to "getMobileTxBytes",
    "getTotalRxByte" to "getWifiRxBytes",
    "getTotalTxByte" to "getWifiTxBytes",
)

private val removeCountersLibraryPatch = rawResourcePatch {
    execute {
        get("lib").listFiles { file -> file.isDirectory }?.forEach { architecture ->
            architecture.resolve(COUNTERS_LIBRARY).delete()
        }
    }
}

internal val replaceInterfaceCountersPatch = bytecodePatch {
    dependsOn(internetspeedmeterInterfaceCountersExtensionPatch)
    dependsOn(removeCountersLibraryPatch)

    execute {
        val classDef = InterfaceCountersInitializerFingerprint.classDef
        InterfaceCountersInitializerFingerprint.method.removeInstruction(
            InterfaceCountersInitializerFingerprint.instructionMatches.last().index,
        )

        val natives = classDef.directMethods.filter { AccessFlags.NATIVE.isSet(it.accessFlags) }
        val nativeNames = natives.map { it.name }
        check(nativeNames.toSet() == REPLACEMENTS.keys) {
            "Unexpected native methods in ${classDef.type}: expected ${REPLACEMENTS.keys}, found $nativeNames"
        }

        natives.forEach { native ->
            val parameterType = native.parameterTypes.single().toString()
            classDef.directMethods.remove(native)
            classDef.directMethods.add(
                ImmutableMethod(
                    classDef.type,
                    native.name,
                    listOf(ImmutableMethodParameter(parameterType, null, null)),
                    "J",
                    native.accessFlags and AccessFlags.NATIVE.value.inv(),
                    null,
                    null,
                    MutableMethodImplementation(2),
                ).toMutable().apply {
                    addInstructions(
                        0,
                        """
                            invoke-static { p0 }, $EXTENSION_CLASS_DESCRIPTOR->${REPLACEMENTS.getValue(native.name)}($parameterType)J
                            move-result-wide v0
                            return-wide v0
                        """,
                    )
                },
            )
        }
    }
}
