/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.bereal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.util.ArrayList;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import app.hxreborn.extension.bereal.download.DownloadFolder;
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

    private static final Uri PICKED_FOLDER = Uri
        .parse("content://com.android.externalstorage.documents/tree/primary%3APictures%2FBeReal");

    @Before
    public void enableGallery() {
        Utils.setContext(RuntimeEnvironment.getApplication());
        PatchSettings.GALLERY_PHOTOS.save(true);
    }

    @After
    public void restoreDefaults() {
        PatchSettings.GALLERY_PHOTOS.save(false);
        PatchSettings.DOWNLOAD_FOLDER.resetToDefault();
    }

    @Test
    public void releasesThePickedFolderWhenAPresetIsChosen() {
        // given
        final PatchesSettingsActivity activity = Robolectric.buildActivity(PatchesSettingsActivity.class).setup().get();
        DownloadFolder.choose(resolver(), PICKED_FOLDER);

        // when
        activity.useDownloadPreset(DownloadFolder.PICTURES_FOLDER);

        // then
        assertEquals(0, resolver().getPersistedUriPermissions().size());
    }

    @Test
    public void undoKeepsAccessToTheFolderFromBeforeTheReset() {
        // given
        final PatchesSettingsActivity activity = Robolectric.buildActivity(PatchesSettingsActivity.class).setup().get();
        DownloadFolder.choose(resolver(), PICKED_FOLDER);
        activity.resetToDefaults();
        DownloadFolder.choose(resolver(), PICKED_FOLDER);
        activity.useDownloadPreset(DownloadFolder.DCIM_FOLDER);

        // when
        undo(activity).performClick();

        // then
        assertEquals(1, resolver().getPersistedUriPermissions().size());
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

    @Test
    public void releasesTheFolderFromBeforeTheResetWhenTheActivityIsRestoredAfterTheWindow() {
        // given
        final ActivityController<PatchesSettingsActivity> controller = Robolectric
            .buildActivity(PatchesSettingsActivity.class)
            .setup();
        DownloadFolder.choose(resolver(), PICKED_FOLDER);
        controller.get().resetToDefaults();
        final Bundle state = new Bundle();
        controller.saveInstanceState(state);
        controller.pause().stop().destroy();
        ShadowSystemClock.advanceBy(Duration.ofSeconds(6));

        // when
        Robolectric.buildActivity(PatchesSettingsActivity.class).setup(state);

        // then
        assertEquals(0, resolver().getPersistedUriPermissions().size());
    }

    private static ContentResolver resolver() {
        return RuntimeEnvironment.getApplication().getContentResolver();
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
