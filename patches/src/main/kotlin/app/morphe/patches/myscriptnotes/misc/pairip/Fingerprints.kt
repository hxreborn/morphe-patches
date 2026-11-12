/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.myscriptnotes.misc.pairip

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.StringComparisonType
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.string
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.misc.smali.descriptor
import com.android.tools.smali.dexlib2.Opcode

internal const val MAIN_ACTIVITY = "Lcom/myscript/nebo/main/ui/MainActivity;"

internal object ApplicationCreateFingerprint : Fingerprint(
    definingClass = "Lcom/myscript/nebo/NeboApplication;",
    name = "onCreate",
    parameters = emptyList(),
    returnType = "V",
)

internal object LaunchCreateFingerprint : Fingerprint(
    definingClass = "Lcom/myscript/nebo/launch/LaunchActivity;",
    name = "onCreate",
    parameters = listOf("Landroid/os/Bundle;"),
    returnType = "V",
)

internal object ContentInitializationFingerprint : Fingerprint(
    definingClass = "Lcom/myscript/nebo/init/InitContentActivity;",
    name = "onCreate",
    parameters = listOf("Landroid/os/Bundle;"),
    returnType = "V",
)

internal object MainCreateFingerprint : Fingerprint(
    definingClass = MAIN_ACTIVITY,
    name = "onCreate",
    parameters = listOf("Landroid/os/Bundle;"),
    returnType = "V",
)

internal object BaseCreateFingerprint : Fingerprint(
    definingClass = "Lcom/myscript/nebo/NeboBaseActivity;",
    name = "onCreate",
    parameters = listOf("Landroid/os/Bundle;"),
    returnType = "V",
)

internal object BaseStartFingerprint : Fingerprint(
    definingClass = "Lcom/myscript/nebo/NeboBaseActivity;",
    name = "onStart",
    parameters = emptyList(),
    returnType = "V",
)

internal object CollectionResultCallbackFingerprint : Fingerprint(
    name = "<init>",
    parameters = listOf("Ljava/lang/Object;"),
    returnType = "V",
    strings = listOf("onCollectionOperationResult(Ljava/lang/String;Landroid/os/Bundle;)V"),
)

internal object FolderResultCallbackFingerprint : Fingerprint(
    name = "<init>",
    parameters = listOf("Ljava/lang/Object;"),
    returnType = "V",
    strings = listOf("onFolderEditorResult(Ljava/lang/String;Landroid/os/Bundle;)V"),
)

internal object FolderChangedCallbackFingerprint : Fingerprint(
    name = "<init>",
    parameters = listOf("Ljava/lang/Object;"),
    returnType = "V",
    strings = listOf("onOpenedFolderChange(Lcom/myscript/snt/core/NotebookKey;)V"),
)

internal object LibraryChangedCallbackFingerprint : Fingerprint(
    name = "<init>",
    parameters = listOf("Ljava/lang/Object;"),
    returnType = "V",
    strings = listOf("onLibraryUpdate(Lcom/myscript/nebo/dms/core/LibraryViewModel\$State;)V"),
)

internal object PdfImportCallbackFingerprint : Fingerprint(
    name = "<init>",
    parameters = listOf("Ljava/lang/Object;"),
    returnType = "V",
    strings = listOf("onPdfImportStateChanged(Lcom/myscript/nebo/note/NotesViewModel\$ImportStateUI;)V"),
)

internal object WhatsNewCallbackFingerprint : Fingerprint(
    name = "<init>",
    parameters = listOf(
        "Lcom/myscript/nebo/whatsnew/ApplicationState;",
        "Lcom/myscript/nebo/whatsnew/FancyWhatsNewData;",
        MAIN_ACTIVITY,
    ),
    returnType = "V",
)

internal object SyncCanceledConsumerFingerprint : Fingerprint(
    definingClass = MAIN_ACTIVITY,
    parameters = listOf(MAIN_ACTIVITY, "Ljava/lang/Boolean;"),
    returnType = "Lkotlin/Unit;",
    filters = listOf(methodCall(definingClass = MAIN_ACTIVITY, name = "onSyncCanceledOverCellular")),
)

