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
package app.hxreborn.extension.readera;

import org.junit.Assert;
import org.junit.Test;

public class SelectionSourceTest {

    private static final String SOURCE = "Frankenstein\nMary Shelley";

    private static final String TEXT = "It was on a dreary night of November.";

    @Test
    public void putsSourceAndBlankLineBeforeText() {
        Assert.assertEquals("Frankenstein\nMary Shelley\n\nIt was on a dreary night of November.",
                SelectionSource.withSource(SOURCE, TEXT));
    }

    @Test
    public void returnsTextAloneForMissingSource() {
        Assert.assertEquals(TEXT, SelectionSource.withSource(null, TEXT));
    }

    @Test
    public void returnsTextAloneForEmptySource() {
        Assert.assertEquals(TEXT, SelectionSource.withSource("", TEXT));
    }

}
