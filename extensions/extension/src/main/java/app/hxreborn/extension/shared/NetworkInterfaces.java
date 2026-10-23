/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.shared;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("unused")
public final class NetworkInterfaces {

    private static final List<String> VPN_INTERFACES = Arrays.asList("tun", "tun0", "pptp");

    private NetworkInterfaces() {

    }

    public static boolean isVpnUp() {
        try {
            for (NetworkInterface networkInterface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (networkInterface.isUp() && VPN_INTERFACES.contains(networkInterface.getName())) {
                    return true;
                }
            }
        } catch (SocketException ex) {
            return false;
        }
        return false;
    }

}