internal object CloudExpiredConsumerFingerprint : Fingerprint(
    definingClass = MAIN_ACTIVITY,
    parameters = listOf(MAIN_ACTIVITY, "Ljava/lang/Boolean;"),
    returnType = "Lkotlin/Unit;",
    filters = listOf(methodCall(definingClass = MAIN_ACTIVITY, name = "onCloudTokenExpired")),
)

internal object LanguageCanceledConsumerFingerprint : Fingerprint(
    definingClass = MAIN_ACTIVITY,
    parameters = listOf(MAIN_ACTIVITY, "Ljava/lang/Boolean;"),
    returnType = "Lkotlin/Unit;",
    filters = listOf(methodCall(definingClass = MAIN_ACTIVITY, name = "onLanguageDownloadCanceledOverCellular")),
)

internal object MainContentConsumerFingerprint : Fingerprint(
    definingClass = MAIN_ACTIVITY,
    parameters = listOf(MAIN_ACTIVITY, "Landroidx/compose/runtime/Composer;", "I"),
    returnType = "Lkotlin/Unit;",
    filters = listOf(methodCall(definingClass = "Lcom/myscript/nebo/main/ui/compose/MainScreenKt;", name = "MainScreen")),
)

internal const val EDITOR_ACTIVITY = "Lcom/myscript/nebo/editing/EditingActivity;"
internal const val INSPECTOR_ACTIVITY = "Lcom/myscript/nebo/inspector/NeboInspectorActivity;"

internal object EditorCreateFingerprint : Fingerprint(
    definingClass = "Lcom/myscript/nebo/editing/EditingActivity;",
    name = "onCreate",
    parameters = listOf("Landroid/os/Bundle;"),
    returnType = "V",
)

internal object EditorResumeFingerprint : Fingerprint(
    definingClass = "Lcom/myscript/nebo/editing/EditingActivity;",
    name = "onResume",
    parameters = emptyList(),
    returnType = "V",
)

internal object EditorStopFingerprint : Fingerprint(
    definingClass = "Lcom/myscript/nebo/editing/EditingActivity;",
    name = "onStop",
    parameters = emptyList(),
    returnType = "V",
)

internal object CreditsCreateFingerprint : Fingerprint(
    definingClass = "Lcom/myscript/nebo/about/CreditsActivity;",
    name = "onCreate",
    parameters = listOf("Landroid/os/Bundle;"),
    returnType = "V",
)

internal object ClipboardQueryFingerprint : Fingerprint(
    definingClass = "Lcom/myscript/nebo/clipboard/ClipboardContentProvider;",
    name = "query",
    parameters = listOf("Landroid/net/Uri;", "[Ljava/lang/String;", "Ljava/lang/String;", "[Ljava/lang/String;", "Ljava/lang/String;"),
    returnType = "Landroid/database/Cursor;",
)

internal object StylusReceiveFingerprint : Fingerprint(
    definingClass = "Lcom/myscript/nebo/editing/StylusDoubleTapReceiverImpl;",
    name = "onReceive",
    parameters = listOf("Landroid/content/Context;", "Landroid/content/Intent;"),
    returnType = "V",
)

internal object InspectorCreateFingerprint : Fingerprint(
    definingClass = "Lcom/myscript/nebo/inspector/NeboInspectorActivity;",
    name = "onCreate",
    parameters = listOf("Landroid/os/Bundle;"),
    returnType = "V",
)

internal object InspectorStartFingerprint : Fingerprint(
    definingClass = "Lcom/myscript/nebo/inspector/NeboInspectorActivity;",
    name = "onStart",
    parameters = emptyList(),
    returnType = "V",
)

internal object WebViewCreateFingerprint : Fingerprint(
    definingClass = "Lcom/myscript/nebo/sso/ui/WebViewActivity;",
    name = "onCreate",
    parameters = listOf("Landroid/os/Bundle;"),
    returnType = "V",
)

internal object LoginCreateFingerprint : Fingerprint(
    definingClass = "Lcom/myscript/nebo/sso/ui/login/LoginActivity;",
    name = "onCreate",
    parameters = listOf("Landroid/os/Bundle;"),
    returnType = "V",
)

