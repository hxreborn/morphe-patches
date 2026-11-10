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
package app.hxreborn.extension.protonvpn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("unused")
public final class ExcludedLocations {

    private static volatile Object latestExcludedLocations;

    private ExcludedLocations() {

    }

    public static void onExcludedLocationsCreated(Object excludedLocations) {
        latestExcludedLocations = excludedLocations;
    }

    public static List<?> countriesWithoutExcludedServers(List<?> countries) {
        Object excludedLocations = latestExcludedLocations;
        if (excludedLocations == null) {
            return countries;
        }
        List<Object> allowedCountries = new ArrayList<>(countries.size());
        for (Object country : countries) {
            List<?> servers = serverList(country);
            List<Object> allowedServers = new ArrayList<>(servers.size());
            for (Object server : servers) {
                if (!isExcluded(server, excludedLocations)) {
                    allowedServers.add(server);
                }
            }
            if (allowedServers.size() == servers.size()) {
                allowedCountries.add(country);
            } else if (!allowedServers.isEmpty()) {
                allowedCountries.add(copyWithServers(country, allowedServers));
            }
        }
        return allowedCountries;
    }

    public static Object serverUnlessExcludedForFreeUser(Object server, Object vpnUser, Object excludedLocations) {
        if (server == null || vpnUser == null || !isFreeUser(vpnUser) || !isExcluded(server, excludedLocations)) {
            return server;
        }
        return null;
    }

    static boolean isFreeUser(Object vpnUser) {
        return false;
    }

    static List<?> serverList(Object country) {
        return Collections.emptyList();
    }

    static boolean isExcluded(Object server, Object excludedLocations) {
        return false;
    }

    static Object copyWithServers(Object country, List<?> servers) {
        return country;
    }

}
