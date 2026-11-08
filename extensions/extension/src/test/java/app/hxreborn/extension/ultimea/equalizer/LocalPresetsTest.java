/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Additional term under GPLv3 Section 7(b): When conveying this material
 * or modified versions, preserve this notice and credit hxreborn for this
 * material in any Appropriate Legal Notices displayed by works containing it.
 */
package app.hxreborn.extension.ultimea.equalizer;

import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Looper;
import android.widget.ListAdapter;
import android.widget.ListView;
import org.json.JSONException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowAlertDialog;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, shadows = LocalPresetsTest.PollShadow.class)
public class LocalPresetsTest {

    private ActivityController<Activity> controller;

    private Activity activity;

    private EqualizerPresetStore store;

    private final EqualizerCurve curve = new EqualizerCurve(8, new int[10]);

    @Before
    public void createActivity() {
        this.controller = Robolectric.buildActivity(Activity.class).setup();
        this.activity = this.controller.get();
        this.store = new EqualizerPresetStore(this.activity);
        this.activity.getSharedPreferences("hx_ultimea_equalizer_presets", 0).edit().clear().commit();
    }

    @After
    public void destroyActivity() {
        this.controller.pause().stop().destroy();
        this.activity.getSharedPreferences("hx_ultimea_equalizer_presets", 0).edit().clear().commit();
    }