internal object PaywallCreateFingerprint : Fingerprint(
    definingClass = "Lcom/revenuecat/purchases/ui/revenuecatui/activity/PaywallActivity;",
    name = "onCreate",
    parameters = listOf("Landroid/os/Bundle;"),
    returnType = "V",
)

internal object NotePropertiesCallbackFingerprint : Fingerprint(
    name = "<init>",
    parameters = listOf("Ljava/lang/Object;"),
    strings = listOf("onNotePropertiesChanged(Lcom/myscript/nebo/editing/NoteProperties;)V"),
)

internal object TooltipCallbackFingerprint : Fingerprint(
    name = "<init>",
    parameters = listOf("Ljava/lang/Object;"),
    strings = listOf("updateDiscoveryTooltip(Lcom/myscript/nebo/discoverability/ui/TooltipUIState;)V"),
)

internal object EditorToolbarConsumerFingerprint : Fingerprint(
    definingClass = EDITOR_ACTIVITY,
    parameters = listOf("Lcom/myscript/nebo/editing/EditingActivity;", "Lcom/myscript/snt/core/ToolbarConfiguration;"),
    returnType = "Lkotlin/Unit;",
    strings = listOf("Toolbar update"),
)

internal object EditorToolConsumerFingerprint : Fingerprint(
    filters = listOf(methodCall(definingClass = "Lcom/myscript/nebo/editing/NoteEditorViewModel;", name = "setDiscoverable")),
    definingClass = EDITOR_ACTIVITY,
    parameters = listOf("Lcom/myscript/nebo/editing/EditingActivity;", "Lcom/myscript/snt/core/ToolType;"),
    returnType = "Lkotlin/Unit;",
)

internal object EditorWritingConsumerFingerprint : Fingerprint(
    definingClass = EDITOR_ACTIVITY,
    parameters = listOf("Lcom/myscript/nebo/editing/EditingActivity;", "Ljava/lang/String;", "Landroid/os/Bundle;"),
    returnType = "V",
    strings = listOf("IS_WRITING_KEY"),
)

internal object EditorCloudConsumerFingerprint : Fingerprint(
    definingClass = EDITOR_ACTIVITY,
    parameters = listOf("Lcom/myscript/nebo/editing/EditingActivity;", "Ljava/lang/Boolean;"),
    returnType = "Lkotlin/Unit;",
    filters = listOf(methodCall(definingClass = EDITOR_ACTIVITY, name = "onCloudTokenExpired")),
)

internal object EditorNoteNameConsumerFingerprint : Fingerprint(
    definingClass = EDITOR_ACTIVITY,
    parameters = listOf("Lcom/myscript/nebo/editing/EditingActivity;", "Ljava/lang/String;", "Landroid/os/Bundle;"),
    returnType = "V",
    strings = listOf("NOTE_NAME_KEY"),
)

internal object InspectorStateConsumerFingerprint : Fingerprint(
    filters = listOf(methodCall(definingClass = INSPECTOR_ACTIVITY, name = "invalidateOptionsMenu")),
    definingClass = INSPECTOR_ACTIVITY,
    parameters = listOf("Lcom/myscript/nebo/inspector/NeboInspectorActivity;", "Lcom/myscript/nebo/inspector/State;"),
    returnType = "Lkotlin/Unit;",
)

internal object InspectorDetailsConsumerFingerprint : Fingerprint(
    definingClass = INSPECTOR_ACTIVITY,
    parameters = listOf("Lcom/myscript/nebo/inspector/NeboInspectorActivity;", "Landroid/view/View;"),
    returnType = "V",
    filters = listOf(methodCall(definingClass = INSPECTOR_ACTIVITY, name = "onToggleDetails")),
)

internal object InspectorCopyConsumerFingerprint : Fingerprint(
    definingClass = INSPECTOR_ACTIVITY,
    parameters = listOf("Lcom/myscript/nebo/inspector/NeboInspectorActivity;", "Landroid/view/View;"),
    returnType = "Z",
    filters = listOf(methodCall(definingClass = INSPECTOR_ACTIVITY, name = "onCopyDetails")),
)

