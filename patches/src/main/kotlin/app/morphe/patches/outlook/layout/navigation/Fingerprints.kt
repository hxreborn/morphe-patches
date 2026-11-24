/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.outlook.layout.navigation

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.newInstance
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

private const val NAVIGATION_PACKAGE = "Lcom/microsoft/office/outlook/platform/navigation"
private const val CENTRAL_UI_PACKAGE = "Lcom/microsoft/office/outlook/ui/central"
internal const val APP_GROUPS_CLASS = "$NAVIGATION_PACKAGE/AppGroups;"
internal const val MENU_VIEW_CLASS = "Lcom/microsoft/office/outlook/uikit/widget/MenuView;"
private const val UI_STRINGS_CLASS = "Lcom/microsoft/office/outlook/uistrings/R\$string;"
private const val APPS_TAB_LABEL = "meta_os_apps"
private const val NAVIGATION_SUITE_TYPE_CLASS = "Lcom/microsoft/office/outlook/platform/ui/NavigationSuiteTypeKt;"
private const val CENTRAL_SCAFFOLD_CLASS = "$CENTRAL_UI_PACKAGE/CentralScaffoldKt;"
private const val NAVIGATION_BAR_VISIBLE_FIELD = "\$navigationBarVisible\$delegate"
private const val NAVIGATION_SUITE_STATE_FIELD = "\$navigationSuiteState"
private const val VISIBILITY_KEY_TO_EFFECT_WINDOW = 16
internal const val CALENDAR_CONTRIBUTION_CLASS =
    "Lcom/acompli/acompli/ui/event/list/CalendarCoreNavigationAppContribution;"

internal object AppGroupsFingerprint : Fingerprint(
    definingClass = "$NAVIGATION_PACKAGE/NavigationAppManager;",
    name = "getAppGroups",
    returnType = APP_GROUPS_CLASS,
    parameters = emptyList(),
    filters = listOf(
        methodCall(definingClass = APP_GROUPS_CLASS, name = "<init>", parameters = listOf("Ljava/util/List;", "Ljava/util/List;")),
    ),
)

internal object SaveAppGroupsFingerprint : Fingerprint(
    definingClass = "$NAVIGATION_PACKAGE/NavigationAppManager;",
    name = "saveAppGroups",
    isStatic = false,
    returnType = "V",
    parameters = listOf(APP_GROUPS_CLASS),
)

internal object LegacyTabsAddedFingerprint : Fingerprint(
    definingClass = "$NAVIGATION_PACKAGE/NavigationAppContributionComposer;",
    isStatic = true,
    returnType = "Ljava/util/List;",
    filters = listOf(
        fieldAccess(opcode = Opcode.IGET_OBJECT, type = MENU_VIEW_CLASS),
        methodCall(definingClass = MENU_VIEW_CLASS, name = "removeGroup"),
        methodCall(definingClass = MENU_VIEW_CLASS, name = "getAvailableItemCount"),
        string(" tabs, available slots: "),
        methodCall(definingClass = MENU_VIEW_CLASS, name = "addMenuItem"),
    ),
)

internal object ComposeAppsTabFingerprint : Fingerprint(
    definingClass = CENTRAL_SCAFFOLD_CLASS,
    filters = listOf(
        fieldAccess(definingClass = UI_STRINGS_CLASS, name = APPS_TAB_LABEL, opcode = Opcode.SGET),
        methodCall(definingClass = "Ljava/util/Collection;", name = "isEmpty"),
        opcode(Opcode.MOVE_RESULT, MatchAfterImmediately()),
        opcode(Opcode.IF_NEZ, MatchAfterImmediately()),
        newInstance("$CENTRAL_UI_PACKAGE/CentralNavigationItem;"),
    ),
)

internal object LegacyNavigationBarVisibilityFingerprint : Fingerprint(
    definingClass = "Lcom/acompli/acompli/CentralActivity;",
    name = "onNavigationBarVisibilityChanged",
    isStatic = false,
    returnType = "V",
    parameters = listOf("Z", "Z"),
)

private fun isNavigationRailCall() = methodCall(
    definingClass = NAVIGATION_SUITE_TYPE_CLASS,
    parameters = listOf("Ljava/lang/String;"),
    returnType = "Z",
)

internal object ComposeNavigationSuiteEffectFingerprint : Fingerprint(
    name = "invokeSuspend",
    returnType = "Ljava/lang/Object;",
    filters = listOf(
        fieldAccess(name = NAVIGATION_BAR_VISIBLE_FIELD, opcode = Opcode.IGET_OBJECT),
        methodCall(definingClass = CENTRAL_SCAFFOLD_CLASS, returnType = "Z", location = MatchAfterImmediately()),
        opcode(Opcode.MOVE_RESULT, MatchAfterImmediately()),
        fieldAccess(name = NAVIGATION_SUITE_STATE_FIELD, opcode = Opcode.IGET_OBJECT),
    ),
)

internal object ComposeNavigationSuiteKeyFingerprint : Fingerprint(
    definingClass = CENTRAL_SCAFFOLD_CLASS,
    filters = listOf(
        methodCall(definingClass = CENTRAL_SCAFFOLD_CLASS, returnType = "Z"),
        opcode(Opcode.MOVE_RESULT, MatchAfterImmediately()),
        newInstance(
            { ComposeNavigationSuiteEffectFingerprint.classDef },
            MatchAfterWithin(VISIBILITY_KEY_TO_EFFECT_WINDOW),
        ),
        methodCall(
            definingClass = { ComposeNavigationSuiteEffectFingerprint.classDef.type },
            name = "<init>",
            location = MatchAfterImmediately(),
        ),
        isNavigationRailCall(),
    ),
)

internal object ComposeDynamicActionBarFingerprint : Fingerprint(
    definingClass = CENTRAL_SCAFFOLD_CLASS,
    filters = listOf(
        isNavigationRailCall(),
        opcode(Opcode.MOVE_RESULT, MatchAfterImmediately()),
        opcode(Opcode.IF_NEZ, MatchAfterImmediately()),
        methodCall(definingClass = CENTRAL_SCAFFOLD_CLASS, returnType = "Z", location = MatchAfterImmediately()),
        opcode(Opcode.MOVE_RESULT, MatchAfterImmediately()),
        methodCall(
            definingClass = "Lcom/microsoft/office/outlook/navigation3/CentralScaffoldDynamicActionBarKt;",
            name = "CentralScaffoldDynamicActionBar",
        ),
    ),
)
