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
package app.hxreborn.extension.biwenger;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public final class AdRequestTest {

    @Test
    public void recognizesAnAdMobRequest() throws JSONException {
        // given
        final String message = givenMessage("AdMob");

        // when
        final boolean adRequest = AdRequest.matches(message);

        // then
        assertTrue(adRequest);
    }

    @Test
    public void recognizesAnAdManagerRequest() throws JSONException {
        // given
        final String message = givenMessage("AdManager");

        // when
        final boolean adRequest = AdRequest.matches(message);

        // then
        assertTrue(adRequest);
    }

    @Test
    public void ignoresOtherEvents() throws JSONException {
        // given
        final String message = givenMessage("storage");

        // when
        final boolean adRequest = AdRequest.matches(message);

        // then
        assertFalse(adRequest);
    }

    @Test
    public void ignoresMessagesThatAreNotJson() {
        // when
        final boolean adRequest = AdRequest.matches("not json");

        // then
        assertFalse(adRequest);
    }

    @Test
    public void ignoresMessagesWithoutAnEvent() throws JSONException {
        // given
        final String message = new JSONObject().put("params", JSONObject.NULL).toString();

        // when
        final boolean adRequest = AdRequest.matches(message);

        // then
        assertFalse(adRequest);
    }

    private static String givenMessage(String event) throws JSONException {
        return new JSONObject().put("event", event).put("params", new JSONObject().put("action", "prepare")).toString();
    }

}
