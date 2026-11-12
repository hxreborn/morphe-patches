/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.idlefish.misc.login

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall

private const val LOGIN_MODULE = "Lcom/taobao/idlefish/login/LoginModule;"

internal const val LOGIN_HANDLER = "Lcom/taobao/tao/remotebusiness/login/LoginHandler;"

internal object FirstLaunchLoginGuideFingerprint : Fingerprint(
    definingClass = LOGIN_MODULE,
    name = "needShowFirstLaunchLoginGuide",
)

internal object ExtraOpenLoginFingerprint : Fingerprint(
    definingClass = LOGIN_MODULE,
    name = "extraOpenLogin",
)

internal object RemoteLoginFingerprint : Fingerprint(
    definingClass = "Lcom/taobao/tao/remotebusiness/login/RemoteLogin;",
    name = "login",
    returnType = "V",
    parameters = listOf("Lmtopsdk/mtop/intf/Mtop;", "Ljava/lang/String;", "Z", "Ljava/lang/Object;"),
    filters = listOf(
        methodCall(definingClass = LOGIN_HANDLER, name = "instance"),
    ),
)
