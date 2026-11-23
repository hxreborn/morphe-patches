/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.outlook.misc.settings

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resourceLiteral
import com.android.tools.smali.dexlib2.Opcode

private const val SETTINGS_UI_PACKAGE = "Lcom/microsoft/office/outlook/settingsui/compose"

internal object OutlookApplicationCreateFingerprint : Fingerprint(
    definingClass = "Lcom/acompli/acompli/OutlookApplication;",
    name = "onMAMCreate",
    returnType = "V",
    parameters = emptyList(),
)

internal object SettingsComponentsFingerprint : Fingerprint(
    definingClass = "$SETTINGS_UI_PACKAGE/componenthelpers/SettingsComponentHelper;",
    name = "getComponents\$SettingsUi_release",
    returnType = "Ljava/util/List;",
    filters = listOf(
        fieldAccess(definingClass = "$SETTINGS_UI_PACKAGE/Category;", name = "GENERAL", opcode = Opcode.SGET_OBJECT),
        fieldAccess(definingClass = "$SETTINGS_UI_PACKAGE/SettingName;", name = "SETTINGS_MAIL", opcode = Opcode.SGET_OBJECT),
        methodCall(
            definingClass = "$SETTINGS_UI_PACKAGE/SettingComponent;",
            name = "<init>",
            opcode = Opcode.INVOKE_DIRECT_RANGE,
        ),
        opcode(Opcode.RETURN_OBJECT),
    ),
)

internal object MailSettingsIconFingerprint : Fingerprint(
    definingClass = "$SETTINGS_UI_PACKAGE/componenthelpers/ComposableSingletons\$SettingsComponentHelperKt;",
    returnType = "L",
    parameters = listOf("L", "I"),
    filters = listOf(
        resourceLiteral(ResourceType.DRAWABLE, "ic_fluent_mail_24_regular"),
        methodCall(definingClass = "$SETTINGS_UI_PACKAGE/ui/SettingsListItemKt;", returnType = "V"),
        fieldAccess(opcode = Opcode.SGET_OBJECT),
    ),
)

internal object SettingsSearchExclusionFingerprint : Fingerprint(
    definingClass = "$SETTINGS_UI_PACKAGE/ui/SettingsSearchHelpersKt;",
    name = "getNotIncludedInSearchSearchTerms",
    parameters = emptyList(),
)

internal object SettingsRowClickFingerprint : Fingerprint(
    definingClass = "$SETTINGS_UI_PACKAGE/ui/SettingsListLayoutKt;",
    isStatic = true,
    parameters = listOf(
        "$SETTINGS_UI_PACKAGE/viewmodels/SettingsBaseViewModel;",
        "$SETTINGS_UI_PACKAGE/Component;",
    ),
    filters = listOf(
        methodCall(name = "getCurrentClickedComponent", parameters = emptyList()),
        opcode(Opcode.CHECK_CAST),
        methodCall(name = "getDeepLinkUri", parameters = emptyList(), returnType = "Ljava/lang/String;"),
        methodCall(name = "setValue", parameters = listOf("Ljava/lang/Object;")),
    ),
)
