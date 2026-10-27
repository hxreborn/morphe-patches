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
