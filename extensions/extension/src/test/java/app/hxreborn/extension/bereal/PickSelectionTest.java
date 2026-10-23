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
    public void asksForTheBackPhotoFirst() {
        final PickSelection<String> selection = new PickSelection<>();

        assertEquals(PickSelection.Step.BACK, selection.step());
    }

    @Test
    public void takesTheFirstPickAsBackAndTheSecondAsFront() {
        final PickSelection<String> selection = new PickSelection<>();

        selection.toggle("a");
        selection.toggle("b");

        assertEquals("a", selection.back());
        assertEquals("b", selection.front());
        assertEquals(PickSelection.Step.DONE, selection.step());
    }

    @Test
    public void asksForTheFrontPhotoAfterTheBackPick() {
        final PickSelection<String> selection = new PickSelection<>();

        selection.toggle("a");

        assertEquals(PickSelection.Step.FRONT, selection.step());
    }

    @Test
    public void promotesTheFrontPhotoWhenTheBackPickIsToggledOff() {
        final PickSelection<String> selection = new PickSelection<>();
        selection.toggle("a");
        selection.toggle("b");

        selection.toggle("a");

        assertEquals("b", selection.back());
        assertNull(selection.front());
        assertEquals(PickSelection.Step.FRONT, selection.step());
    }

    @Test
    public void clearsTheFrontPickWhenItIsToggledOff() {
        final PickSelection<String> selection = new PickSelection<>();
        selection.toggle("a");
        selection.toggle("b");

        selection.toggle("b");

        assertEquals("a", selection.back());
        assertNull(selection.front());
    }

    @Test
    public void replacesTheFrontPickWhenBothAreTaken() {
        final PickSelection<String> selection = new PickSelection<>();
        selection.toggle("a");
        selection.toggle("b");

        selection.toggle("c");

        assertEquals("a", selection.back());
        assertEquals("c", selection.front());
    }

    @Test
    public void forgetsEveryPickOnClear() {
        final PickSelection<String> selection = new PickSelection<>();
        selection.toggle("a");
        selection.toggle("b");

        selection.clear();

        assertTrue(selection.isEmpty());
        assertEquals(PickSelection.Step.BACK, selection.step());
    }

    @Test
    public void dropsPicksThatAreNoLongerAvailable() {
        final PickSelection<String> selection = new PickSelection<>();
        selection.toggle("a");
        selection.toggle("b");

        selection.retain(Arrays.asList("b", "c"));

        assertEquals("b", selection.back());
        assertNull(selection.front());
    }

}
