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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import app.hxreborn.extension.proton.PatchedBuild;

@RunWith(RobolectricTestRunner.class)
public final class SwitchThumbIconTest {

    private static final long ORIGINAL_COLOR = 0x1234L;

    @Before
    public void useApplicationContext() {
        PatchedBuild.useApplicationContext();
    }

    @Test
    public void leavesTheThumbContentAloneWhenUnpatched() {
        assertNull(SwitchThumbIcon.thumbContent(Function.class, true));
        assertNull(SwitchThumbIcon.thumbContent(Function.class, false));
    }

    @Test
    public void leavesTheThumbColorAloneWhenUnpatched() {
        assertEquals(ORIGINAL_COLOR, SwitchThumbIcon.checkedThumbColor(ORIGINAL_COLOR));
    }

    @Test
    @Config(shadows = PatchedBuild.Switches.class)
    public void providesThumbContentOfTheComposableType() {
        // when
        final Object content = SwitchThumbIcon.thumbContent(Function.class, true);

        // then
        assertTrue(content instanceof Function);
    }

    @Test
    @Config(shadows = PatchedBuild.Switches.class)
    public void reusesTheContentForTheSameState() {
        assertSame(SwitchThumbIcon.thumbContent(Function.class, true),
                SwitchThumbIcon.thumbContent(Function.class, true));
        assertSame(SwitchThumbIcon.thumbContent(Function.class, false),
                SwitchThumbIcon.thumbContent(Function.class, false));
    }

    @Test
    @Config(shadows = PatchedBuild.Switches.class)
    public void providesDifferentContentForEachState() {
        assertNotSame(SwitchThumbIcon.thumbContent(Function.class, true),
                SwitchThumbIcon.thumbContent(Function.class, false));
    }

    @Test
    @Config(shadows = PatchedBuild.Switches.class)
    public void describesTheContentByItsState() {
        assertEquals("SwitchThumbIcon(checked)", SwitchThumbIcon.thumbContent(Function.class, true).toString());
        assertEquals("SwitchThumbIcon(unchecked)", SwitchThumbIcon.thumbContent(Function.class, false).toString());
    }

    @Test
    @Config(shadows = PatchedBuild.Switches.class)
    public void comparesTheContentByIdentity() {
        // given
        final Object checked = SwitchThumbIcon.thumbContent(Function.class, true);
        final Object unchecked = SwitchThumbIcon.thumbContent(Function.class, false);

        // when
        assertTrue(checked.equals(checked));
        assertFalse(checked.equals(unchecked));
        assertEquals(System.identityHashCode(checked), checked.hashCode());
    }

    @Test
    @Config(shadows = PatchedBuild.Switches.class)
    @SuppressWarnings("unchecked")
    public void returnsNullWhenTheContentIsInvoked() {
        // given
        final Function<Object, Object> content = (Function<Object, Object>) SwitchThumbIcon.thumbContent(Function.class,
                true);

        // when
        final Object result = content.apply(new Object());

        // then
        assertNull(result);
    }

    @Test
    @Config(shadows = PatchedBuild.Switches.class)
    public void keepsTheOriginalThumbColorWithoutTheInvertedTextAttribute() {
        assertEquals(ORIGINAL_COLOR, SwitchThumbIcon.checkedThumbColor(ORIGINAL_COLOR));
    }

}
