/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Additional term under GPLv3 Section 7(b): When conveying this material
 * or modified versions, preserve this notice and credit hxreborn for this
 * material in any Appropriate Legal Notices displayed by works containing it.
 */
package app.hxreborn.extension.netanalyzer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

final class WakeOnLan {

    static final int DEFAULT_PORT = 9;

    static final int MIN_PORT = 1;

    static final int MAX_PORT = 65535;

    static final int MAC_LENGTH = 6;

    static final int MAC_REPETITIONS = 16;

    static final int PACKET_LENGTH = MAC_LENGTH * (1 + MAC_REPETITIONS);

    private static final Pattern MAC_PATTERN = Pattern.compile("^([0-9A-Fa-f]{2}[:]){5}[0-9A-Fa-f]{2}$");

    private static final String MAC_SEPARATOR = ":";

    private static final int HEX_RADIX = 16;

    private static final byte SYNC_BYTE = (byte) 0xFF;

    private static final int SEND_COUNT = 3;

    private static final long SEND_DELAY_MILLIS = 10;

    private static final String IPV4_LIMITED_BROADCAST = "255.255.255.255";

    private static final String IPV6_ALL_NODES = "ff02::1%";

    private static final String WIFI_INTERFACE_PREFIX = "wlan";

    private WakeOnLan() {
    }

    static boolean isValidMac(String mac) {
        return MAC_PATTERN.matcher(mac.trim()).matches();
    }

    static byte[] packet(String mac) {
        final String[] octets = mac.trim().split(MAC_SEPARATOR);
        if (octets.length != MAC_LENGTH) {
            throw new IllegalArgumentException("Not a MAC address: " + mac);
        }
        final byte[] address = new byte[MAC_LENGTH];
        for (int i = 0; i < MAC_LENGTH; i++) {
            address[i] = (byte) Integer.parseInt(octets[i], HEX_RADIX);
        }
        final byte[] packet = new byte[PACKET_LENGTH];
        for (int i = 0; i < MAC_LENGTH; i++) {
            packet[i] = SYNC_BYTE;
        }
        for (int repetition = 1; repetition <= MAC_REPETITIONS; repetition++) {
            System.arraycopy(address, 0, packet, repetition * MAC_LENGTH, MAC_LENGTH);
        }
        return packet;
    }

    static int effectivePort(int port) {
        return (port >= MIN_PORT && port <= MAX_PORT) ? port : DEFAULT_PORT;
    }

    static void send(String mac, boolean remote, String host, int port) throws IOException {
        final byte[] packet = packet(mac);
        final List<InetAddress> targets = new ArrayList<>();
        final int targetPort;
        if (remote && host != null && !host.trim().isEmpty()) {
            targets.add(InetAddress.getByName(host.trim()));
            targetPort = effectivePort(port);
        } else {
            targets.add(InetAddress.getByName(IPV4_LIMITED_BROADCAST));
            targetPort = DEFAULT_PORT;
        }
        if (!remote) {
            addLocalTargets(targets);
        }
        int delivered = 0;
        IOException failure = null;
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setBroadcast(true);
            for (InetAddress target : targets) {
                try {
                    sendRepeated(socket, packet, target, targetPort);
                    delivered++;
                } catch (IOException ex) {
                    failure = ex;
                }
            }
        }
        if (delivered == 0 && failure != null) {
            throw failure;
        }
    }

    private static void sendRepeated(DatagramSocket socket, byte[] packet, InetAddress target, int port)
            throws IOException {
        for (int i = 0; i < SEND_COUNT; i++) {
            if (i > 0) {
                try {
                    Thread.sleep(SEND_DELAY_MILLIS);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IOException(ex);
                }
            }
            socket.send(new DatagramPacket(packet, packet.length, target, port));
        }
    }

    private static void addLocalTargets(List<InetAddress> targets) throws SocketException {
        final List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
        NetworkInterface chosen = null;
        InetAddress broadcast = null;
        for (NetworkInterface candidate : interfaces) {
            if (!candidate.isUp() || candidate.isLoopback()) {
                continue;
            }
            final InetAddress candidateBroadcast = broadcastOf(candidate);
            if (candidateBroadcast == null) {
                continue;
            }
            if (chosen == null || candidate.getName().startsWith(WIFI_INTERFACE_PREFIX)) {
                chosen = candidate;
                broadcast = candidateBroadcast;
            }
            if (chosen.getName().startsWith(WIFI_INTERFACE_PREFIX)) {
                break;
            }
        }
        if (chosen == null) {
            return;
        }
        targets.add(broadcast);
        try {
            targets.add(InetAddress.getByName(IPV6_ALL_NODES + chosen.getName()));
        } catch (IOException ex) {
            return;
        }
    }

    private static InetAddress broadcastOf(NetworkInterface candidate) {
        for (InterfaceAddress address : candidate.getInterfaceAddresses()) {
            if (address.getBroadcast() != null) {
                return address.getBroadcast();
            }
        }
        return null;
    }

}
