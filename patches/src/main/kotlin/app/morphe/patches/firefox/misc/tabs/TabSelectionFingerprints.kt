/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.firefox.misc.tabs

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import com.android.tools.smali.dexlib2.Opcode

private const val TAB_LIST_REDUCER = "Lmozilla/components/browser/state/reducer/TabListReducer;"
private const val TAB_LIST_REDUCER_KT = "Lmozilla/components/browser/state/reducer/TabListReducerKt;"
private const val BROWSER_STATE = "Lmozilla/components/browser/state/state/BrowserState;"
private const val TAB_LIST_ACTION = "Lmozilla/components/browser/state/action/TabListAction;"
private const val REMOVE_TAB_ACTION = "Lmozilla/components/browser/state/action/TabListAction\$RemoveTabAction;"

internal object TabListReduceFingerprint : Fingerprint(
    definingClass = TAB_LIST_REDUCER,
    name = "reduce",
    returnType = BROWSER_STATE,
    parameters = listOf(BROWSER_STATE, TAB_LIST_ACTION),
    filters = listOf(
        fieldAccess(definingClass = BROWSER_STATE, name = "selectedTabId", opcode = Opcode.IGET_OBJECT),
        fieldAccess(definingClass = REMOVE_TAB_ACTION, name = "selectParentIfExists", opcode = Opcode.IGET_BOOLEAN),
    ),
)

internal object FindNewSelectedTabIdFingerprint : Fingerprint(
    definingClass = TAB_LIST_REDUCER_KT,
    name = "findNewSelectedTabId",
    returnType = "Ljava/lang/String;",
    parameters = listOf("Ljava/util/ArrayList;", "Z", "I", "Ljava/util/Set;"),
)
