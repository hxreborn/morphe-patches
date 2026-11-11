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
package app.hxreborn.extension.idlefish;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public final class ShareLinkTest {

    private static final String DETAIL_DEEP_LINK = "fleamarket://awesome_detail?itemId=937029496728"
            + "&hitNativeDetail=true&flutter=true&needNotPreGet=true&ignoreMiddle=true&id=937029496728";

    private static String shareInfoWithUrl(String url) {
        return "{\"sceneType\":\"item\",\"url\":\"" + url + "\"}";
    }

    @Test
    public void buildsThePublicItemUrlFromTheDetailDeepLink() {
        assertEquals("https://h5.m.goofish.com/item?id=937029496728",
                ShareLink.publicItemUrl(shareInfoWithUrl(DETAIL_DEEP_LINK)));
    }

    @Test
    public void returnsNullWhenTheDeepLinkHasNoItemId() {
        assertNull(ShareLink.publicItemUrl(shareInfoWithUrl("fleamarket://awesome_detail?hitNativeDetail=true")));
    }

    @Test
    public void returnsNullWhenTheItemIdIsEmpty() {
        assertNull(ShareLink.publicItemUrl(shareInfoWithUrl("fleamarket://awesome_detail?itemId=")));
    }

    @Test
    public void returnsNullWhenTheItemIdHoldsNonDigits() {
        assertNull(ShareLink.publicItemUrl(shareInfoWithUrl("fleamarket://awesome_detail?itemId=1%26url%3Dhttp")));
    }

    @Test
    public void returnsNullWhenTheItemIdHoldsNonAsciiDigits() {
        assertNull(ShareLink.publicItemUrl(shareInfoWithUrl("fleamarket://awesome_detail?itemId=%D9%A3%D9%A4")));
    }

    @Test
    public void returnsNullForAnOpaqueLink() {
        assertNull(ShareLink.publicItemUrl(shareInfoWithUrl("mailto:someone")));
    }

    @Test
    public void returnsNullForNullShareInfo() {
        assertNull(ShareLink.publicItemUrl(null));
    }

    @Test
    public void returnsNullForMalformedShareInfo() {
        assertNull(ShareLink.publicItemUrl("{"));
    }

    @Test
    public void returnsNullForAJsonArray() {
        assertNull(ShareLink.publicItemUrl("[]"));
    }

    @Test
    public void returnsNullWhenTheUrlIsMissing() {
        assertNull(ShareLink.publicItemUrl("{}"));
    }

    @Test
    public void returnsNullWhenTheUrlIsNotAString() {
        assertNull(ShareLink.publicItemUrl("{\"url\":5}"));
    }

    @Test
    public void returnsNullWhenTheUrlIsJsonNull() {
        assertNull(ShareLink.publicItemUrl("{\"url\":null}"));
    }

}
