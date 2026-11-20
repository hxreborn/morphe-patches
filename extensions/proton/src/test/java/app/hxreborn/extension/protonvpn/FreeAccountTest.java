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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public final class FreeAccountTest {

    static final class UserInfo {

        private final Object vpnUser;

        UserInfo(Object vpnUser) {
            this.vpnUser = vpnUser;
        }

        public Object getVpnUser() {
            return this.vpnUser;
        }

    }

    static final class VpnUser {

        private final boolean free;

        VpnUser(boolean free) {
            this.free = free;
        }

        public boolean isFreeUser() {
            return this.free;
        }

    }

    @Before
    @After
    public void signOut() {
        FreeAccount.onUserInfoInvalidated();
    }

    @Test
    public void startsSignedOut() {
        assertFalse(FreeAccount.isSignedIn());
    }

    @Test
    public void freeUserIsSignedIn() {
        // when
        FreeAccount.onUserInfoChanged(new UserInfo(new VpnUser(true)));

        // then
        assertTrue(FreeAccount.isSignedIn());
    }

    @Test
    public void paidUserIsNotSignedIn() {
        // when
        FreeAccount.onUserInfoChanged(new UserInfo(new VpnUser(false)));

        // then
        assertFalse(FreeAccount.isSignedIn());
    }

    @Test
    public void userInfoWithoutVpnUserIsNotSignedIn() {
        // when
        FreeAccount.onUserInfoChanged(new UserInfo(null));

        // then
        assertFalse(FreeAccount.isSignedIn());
    }

    @Test
    public void missingUserInfoIsNotSignedIn() {
        // when
        FreeAccount.onUserInfoChanged(null);

        // then
        assertFalse(FreeAccount.isSignedIn());
    }

    @Test
    public void latestUserInfoWins() {
        FreeAccount.onUserInfoChanged(new UserInfo(new VpnUser(true)));
        FreeAccount.onUserInfoChanged(new UserInfo(new VpnUser(false)));
        assertFalse(FreeAccount.isSignedIn());

        FreeAccount.onUserInfoChanged(new UserInfo(new VpnUser(true)));
        FreeAccount.onUserInfoChanged(new UserInfo(null));
        assertFalse(FreeAccount.isSignedIn());

        FreeAccount.onUserInfoChanged(new UserInfo(new VpnUser(true)));
        FreeAccount.onUserInfoChanged(null);
        assertFalse(FreeAccount.isSignedIn());
    }

    @Test
    public void invalidatingSignsOut() {
        // given
        FreeAccount.onUserInfoChanged(new UserInfo(new VpnUser(true)));

        // when
        FreeAccount.onUserInfoInvalidated();

        // then
        assertFalse(FreeAccount.isSignedIn());
    }

}
