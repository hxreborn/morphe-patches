/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.blossomblast.shared

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.document
import app.morphe.patcher.patch.mutableClassDefBy
import app.morphe.patcher.patch.patch
import app.morphe.patcher.patch.resourceFile
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import app.morphe.patches.shared.misc.extension.blossomblastGameModifiersExtensionPatch
import app.morphe.patches.shared.misc.smali.descriptor
import app.morphe.patches.shared.misc.smali.replaceStub
import app.morphe.patches.shared.sha256Hex
import app.morphe.util.addInstructionsAtControlFlowLabel
import app.morphe.util.findInstructionIndicesReversedOrThrow
import app.morphe.util.inputStreamFromBundledResource
import com.android.tools.smali.dexlib2.Opcode
import org.w3c.dom.Element

internal const val MODIFIERS = "Lapp/hxreborn/extension/blossomblast/GameModifiers;"
private const val GAME_LIBRARY = "lib/arm64-v8a/libBlossomFF.so"
private const val GAME_LIBRARY_SHA256 = "1bc3bb08220612777091e8ea88137d90829c19f4a505f5030eccc5f636c24c8f"
private const val GAME_BUILD_ID = "b4b8b2a2df4ca5fbe98f734511725652b11aad90"
private const val BRIDGE_LIBRARY = "libblossomblast.so"
private const val BRIDGE_DIRECTORY = "blossomblast"
private const val ENGINE_TYPE_NAME = "N2ff7Console7Command6EngineE"
private const val ENGINE_REFERENCE = "console_autoexec.txt"
private const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
private const val HEX_CHARACTERS_PER_BYTE = 2
private const val ARRAY_REGISTERS = 1
private const val BYTE_WIDTH = 1
private const val STRING_ARRAY_REGISTERS = 3

private val addModifiersLibraryPatch = patch {
    execute {
        val gameLibrary = resourceFile(GAME_LIBRARY)
        if (!gameLibrary.isFile) throw PatchException("Missing Blossom Blast native library")
        val hash = gameLibrary.sha256Hex()
        if (hash != GAME_LIBRARY_SHA256) {
            throw PatchException("Unsupported Blossom Blast native library: SHA-256 $hash, expected $GAME_LIBRARY_SHA256")
        }
        document("AndroidManifest.xml").use { manifest ->
            val application = manifest.getElementsByTagName("application").item(0) as? Element
                ?: throw PatchException("Blossom Blast manifest has no application element")
            application.setAttributeNS(ANDROID_NAMESPACE, "android:extractNativeLibs", "true")
        }
        val bridge = inputStreamFromBundledResource(BRIDGE_DIRECTORY, BRIDGE_LIBRARY)
            ?: throw PatchException("Missing Blossom Blast game modifier library")
        resourceFile("lib/arm64-v8a/$BRIDGE_LIBRARY", copy = false).writeBytes(bridge.use { it.readBytes() })
    }
}

internal val blossomBlastModifiersPatch = patch {
    dependsOn(blossomblastGameModifiersExtensionPatch, addModifiersLibraryPatch)

    execute {
        val gameThread = NativeApplicationGameThreadFingerprint.originalMethod
        mutableClassDefBy(MODIFIERS).apply {
            replaceArrayStub(
                "gameBuildId",
                "B",
                BYTE_WIDTH,
                GAME_BUILD_ID.chunked(HEX_CHARACTERS_PER_BYTE).map { "0x${it}t" },
            )
            replaceStringStub("engineTypeName", ENGINE_TYPE_NAME)
            replaceStringStub("engineReference", ENGINE_REFERENCE)
            replaceStub("runOnGameThread", 2, """
                check-cast p0, ${gameThread.definingClass}
                invoke-virtual {p0, p1}, ${descriptor(gameThread)}
                return-void
            """)
        }
        val nativeHandleSetter = NativeApplicationHandleSetterFingerprint.method
        nativeHandleSetter.findInstructionIndicesReversedOrThrow(Opcode.RETURN_VOID).forEach { index ->
            nativeHandleSetter.addInstructionsAtControlFlowLabel(
                index,
                "invoke-static/range { p0 .. p2 }, $MODIFIERS->onNativeHandle(Ljava/lang/Object;J)V",
            )
        }
    }
}

internal fun MutableClass.replaceStringStub(name: String, value: String) = replaceStub(name, 1, """
    const-string v0, "$value"
    return-object v0
""")

internal fun MutableClass.replaceStringArrayStub(name: String, values: List<String>) =
    replaceStub(name, STRING_ARRAY_REGISTERS, buildString {
        appendLine("const/16 v0, ${values.size}")
        appendLine("new-array v0, v0, [Ljava/lang/String;")
        values.forEachIndexed { index, value ->
            appendLine("const/16 v1, $index")
            appendLine("const-string v2, \"$value\"")
            appendLine("aput-object v2, v0, v1")
        }
        appendLine("return-object v0")
    })

internal fun MutableClass.replaceArrayStub(name: String, elementType: String, width: Int, literals: List<String>) =
    replaceStub(name, ARRAY_REGISTERS, buildString {
        appendLine("const/16 v0, ${literals.size}")
        appendLine("new-array v0, v0, [$elementType")
        appendLine("fill-array-data v0, :values")
        appendLine("return-object v0")
        appendLine(":values")
        appendLine(".array-data $width")
        literals.forEach { appendLine(it) }
        appendLine(".end array-data")
    })
