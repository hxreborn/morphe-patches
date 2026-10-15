/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.netanalyzer;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.util.Map;
import java.util.Objects;

final class BonjourService {

    final String name;

    final String domain;

    final String server;

    final String application;

    final String type;

    final int port;

    final Inet4Address address4;

    final Inet6Address address6;

    final Map<String, String> txt;

    BonjourService(String name, String domain, String server, String application, String type, int port,
            Inet4Address address4, Inet6Address address6, Map<String, String> txt) {
        this.name = name;
        this.domain = domain;
        this.server = server;
        this.application = application;
        this.type = type;
        this.port = port;
        this.address4 = address4;
        this.address6 = address6;
        this.txt = txt;
    }

    boolean sameAs(BonjourService other) {
        return Objects.equals(this.name, other.name) && Objects.equals(this.type, other.type)
                && Objects.equals(this.server, other.server) && this.port == other.port
                && Objects.equals(this.txt, other.txt) && Objects.equals(this.address4, other.address4)
                && Objects.equals(this.address6, other.address6);
    }

}