    @Test
    public void releasesBothTimedOutReadsAndImportsAfterActivityRecreation() throws Exception {
        android.net.Uri stalled = android.net.Uri.parse("content://presets/stalled");
        java.io.File file = java.io.File.createTempFile("preset-timeout", ".json", this.activity.getCacheDir());
        java.util.concurrent.CountDownLatch release = new java.util.concurrent.CountDownLatch(1);
        java.lang.reflect.Field executor = LocalPresets.class.getDeclaredField("FILES");
        executor.setAccessible(true);
        java.util.concurrent.ThreadPoolExecutor workers = (java.util.concurrent.ThreadPoolExecutor) executor.get(null);
        try (java.io.FileInputStream first = new java.io.FileInputStream(file);
                java.io.FileInputStream second = new java.io.FileInputStream(file)) {
            PollShadow.blocked.add(first.getFD());
            PollShadow.blocked.add(second.getFD());
            PollShadow.entered = new java.util.concurrent.CountDownLatch(2);
            PollShadow.release = release;
            LocalPresets.open(this.activity, this.curve, (preset) -> {
            });
            choose("Import");
            int request = Shadows.shadowOf(this.activity).getNextStartedActivityForResult().requestCode;
            for (java.io.FileInputStream input : new java.io.FileInputStream[] { first, second }) {
                Shadows.shadowOf(this.activity.getContentResolver()).registerInputStream(stalled, input);
                LocalPresets.onActivityResult(this.activity, request, Activity.RESULT_OK,
                        new android.content.Intent().setData(stalled));
                long enteredDeadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(2);
                while (PollShadow.entered.getCount() > (input == first ? 1 : 0)
                        && System.nanoTime() < enteredDeadline) {
                    Thread.yield();
                }
            }
            Assert.assertEquals(0, PollShadow.entered.getCount());
            LocalPresets.onActivityResult(this.activity, request, Activity.RESULT_OK,
                    new android.content.Intent().setData(stalled));
            Assert.assertEquals("Too many preset transfers. Try again when one finishes.",
                    org.robolectric.shadows.ShadowToast.getTextOfLatestToast());
            release.countDown();
            long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(3);
            while (workers.getActiveCount() != 0 && System.nanoTime() < deadline) {
                Thread.yield();
            }
            Assert.assertEquals(0, workers.getActiveCount());
            Shadows.shadowOf(Looper.getMainLooper()).idle();
            Assert.assertEquals("Import failed. Check local presets.",
                    org.robolectric.shadows.ShadowToast.getTextOfLatestToast());
            Assert.assertTrue(this.store.load().isEmpty());
            Assert.assertThrows(IOException.class, first::read);
            Assert.assertThrows(IOException.class, second::read);
            this.controller.pause().stop().destroy();
            this.controller = Robolectric.buildActivity(Activity.class).setup();
            this.activity = this.controller.get();
            try (java.io.FileOutputStream output = new java.io.FileOutputStream(file)) {
                output.write(EqualizerPresetFile.json("Retry", this.curve)
                    .toString()
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            LocalPresets.open(this.activity, this.curve, (preset) -> {
            });
            choose("Import");
            request = Shadows.shadowOf(this.activity).getNextStartedActivityForResult().requestCode;
            LocalPresets.onActivityResult(this.activity, request, Activity.RESULT_OK,
                    new android.content.Intent().setData(android.net.Uri.fromFile(file)));
            deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(3);
            while ((this.store.load().isEmpty() || workers.getActiveCount() != 0) && System.nanoTime() < deadline) {
                Thread.yield();
            }
            Assert.assertEquals("Retry", this.store.load().get(0).name);
        } finally {
            release.countDown();
            Assert.assertTrue(file.delete());
        }
    }

    @Test
    public void deleteSelectedPresetThroughConfirmation() throws JSONException, IOException {
        // given
        EqualizerPreset retained = this.store.add("Retain", this.curve);
        this.store.add("Delete target", this.curve.withGain(5, -1));
        LocalPresets.open(this.activity, this.curve, (curve) -> Assert.fail("Delete must not select a curve"));

        // when
        choose("Delete target");
        choose("Delete");
        AlertDialog confirmation = ShadowAlertDialog.getLatestAlertDialog();
        Assert.assertTrue(confirmation.isShowing());
        confirmation.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        // then
        List<EqualizerPreset> saved = this.store.load();
        Assert.assertEquals(1, saved.size());
        Assert.assertEquals(retained.id, saved.get(0).id);
        Assert.assertEquals(retained.curve, saved.get(0).curve);
    }

    @Test
    public void preservePresetAfterCancellingDelete() throws JSONException, IOException {
        // given
        EqualizerPreset original = this.store.add("Retain", this.curve);
        LocalPresets.open(this.activity, this.curve, (curve) -> Assert.fail("Cancel must not select a curve"));

        // when
        choose("Retain");
        choose("Delete");
        ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        // then
        List<EqualizerPreset> saved = this.store.load();
        Assert.assertEquals(1, saved.size());
        Assert.assertEquals(original.id, saved.get(0).id);
        Assert.assertEquals(original.curve, saved.get(0).curve);
    }

    private void choose(String title) {
        ListView list = ShadowAlertDialog.getLatestAlertDialog().getListView();
        ListAdapter adapter = list.getAdapter();
        for (int index = 0; index < adapter.getCount(); index++) {
            if (title.equals(adapter.getItem(index).toString())) {
                list.performItemClick(null, index, adapter.getItemId(index));
                return;
            }
        }
        Assert.fail("Missing preset action: " + title);
    }

    @org.robolectric.annotation.Implements(android.system.Os.class)
    public static class PollShadow {

        static final java.util.Set<java.io.FileDescriptor> blocked = java.util.Collections
            .synchronizedSet(new java.util.HashSet<>());

        static java.util.concurrent.CountDownLatch entered;

        static java.util.concurrent.CountDownLatch release;

        static boolean timeout = true;

        @org.robolectric.annotation.Implementation
        protected static int poll(android.system.StructPollfd[] descriptors, int timeoutMs) {
            Assert.assertTrue(timeoutMs > 0 && timeoutMs <= 30000);
            if (blocked.contains(descriptors[0].fd)) {
                entered.countDown();
                try {
                    Assert.assertTrue(release.await(5, java.util.concurrent.TimeUnit.SECONDS));
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(ex);
                }
                if (timeout) {
                    return 0;
                }
            }
            return 1;
        }

    }

}
