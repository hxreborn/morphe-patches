/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.bereal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.util.ArrayList;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import app.morphe.extension.shared.Utils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowSystemClock;

@RunWith(RobolectricTestRunner.class)
public final class PatchesSettingsActivityTest {

    @Before
    public void enableGallery() {
        Utils.setContext(RuntimeEnvironment.getApplication());
        PatchSettings.GALLERY_PHOTOS.save(true);
    }

    @After
    public void restoreDefaults() {
        PatchSettings.GALLERY_PHOTOS.save(false);
    }

    @Test
    public void undoRestoresTheSettingsFromBeforeTheFirstReset() {
        // given
        final ActivityController<PatchesSettingsActivity> controller = Robolectric
            .buildActivity(PatchesSettingsActivity.class)
            .setup();
        controller.get().resetToDefaults();
        controller.get().resetToDefaults();

        // when
        undo(controller.get()).performClick();

        // then
        assertTrue(PatchSettings.GALLERY_PHOTOS.get());
    }

    @Test
    public void undoRestoresTheSettingsAfterTheActivityIsRecreated() {
        // given
        final ActivityController<PatchesSettingsActivity> controller = Robolectric
            .buildActivity(PatchesSettingsActivity.class)
            .setup();
        controller.get().resetToDefaults();
        controller.recreate();

        // when
        undo(controller.get()).performClick();

        // then
        assertTrue(PatchSettings.GALLERY_PHOTOS.get());
    }

    @Test
    public void dropsTheUndoWhenTheActivityIsRestoredAfterTheWindow() {
        // given
        final ActivityController<PatchesSettingsActivity> controller = Robolectric
            .buildActivity(PatchesSettingsActivity.class)
            .setup();
        controller.get().resetToDefaults();
        final Bundle state = new Bundle();
        controller.saveInstanceState(state);
        ShadowSystemClock.advanceBy(Duration.ofSeconds(6));

        // when
        final PatchesSettingsActivity restored = Robolectric.buildActivity(PatchesSettingsActivity.class)
            .setup(state)
            .get();

        // then
        assertFalse(hasVisibleUndo(restored));
    }

    private static boolean hasVisibleUndo(PatchesSettingsActivity activity) {
        try {
            undo(activity);
            return true;
        } catch (AssertionError ex) {
            return false;
        }
    }

    private static View undo(PatchesSettingsActivity activity) {
        final ArrayList<View> found = new ArrayList<>();
        activity.getWindow().getDecorView().findViewsWithText(found, "Undo", View.FIND_VIEWS_WITH_TEXT);
        for (View view : found) {
            if (view instanceof TextView && "Undo".contentEquals(((TextView) view).getText()) && view.isShown()) {
                return view;
            }
        }
        throw new AssertionError("No visible Undo action");
    }

}
