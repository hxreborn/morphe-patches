/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.outlook.misc.shortcut

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.string

internal object SearchDeepLinkParserFingerprint : Fingerprint(
    definingClass = "Lcom/microsoft/office/outlook/util/SearchDeeplinkParser;",
    name = "parse",
    filters = listOf(string("querytext")),
)

internal object DirectShareShortcutsFingerprint : Fingerprint(
    definingClass = "Lcom/microsoft/office/outlook/shortcut/DirectShareContactsProvider;",
    returnType = "V",
    filters = listOf(
        methodCall(
            definingClass = "Landroid/content/pm/ShortcutManager;",
            name = "setDynamicShortcuts",
            parameters = listOf("Ljava/util/List;"),
        ),
    ),
)
