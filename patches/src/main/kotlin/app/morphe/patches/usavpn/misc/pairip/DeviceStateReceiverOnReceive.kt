/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.usavpn.misc.pairip

private const val CONNECT_STATE = "Lde/blinkt/openvpn/core/DeviceStateReceiver\$connectState;"

internal fun deviceStateOnReceive() = """
    invoke-virtual {p2}, Landroid/content/Intent;->getAction()Ljava/lang/String;
    move-result-object v0
    const-string v1, "android.net.conn.CONNECTIVITY_CHANGE"
    invoke-virtual {v1, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
    move-result v1
    if-eqz v1, :screen_off
    invoke-virtual {p0, p1}, $DEVICE_STATE_RECEIVER->b(Landroid/content/Context;)V
    return-void
    :screen_off
    const-string v1, "android.intent.action.SCREEN_OFF"
    invoke-virtual {v1, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
    move-result v1
    if-eqz v1, :screen_on
    invoke-static {p1}, Lde/blinkt/openvpn/core/Preferences;->a(Landroid/content/Context;)$SHARED_PREFERENCES
    move-result-object v0
    const-string v1, "screenoff"
    const/4 v2, 0x0
    invoke-interface {v0, v1, v2}, $SHARED_PREFERENCES->getBoolean(Ljava/lang/String;Z)Z
    move-result v0
    if-eqz v0, :done
    sget-object v0, Lde/blinkt/openvpn/core/ProfileManager;->c:Lde/blinkt/openvpn/VpnProfile;
    if-eqz v0, :pending
    iget-boolean v0, v0, Lde/blinkt/openvpn/VpnProfile;->G:Z
    if-nez v0, :pending
    const v0, ${stringId("screen_nopersistenttun")}
    invoke-static {v0}, Lde/blinkt/openvpn/core/VpnStatus;->h(I)V
    :pending
    sget-object v0, $CONNECT_STATE->b:$CONNECT_STATE
    iput-object v0, p0, $DEVICE_STATE_RECEIVER->d:$CONNECT_STATE
    new-instance v0, Lde/blinkt/openvpn/core/DeviceStateReceiver${'$'}Datapoint;
    invoke-static {}, Ljava/lang/System;->currentTimeMillis()J
    move-result-wide v1
    const-wide/32 v3, 0x10000
    invoke-direct {v0, v1, v2, v3, v4}, Lde/blinkt/openvpn/core/DeviceStateReceiver${'$'}Datapoint;-><init>(JJ)V
    iget-object v1, p0, $DEVICE_STATE_RECEIVER->i:Ljava/util/LinkedList;
    invoke-virtual {v1, v0}, Ljava/util/LinkedList;->add(Ljava/lang/Object;)Z
    sget-object v0, $CONNECT_STATE->c:$CONNECT_STATE
    iget-object v1, p0, $DEVICE_STATE_RECEIVER->c:$CONNECT_STATE
    if-eq v1, v0, :disconnected
    iget-object v1, p0, $DEVICE_STATE_RECEIVER->e:$CONNECT_STATE
    if-ne v1, v0, :done
    :disconnected
    iput-object v0, p0, $DEVICE_STATE_RECEIVER->d:$CONNECT_STATE
    return-void
    :screen_on
    const-string v1, "android.intent.action.SCREEN_ON"
    invoke-virtual {v1, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
    move-result v0
    if-eqz v0, :done
    invoke-virtual {p0}, $DEVICE_STATE_RECEIVER->c()Z
    move-result v0
    sget-object v1, $CONNECT_STATE->a:$CONNECT_STATE
    iput-object v1, p0, $DEVICE_STATE_RECEIVER->d:$CONNECT_STATE
    iget-object v1, p0, $DEVICE_STATE_RECEIVER->a:Landroid/os/Handler;
    iget-object v2, p0, $DEVICE_STATE_RECEIVER->g:Ljava/lang/Runnable;
    invoke-virtual {v1, v2}, Landroid/os/Handler;->removeCallbacks(Ljava/lang/Runnable;)V
    invoke-virtual {p0}, $DEVICE_STATE_RECEIVER->c()Z
    move-result v1
    iget-object v2, p0, $DEVICE_STATE_RECEIVER->b:Lde/blinkt/openvpn/core/OpenVPNManagement;
    if-eq v1, v0, :still_paused
    invoke-interface {v2}, Lde/blinkt/openvpn/core/OpenVPNManagement;->resume()V
    return-void
    :still_paused
    if-nez v1, :done
    invoke-virtual {p0}, $DEVICE_STATE_RECEIVER->a()Lde/blinkt/openvpn/core/OpenVPNManagement${'$'}pauseReason;
    move-result-object v0
    invoke-interface {v2, v0}, Lde/blinkt/openvpn/core/OpenVPNManagement;->a(Lde/blinkt/openvpn/core/OpenVPNManagement${'$'}pauseReason;)V
    :done
    return-void
"""
