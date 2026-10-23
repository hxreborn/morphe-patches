/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.bereal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

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

@RunWith(RobolectricTestRunner.class)
public final class RetakeCountActivityTest {

    @Before
    public void attachContext() {
        Utils.setContext(RuntimeEnvironment.getApplication());
        PatchSettings.CUSTOM_RETAKE_COUNT.save(true);
        RetakeCount.retakeCount(0);
    }

    @After
    public void restoreDefaults() {
        RetakeCount.retakeCount(0);
        PatchSettings.CUSTOM_RETAKE_COUNT.save(false);
    }

    @Test
    public void startsAtTheRealCount() {
        RetakeCount.sendStarted(3);

        final RetakeCountActivity activity = open();

        assertTrue(hasExactText(activity, "3"));
        assertTrue(hasExactText(activity, "Real count: 3"));
    }

    @Test
    public void appliesTheSteppedCount() {
        RetakeCount.sendStarted(3);
        final RetakeCountActivity activity = open();

        click(describedAs(activity, "Increase"));
        click(describedAs(activity, "Increase"));
        click(texts(activity, "Apply").get(0));

        assertEquals(5, RetakeCount.retakeCount(3));
        assertTrue(activity.isFinishing());
    }

    @Test
    public void stopsAtZero() {
        RetakeCount.sendStarted(1);
        final RetakeCountActivity activity = open();

        click(describedAs(activity, "Decrease"));
        click(describedAs(activity, "Decrease"));
        click(texts(activity, "Apply").get(0));

        assertEquals(0, RetakeCount.retakeCount(1));
    }

    @Test
    public void keepsTheRealCountWhenAskedTo() {
        RetakeCount.sendStarted(3);
        final RetakeCountActivity activity = open();

        click(describedAs(activity, "Increase"));
        click(texts(activity, "Use real count").get(0));

        assertEquals(3, RetakeCount.retakeCount(3));
    }

    @Test
    public void keepsTheRealCountWhenDismissed() {
        RetakeCount.sendStarted(3);
        final ActivityController<RetakeCountActivity> controller = Robolectric.buildActivity(RetakeCountActivity.class);
        controller.create().start().resume();
        click(describedAs(controller.get(), "Increase"));

        controller.get().finish();
        controller.pause().stop().destroy();

        assertEquals(3, RetakeCount.retakeCount(3));
    }

    @Test
    public void givesTheTapTargetsAtLeast48dp() {
        RetakeCount.sendStarted(3);
        final RetakeCountActivity activity = open();
        final float density = activity.getResources().getDisplayMetrics().density;

        assertTrue(((TextView) describedAs(activity, "Increase")).getMinHeight() >= 48 * density);
        assertTrue(((TextView) describedAs(activity, "Decrease")).getMinWidth() >= 48 * density);
    }

    private static RetakeCountActivity open() {
        return Robolectric.buildActivity(RetakeCountActivity.class).setup().get();
    }

    private static View describedAs(RetakeCountActivity activity, String description) {
        final ArrayList<View> found = new ArrayList<>();
        activity.getWindow()
            .getDecorView()
            .findViewsWithText(found, description, View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION);
        return found.get(0);
    }

    private static ArrayList<View> texts(RetakeCountActivity activity, String text) {
        final ArrayList<View> found = new ArrayList<>();
        activity.getWindow().getDecorView().findViewsWithText(found, text, View.FIND_VIEWS_WITH_TEXT);
        return found;
    }

    private static boolean hasExactText(RetakeCountActivity activity, String text) {
        for (View view : texts(activity, text)) {
            if (view instanceof TextView && text.contentEquals(((TextView) view).getText())) {
                return true;
            }
        }
        return false;
    }

    private static void click(View view) {
        assertTrue(view instanceof TextView);
        view.performClick();
    }

}
