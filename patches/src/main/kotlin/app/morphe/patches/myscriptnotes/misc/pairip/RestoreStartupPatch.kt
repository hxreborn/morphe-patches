/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.myscriptnotes.misc.pairip

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

private const val EDITOR_CONTENT_KEY = -0x31579c24

private val STARTUP_METHODS = listOf(
    ApplicationCreateFingerprint,
    LaunchCreateFingerprint,
    ContentInitializationFingerprint,
    MainCreateFingerprint,
    BaseCreateFingerprint,
    BaseStartFingerprint,
)

private val ADDITIONAL_METHODS = listOf(
    EditorCreateFingerprint,
    EditorResumeFingerprint,
    EditorStopFingerprint,
    CreditsCreateFingerprint,
    ClipboardQueryFingerprint,
    StylusReceiveFingerprint,
    InspectorCreateFingerprint,
    InspectorStartFingerprint,
    WebViewCreateFingerprint,
    LoginCreateFingerprint,
    PaywallCreateFingerprint,
    ProxyBillingCreateFingerprint,
    BillingReceiveFingerprint,
)

private val RESTORED_SOURCES = listOf(
    "NeboApplication.smali",
    "LaunchActivity.smali",
    "InitContentActivity.smali",
    "MainActivity.smali",
    "NeboBaseActivity.smali",
    "EditingActivity.smali",
    "CreditsActivity.smali",
    "ClipboardContentProvider.smali",
    "StylusDoubleTapReceiverImpl.smali",
    "NeboInspectorActivity.smali",
    "WebViewActivity.smali",
    "LoginActivity.smali",
    "PaywallActivity.smali",
    "ProxyBillingActivity.smali",
    "BillingReceiver.smali",
)

internal val restoreStartupPatch = bytecodePatch {
    dependsOn(removePairipVirtualizationPatch)

    execute {
        val startupTargets = STARTUP_METHODS.map { it.matchSingle().method }
        val (collectionResult, _) = CollectionResultCallbackFingerprint.matchAll(2..2)
        val startupReplacements = mapOf(
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
        val targets = (startupTargets + ADDITIONAL_METHODS.map { it.matchSingle().method }).associateBy(::descriptor)
        val replacements = startupReplacements + mapOf(
            "Lstub/EditorObserver;" to EditorObserverFingerprint.matchSingle().method.definingClass,
            "Lstub/EditorSyncListener;" to classDefBy(EDITOR_ACTIVITY).fields.single { it.name == "syncListener" }.type,
            "Lstub/EditorMethods;->setContentType(Ljava/lang/String;)V" to descriptor(EditorContentTypeSetterFingerprint.matchSingle().method),
            "Lstub/EditorMethods;->NoteEditorViewModel()Lcom/myscript/nebo/editing/NoteEditorViewModel;" to descriptor(editorGetterFingerprint("Lcom/myscript/nebo/editing/NoteEditorViewModel;").matchSingle().method),
            "Lstub/EditorMethods;->ChildPagesViewModel()Lcom/myscript/nebo/editing/ChildPagesViewModel;" to descriptor(editorGetterFingerprint("Lcom/myscript/nebo/editing/ChildPagesViewModel;").matchSingle().method),
            "Lstub/EditorMethods;->SearchPageViewModel()Lcom/myscript/nebo/editing/search/SearchPageViewModel;" to descriptor(editorGetterFingerprint("Lcom/myscript/nebo/editing/search/SearchPageViewModel;").matchSingle().method),
            "Lstub/EditorMethods;->AIContainerViewModel()Lcom/myscript/nebo/ai/presentation/compose/AIContainerViewModel;" to descriptor(editorGetterFingerprint("Lcom/myscript/nebo/ai/presentation/compose/AIContainerViewModel;").matchSingle().method),
            "Lstub/EditorMethods;->AIQuizViewModel()Lcom/myscript/nebo/ai/presentation/AIQuizViewModel;" to descriptor(editorGetterFingerprint("Lcom/myscript/nebo/ai/presentation/AIQuizViewModel;").matchSingle().method),
            "Lstub/InspectorObserver;" to InspectorObserverFingerprint.matchSingle().method.definingClass,
            "Lstub/LoginObserver;" to LoginObserverFingerprint.matchSingle().method.definingClass,
            "Lstub/BillingReceiver;" to BillingReceiveFingerprint.matchSingle().method.definingClass,
            "EDITOR_CONTENT_KEY" to EDITOR_CONTENT_KEY.toString(),
            "Lstub/SearchNavigationCallback;" to SearchNavigationCallbackFingerprint.matchSingle().method.definingClass,
            "Lstub/LaserObserver;" to laserObserverClass(),
            "Lstub/ZoomObserver;" to ZoomObserverFingerprint.matchSingle().method.definingClass,
            "Lstub/ToolbarCallback;" to callbackClass(EditorToolbarConsumerFingerprint, listOf("Ljava/lang/Object;")),
            "Lstub/ToolCallback;" to callbackClass(EditorToolConsumerFingerprint, listOf("Ljava/lang/Object;")),
            "Lstub/WritingCallback;" to listenerClass(EditorWritingConsumerFingerprint, "onFragmentResult", "V"),
            "Lstub/NotePropertiesCallback;" to NotePropertiesCallbackFingerprint.matchSingle().method.definingClass,
            "Lstub/TooltipCallback;" to TooltipCallbackFingerprint.matchSingle().method.definingClass,
            "Lstub/EditorCloudCallback;" to callbackClass(EditorCloudConsumerFingerprint, listOf("Ljava/lang/Object;")),
            "Lstub/NoteNameCallback;" to listenerClass(EditorNoteNameConsumerFingerprint, "onFragmentResult", "V"),
            "Lstub/EditorContentCallback;" to editorContentClass(),
            "Lstub/EditorResumeCallback;" to EditorResumeCallbackFingerprint.matchSingle().method.definingClass,
            "Lstub/InspectorStateCallback;" to callbackClass(InspectorStateConsumerFingerprint, listOf("Ljava/lang/Object;")),
            "Lstub/InspectorDetailsCallback;" to listenerClass(InspectorDetailsConsumerFingerprint, "onClick", "V"),
            "Lstub/InspectorCopyCallback;" to listenerClass(InspectorCopyConsumerFingerprint, "onLongClick", "Z"),
            "Lstub/InspectorReloadCallback;" to listenerClass(InspectorReloadConsumerFingerprint, "onRefresh", "V"),
            "Lstub/InspectorInformationCallback;" to InspectorInformationCallbackFingerprint.matchSingle().method.definingClass,
            "Lstub/WebViewClient;" to WebViewClientFingerprint.matchSingle().method.definingClass,
            "Lstub/LoginResultCallback;" to LoginResultCallbackFingerprint.matchSingle().method.definingClass,
            "Lstub/LoginStateCallback;" to LoginStateCallbackFingerprint.matchSingle().method.definingClass,
            "Lstub/PaywallListener;" to PaywallListenerFingerprint.matchSingle().method.definingClass,
            "Lstub/PaywallContent;" to paywallContentClass(),
        )

        val sources = RESTORED_SOURCES.map { name ->
            val template = inputStreamFromBundledResource("myscriptnotes/restored", name)!!.use {
                it.readBytes().decodeToString()
            }
            replacements.entries.fold(template) { source, (stub, target) -> source.replace(stub, target) }
                .also { check("Lstub/" !in it) { "Unresolved reference in $name" } }
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
