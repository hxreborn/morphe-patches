/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.idlefish.misc.share

import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.idlefish.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.addInstructionsAtControlFlowLabel
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

private const val SHARE_LINK = "Lapp/hxreborn/extension/idlefish/ShareLink;"

private const val SHARE_SYSTEM_PARAMS = "Lcom/taobao/idlefish/share/plugin/ShareSystemController${'$'}ShareSystemParams;"

@Suppress("unused")
val shareWithoutLoginPatch = bytecodePatch(
    name = "Allow sharing without login",
    description = "Shares a public listing link through the Android share sheet when signed out.",
) {
    compatibleWith(AppCompatibilities.IDLEFISH)

    dependsOn(spoofSignaturePatch)

    execute {
        rewriteShareRoute()
    }
}

internal fun BytecodePatchContext.rewriteShareRoute() {
    val shareRoute = ShareRouteFingerprint.matchSingle()
    val (queryParameterRead, sharePageParse) = shareRoute.instructionMatches
    val shareInfoKey = (queryParameterRead.instruction as FiveRegisterInstruction).registerD

    shareRoute.method.addInstructionsAtControlFlowLabel(
        sharePageParse.index,
        """
            const-class v1, Lcom/taobao/idlefish/protocol/login/PLogin;
            invoke-static {v1}, Lcom/taobao/idlefish/xmc/XModuleCenter;->moduleForProtocol(Ljava/lang/Class;)Lcom/taobao/idlefish/protocol/Protocol;
            move-result-object v1
            check-cast v1, Lcom/taobao/idlefish/protocol/login/PLogin;
            invoke-interface {v1}, Lcom/taobao/idlefish/protocol/login/PLogin;->getLoginInfo()Lcom/taobao/idlefish/protocol/login/LoginInfo;
            move-result-object v1
            invoke-interface {v1}, Lcom/taobao/idlefish/protocol/login/LoginInfo;->isLogin()Z
            move-result v1
            if-nez v1, :original
            invoke-interface {p3}, Lcom/taobao/idlefish/protocol/nav/IRouteRequest;->getExtras()Landroid/os/Bundle;
            move-result-object v1
            if-eqz v1, :original
            invoke-virtual {v1, v$shareInfoKey}, Landroid/os/BaseBundle;->getString(Ljava/lang/String;)Ljava/lang/String;
            move-result-object v1
            invoke-static {v1}, $SHARE_LINK->publicItemUrl(Ljava/lang/String;)Ljava/lang/String;
            move-result-object v1
            if-eqz v1, :original
            new-instance v3, $SHARE_SYSTEM_PARAMS
            invoke-direct {v3}, $SHARE_SYSTEM_PARAMS-><init>()V
            iput-object v1, v3, $SHARE_SYSTEM_PARAMS->text:Ljava/lang/String;
            const-string v1, "text"
            iput-object v1, v3, $SHARE_SYSTEM_PARAMS->contentType:Ljava/lang/String;
            const/4 v1, 0x0
            invoke-static {p1, v3, v1}, Lcom/taobao/idlefish/share/plugin/ShareSystemController;->share(Landroid/content/Context;${SHARE_SYSTEM_PARAMS}Lcom/taobao/idlefish/share/IShareListener;)V
            const/4 p1, 0x1
            return p1
            :original
            nop
        """,
    )
}
