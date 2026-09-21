/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonpass.misc.inappmessages

import app.morphe.patcher.Fingerprint

internal object StoreInAppMessagesFingerprint : Fingerprint(
    returnType = "Ljava/lang/Object;",
    parameters = listOf("L", "Ljava/util/List;", "L"),
    strings = listOf("storeMessages"),
)
