/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.etsy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public final class EtsyAdsFilterTest {

    private static final Object LISTING = new Component("ListingCardApiModel(id=1)");

    private static final Object SECOND_LISTING = new Component("ListingCardApiModel(id=2)");

    private static final Object AD_LABEL = new Component("AdLabelApiModel(text=Ad by seller)");

    @Test
    public void passesNullThrough() {
        assertNull(EtsyAdsFilter.removeAds(null));
    }

    @Test
    public void passesAnEmptyListThrough() {
        // given
        final List<Object> empty = new ArrayList<>();

        // when
        final List<?> result = EtsyAdsFilter.removeAds(empty);

        // then
        assertSame(empty, result);
    }

    @Test
    public void removesComponentsCarryingAnAdLabel() {
        // when
        final List<?> kept = EtsyAdsFilter.removeAds(Arrays.asList(LISTING, AD_LABEL, SECOND_LISTING));

        // then
        assertEquals(Arrays.asList(LISTING, SECOND_LISTING), kept);
    }

    @Test
    public void keepsEveryComponentWhenNoneIsPromoted() {
        // given
        final List<Object> components = Arrays.asList(LISTING, SECOND_LISTING);

        // when
        final List<?> kept = EtsyAdsFilter.removeAds(components);

        // then
        assertEquals(components, kept);
        assertNotSame(components, kept);
    }

    @Test
    public void removesAllComponentsWhenEveryOneIsPromoted() {
        assertTrue(EtsyAdsFilter.removeAds(Arrays.asList(AD_LABEL, AD_LABEL)).isEmpty());
    }

    @Test
    public void keepsOrderOfTheSurvivors() {
        // when
        final List<?> kept = EtsyAdsFilter.removeAds(Arrays.asList(AD_LABEL, SECOND_LISTING, AD_LABEL, LISTING));

        // then
        assertEquals(Arrays.asList(SECOND_LISTING, LISTING), kept);
    }

    @Test
    public void keepsNullComponents() {
        // when
        final List<?> kept = EtsyAdsFilter.removeAds(Arrays.asList(LISTING, null, AD_LABEL, null));

        // then
        assertEquals(Arrays.asList(LISTING, null, null), kept);
    }

    @Test
    public void matchesTheLabelAnywhereInTheDescription() {
        // given
        final Object nested = new Component("Wrapper(inner=AdLabelApiModel(text=Ad))");

        // when
        final List<?> kept = EtsyAdsFilter.removeAds(Collections.singletonList(nested));

        // then
        assertTrue(kept.isEmpty());
    }

    @Test
    public void doesNotMatchOtherAdRelatedNames() {
        // given
        final Object similar = new Component("AdApiModel(adLabel=none)");

        // when
        final List<?> kept = EtsyAdsFilter.removeAds(Collections.singletonList(similar));

        // then
        assertEquals(Collections.singletonList(similar), kept);
    }

    @Test
    public void doesNotModifyTheInputList() {
        // given
        final List<Object> components = new ArrayList<>(Arrays.asList(LISTING, AD_LABEL));

        // when
        EtsyAdsFilter.removeAds(components);

        // then
        assertEquals(Arrays.asList(LISTING, AD_LABEL), components);
    }

    private static final class Component {

        private final String description;

        Component(String description) {
            this.description = description;
        }

        @Override
        public String toString() {
            return this.description;
        }

    }

}
