/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.bereal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.concurrent.atomic.AtomicInteger;

import android.content.Intent;
import app.morphe.extension.shared.Utils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;

@RunWith(RobolectricTestRunner.class)
public final class RetakeCountTest {

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
    public void sendsTheRealCountWhenNoPromptWasShown() {
        assertEquals(4, RetakeCount.retakeCount(4));
    }

    @Test
    public void sendsTheChosenCount() {
        RetakeCount.sendStarted(2);
        RetakeCount.choose(Integer.valueOf(7));

        assertEquals(7, RetakeCount.retakeCount(2));
    }

    @Test
    public void asksAgainForTheNextPost() {
        RetakeCount.sendStarted(2);
        RetakeCount.choose(Integer.valueOf(7));
        RetakeCount.retakeCount(2);

        assertEquals(3, RetakeCount.retakeCount(3));
    }

    @Test
    public void sendsTheRealCountWhenThePromptIsDismissed() {
        RetakeCount.sendStarted(2);
        RetakeCount.choose(null);

        assertEquals(2, RetakeCount.retakeCount(2));
    }

    @Test
    public void sendsZeroForANegativeChoice() {
        RetakeCount.sendStarted(2);
        RetakeCount.choose(Integer.valueOf(-3));

        assertEquals(0, RetakeCount.retakeCount(2));
    }

    @Test
    public void sendsTheRealCountWhenTheSettingIsOff() {
        PatchSettings.CUSTOM_RETAKE_COUNT.save(false);
        RetakeCount.sendStarted(2);
        RetakeCount.choose(Integer.valueOf(7));

        assertEquals(2, RetakeCount.retakeCount(2));
    }

    @Test
    public void opensThePromptWhenAPostStarts() {
        RetakeCount.sendStarted(5);

        final Intent started = Shadows.shadowOf(RuntimeEnvironment.getApplication()).getNextStartedActivity();
        assertNotNull(started);
        assertEquals(RetakeCountActivity.class.getName(), started.getComponent().getClassName());
        assertEquals(5, RetakeCount.realCount());
    }

    @Test
    public void staysSilentWhenTheSettingIsOff() {
        PatchSettings.CUSTOM_RETAKE_COUNT.save(false);

        RetakeCount.sendStarted(5);

        assertNull(Shadows.shadowOf(RuntimeEnvironment.getApplication()).getNextStartedActivity());
    }

    @Test
    public void waitsOffTheMainThreadForTheChoice() throws InterruptedException {
        RetakeCount.sendStarted(2);
        final AtomicInteger sent = new AtomicInteger(-1);
        final Thread upload = new Thread(() -> sent.set(RetakeCount.retakeCount(2)));
        upload.start();

        RetakeCount.choose(Integer.valueOf(9));
        upload.join(5000);

        assertEquals(9, sent.get());
    }

}
