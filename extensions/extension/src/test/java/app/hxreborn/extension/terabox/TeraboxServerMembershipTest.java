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

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLog;

@RunWith(RobolectricTestRunner.class)
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
    public void readsTheVipFlagFromABoxedField() {
        assertTrue(TeraboxServerMembership.isVip(new BoxedVipInfo(Boolean.TRUE, 1)));
        assertFalse(TeraboxServerMembership.isVip(new BoxedVipInfo(Boolean.FALSE, 1)));
    }

    @Test
    public void reportsTheLevelOfAMember() {
        assertEquals("1", TeraboxServerMembership.vipType(new VipInfo(true, 1)));
        assertEquals("2", TeraboxServerMembership.vipType(new VipInfo(true, 2)));
        assertEquals("0", TeraboxServerMembership.vipType(new VipInfo(true, 0)));
        assertEquals("2", TeraboxServerMembership.vipType(new BoxedVipInfo(Boolean.TRUE, 2)));
    }

    @Test
    public void reportsNoLevelForANonMember() {
        assertEquals("0", TeraboxServerMembership.vipType(new VipInfo(false, 0)));
        assertEquals("0", TeraboxServerMembership.vipType(new VipInfo(false, 3)));
        assertEquals("0", TeraboxServerMembership.vipType(new BoxedVipInfo(Boolean.FALSE, 3)));
    }

    @Test
    public void treatsAnObjectWithoutTheVipFlagAsNotAMember() {
        assertFalse(TeraboxServerMembership.isVip(new Object()));
        assertEquals("0", TeraboxServerMembership.vipType(new Object()));
        final List<ShadowLog.LogItem> warnings = ShadowLog.getLogsForTag("TeraboxServerMembership");
        assertEquals(2, warnings.size());
        assertEquals("Could not read isVip", warnings.get(0).msg);
    }

    @Test
    public void reportsLevelZeroWhenAMemberHasNoLevelField() {
        assertTrue(TeraboxServerMembership.isVip(new FlagOnly()));
        assertEquals("0", TeraboxServerMembership.vipType(new FlagOnly()));
    }

    @SuppressWarnings("unused")
    private static final class FlagOnly {

        private final boolean isVip = true;

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

    @SuppressWarnings("unused")
    private static final class BoxedVipInfo {

        private final Boolean isVip;

        private final Integer vipLevel;

        BoxedVipInfo(Boolean isVip, Integer vipLevel) {
            this.isVip = isVip;
            this.vipLevel = vipLevel;
        }

    }

}
