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
package app.hxreborn.extension.audiomack;

import org.junit.Assert;
import org.junit.Test;

public final class SongTagsTest {

    @Test
    public void namesTheFileArtistThenTitle() {
        Assert.assertEquals("Rema - Corny", new SongTags("5967145", "Corny", "Rema", null, null).fileName());
    }

    @Test
    public void replacesCharactersFileSystemsReject() {
        Assert.assertEquals("AC_DC - What_ Why_", new SongTags("1", "What? Why/", "AC/DC", null, null).fileName());
    }

    @Test
    public void namesTheFileByTitleWhenTheArtistIsMissing() {
        Assert.assertEquals("Corny", new SongTags("5967145", "Corny", " ", null, null).fileName());
    }

    @Test
    public void namesTheFileByIdWhenTheTitleIsMissing() {
        Assert.assertEquals("5967145", new SongTags("5967145", null, "Rema", null, null).fileName());
    }

}
