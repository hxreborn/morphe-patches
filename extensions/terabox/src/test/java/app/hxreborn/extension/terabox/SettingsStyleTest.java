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
package app.hxreborn.extension.terabox;

import android.content.Context;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = { 23, 36 }, shadows = FakeHostResources.class)
public final class SettingsStyleTest {

    @Test
    public void readsSurfacesAndTextFromHostColors() {
        Context context = RuntimeEnvironment.getApplication();
        SettingsStyle style = new SettingsStyle(context);

        Assert.assertEquals(context.getColor(FakeHostResources.PAGE_BACKGROUND), style.screenBackground);
        Assert.assertEquals(context.getColor(FakeHostResources.CARD_BACKGROUND), style.cardSurface);
        Assert.assertEquals(context.getColor(FakeHostResources.DIVIDER), style.divider);
        Assert.assertEquals(context.getColor(FakeHostResources.PRIMARY_TEXT), style.primaryText);
        Assert.assertEquals(context.getColor(FakeHostResources.SECONDARY_TEXT), style.secondaryText);
    }

}
