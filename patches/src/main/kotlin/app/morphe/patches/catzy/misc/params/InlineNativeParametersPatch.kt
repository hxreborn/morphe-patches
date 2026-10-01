/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.catzy.misc.params

import app.morphe.patcher.extensions.InstructionExtensions.removeInstruction
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.returnEarly
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod

private const val PARAMS_CONFIG_CLASS = "Lcom/nieruo/params/ParamsConfig;"
private const val ARM64_LIBRARY_DIRECTORY = "lib/arm64-v8a"
private const val PARAMS_LIBRARY = "libparams.so"
private const val CONCEAL_LIBRARY = "libconceal.so"
private const val STRING_TYPE = "Ljava/lang/String;"
private const val LOAD_LIBRARY = "Ljava/lang/System;->loadLibrary(Ljava/lang/String;)V"

private const val FIRST_PARAMETER_OFFSET = 0x998

private val PARAMETER_ORDER = listOf(
    "httpCryptoIv",
    "serverUrl",
    "ZtAppKey",
    "httpCryptoKey",
    "ztServerUrl",
    "adjustAppToken",
    "ossEndpoint",
    "realmCryptoKey",
    "ossSk",
    "googleAuthWebClientId",
    "appLovinSdkKey",
    "ossAk",
    "TDAppId",
    "resourceCryptoKey",
    "ZtAppId",
)

private val FIXED_PARAMETER_LENGTHS = mapOf(
    "httpCryptoIv" to 16,
    "httpCryptoKey" to 32,
    "realmCryptoKey" to 64,
    "resourceCryptoKey" to 64,
)

private lateinit var nativeParameters: Map<String, String>

private fun ByteArray.readParameter(offset: Int): String {
    var end = offset
    while (end < size && this[end] != 0.toByte()) end++

    val value = String(this, offset, end - offset, Charsets.US_ASCII)
    check(end < size && value.isNotEmpty() && value.all { it.code in 0x20..0x7e }) {
        "No printable value at 0x${offset.toString(16)} in $PARAMS_LIBRARY"
    }

    return value
}

private val extractNativeParametersPatch = rawResourcePatch {
    execute {
        val library = get("$ARM64_LIBRARY_DIRECTORY/$PARAMS_LIBRARY")
        check(library.exists()) { "No $ARM64_LIBRARY_DIRECTORY/$PARAMS_LIBRARY" }

        val contents = library.readBytes()
        var offset = FIRST_PARAMETER_OFFSET

        nativeParameters = PARAMETER_ORDER.associateWith { name ->
            val value = contents.readParameter(offset)
            offset += value.length + 1

            FIXED_PARAMETER_LENGTHS[name]?.let { length ->
                check(value.length == length) {
                    "$name is ${value.length} characters, not $length, $PARAMS_LIBRARY layout changed"
                }
            }

            value
        }

        get("lib").listFiles { file -> file.isDirectory }?.forEach { architecture ->
            architecture.resolve(PARAMS_LIBRARY).delete()
            architecture.resolve(CONCEAL_LIBRARY).delete()
        }
    }
}

internal val inlineNativeParametersPatch = bytecodePatch {
    dependsOn(extractNativeParametersPatch)

    execute {
        val classDef = mutableClassDefBy(PARAMS_CONFIG_CLASS)

        val staticInitializer = classDef.directMethods.single { it.name == "<clinit>" }
        staticInitializer.removeInstruction(
            staticInitializer.indexOfFirstInstructionOrThrow(methodCall(LOAD_LIBRARY)),
        )

        val nativeGetters = classDef.directMethods.filter {
            AccessFlags.NATIVE.isSet(it.accessFlags) &&
                it.returnType == STRING_TYPE &&
                it.parameters.isEmpty()
        }
        check(nativeGetters.isNotEmpty()) { "$PARAMS_CONFIG_CLASS has no native parameter getters" }

        val unmappedGetters = nativeGetters.map { it.name } - PARAMETER_ORDER.toSet()
        check(unmappedGetters.isEmpty()) {
            "$PARAMS_CONFIG_CLASS has unmapped native getters: $unmappedGetters"
        }

        nativeGetters.forEach { getter ->
            classDef.directMethods.remove(getter)
            classDef.directMethods.add(
                ImmutableMethod(
                    PARAMS_CONFIG_CLASS,
                    getter.name,
                    emptyList(),
                    STRING_TYPE,
                    getter.accessFlags and AccessFlags.NATIVE.value.inv(),
                    null,
                    null,
                    MutableMethodImplementation(2),
                ).toMutable().apply { returnEarly(nativeParameters.getValue(getter.name)) },
            )
        }
    }
}
