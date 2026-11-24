/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.outlook.search.bulkactions

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.checkCast
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.instanceOf
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.Opcode

internal const val HAS_SELECTION_STATE_CLASS = "Lcom/microsoft/office/outlook/mail/SelectionUiState\$HasSelection;"
internal const val ON_TAP_CLASS = "Lcom/microsoft/office/outlook/mail/ConversationListUIAction\$OnTap;"
private const val LIST_ACTION_CLASS = "Lcom/microsoft/office/outlook/mail/ConversationListUIAction;"
internal const val SEARCH_CONVERSATIONS_VIEW_MODEL_CLASS =
    "Lcom/microsoft/office/outlook/search/viewmodels/SearchConversationsBaseViewModel;"
private const val FEATURE_CLASS = "Lcom/microsoft/office/outlook/feature/FeatureManager\$Feature;"

private fun featureCheck(featureName: String) = listOf(
    fieldAccess(definingClass = FEATURE_CLASS, name = featureName, opcode = Opcode.SGET_OBJECT),
    methodCall(
        definingClass = "Lcom/microsoft/office/outlook/feature/FeatureSnapshot;",
        name = "isFeatureOn",
        location = MatchAfterImmediately(),
    ),
    opcode(Opcode.MOVE_RESULT, location = MatchAfterImmediately()),
)

internal object MailSearchResultsViewModelConstructorFingerprint : Fingerprint(
    definingClass = "Lcom/microsoft/office/outlook/search/viewmodels/MailSearchResultsViewModel;",
    name = "<init>",
    filters = featureCheck("MESSAGE_LIST_CONVERSATION_ACTION_BAR") +
        featureCheck("CENTRAL_DYNAMIC_ACTION_BAR") +
        featureCheck("SEARCH_LIST_CONVERSATION_ACTION_BAR"),
)

internal object SearchListActionFingerprint : Fingerprint(
    definingClass = "Lcom/acompli/acompli/ui/search/SearchListFragmentV2;",
    returnType = "V",
    parameters = listOf(LIST_ACTION_CLASS),
    filters = listOf(
        methodCall(returnType = "Lcom/microsoft/office/outlook/search/viewmodels/MailSearchResultsViewModel;"),
        methodCall(definingClass = SEARCH_CONVERSATIONS_VIEW_MODEL_CLASS, name = "onConversationSelected"),
        instanceOf(ON_TAP_CLASS),
        opcode(Opcode.IF_EQZ, location = MatchAfterImmediately()),
        checkCast(ON_TAP_CLASS, location = MatchAfterImmediately()),
    ),
)

internal object SelectionUiStateFingerprint : Fingerprint(
    definingClass = SEARCH_CONVERSATIONS_VIEW_MODEL_CLASS,
    name = "getSelectionUiState",
    parameters = listOf(),
)
