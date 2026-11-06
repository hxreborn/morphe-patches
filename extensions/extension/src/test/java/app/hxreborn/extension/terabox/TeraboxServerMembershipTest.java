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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = TeraboxServerMembershipTest.HostVipInfo.class)
public final class TeraboxServerMembershipTest {

    @Test
    public void missingInfoIsNotAMember() {
        assertFalse(TeraboxServerMembership.isVip(null));
        assertEquals("0", TeraboxServerMembership.vipType(null));
    }

    @Test
    public void readsTheVipFlag() {
        assertTrue(TeraboxServerMembership.isVip(new VipInfo(true, 1)));
        assertFalse(TeraboxServerMembership.isVip(new VipInfo(false, 1)));
    }

    @Test
    public void reportsTheLevelOfAMember() {
        assertEquals("1", TeraboxServerMembership.vipType(new VipInfo(true, 1)));
        assertEquals("2", TeraboxServerMembership.vipType(new VipInfo(true, 2)));
        assertEquals("0", TeraboxServerMembership.vipType(new VipInfo(true, 0)));
    }

    @Test
    public void reportsNoLevelForANonMember() {
        assertEquals("0", TeraboxServerMembership.vipType(new VipInfo(false, 0)));
        assertEquals("0", TeraboxServerMembership.vipType(new VipInfo(false, 3)));
    }

    @Implements(value = TeraboxServerMembership.class, isInAndroidSdk = false)
    public static final class HostVipInfo {

        @Implementation
        public static boolean serverIsVip(Object vipInfo) {
            return ((VipInfo) vipInfo).isVip;
        }

        @Implementation
        public static int serverVipLevel(Object vipInfo) {
            return ((VipInfo) vipInfo).vipLevel;
        }

    }

    @SuppressWarnings("unused")
    private static final class VipInfo {

        private final boolean isVip;

        private final int vipLevel;

        VipInfo(boolean isVip, int vipLevel) {
            this.isVip = isVip;
            this.vipLevel = vipLevel;
        }

    }

}
