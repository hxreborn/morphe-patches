/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.reactnative

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.Opcode

internal object NetworkResponseReceivedFingerprint : Fingerprint(
    isStatic = true,
    returnType = "V",
    parameters = listOf(
        "Lcom/facebook/react/bridge/ReactApplicationContext;",
        "I",
        "I",
        "Lcom/facebook/react/bridge/WritableMap;",
        "Ljava/lang/String;",
    ),
    strings = listOf("didReceiveNetworkResponse"),
)

internal object TextResponseReceivedFingerprint : Fingerprint(
    isStatic = true,
    returnType = "V",
    parameters = listOf("Lcom/facebook/react/bridge/ReactApplicationContext;", "I", "Ljava/lang/String;"),
    strings = listOf("didReceiveNetworkData"),
)

internal object BlobResponseDataFingerprint : Fingerprint(
    returnType = "Lcom/facebook/react/bridge/WritableMap;",
    strings = listOf("blobId", "offset", "size"),
    filters = listOf(
        methodCall(parameters = emptyList(), returnType = "[B"),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = InstructionLocation.MatchAfterImmediately()),
        methodCall(
            definingClass = "Lcom/facebook/react/modules/blob/BlobModule;",
            name = "store",
            parameters = listOf("[B"),
            returnType = "Ljava/lang/String;",
        ),
    ),
)

internal object ScrollViewLayoutFingerprint : Fingerprint(
    name = "onLayout",
    parameters = listOf("Z", "I", "I", "I", "I"),
    returnType = "V",
    custom = { _, classDef ->
        classDef.superclass == "Landroid/widget/ScrollView;" &&
            classDef.type.startsWith("Lcom/facebook/react/views/scroll/")
    },
)
