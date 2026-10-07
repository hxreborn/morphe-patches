/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.protonvpn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public final class FreeServerLocationsTest {

    enum Filter {

        Free, All, Custom {

            @Override
            public String toString() {
                return "custom";
            }

        }

    }

    enum ProfileType {

        Standard, SecureCore, P2P

    }

    enum Availability {

        AVAILABLE, UNAVAILABLE_PLAN, UNAVAILABLE_PROTOCOL

    }

    static final class Item {

        private final int tier;

        Item(int tier) {
            this.tier = tier;
        }

        public int getTier() {
            return this.tier;
        }

    }

    static final class Server {

        private final boolean free;

        Server(boolean free) {
            this.free = free;
        }

        public boolean isFreeServer() {
            return this.free;
        }

    }

    static final class Country {

        private final List<Server> servers;

        Country(Server... servers) {
            this.servers = Arrays.asList(servers);
        }

        public List<Server> getServerList() {
            return this.servers;
        }

    }

    static final class Profile {

        private final Availability availability;

        Profile(Availability availability) {
            this.availability = availability;
        }

        public Availability getAvailability() {
            return this.availability;
        }

    }

    @Before
    @After
    public void signOut() {
        FreeAccount.onUserInfoInvalidated();
    }

    private static void signInAsFreeUser() {
        FreeAccount.onUserInfoChanged(new FreeAccountTest.UserInfo(new FreeAccountTest.VpnUser(true)));
    }

    @Test
    public void otherAccountsKeepTheStockExclusion() {
        assertTrue(FreeServerLocations.shouldExcludeServer(true));
        assertFalse(FreeServerLocations.shouldExcludeServer(false));
    }

    @Test
    public void freeAccountInvertsTheStockExclusion() {
        signInAsFreeUser();
        assertFalse(FreeServerLocations.shouldExcludeServer(true));
        assertTrue(FreeServerLocations.shouldExcludeServer(false));
    }

    @Test
    public void freeUserOnAFreeItemIsCheckedAsTierOne() {
        assertEquals(Integer.valueOf(1), FreeServerLocations.tierForAvailabilityCheck(new Item(0), 0));
    }

    @Test
    public void freeUserOnAPaidItemKeepsTierZero() {
        assertEquals(Integer.valueOf(0), FreeServerLocations.tierForAvailabilityCheck(new Item(1), 0));
        assertEquals(Integer.valueOf(0), FreeServerLocations.tierForAvailabilityCheck(new Item(2), 0));
    }

    @Test
    public void tierIsUnchangedForNullItem() {
        assertEquals(Integer.valueOf(1), FreeServerLocations.tierForAvailabilityCheck(null, 1));
        assertEquals(Integer.valueOf(2), FreeServerLocations.tierForAvailabilityCheck(null, 2));
        assertEquals(Integer.valueOf(-1), FreeServerLocations.tierForAvailabilityCheck(null, -1));
        assertNull(FreeServerLocations.tierForAvailabilityCheck(null, null));
    }

    @Test
    public void paidUsersKeepTheirTierOnFreeItems() {
        assertEquals(Integer.valueOf(1), FreeServerLocations.tierForAvailabilityCheck(new Item(0), 1));
        assertEquals(Integer.valueOf(2), FreeServerLocations.tierForAvailabilityCheck(new Item(0), 2));
    }

    @Test
    public void freeUserSeesOnlyFreeTierItemsInOrder() {
        // given
        final Item firstFree = new Item(0);
        final Item secondFree = new Item(0);
        final List<Item> items = Arrays.asList(new Item(2), firstFree, new Item(1), secondFree, new Item(3));

        // when
        final List<?> visible = FreeServerLocations.itemsVisibleToTier(items, 0);

        // then
        assertEquals(Arrays.asList(firstFree, secondFree), visible);
        assertEquals(5, items.size());
    }

    @Test
    public void freeUserWithNoFreeItemsSeesNothing() {
        assertTrue(FreeServerLocations.itemsVisibleToTier(Arrays.asList(new Item(1), new Item(2)), 0).isEmpty());
        assertTrue(FreeServerLocations.itemsVisibleToTier(Collections.<Item>emptyList(), 0).isEmpty());
    }

    @Test
    public void nonFreeTiersSeeEveryItem() {
        final List<Item> items = Arrays.asList(new Item(0), new Item(1), new Item(2));
        assertSame(items, FreeServerLocations.itemsVisibleToTier(items, 1));
        assertSame(items, FreeServerLocations.itemsVisibleToTier(items, 2));
        assertSame(items, FreeServerLocations.itemsVisibleToTier(items, -1));
        assertSame(items, FreeServerLocations.itemsVisibleToTier(items, null));
    }

    @Test
    public void selectedFilterIsUntouchedForOtherAccounts() {
        assertSame(Filter.Free, FreeServerLocations.resolveSelectedFilter(Filter.Free));
        assertSame(Filter.Custom, FreeServerLocations.resolveSelectedFilter(Filter.Custom));
        assertNull(FreeServerLocations.resolveSelectedFilter(null));
    }

    @Test
    public void freeAccountResolvesTheSelectedFilterToAll() {
        signInAsFreeUser();
        assertSame(Filter.All, FreeServerLocations.resolveSelectedFilter(Filter.Free));
        assertSame(Filter.All, FreeServerLocations.resolveSelectedFilter(Filter.All));
        assertSame(Filter.All, FreeServerLocations.resolveSelectedFilter(Filter.Custom));
        assertNull(FreeServerLocations.resolveSelectedFilter(null));
    }

    @Test
    public void filterButtonsAreHiddenForFreeAccounts() {
        final List<String> buttons = Arrays.asList("Free", "All");
        assertSame(buttons, FreeServerLocations.resolveFilterButtons(buttons));

        signInAsFreeUser();
        assertTrue(FreeServerLocations.resolveFilterButtons(buttons).isEmpty());
        assertTrue(FreeServerLocations.resolveFilterButtons(Collections.<String>emptyList()).isEmpty());
    }

    @Test
    public void countriesAreUntouchedForOtherAccounts() {
        // given
        final List<Country> countries = Arrays.asList(new Country(new Server(false)), new Country());

        // when
        final List<?> result = FreeServerLocations.countriesForAccount(countries);

        // then
        assertSame(countries, result);
    }

    @Test
    public void freeAccountKeepsOnlyCountriesWithAFreeServer() {
        // given
        signInAsFreeUser();
        final Country mixed = new Country(new Server(false), new Server(true), new Server(false));
        final Country onlyFree = new Country(new Server(true));
        final List<Country> countries = Arrays.asList(new Country(new Server(false), new Server(false)), mixed,
                new Country(), onlyFree);

        // when
        final List<?> kept = FreeServerLocations.countriesForAccount(countries);

        // then
        assertEquals(Arrays.asList(mixed, onlyFree), kept);
        assertEquals(4, countries.size());
    }

    @Test
    public void freeAccountWithNoCountriesGetsNone() {
        // given
        signInAsFreeUser();

        // when
        final List<?> result = FreeServerLocations.countriesForAccount(Collections.<Country>emptyList());

        // then
        assertTrue(result.isEmpty());
    }

    @Test
    public void profileTypesAreUntouchedForOtherAccounts() {
        // given
        final List<ProfileType> types = Arrays.asList(ProfileType.values());

        // when
        final List<?> result = FreeServerLocations.profileTypesForAccount(types);

        // then
        assertSame(types, result);
    }

    @Test
    public void freeAccountKeepsOnlyTheStandardProfileType() {
        signInAsFreeUser();
        assertEquals(Collections.singletonList(ProfileType.Standard),
                FreeServerLocations.profileTypesForAccount(Arrays.asList(ProfileType.values())));
        assertTrue(FreeServerLocations.profileTypesForAccount(Arrays.asList(ProfileType.P2P, ProfileType.SecureCore))
            .isEmpty());
    }

    @Test
    public void serversAreUntouchedForOtherAccounts() {
        // given
        final List<Server> servers = Arrays.asList(new Server(true), new Server(false));

        // when
        final List<?> result = FreeServerLocations.serversForAccount(servers);

        // then
        assertSame(servers, result);
    }

    @Test
    public void freeAccountKeepsOnlyFreeServersInOrder() {
        // given
        signInAsFreeUser();
        final Server first = new Server(true);
        final Server second = new Server(true);
        final List<Server> servers = Arrays.asList(new Server(false), first, new Server(false), second);

        // when
        final List<?> kept = FreeServerLocations.serversForAccount(servers);

        // then
        assertEquals(Arrays.asList(first, second), kept);
        assertNotSame(servers, kept);
        assertEquals(4, servers.size());
    }

    @Test
    public void profilesAreUntouchedForOtherAccounts() {
        // given
        final List<Profile> profiles = Arrays.asList(new Profile(Availability.UNAVAILABLE_PLAN),
                new Profile(Availability.AVAILABLE));

        // when
        final List<?> result = FreeServerLocations.profilesForAccount(profiles);

        // then
        assertSame(profiles, result);
    }

    @Test
    public void freeAccountDropsOnlyProfilesUnavailableOnItsPlan() {
        // given
        signInAsFreeUser();
        final Profile available = new Profile(Availability.AVAILABLE);
        final Profile protocol = new Profile(Availability.UNAVAILABLE_PROTOCOL);
        final List<Profile> profiles = new ArrayList<>(Arrays.asList(new Profile(Availability.UNAVAILABLE_PLAN),
                available, new Profile(Availability.UNAVAILABLE_PLAN), protocol));

        // when
        final List<?> kept = FreeServerLocations.profilesForAccount(profiles);

        // then
        assertEquals(Arrays.asList(available, protocol), kept);
        assertEquals(4, profiles.size());
    }

}
