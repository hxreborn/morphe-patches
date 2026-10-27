/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.bereal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

public final class PickSelectionTest {

    @Test
    public void targetsTheBackPhotoFirst() {
        final PickSelection<String> selection = new PickSelection<>();

        assertEquals(PickSelection.Side.BACK, selection.target());
    }

    @Test
    public void takesTheFirstPickAsBackAndTheSecondAsFront() {
        // given
        final PickSelection<String> selection = new PickSelection<>();

        // when
        selection.toggle("a");
        selection.toggle("b");

        // then
        assertEquals("a", selection.back());
        assertEquals("b", selection.front());
    }

    @Test
    public void targetsTheFrontPhotoAfterTheBackPick() {
        // given
        final PickSelection<String> selection = new PickSelection<>();

        // when
        selection.toggle("a");

        // then
        assertEquals(PickSelection.Side.FRONT, selection.target());
    }

    @Test
    public void putsAPickOnTheTargetedSide() {
        // given
        final PickSelection<String> selection = new PickSelection<>();
        selection.target(PickSelection.Side.FRONT);

        // when
        selection.toggle("a");

        // then
        assertNull(selection.back());
        assertEquals("a", selection.front());
    }

    @Test
    public void keepsTheFrontPickWhenTheBackPickIsToggledOff() {
        // given
        final PickSelection<String> selection = new PickSelection<>();
        selection.toggle("a");
        selection.toggle("b");

        // when
        selection.toggle("a");

        // then
        assertNull(selection.back());
        assertEquals("b", selection.front());
    }

    @Test
    public void targetsTheSideThatWasToggledOff() {
        // given
        final PickSelection<String> selection = new PickSelection<>();
        selection.toggle("a");
        selection.toggle("b");

        // when
        selection.toggle("a");

        // then
        assertEquals(PickSelection.Side.BACK, selection.target());
    }

    @Test
    public void replacesThePickOnTheTargetedSideWhenBothAreTaken() {
        // given
        final PickSelection<String> selection = new PickSelection<>();
        selection.toggle("a");
        selection.toggle("b");
        selection.target(PickSelection.Side.BACK);

        // when
        selection.toggle("c");

        // then
        assertEquals("c", selection.back());
        assertEquals("b", selection.front());
    }

    @Test
    public void swapsTheBackAndFrontPicks() {
        // given
        final PickSelection<String> selection = new PickSelection<>();
        selection.toggle("a");
        selection.toggle("b");

        // when
        selection.swap();

        // then
        assertEquals("b", selection.back());
        assertEquals("a", selection.front());
    }

    @Test
    public void targetsTheEmptySideAfterASwapWithOnePick() {
        // given
        final PickSelection<String> selection = new PickSelection<>();
        selection.toggle("a");
        selection.swap();

        // when
        selection.toggle("b");

        // then
        assertEquals("b", selection.back());
        assertEquals("a", selection.front());
    }

    @Test
    public void forgetsEveryPickOnClear() {
        // given
        final PickSelection<String> selection = new PickSelection<>();
        selection.toggle("a");
        selection.toggle("b");

        // when
        selection.clear();

        // then
        assertTrue(selection.isEmpty());
        assertEquals(PickSelection.Side.BACK, selection.target());
    }

    @Test
    public void dropsPicksThatAreNoLongerAvailable() {
        // given
        final PickSelection<String> selection = new PickSelection<>();
        selection.toggle("a");
        selection.toggle("b");

        // when
        selection.retain(Arrays.asList("b", "c"));

        // then
        assertNull(selection.back());
        assertEquals("b", selection.front());
    }

    @Test
    public void fillsTheEmptiedSideWithTheNextPick() {
        // given
        final PickSelection<String> selection = new PickSelection<>();
        selection.toggle("a");
        selection.toggle("b");
        selection.retain(Arrays.asList("b", "c"));

        // when
        selection.toggle("c");

        // then
        assertEquals("c", selection.back());
    }

}
