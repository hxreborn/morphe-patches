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
package app.hxreborn.extension.raindrop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

public final class LinkCheckerTest {

    @Test
    public void rejectsPrivateAndLocalHosts() {
        for (String host : new String[] { "localhost", "printer", "192.168.1.10", "10.0.0.5", "172.16.4.2", "127.0.0.1",
                "169.254.1.1", "[::1]", "[fd12::1]", "nas.local", "router.lan", "wiki.corp", "app.internal",
                "box.home.arpa" }) {
            assertFalse(host, LinkChecker.isPublicHost(host));
        }
    }

    @Test
    public void acceptsPublicHosts() {
        for (String host : new String[] { "example.com", "github.com", "93.184.216.34", "en.wikipedia.org",
                "[2606:4700::1111]" }) {
            assertTrue(host, LinkChecker.isPublicHost(host));
        }
    }

    @Test
    public void classifiesResolvedAddresses() throws UnknownHostException {
        for (String address : new String[] { "100.64.1.1", "100.127.255.254", "0.1.2.3", "224.0.0.1", "fd00::1",
                "fc00::1", "fe80::1" }) {
            assertFalse(address, LinkChecker.isPublicAddress(InetAddress.getByName(address)));
        }
        for (String address : new String[] { "100.128.0.1", "100.63.255.255", "8.8.8.8", "2606:4700::1111" }) {
            assertTrue(address, LinkChecker.isPublicAddress(InetAddress.getByName(address)));
        }
    }

    @Test
    public void decodesNamedAndNumericEntities() {
        assertEquals("AT&T <b> \"q\" 'a' é é",
                LinkChecker.decodeEntities("AT&amp;T &lt;b&gt; &quot;q&quot; &apos;a&apos; &#233; &#xE9;"));
        assertEquals("a b", LinkChecker.decodeEntities("a&nbsp;b"));
    }

    @Test
    public void readsCharsetFromContentType() {
        assertEquals("ISO-8859-1", LinkChecker.charsetOf("text/html; charset=iso-8859-1").name());
        assertEquals(StandardCharsets.UTF_8, LinkChecker.charsetOf("text/html"));
        assertEquals(StandardCharsets.UTF_8, LinkChecker.charsetOf("text/html; charset=bogus-charset"));
    }

}
