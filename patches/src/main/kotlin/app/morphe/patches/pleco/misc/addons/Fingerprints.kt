/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.pleco.misc.addons

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.AccessFlags

internal object PurchaseOwnedFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "Z",
    parameters = listOf("Lcom/pleco/chinesesystem/plecoengine/", "I"),
    filters = listOf(
        methodCall(
            definingClass = "Lcom/pleco/chinesesystem/plecoengine/plecoengineJNI;",
            name = "el",
            parameters = listOf("J", "I"),
            returnType = "Z",
        ),
    ),
)
