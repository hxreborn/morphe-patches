/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.myscriptnotes.misc.pairip

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall

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