internal object InspectorReloadConsumerFingerprint : Fingerprint(
    definingClass = INSPECTOR_ACTIVITY,
    parameters = listOf("Lcom/myscript/nebo/inspector/NeboInspectorActivity;"),
    returnType = "V",
    filters = listOf(methodCall(definingClass = INSPECTOR_ACTIVITY, name = "onReloadNeboFiles")),
)

internal object EditorContentConsumerFingerprint : Fingerprint(
    definingClass = EDITOR_ACTIVITY,
    parameters = listOf(EDITOR_ACTIVITY, "Landroidx/compose/runtime/Composer;", "I"),
    filters = listOf(methodCall(definingClass = "Lcom/myscript/nebo/editing/ui/compose/EditingScreenKt;", name = "EditingScreen")),
)

internal object EditorResumeCallbackFingerprint : Fingerprint(
    name = "<init>",
    parameters = listOf("Lcom/myscript/nebo/account/UserProfileManager;", "Lkotlin/coroutines/Continuation;"),
    returnType = "V",
)

internal object InspectorInformationCallbackFingerprint : Fingerprint(
    name = "<init>",
    parameters = listOf("Landroid/widget/TextView;", "Lcom/myscript/nebo/inspector/InspectorInformation;", "Lkotlin/coroutines/Continuation;"),
    returnType = "V",
)

internal object WebViewClientFingerprint : Fingerprint(
    custom = { _, classDef -> classDef.superclass == "Landroid/webkit/WebViewClient;" },
    name = "<init>",
    parameters = listOf("Lcom/myscript/nebo/sso/ui/WebViewActivity;"),
    returnType = "V",
)

internal object LoginResultCallbackFingerprint : Fingerprint(
    name = "<init>",
    parameters = listOf("Lcom/myscript/nebo/sso/ui/login/LoginActivity;", "Ljava/lang/String;"),
    returnType = "V",
)

internal object LoginStateCallbackFingerprint : Fingerprint(
    name = "<init>",
    parameters = listOf("Lcom/myscript/nebo/sso/ui/login/LoginActivity;", "Lkotlin/coroutines/Continuation;"),
    returnType = "V",
)

internal object PaywallListenerFingerprint : Fingerprint(
    name = "<init>",
    parameters = listOf("Lcom/revenuecat/purchases/ui/revenuecatui/PaywallListener;", "Lcom/revenuecat/purchases/ui/revenuecatui/activity/PaywallActivity;"),
    returnType = "V",
)

internal object SearchNavigationCallbackFingerprint : Fingerprint(
    name = "<init>",
    parameters = listOf("Ljava/lang/Object;"),
    strings = listOf("navigateToSearchHit(Ljava/lang/String;)V"),
)

internal object LaserObserverFingerprint : Fingerprint(
    name = "invokeSuspend",
    filters = listOf(methodCall(definingClass = "Lcom/myscript/nebo/penpanel/ToolbarViewModel;", name = "isLaserPointerActive")),
)

internal object ZoomObserverFingerprint : Fingerprint(
    name = "invokeSuspend",
    filters = listOf(methodCall(definingClass = "Lcom/myscript/nebo/editing/zoom/ZoomIndicatorViewModel;", name = "getActions")),
)

internal fun editorGetterFingerprint(returnType: String) = Fingerprint(
    definingClass = EDITOR_ACTIVITY,
    parameters = emptyList(),
    returnType = returnType,
)

internal object EditorContentTypeSetterFingerprint : Fingerprint(
    definingClass = EDITOR_ACTIVITY,
    parameters = listOf("Ljava/lang/String;"),
    returnType = "V",
    filters = listOf(fieldAccess(definingClass = EDITOR_ACTIVITY, name = "editingContentType", opcode = Opcode.IPUT_OBJECT)),
)

