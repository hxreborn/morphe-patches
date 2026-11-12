/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.candycrushsaga.misc.boosters

import app.morphe.patcher.Fingerprint

internal object HttpResponseFingerprint : Fingerprint(
    definingClass = "Lcom/king/http/HttpManager;",
    name = "onResponse",
    returnType = "V",
    parameters = listOf(
        "J", "Z", "I", "I", "[Lcom/king/http/HttpHeaders\$Header;", "I", "Ljava/lang/String;", "I", "I",
    ),
)

internal object HttpRequestFingerprint : Fingerprint(
    definingClass = "Lcom/king/http/HttpManager;",
    name = "createTask",
    returnType = "Lcom/king/http/HttpTask;",
    parameters = listOf("J", "I", "Ljava/lang/String;", "I", "Z", "Lcom/king/http/HttpHeaders;", "[B"),
)

internal object HttpDataFingerprint : Fingerprint(
    definingClass = "Lcom/king/http/HttpManager;",
    name = "onData",
    returnType = "V",
    parameters = listOf("J", "[B", "I"),
)

internal object NativeHandleSetterFingerprint : Fingerprint(
    definingClass = "Lcom/king/main/EngineActivity;",
    name = "setNativeHandle",
    returnType = "V",
    parameters = listOf("J"),
)

internal object RunOnGameThreadFingerprint : Fingerprint(
    definingClass = "Lcom/king/main/EngineActivity;",
    name = "runOnGameThread",
    returnType = "V",
    parameters = listOf("Ljava/lang/Runnable;"),
)

internal object NativeApplicationGetterFingerprint : Fingerprint(
    definingClass = "Lcom/king/main/EngineActivity;",
    name = "getNativeApplication",
    returnType = "Lcom/king/core/gameactivity/NativeApplication;",
    parameters = emptyList(),
)

internal object NativeHandleGetterFingerprint : Fingerprint(
    definingClass = "Lcom/king/core/gameactivity/NativeApplication;",
    name = "getNativeHandle",
    returnType = "J",
    parameters = emptyList(),
)
