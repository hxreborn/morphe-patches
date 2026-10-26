/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.firefox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class PatchSettingsTest {

    @Test
    public void acceptsHttpsAddressWithHost() {
        assertTrue(PatchSettings.isWebUrl("https://duckduckgo.com"));
    }

    @Test
    public void acceptsHttpAddressWithPath() {
        assertTrue(PatchSettings.isWebUrl("http://example.org/start?q=1"));
    }

    @Test
    public void acceptsAddressWithSurroundingSpaces() {
        assertTrue(PatchSettings.isWebUrl("  https://duckduckgo.com  "));
    }

    @Test
    public void rejectsAddressWithoutScheme() {
        assertFalse(PatchSettings.isWebUrl("duckduckgo.com"));
    }

    @Test
    public void rejectsAddressWithoutHost() {
        assertFalse(PatchSettings.isWebUrl("https://"));
    }

    @Test
    public void rejectsOtherSchemes() {
        assertFalse(PatchSettings.isWebUrl("ftp://duckduckgo.com"));
    }

    @Test
    public void rejectsEmptyText() {
        assertFalse(PatchSettings.isWebUrl(""));
    }

    @Test
    public void rejectsMalformedAddress() {
        assertFalse(PatchSettings.isWebUrl("https://duck duckgo.com"));
    }

    @Test
    public void readsTheHostOfAnAddress() {
        assertEquals("duckduckgo.com", PatchSettings.hostOf("https://duckduckgo.com/start?q=1"));
    }

    @Test
    public void readsNoHostFromAnAddressWithoutOne() {
        assertEquals("", PatchSettings.hostOf("duckduckgo.com"));
    }

}
