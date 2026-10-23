/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.bereal.fof;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import app.hxreborn.extension.bereal.PatchSettings;
import app.morphe.extension.shared.Utils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class FriendsOfFriendsTabTest {

    @Before
    public void useApplicationContext() {
        Utils.setContext(RuntimeEnvironment.getApplication());
    }

    @After
    public void restoreDefault() {
        PatchSettings.REMOVE_FRIENDS_OF_FRIENDS_TAB.save(false);
    }

    @Test
    public void addsTheTabWhenNotRemoved() {
        // given
        List<Object> tabs = givenTabs("friends");

        // when
        boolean added = FriendsOfFriendsTab.addUnlessRemoved(tabs, "fof", false);

        // then
        assertTrue(added);
        assertEquals(Arrays.asList("friends", "fof"), tabs);
    }

    @Test
    public void skipsTheTabWhenRemoved() {
        // given
        List<Object> tabs = givenTabs("friends");

        // when
        boolean added = FriendsOfFriendsTab.addUnlessRemoved(tabs, "fof", true);

        // then
        assertFalse(added);
        assertEquals(Arrays.asList("friends"), tabs);
    }

    @Test
    public void addsTheTabWhenTheStoredSettingIsOff() {
        // given
        List<Object> tabs = givenTabs("friends");
        PatchSettings.REMOVE_FRIENDS_OF_FRIENDS_TAB.save(false);

        // when
        boolean added = FriendsOfFriendsTab.addTab(tabs, "fof");

        // then
        assertTrue(added);
        assertEquals(Arrays.asList("friends", "fof"), tabs);
    }

    @Test
    public void skipsTheTabWhenTheStoredSettingIsOn() {
        // given
        List<Object> tabs = givenTabs("friends");
        PatchSettings.REMOVE_FRIENDS_OF_FRIENDS_TAB.save(true);

        // when
        boolean added = FriendsOfFriendsTab.addTab(tabs, "fof");

        // then
        assertFalse(added);
        assertEquals(Arrays.asList("friends"), tabs);
    }

    private static List<Object> givenTabs(Object... tabs) {
        return new ArrayList<>(Arrays.asList(tabs));
    }

}
