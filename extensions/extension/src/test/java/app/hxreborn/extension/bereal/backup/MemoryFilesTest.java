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
package app.hxreborn.extension.bereal.backup;

import static org.junit.Assert.assertEquals;

import java.time.ZoneId;

import org.junit.Test;

public final class MemoryFilesTest {

    private static final long TAKEN_AT = 1793046563000L;

    private static final ZoneId ATHENS = ZoneId.of("Europe/Athens");

    @Test
    public void namesAFileAfterTheLocalCaptureTime() {
        assertEquals("2026-10-26_22-29-23", MemoryFiles.base(TAKEN_AT, 0, "post-1", ATHENS));
    }

    @Test
    public void fallsBackToThePostTimeThenThePostId() {
        assertEquals(5L, MemoryFiles.time(5, 9));
        assertEquals(9L, MemoryFiles.time(0, 9));
        assertEquals("2026-10-26_22-29-23", MemoryFiles.base(0, TAKEN_AT, "post-1", ATHENS));
        assertEquals("post_1", MemoryFiles.base(0, 0, "post/1", ATHENS));
    }

    @Test
    public void suffixesCollisionsFromTheSecondAttempt() {
        assertEquals("b", MemoryFiles.suffixed("b", 1));
        assertEquals("b_2", MemoryFiles.suffixed("b", 2));
    }

    @Test
    public void readsTheExtensionFromTheUrlPath() {
        assertEquals("webp", MemoryFiles.extension("https://cdn.example/a.webp?x=1", "jpg"));
        assertEquals("jpg", MemoryFiles.extension("https://cdn.example/a.JPEG", "webp"));
        assertEquals("mp4", MemoryFiles.extension("https://cdn.example/a.bin", "mp4"));
        assertEquals("webp", MemoryFiles.extension("not a url", "webp"));
    }

    @Test
    public void mapsExtensionsToMimeTypes() {
        assertEquals("image/jpeg", MemoryFiles.mimeType("jpg"));
        assertEquals("video/mp4", MemoryFiles.mimeType("mp4"));
        assertEquals("image/webp", MemoryFiles.mimeType("webp"));
    }

}
