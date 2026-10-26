/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from fork-maintainers/iceraven-browser (MPL-2.0):
 * https://github.com/fork-maintainers/iceraven-browser/commit/56c08e8000c4e16c88010ddf2e991016ddb50a8c
 * Commit 56c08e8000c4e16c88010ddf2e991016ddb50a8c (2026-10-20),
 * app/src/main/java/org/mozilla/fenix/settings/account/AccountUiView.kt
 */
package app.morphe.patches.firefox.layout.signin

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.Opcode

private const val ACCOUNT_UI_VIEW = "Lorg/mozilla/fenix/settings/account/AccountUiView;"
internal const val R_STRING = "Lorg/mozilla/fenix/R\$string;"
internal const val PREFERENCE = "Landroidx/preference/Preference;"
private const val SYNC_PREFERENCE_VIEW = "Lorg/mozilla/fenix/settings/SyncPreferenceView;"
private const val SHARE_SIGN_IN = "Lorg/mozilla/fenix/share/listadapters/SyncShareOption\$SignIn;"

internal object AccountUiStateFingerprint : Fingerprint(
    definingClass = ACCOUNT_UI_VIEW,
    returnType = "V",
    filters = listOf(
        methodCall(definingClass = "Lmozilla/components/service/fxa/manager/FxaAccountManager;", name = "authenticatedAccount"),
        fieldAccess(opcode = Opcode.IGET_OBJECT, definingClass = ACCOUNT_UI_VIEW, name = "preferenceSignIn"),
    ),
)

internal object AddressSyncVisibilityFingerprint : Fingerprint(
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Ljava/lang/Object;"),
    filters = listOf(
        fieldAccess(opcode = Opcode.SGET, definingClass = R_STRING, name = "pref_key_addresses_sync_cards_across_devices"),
        methodCall(definingClass = "Lorg/mozilla/fenix/utils/Settings;", name = "isAddressSyncEnabled"),
        methodCall(definingClass = PREFERENCE, name = "setVisible"),
    ),
)

internal object MenuAccountRowFingerprint : Fingerprint(
    definingClass = "Lorg/mozilla/fenix/components/menu/compose/MainMenuKt\$\$",
    filters = listOf(
        methodCall(
            definingClass = "Lorg/mozilla/fenix/components/menu/compose/header/MozillaAccountMenuItemKt;",
            name = "MozillaAccountMenuItem",
        ),
    ),
)

internal object SyncedTabsSignInPageFingerprint : Fingerprint(
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Ljava/lang/Object;", "Ljava/lang/Object;"),
    filters = listOf(
        fieldAccess(opcode = Opcode.SGET, definingClass = R_STRING, name = "tab_manager_empty_synced_tabs_page_description"),
        fieldAccess(opcode = Opcode.SGET, definingClass = R_STRING, name = "tab_manager_empty_synced_tabs_page_sign_in_cta"),
        methodCall(definingClass = "Landroidx/compose/runtime/Composer;", name = "endNode"),
    ),
)

internal object ShareSignInOptionFingerprint : Fingerprint(
    filters = listOf(
        fieldAccess(opcode = Opcode.SGET_OBJECT, definingClass = SHARE_SIGN_IN, name = "INSTANCE"),
        methodCall(definingClass = "Lkotlin/collections/CollectionsKt__CollectionsKt;", name = "listOf", parameters = listOf("Ljava/lang/Object;"), location = MatchAfterWithin(2)),
        opcode(Opcode.MOVE_RESULT_OBJECT, MatchAfterImmediately()),
    ),
)

internal object BookmarksSignInButtonFingerprint : Fingerprint(
    returnType = "V",
    filters = listOf(
        fieldAccess(opcode = Opcode.SGET, definingClass = R_STRING, name = "bookmark_empty_list_guest_cta"),
    ),
)

internal object SyncPreferenceLoggedOffFingerprint : Fingerprint(
    definingClass = SYNC_PREFERENCE_VIEW,
    name = "<init>",
    filters = listOf(
        methodCall(definingClass = "Lmozilla/components/service/fxa/manager/FxaAccountManager;", name = "accountNeedsReauth"),
        methodCall(definingClass = PREFERENCE, name = "setTitle", location = MatchAfterWithin(6)),
        methodCall(definingClass = SYNC_PREFERENCE_VIEW, name = "updateSyncPreferenceStatus", location = MatchAfterWithin(6)),
        methodCall(definingClass = PREFERENCE, name = "setTitle", location = MatchAfterWithin(8)),
    ),
)

internal object SendToDevicesChooserActionFingerprint : Fingerprint(
    returnType = "Landroid/service/chooser/ChooserAction;",
    parameters = listOf("Landroid/content/Context;", "Ljava/util/List;", "Ljava/util/List;", "Z"),
)
