/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.usavpn.misc.pairip

internal fun launchVpnOnCreate() = """
    invoke-super {p0, p1}, Landroid/app/Activity;->onCreate(Landroid/os/Bundle;)V
    invoke-virtual {p0}, Landroid/app/Activity;->getIntent()Landroid/content/Intent;
    move-result-object v0
    invoke-virtual {v0}, Landroid/content/Intent;->getAction()Ljava/lang/String;
    move-result-object v1
    const-string v2, "android.intent.action.MAIN"
    invoke-virtual {v2, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
    move-result v1
    if-eqz v1, :done
    const-string v1, "de.blinkt.openvpn.shortcutProfileUUID"
    invoke-virtual {v0, v1}, Landroid/content/Intent;->getStringExtra(Ljava/lang/String;)Ljava/lang/String;
    move-result-object v1
    const-string v2, "de.blinkt.openvpn.showNoLogWindow"
    const/4 v3, 0x0
    invoke-virtual {v0, v2, v3}, Landroid/content/Intent;->getBooleanExtra(Ljava/lang/String;Z)Z
    move-result v2
    iput-boolean v2, p0, $LAUNCH_VPN->b:Z
    const/16 v2, 0xa
    invoke-static {p0, v3, v2, v1}, Lde/blinkt/openvpn/core/ProfileManager;->b(Landroid/content/Context;IILjava/lang/String;)Lde/blinkt/openvpn/VpnProfile;
    move-result-object v0
    if-nez v0, :found
    invoke-virtual {p0}, Landroid/app/Activity;->finish()V
    return-void
    :found
    iput-object v0, p0, $LAUNCH_VPN->a:Lde/blinkt/openvpn/VpnProfile;
    invoke-virtual {v0}, Lde/blinkt/openvpn/VpnProfile;->a()I
    move-result v0
    const v1, ${stringId("no_error_found")}
    const v2, ${stringId("error")}
    if-eq v0, v1, :prepare
    new-instance v1, Landroid/app/AlertDialog${'$'}Builder;
    invoke-direct {v1, p0}, Landroid/app/AlertDialog${'$'}Builder;-><init>(Landroid/content/Context;)V
    invoke-virtual {v1, v2}, Landroid/app/AlertDialog${'$'}Builder;->setTitle(I)Landroid/app/AlertDialog${'$'}Builder;
    invoke-virtual {v1, v0}, Landroid/app/AlertDialog${'$'}Builder;->setMessage(I)Landroid/app/AlertDialog${'$'}Builder;
    new-instance v0, Lde/blinkt/openvpn/LaunchVPN${'$'}3;
    invoke-direct {v0, p0}, Lde/blinkt/openvpn/LaunchVPN${'$'}3;-><init>($LAUNCH_VPN)V
    const v2, 0x104000a
    invoke-virtual {v1, v2, v0}, Landroid/app/AlertDialog${'$'}Builder;->setPositiveButton(ILandroid/content/DialogInterface${'$'}OnClickListener;)Landroid/app/AlertDialog${'$'}Builder;
    new-instance v0, Lde/blinkt/openvpn/LaunchVPN${'$'}4;
    invoke-direct {v0, p0}, Lde/blinkt/openvpn/LaunchVPN${'$'}4;-><init>($LAUNCH_VPN)V
    invoke-virtual {v1, v0}, Landroid/app/AlertDialog${'$'}Builder;->setOnCancelListener(Landroid/content/DialogInterface${'$'}OnCancelListener;)Landroid/app/AlertDialog${'$'}Builder;
    new-instance v0, Lde/blinkt/openvpn/LaunchVPN${'$'}5;
    invoke-direct {v0, p0}, Lde/blinkt/openvpn/LaunchVPN${'$'}5;-><init>($LAUNCH_VPN)V
    invoke-virtual {v1, v0}, Landroid/app/AlertDialog${'$'}Builder;->setOnDismissListener(Landroid/content/DialogInterface${'$'}OnDismissListener;)Landroid/app/AlertDialog${'$'}Builder;
    invoke-virtual {v1}, Landroid/app/AlertDialog${'$'}Builder;->show()Landroid/app/AlertDialog;
    return-void
    :prepare
    invoke-static {p0}, Landroid/net/VpnService;->prepare(Landroid/content/Context;)Landroid/content/Intent;
    move-result-object v0
    const/16 v1, 0x46
    if-eqz v0, :granted
    const-string v3, "USER_VPN_PERMISSION"
    const-string v4, ""
    sget-object v5, Lde/blinkt/openvpn/core/ConnectionStatus;->i:Lde/blinkt/openvpn/core/ConnectionStatus;
    invoke-static {v3, v4, v2, v5}, Lde/blinkt/openvpn/core/VpnStatus;->z(Ljava/lang/String;Ljava/lang/String;ILde/blinkt/openvpn/core/ConnectionStatus;)V
    invoke-virtual {p0, v0, v1}, Landroid/app/Activity;->startActivityForResult(Landroid/content/Intent;I)V
    return-void
    :granted
    const/4 v0, -0x1
    const/4 v2, 0x0
    invoke-virtual {p0, v1, v0, v2}, $LAUNCH_VPN->onActivityResult(IILandroid/content/Intent;)V
    :done
    return-void
"""
