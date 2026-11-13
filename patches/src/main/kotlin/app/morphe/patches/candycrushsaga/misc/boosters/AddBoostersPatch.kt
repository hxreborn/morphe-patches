/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.candycrushsaga.misc.boosters

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64StandardInstall
import app.morphe.patches.shared.misc.smali.descriptor
import app.morphe.patches.shared.misc.smali.replaceStub
import app.morphe.util.addInstructionsAtControlFlowLabel
import app.morphe.util.findInstructionIndicesReversedOrThrow
import app.morphe.util.inputStreamFromBundledResource
import com.android.tools.smali.dexlib2.Opcode
import java.security.MessageDigest
import org.w3c.dom.Element

private const val GAME_LIBRARY = "lib/arm64-v8a/libcandycrushsaga.so"
private const val GAME_LIBRARY_SHA256 = "f658c3ae188b11f10d5547590e1a0172cf95c3255f1e4f5576841acf68fb487e"
private const val HASH_BUFFER_SIZE = 65536
private const val BOOSTERS = "Lapp/hxreborn/extension/candycrushsaga/Boosters;"

private val addBoosterLibraryPatch = resourcePatch {
    execute {
        val gameLibrary = get(GAME_LIBRARY)
        if (!gameLibrary.isFile) throw PatchException("Missing Candy Crush native library")
        val digest = MessageDigest.getInstance("SHA-256")
        gameLibrary.inputStream().use { input ->
            val buffer = ByteArray(HASH_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        val hash = digest.digest().joinToString("") { "%02x".format(it) }
        if (hash != GAME_LIBRARY_SHA256) {
            throw PatchException("Unsupported Candy Crush native library: SHA-256 $hash, expected $GAME_LIBRARY_SHA256")
        }
        val resource = inputStreamFromBundledResource("candycrushsaga", "libccsboosters.so")
            ?: throw PatchException("Missing Candy Crush booster library")
        get("lib/arm64-v8a/libccsboosters.so", copy = false).writeBytes(resource.use { it.readBytes() })
        document("AndroidManifest.xml").use { manifest ->
            val application = manifest.getElementsByTagName("application").item(0) as? Element
                ?: throw PatchException("Candy Crush manifest has no application element")
            application.setAttributeNS(
                "http://schemas.android.com/apk/res/android",
                "android:extractNativeLibs",
                "true",
            )
        }
    }
}

internal val candyCrushRewardsPatch = bytecodePatch {
    extendWith("extensions/extension.mpe")
    dependsOn(addBoosterLibraryPatch)

    execute {
        val nativeHandleSetter = NativeHandleSetterFingerprint.matchAll(1..1).single().method
        val runOnGameThread = RunOnGameThreadFingerprint.matchAll(1..1).single().originalMethod
        val nativeApplicationGetter = NativeApplicationGetterFingerprint.matchAll(1..1).single().originalMethod
        val nativeHandleGetter = NativeHandleGetterFingerprint.matchAll(1..1).single().originalMethod
        val activityType = runOnGameThread.definingClass
        mutableClassDefBy(BOOSTERS).apply {
            replaceStub("runOnGameThread", 2, """
                check-cast p0, $activityType
                invoke-virtual {p0, p1}, ${descriptor(runOnGameThread)}
                return-void
            """)
            replaceStub("getNativeHandle", 3, """
                check-cast p0, $activityType
                invoke-virtual {p0}, ${descriptor(nativeApplicationGetter)}
                move-result-object v0
                if-eqz v0, :missing
                invoke-virtual {v0}, ${descriptor(nativeHandleGetter)}
                move-result-wide v0
                return-wide v0
                :missing
                const-wide/16 v0, 0x0
                return-wide v0
            """)
        }
        nativeHandleSetter.findInstructionIndicesReversedOrThrow(Opcode.RETURN_VOID).forEach { index ->
            nativeHandleSetter.addInstructionsAtControlFlowLabel(
                index,
                "invoke-static/range { p0 .. p2 }, $BOOSTERS->onNativeHandle(Landroid/app/Activity;J)V",
            )
        }
        HttpRequestFingerprint.matchAll(1..1).single().method.addInstructions(
            0,
            "invoke-static/range { p1 .. p8 }, " +
                "$BOOSTERS->onRequest(JILjava/lang/String;IZLjava/lang/Object;[B)V",
        )
        HttpDataFingerprint.matchAll(1..1).single().method.addInstructions(
            0,
            "invoke-static/range { p1 .. p4 }, $BOOSTERS->onData(J[BI)V",
        )
        val response = HttpResponseFingerprint.matchAll(1..1).single().method
        response.findInstructionIndicesReversedOrThrow(Opcode.RETURN_VOID).forEach { index ->
            response.addInstructionsAtControlFlowLabel(
                index,
                "invoke-static/range { p1 .. p3 }, $BOOSTERS->onResponse(JZ)V",
            )
        }
    }
}

@Suppress("unused")
val addBoostersPatch = bytecodePatch(
    name = "Add boosters",
    description = "Adds 1,000 of each of ten boosters every time the game starts. " +
        "Each booster unlocks at its usual level. Requires a standard install.",
) {
    compatibleWith(AppCompatibilities.CANDY_CRUSH_SAGA)
    availability(requireArm64StandardInstall)
    dependsOn(candyCrushRewardsPatch)

    execute {
        mutableClassDefBy(BOOSTERS).replaceStub("boostersEnabled", 1, """
            const/4 v0, 0x1
            return v0
        """)
    }
}
