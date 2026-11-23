/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.devcheck.misc.pairip

import app.morphe.patcher.patch.patch
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resource.resourceId
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.shared.misc.pairip.removePairipVirtualizationPatch
import app.morphe.patches.shared.misc.pairip.replaceVirtualizedBody
import app.morphe.patches.shared.misc.smali.assembleDex
import app.morphe.util.inputStreamFromBundledResource
import java.io.ByteArrayInputStream
import java.util.function.Supplier

private const val RESTORED_CLASS = "LDevCheckRestored;"

private val RESTORED_SOURCES = listOf(
    "DevCheckRestored.smali",
    "DevCheckRestored\$BluetoothMessage.smali",
    "DevCheckRestored\$HardwareReload.smali",
)

private val stringResourceRead = Regex("""sget ([vp]\d+), Lstub/StringRes;->(\w+):I""")

private lateinit var restoredSmali: List<String>

private lateinit var virtualizedMethods: Map<MutableMethod, String>

private val resolveRestoredMethodsPatch = patch {
    execute {
        val (stubs, methods) = resolveRestoredMethodBindings()
        val replacements = stubs.entries.sortedByDescending { it.key.length }
        restoredSmali = RESTORED_SOURCES.map { name ->
            val template = inputStreamFromBundledResource("devcheck/restored", name)!!.use {
                it.readBytes().decodeToString()
            }
            val smali = replacements
                .fold(template) { smali, (stub, real) -> smali.replace(stub, real) }
                .replace(stringResourceRead) { read ->
                    "const ${read.groupValues[1]}, ${resourceId(ResourceType.STRING, read.groupValues[2])}"
                }
            check("Lstub/" !in smali) { "Unresolved stub reference in $name" }
            smali
        }
        virtualizedMethods = methods
    }
}

internal val restoreVirtualizedMethodsPatch = patch {
    dependsOn(removePairipVirtualizationPatch, resolveRestoredMethodsPatch)

    extendWith(Supplier { ByteArrayInputStream(assembleDex(restoredSmali)) })

    execute {
        virtualizedMethods.forEach { (method, restoredName) ->
            val parameters = method.definingClass + method.parameterTypes.joinToString("")
            val call = "invoke-static/range {p0 .. p${method.parameterTypes.size}}, " +
                "$RESTORED_CLASS->$restoredName($parameters)${method.returnType}"
            val returnSmali = if (method.returnType == "V") "return-void" else "move-result-object v0\nreturn-object v0"
            method.replaceVirtualizedBody("$call\n$returnSmali")
        }
    }
}
