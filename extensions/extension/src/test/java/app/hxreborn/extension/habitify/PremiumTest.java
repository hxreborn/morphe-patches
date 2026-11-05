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
package app.hxreborn.extension.habitify;

import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public final class PremiumTest {

    private static final String USER_ID = "test-user";

    private static final String USER_URL = "https://example.firebaseio.com/users/" + USER_ID;

    private static Object unlockValue(String url, String userId, Object value) {
        return Premium.unlockValue(Premium.userRelativePath(url, userId), value);
    }

    @Test
    public void keepsOtherUsersUnchanged() {
        Map<String, Object> original = new HashMap<>();
        Assert.assertSame(original, unlockValue(USER_URL, "another-user", original));
    }

    @Test
    public void keepsUnrelatedPathsUnchanged() {
        Map<String, Object> original = new HashMap<>();
        Assert.assertSame(original, unlockValue(USER_URL + "/habits", USER_ID, original));
    }

    @Test
    public void keepsSignedOutSnapshotsUnchanged() {
        Map<String, Object> original = new HashMap<>();
        Assert.assertSame(original, unlockValue(USER_URL, null, original));
    }

    @Test
    public void keepsUserIdPrefixesUnchanged() {
        Assert.assertNull(Premium.userRelativePath(USER_URL + "-other", USER_ID));
    }

    @Test
    public void grantsLifetimeProToFreeUsers() {
        Map<?, ?> result = (Map<?, ?>) unlockValue(USER_URL, USER_ID, new HashMap<>());
        Assert.assertEquals(1L, result.get("premiumStatusAndroid"));
        Assert.assertEquals("PRO", ((Map<?, ?>) result.get("subscription")).get("productId"));
    }

    @Test
    public void removesExpiredSubscriptionDate() {
        Map<String, Object> original = new HashMap<>();
        original.put("productId", "PLUS");
        original.put("expiresAt", "2000-01-01T00:00:00Z");
        Map<?, ?> result = (Map<?, ?>) unlockValue(USER_URL + "/subscription", USER_ID, original);
        Assert.assertEquals("PRO", result.get("productId"));
        Assert.assertFalse(result.containsKey("expiresAt"));
    }

    @Test
    public void preservesOriginalSnapshot() {
        Map<String, Object> subscription = new HashMap<>();
        subscription.put("productId", "PLUS");
        Map<String, Object> original = new HashMap<>();
        original.put("subscription", subscription);
        unlockValue(USER_URL, USER_ID, original);
        Assert.assertEquals("PLUS", subscription.get("productId"));
        Assert.assertFalse(original.containsKey("premiumStatusAndroid"));
    }

    @Test
    public void keepsProfileFields() {
        Map<String, Object> original = new HashMap<>();
        original.put("name", "Test profile");
        Map<?, ?> result = (Map<?, ?>) unlockValue(USER_URL, USER_ID, original);
        Assert.assertEquals("Test profile", result.get("name"));
    }

    @Test
    public void createsMissingSubscription() {
        Map<?, ?> result = (Map<?, ?>) unlockValue(USER_URL + "/subscription", USER_ID, null);
        Assert.assertEquals("PRO", result.get("productId"));
    }

    @Test
    public void keepsMissingProfilesAbsent() {
        Assert.assertNull(unlockValue(USER_URL, USER_ID, null));
    }

    @Test
    public void reusesLifetimeProSnapshots() {
        Object original = unlockValue(USER_URL, USER_ID, new HashMap<>());
        Assert.assertSame(original, unlockValue(USER_URL, USER_ID, original));
    }

    @Test
    public void grantsLegacyPremiumFlag() {
        Assert.assertEquals(1L, unlockValue(USER_URL + "/premiumStatusAndroid", USER_ID, 0L));
    }

}