internal object EditorObserverFingerprint : Fingerprint(
    definingClass = "Lcom/myscript/nebo/editing/EditingActivityKt",
    name = "onChanged",
    parameters = listOf("Ljava/lang/Object;"),
    returnType = "V",
    filters = listOf(methodCall(definingClass = "Lkotlin/jvm/functions/Function1;", name = "invoke")),
)

internal object InspectorObserverFingerprint : Fingerprint(
    definingClass = "Lcom/myscript/nebo/inspector/NeboInspectorActivityKt",
    name = "<init>",
    parameters = listOf("Lkotlin/jvm/functions/Function1;"),
)

internal object LoginObserverFingerprint : Fingerprint(
    definingClass = "Lcom/myscript/nebo/sso/ui/login/LoginActivity",
    name = "<init>",
    parameters = listOf("Lkotlin/jvm/functions/Function1;"),
)

internal object ProxyBillingCreateFingerprint : Fingerprint(
    definingClass = "Lcom/android/billingclient/api/ProxyBillingActivity;",
    name = "onCreate",
    parameters = listOf("Landroid/os/Bundle;"),
    returnType = "V",
)

internal object BillingReceiverOwnerFingerprint : Fingerprint(
    strings = listOf("BillingBroadcastManager", "Receiver is not registered."),
)

internal object BillingReceiveFingerprint : Fingerprint(
    classFingerprint = BillingReceiverOwnerFingerprint,
    name = "onReceive",
    parameters = listOf("Landroid/content/Context;", "Landroid/content/Intent;"),
    returnType = "V",
)

internal fun BytecodePatchContext.callbackClass(consumer: Fingerprint, parameters: List<String>): String = Fingerprint(
    name = "invoke",
    parameters = parameters,
    returnType = "Ljava/lang/Object;",
    filters = listOf(methodCall(consumer.matchSingle().method)),
).matchSingle().method.definingClass

internal fun BytecodePatchContext.listenerClass(consumer: Fingerprint, name: String, returnType: String): String = Fingerprint(
    name = name,
    returnType = returnType,
    filters = listOf(methodCall(consumer.matchSingle().method)),
).matchSingle().method.definingClass

internal fun BytecodePatchContext.editorContentClass(): String {
    val consumer = EditorContentConsumerFingerprint.matchSingle().method
    val bridge = Fingerprint(
        definingClass = EDITOR_ACTIVITY,
        parameters = consumer.parameterTypes.map { it.toString() },
        returnType = consumer.returnType,
        filters = listOf(methodCall(consumer)),
    )
    return callbackClass(bridge, listOf("Ljava/lang/Object;", "Ljava/lang/Object;"))
}

internal fun BytecodePatchContext.laserObserverClass(): String {
    val observer = LaserObserverFingerprint.matchSingle().method.definingClass
    val constructor = Fingerprint(
        definingClass = observer,
        name = "<init>",
        parameters = listOf(EDITOR_ACTIVITY, "Lkotlin/coroutines/Continuation;"),
    ).matchSingle().method
    return Fingerprint(
        name = "invokeSuspend",
        filters = listOf(methodCall(constructor)),
    ).matchSingle().method.definingClass
}

internal fun BytecodePatchContext.paywallContentClass(): String = Fingerprint(
    classFingerprint = Fingerprint(
        filters = listOf(
            string(
                "com.revenuecat.purchases.ui.revenuecatui.activity.PaywallActivity.onCreate.<anonymous> (PaywallActivity.kt:",
                StringComparisonType.STARTS_WITH,
            ),
        ),
    ),
    name = "<init>",
    parameters = listOf(
        "Lcom/revenuecat/purchases/ui/revenuecatui/activity/PaywallActivity;",
        "Lcom/revenuecat/purchases/ui/revenuecatui/OfferingSelection\$IdAndPresentedOfferingContext;",
        "Lcom/revenuecat/purchases/ui/revenuecatui/activity/PaywallActivityArgs;",
        PaywallListenerFingerprint.matchSingle().method.definingClass,
        "Lcom/revenuecat/purchases/ui/revenuecatui/PaywallPurchaseLogic;",
        "Lkotlin/jvm/functions/Function1;",
    ),
    returnType = "V",
).matchSingle().method.definingClass
