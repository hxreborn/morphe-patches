/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.muvizwidgets.misc.weather

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.util.adoptChild
import app.morphe.util.getNode
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.indexOfFirstStringInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/hxreborn/extension/muvizwidgets/WeatherBackend;"

private const val APP_CHECK_RESPONSE = """{\"token\":\"e30.e30.e30\",\"ttl\":\"3600s\"}"""

private const val NETWORK_SECURITY_CONFIG = "res/xml/network_security_config.xml"

private val allowLoopbackCleartextPatch = resourcePatch {
    compatibleWith(AppCompatibilities.MUVIZ_WIDGETS)

    execute {
        document(NETWORK_SECURITY_CONFIG).use { document ->
            document.getNode("domain-config").adoptChild("domain") {
                setAttribute("includeSubdomains", "false")
                textContent = "127.0.0.1"
            }
        }
    }
}

@Suppress("unused")
val fixWeatherPatch = bytecodePatch(
    name = "Fix weather",
    description = "Loads weather and finds the current city in the weather widgets. " +
        "Fetches weather from Open-Meteo and the current city from BigDataCloud.",
) {
    compatibleWith(AppCompatibilities.MUVIZ_WIDGETS)

    dependsOn(removePairipProtectionPatch, allowLoopbackCleartextPatch)

    extendWith("extensions/extension.mpe")

    execute {
        val debugProviderType = DebugAppCheckRegistrarFingerprint.matchSingle().method.run {
            val builderCall = indexOfFirstInstructionOrThrow {
                opcode == Opcode.INVOKE_STATIC &&
                    ((this as ReferenceInstruction).reference as? MethodReference)?.parameterTypes ==
                    listOf("Ljava/lang/Class;")
            }
            (getInstruction<ReferenceInstruction>(builderCall - 1).reference as TypeReference).type
        }

        AppCheckProviderInstallFingerprint.matchSingle().method.apply {
            val index = indexOfFirstInstructionOrThrow(Opcode.CONST_CLASS)
            val register = getInstruction<OneRegisterInstruction>(index).registerA
            replaceInstruction(index, "const-class v$register, $debugProviderType")
        }

        AppCheckExchangeFingerprint.matchSingle().method.returnEarly(APP_CHECK_RESPONSE)

        WeatherApiClientFingerprint.matchSingle().method.apply {
            val index = indexOfFirstStringInstructionOrThrow(WEATHER_API_BASE_URL)
            val register = getInstruction<OneRegisterInstruction>(index).registerA

            replaceInstruction(index, "invoke-static { }, $EXTENSION_CLASS_DESCRIPTOR->baseUrl()Ljava/lang/String;")
            addInstruction(index + 1, "move-result-object v$register")
            addInstruction(0, "invoke-static { p0 }, $EXTENSION_CLASS_DESCRIPTOR->attach(Landroid/content/Context;)V")
        }
    }
}
