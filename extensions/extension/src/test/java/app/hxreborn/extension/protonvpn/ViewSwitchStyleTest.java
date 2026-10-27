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

import static org.junit.Assert.assertNull;

import android.view.View;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import app.hxreborn.extension.proton.PatchedBuild;

@RunWith(RobolectricTestRunner.class)
public final class ViewSwitchStyleTest {

    private final View view = new View(RuntimeEnvironment.getApplication());

    @Test
    public void offersNoThumbWithoutTheHostStyles() {
        assertNull(ViewSwitchStyle.thumb(this.view));
    }

    @Test
    public void offersNoTrackWithoutTheHostStyles() {
        assertNull(ViewSwitchStyle.track(this.view));
    }

    @Test
    @Config(qualifiers = "night")
    public void offersNoThumbInDarkThemeWithoutTheHostStyles() {
        PatchedBuild.useApplicationContext();

        assertNull(ViewSwitchStyle.thumb(this.view));
        assertNull(ViewSwitchStyle.track(this.view));
    }

}
