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
package hx.gateway;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public final class CountryAddressesTest {

    private static final String NETHERLANDS_NETWORK = "145.112.0.";

    private static final int LAST_HOST = 254;

    @Test
    public void picksAHostInsideTheCountryNetwork() {
        // when
        String address = CountryAddresses.addressIn("NL");

        // then
        Assertions.assertThat(address).startsWith(NETHERLANDS_NETWORK);
        Assertions.assertThat(Integer.parseInt(address.substring(NETHERLANDS_NETWORK.length())))
            .isBetween(1, LAST_HOST);
    }

    @Test
    public void usesOneHostForEveryCountry() {
        // when
        String netherlands = CountryAddresses.addressIn("NL");
        String india = CountryAddresses.addressIn("IN");

        // then
        Assertions.assertThat(hostOf(netherlands)).isEqualTo(hostOf(india));
    }

    @Test
    public void hasNoAddressForAnUnknownCode() {
        // when
        String address = CountryAddresses.addressIn("N/A");

        // then
        Assertions.assertThat(address).isNull();
    }

    private static String hostOf(String address) {
        return address.substring(address.lastIndexOf('.') + 1);
    }

}
