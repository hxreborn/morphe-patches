/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.myscriptnotes.misc.pairip

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.patches.shared.misc.pairip.removePairipVirtualizationPatch
import app.morphe.patches.shared.misc.pairip.virtualizedMethods
import app.morphe.patches.shared.misc.smali.assembleDex
import app.morphe.patches.shared.misc.smali.descriptor
import app.morphe.util.inputStreamFromBundledResource
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import java.nio.ByteBuffer

private val STARTUP_METHODS = listOf(
    ApplicationCreateFingerprint,
    LaunchCreateFingerprint,
    ContentInitializationFingerprint,
    MainCreateFingerprint,
    BaseCreateFingerprint,
    BaseStartFingerprint,
)

private val STARTUP_SOURCES = listOf(
    "NeboApplication.smali",
    "LaunchActivity.smali",
    "InitContentActivity.smali",
    "MainActivity.smali",
    "NeboBaseActivity.smali",
)

private fun BytecodePatchContext.callbackClass(consumer: Fingerprint, parameters: List<String>): String = Fingerprint(
    name = "invoke",
    parameters = parameters,
    returnType = "Ljava/lang/Object;",
    filters = listOf(methodCall(consumer.matchSingle().method)),
).matchSingle().method.definingClass

internal val restoreStartupPatch = bytecodePatch {
    dependsOn(removePairipVirtualizationPatch)

    execute {
        val targets = STARTUP_METHODS.map { it.matchSingle().method }.associateBy(::descriptor)
        val (collectionResult, _) = CollectionResultCallbackFingerprint.matchAll(2..2)
        val replacements = mapOf(
            "Lstub/CollectionResultCallback;" to collectionResult.method.definingClass,
            "Lstub/FolderResultCallback;" to FolderResultCallbackFingerprint.matchSingle().method.definingClass,
            "Lstub/FolderChangedCallback;" to FolderChangedCallbackFingerprint.matchSingle().method.definingClass,
            "Lstub/LibraryChangedCallback;" to LibraryChangedCallbackFingerprint.matchSingle().method.definingClass,
            "Lstub/PdfImportCallback;" to PdfImportCallbackFingerprint.matchSingle().method.definingClass,
            "Lstub/WhatsNewCallback;" to WhatsNewCallbackFingerprint.matchSingle().method.definingClass,
            "Lstub/SyncCanceledCallback;" to callbackClass(SyncCanceledConsumerFingerprint, listOf("Ljava/lang/Object;")),
            "Lstub/CloudExpiredCallback;" to callbackClass(CloudExpiredConsumerFingerprint, listOf("Ljava/lang/Object;")),
            "Lstub/LanguageCanceledCallback;" to callbackClass(LanguageCanceledConsumerFingerprint, listOf("Ljava/lang/Object;")),
            "Lstub/MainContentCallback;" to callbackClass(
                MainContentConsumerFingerprint,
                listOf("Ljava/lang/Object;", "Ljava/lang/Object;"),
            ),
        )
        if (classDefByOrNull("Lcom/pairip/VMRunner;") == null) return@execute

        val sources = STARTUP_SOURCES.map { name ->
            val template = inputStreamFromBundledResource("myscriptnotes/restored", name)!!.use {
                it.readBytes().decodeToString()
            }
            replacements.entries.fold(template) { source, (stub, target) -> source.replace(stub, target) }
                .also { check("Lstub/" !in it) { "Unresolved callback in $name" } }
        }
        val restored = DexBackedDexFile(Opcodes.getDefault(), ByteBuffer.wrap(assembleDex(sources)))
        restored.classes.forEach { classDef ->
            val targetClass = mutableClassDefBy(classDef.type)
            classDef.methods.forEach { method ->
                val target = targets.getValue(descriptor(method))
                check(targetClass.virtualizedMethods().any { descriptor(it) == descriptor(target) }) {
                    "${descriptor(target)} is not a PairIP stub"
                }
                check(target.accessFlags == method.accessFlags) { "Access flags differ for ${descriptor(target)}" }
                check(targetClass.methods.remove(target)) { "${descriptor(target)} was not found" }
                targetClass.methods.add(
                    ImmutableMethod(
                        target.definingClass,
                        target.name,
                        target.parameters,
                        target.returnType,
                        target.accessFlags,
                        target.annotations,
                        target.hiddenApiRestrictions,
                        method.implementation,
                    ).toMutable(),
                )
            }
        }
    }
}
