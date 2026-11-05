/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.misc.settings

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.Opcode

internal object ApplicationContextFingerprint : Fingerprint(
    definingClass = "Lcom/viber/voip/ManifestViberApplication;",
    name = "attachBaseContext",
    parameters = listOf("Landroid/content/Context;"),
    returnType = "V",
)

internal object PlusAdSwitchFingerprint : Fingerprint(
    filters = listOf(
        methodCall(
            definingClass = "Lcom/viber/voip/feature/viberplus/presentation/settings/ViberPlusSettingsState;",
            name = "isAdsFree",
        ),
        opcode(Opcode.MOVE_RESULT, MatchAfterImmediately()),
        methodCall(
            definingClass = "Landroidx/appcompat/widget/SwitchCompat;",
            name = "setChecked",
            parameters = listOf("Z"),
            location = MatchAfterImmediately(),
        ),
    ),
)

internal object SettingsItemFingerprint : Fingerprint(
    definingClass = "Lcom/viber/voip/user/more/listitems/creators/SettingsItemCreator;",
    name = "create",
    parameters = emptyList(),
    filters = listOf(
        methodCall(name = "<init>", parameters = listOf("Landroid/content/Context;", "I", "I")),
        methodCall(parameters = listOf("I"), returnType = "V"),
        methodCall(parameters = listOf("I"), returnType = "V"),
        methodCall(name = "<init>", parameters = listOf("L")),
        opcode(Opcode.RETURN_OBJECT, location = MatchAfterImmediately()),
    ),
)

internal object MoreCreatorsFingerprint : Fingerprint(
    definingClass = "Lcom/viber/voip/user/more/MoreFragment;",
    name = "getItemCreators",
    parameters = listOf("Landroid/content/Context;"),
    returnType = "Ljava/util/List;",
    filters = listOf(
        methodCall(definingClass = "Lcom/viber/voip/user/more/listitems/creators/SettingsItemCreator;", name = "<init>"),
        methodCall(definingClass = "Ljava/util/ArrayList;", name = "add", location = MatchAfterImmediately()),
        fieldAccess(opcode = Opcode.IGET_OBJECT, location = MatchAfterImmediately()),
    ),
)

internal object MoreClickFingerprint : Fingerprint(
    definingClass = "Lcom/viber/voip/user/more/MoreViewImpl;",
    name = "onPrefItemClick",
    parameters = listOf("I"),
    returnType = "V",
    filters = listOf(
        methodCall(name = "getActivity", parameters = emptyList()),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
    ),
)
