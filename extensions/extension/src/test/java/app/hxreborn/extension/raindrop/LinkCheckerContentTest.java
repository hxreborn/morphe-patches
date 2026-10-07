/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.raindrop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;

import org.junit.Test;

public final class LinkCheckerContentTest {

    @Test
    public void decodesEntitiesCaseInsensitively() {
        assertEquals("& < > \" '", LinkChecker.decodeEntities("&AMP; &Lt; &GT; &QUOT; &Apos;"));
        assertEquals("A A", LinkChecker.decodeEntities("&#X41; &#x41;"));
        assertEquals("A", LinkChecker.decodeEntities("&#65;"));
    }

    @Test
    public void replacesUndecodableEntitiesWithASpace() {
        assertEquals("[ ]", LinkChecker.decodeEntities("[&notanentity;]"));
        assertEquals("[ ]", LinkChecker.decodeEntities("[&#xZZ;]"));
        assertEquals("[ ]", LinkChecker.decodeEntities("[&#x;]"));
        assertEquals("[ ]", LinkChecker.decodeEntities("[&#1114112;]"));
        assertEquals("[ ]", LinkChecker.decodeEntities("[&#99999999999;]"));
    }

    @Test
    public void leavesMalformedEntitiesAlone() {
        assertEquals("a & b", LinkChecker.decodeEntities("a & b"));
        assertEquals("&amp", LinkChecker.decodeEntities("&amp"));
        assertEquals("&#;", LinkChecker.decodeEntities("&#;"));
        assertEquals("&#-5;", LinkChecker.decodeEntities("&#-5;"));
        assertEquals("", LinkChecker.decodeEntities(""));
        assertEquals("no entities", LinkChecker.decodeEntities("no entities"));
    }

    @Test
    public void treatsDecodedCharactersAsLiteralText() {
        assertEquals("$1 \\ $", LinkChecker.decodeEntities("&#36;1 &#92; &#x24;"));
    }

    @Test
    public void decodesAstralCodePoints() {
        assertEquals("😀", LinkChecker.decodeEntities("&#128512;"));
        assertEquals("😀", LinkChecker.decodeEntities("&#x1F600;"));
    }

    @Test
    public void buildsWaybackUrl() {
        assertEquals("https://web.archive.org/web/2/https://example.com/a?b=1",
                LinkChecker.archiveUrl("https://example.com/a?b=1"));
    }

    @Test
    public void reportsNoResultsBeforeAnAccountStoreExists() {
        assertFalse(LinkChecker.isBroken("https://example.com"));
        assertFalse(LinkChecker.isArchived("https://example.com"));
        assertEquals(Collections.emptySet(), LinkChecker.linksContaining("example"));
        assertEquals("", LinkChecker.resultStamp());
    }

    @Test
    public void createsNoStoreWhenAnAccountIsSelectedBeforeInit() {
        LinkChecker.useAccount(7);
        LinkChecker.useAccount(0);
        assertFalse(LinkChecker.isBroken("https://example.com"));
        assertEquals("", LinkChecker.resultStamp());
    }

    @Test
    public void rejectsNonPublicAddressesWrittenAsIpv4MappedIpv6() {
        for (String host : new String[] { "[::ffff:127.0.0.1]", "[::ffff:10.0.0.1]", "[::ffff:192.168.1.1]",
                "[::ffff:169.254.169.254]" }) {
            assertFalse(host, LinkChecker.isPublicHost(host));
        }
        assertTrue(LinkChecker.isPublicHost("[::ffff:8.8.8.8]"));
    }

    @Test
    public void rejectsLoopbackWrittenInShortenedNumericForms() {
        for (String host : new String[] { "2130706433", "127.1", "0.0.0.0", "0" }) {
            assertFalse(host, LinkChecker.isPublicHost(host));
        }
    }

    @Test
    public void ignoresCaseWhenClassifyingHosts() {
        assertFalse(LinkChecker.isPublicHost("NAS.LOCAL"));
        assertFalse(LinkChecker.isPublicHost("LocalHost"));
        assertTrue(LinkChecker.isPublicHost("EXAMPLE.COM"));
    }

    @Test
    public void requiresADotBeforeAPrivateSuffix() {
        assertTrue(LinkChecker.isPublicHost("mylocal.com"));
        assertTrue(LinkChecker.isPublicHost("corp.example.com"));
        assertTrue(LinkChecker.isPublicHost("lan.example.org"));
        assertFalse(LinkChecker.isPublicHost("a.b.lan"));
    }

    @Test
    public void rejectsUnspecifiedOrMulticastIpv6Addresses() throws UnknownHostException {
        for (String address : new String[] { "::", "ff02::1", "ff0e::1" }) {
            assertFalse(address, LinkChecker.isPublicAddress(InetAddress.getByName(address)));
        }
    }

}
