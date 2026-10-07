/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.protonvpn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.view.View;

import app.hxreborn.extension.proton.PatchedBuild;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
public final class PromotionsTest {

    static final class Notification {

        private final int type;

        Notification(int type) {
            this.type = type;
        }

        public int getType() {
            return this.type;
        }

    }

    private Context context;

    @Before
    public void useApplicationContext() {
        this.context = PatchedBuild.useApplicationContext();
    }

    private static List<Notification> notificationsOf(int... types) {
        final List<Notification> notifications = new ArrayList<>();
        for (int type : types) {
            notifications.add(new Notification(type));
        }
        return notifications;
    }

    private static List<Integer> typesOf(List<?> notifications) {
        final List<Integer> types = new ArrayList<>();
        for (Object notification : notifications) {
            types.add(((Notification) notification).getType());
        }
        return types;
    }

    @Test
    public void notificationsAreUntouchedWhenUpsellsAreNotHidden() {
        // given
        final List<Notification> promos = notificationsOf(1, 2, 3, 4, 5);

        // when
        final List<?> result = Promotions.withoutPromoNotifications(promos);

        // then
        assertSame(promos, result);
    }

    @Test
    public void itemsAreUntouchedWhenUpsellsAreNotHidden() {
        // given
        final List<Object> items = Arrays.<Object>asList("country", "city");

        // when
        final List<?> result = Promotions.withoutUpgradeBanners(items);

        // then
        assertSame(items, result);
    }

    @Test
    public void unpatchedBuildIgnoresAStoredHideSetting() {
        PatchedBuild.setUpgradePromotionsHidden(true);
        final List<Notification> promos = notificationsOf(1, 2, 3);
        final List<Object> items = Arrays.<Object>asList("country", "city");
        final View view = new View(this.context);

        assertSame(promos, Promotions.withoutPromoNotifications(promos));
        assertSame(items, Promotions.withoutUpgradeBanners(items));
        assertTrue(Promotions.showsUpsell(true));
        Promotions.hideUpgradeView(view);
        assertEquals(View.VISIBLE, view.getVisibility());
    }

    @Test
    public void upsellIsShownToFreeUsersOnlyWhenNothingIsHidden() {
        assertTrue(Promotions.showsUpsell(true));
        assertFalse(Promotions.showsUpsell(false));
    }

    @Test
    public void upgradeViewIsLeftAloneWhenUpsellsAreNotHidden() {
        final View view = new View(this.context);
        view.setVisibility(View.INVISIBLE);

        Promotions.hideUpgradeView(view);
        Promotions.hideUpgradeView(null);

        assertEquals(View.INVISIBLE, view.getVisibility());
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void everyPromoNotificationTypeIsRemoved() {
        // given
        final List<Notification> promos = notificationsOf(1, 2, 3, 5, 6, 7, 1_000_000);

        // when
        final List<?> kept = Promotions.withoutPromoNotifications(promos);

        // then
        assertTrue(kept.isEmpty());
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void otherNotificationTypesAreKeptInOrder() {
        // given
        final List<Notification> notifications = notificationsOf(8, 1, 4, 0, 2, 999_999, 1_000_001, -1, 7, 4);

        // when
        final List<?> kept = Promotions.withoutPromoNotifications(notifications);

        // then
        assertEquals(Arrays.asList(8, 4, 0, 999_999, 1_000_001, -1, 4), typesOf(kept));
        assertEquals(10, notifications.size());
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void filteredNotificationsAreANewList() {
        final List<Notification> notifications = notificationsOf(4);
        assertNotSame(notifications, Promotions.withoutPromoNotifications(notifications));
        assertTrue(Promotions.withoutPromoNotifications(Collections.<Notification>emptyList()).isEmpty());
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void notificationsAreUntouchedOnceUpsellsAreShownAgain() {
        // given
        PatchedBuild.setUpgradePromotionsHidden(false);
        final List<Notification> promos = notificationsOf(1, 2, 3, 4, 5);

        // when
        final List<?> result = Promotions.withoutPromoNotifications(promos);

        // then
        assertSame(promos, result);
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void itemsThatAreNotUpgradeBannersAreKeptInOrder() {
        // given
        final List<Object> items = Arrays.<Object>asList("country", 3, new Object(), "city");

        // when
        final List<?> kept = Promotions.withoutUpgradeBanners(items);

        // then
        assertEquals(items, kept);
        assertNotSame(items, kept);
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void itemsAreUntouchedOnceUpsellsAreShownAgain() {
        // given
        PatchedBuild.setUpgradePromotionsHidden(false);
        final List<Object> items = Arrays.<Object>asList("country", "city");

        // when
        final List<?> result = Promotions.withoutUpgradeBanners(items);

        // then
        assertSame(items, result);
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void hiddenUpsellIsShownToNobody() {
        assertFalse(Promotions.showsUpsell(true));
        assertFalse(Promotions.showsUpsell(false));
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void upsellIsShownToFreeUsersOnlyOnceUpsellsAreShownAgain() {
        PatchedBuild.setUpgradePromotionsHidden(false);

        assertTrue(Promotions.showsUpsell(true));
        assertFalse(Promotions.showsUpsell(false));
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void upgradeViewIsRemovedFromTheLayout() {
        final View visible = new View(this.context);
        final View invisible = new View(this.context);
        invisible.setVisibility(View.INVISIBLE);

        Promotions.hideUpgradeView(visible);
        Promotions.hideUpgradeView(invisible);

        assertEquals(View.GONE, visible.getVisibility());
        assertEquals(View.GONE, invisible.getVisibility());
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void upgradeViewIsLeftAloneOnceUpsellsAreShownAgain() {
        // given
        PatchedBuild.setUpgradePromotionsHidden(false);
        final View view = new View(this.context);

        // when
        Promotions.hideUpgradeView(view);

        // then
        assertEquals(View.VISIBLE, view.getVisibility());
    }

}
