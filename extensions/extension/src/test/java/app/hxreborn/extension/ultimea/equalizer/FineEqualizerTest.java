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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.android.controller.ActivityController;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class FineEqualizerTest {

    @Test
    @Config(sdk = { 23, 33 }, qualifiers = "night-mdpi", shadows = { ConnectionShadow.class, AppliedFlagsShadow.class,
            EqualizerPresetTransferTest.BasesShadow.class })
    public void fillsEditorWindowAndKeepsCloseRowCompact() {
        try (ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            activity.setContentView(new FrameLayout(activity));
            ConnectionShadow.current = new SoundState(new EqualizerCurve(7, new int[10]), 0);
            FineEqualizer.attach(activity);
            ViewGroup content = activity.findViewById(android.R.id.content);
            ViewGroup entries = (ViewGroup) ((ViewGroup) content.getChildAt(0)).getChildAt(1);
            entries.getChildAt(0).performClick();
            android.app.Dialog editor = org.robolectric.shadows.ShadowDialog.getLatestDialog();
            ViewGroup root = editor.findViewById(android.R.id.content);
            for (int height : new int[] { 480, 800 }) {
                root.measure(View.MeasureSpec.makeMeasureSpec(320, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
                root.layout(0, 0, 320, height);
                android.widget.Button close = button(root, "Close");
                View footer = (View) close.getParent();
                Assert.assertEquals(root.getHeight(), footer.getBottom());
                Assert.assertEquals(close.getMeasuredHeight(), footer.getMeasuredHeight());
                Assert.assertEquals("Fine equalizer",
                        ((android.widget.TextView) ((ViewGroup) root.getChildAt(0)).getChildAt(0)).getText()
                            .toString());
                Assert.assertNotNull(button(root, "Presets"));
                Assert.assertFalse(button(root, "Apply").isEnabled());
            }
            button(root, "Close").performClick();
            Assert.assertFalse(editor.isShowing());
        }
    }

    @Test
    public void retainsHostSizeThroughEqualizerWrapper() {
        try (ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            FrameLayout host = new FrameLayout(activity);
            activity.setContentView(host);
            FineEqualizer.attach(activity);
            ViewGroup content = activity.findViewById(android.R.id.content);
            ViewGroup wrapper = (ViewGroup) content.getChildAt(0);
            Assert.assertEquals(2, wrapper.getChildCount());
            Assert.assertSame(host, wrapper.getChildAt(0));
            ViewGroup entries = (ViewGroup) wrapper.getChildAt(1);
            Assert.assertEquals(2, entries.getChildCount());
            Assert.assertEquals("Fine equalizer", ((android.widget.Button) entries.getChildAt(0)).getText().toString());
            Assert.assertEquals("Remote controls",
                    ((android.widget.Button) entries.getChildAt(1)).getText().toString());
            FineEqualizer.navigation("flutter/navigation",
                    ByteBuffer.wrap("{\"method\":\"routeInformationUpdated\",\"args\":{\"uri\":\"/device-detail\"}}"
                        .getBytes(StandardCharsets.UTF_8)));
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
            Assert.assertEquals(View.VISIBLE, entries.getChildAt(0).getVisibility());
            Assert.assertEquals(View.VISIBLE, entries.getChildAt(1).getVisibility());
            content.measure(View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY));
            content.layout(0, 0, 1080, 1920);
            Assert.assertTrue(entries.getMeasuredHeight() > 0);
            Assert.assertEquals(1920 - entries.getMeasuredHeight(), host.getMeasuredHeight());
            Assert.assertEquals(1080, host.getMeasuredWidth());
            FineEqualizer.navigation("flutter/navigation",
                    ByteBuffer.wrap("{\"method\":\"routeInformationUpdated\",\"args\":{\"uri\":\"/profile\"}}"
                        .getBytes(StandardCharsets.UTF_8)));
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
            Assert.assertEquals(View.GONE, entries.getChildAt(0).getVisibility());
            Assert.assertEquals(View.GONE, entries.getChildAt(1).getVisibility());
            content.measure(View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY));
            content.layout(0, 0, 1080, 1920);
            Assert.assertEquals(1080, host.getMeasuredWidth());
            Assert.assertEquals(1920, host.getMeasuredHeight());
            Assert.assertSame(content, host.getRootView().findViewById(android.R.id.content));
        }
    }

    @Test
    public void ignoresMalformedNavigation() {
        try (ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            activity.setContentView(new FrameLayout(activity));
            FineEqualizer.attach(activity);
            FineEqualizer.navigation("flutter/navigation",
                    ByteBuffer.wrap("{\"method\":\"routeInformationUpdated\",\"args\":{\"uri\":\"/device-detail\"}}"
                        .getBytes(StandardCharsets.UTF_8)));
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
            ViewGroup content = activity.findViewById(android.R.id.content);
            ViewGroup entries = (ViewGroup) ((ViewGroup) content.getChildAt(0)).getChildAt(1);
            FineEqualizer.navigation("other",
                    ByteBuffer.wrap("{\"method\":\"routeInformationUpdated\",\"args\":{\"uri\":\"/profile\"}}"
                        .getBytes(StandardCharsets.UTF_8)));
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
            Assert.assertEquals(View.VISIBLE, entries.getChildAt(0).getVisibility());
            Assert.assertEquals(View.VISIBLE, entries.getChildAt(1).getVisibility());
            for (int invalid = 0; invalid < 3; invalid++) {
                FineEqualizer.navigation(invalid == 1 ? "other" : "flutter/navigation",
                        invalid == 2 ? null : ByteBuffer.wrap("{".getBytes(StandardCharsets.UTF_8)));
                org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
                Assert.assertEquals(View.VISIBLE, entries.getChildAt(0).getVisibility());
                Assert.assertEquals(View.VISIBLE, entries.getChildAt(1).getVisibility());
            }
        }
    }

    @Test
    @Config(shadows = { ConnectionShadow.class, AppliedFlagsShadow.class,
            EqualizerPresetTransferTest.BasesShadow.class })
    public void appliesXupmixOnlyPresetAndRetainsResetBase() throws Exception {
        try (ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            activity.setContentView(new FrameLayout(activity));
            activity.getSharedPreferences("hx_ultimea_equalizer_presets", 0).edit().clear().commit();
            EqualizerCurve curve = new EqualizerCurve(7, new int[10]);
            SoundState original = new SoundState(curve, 0);
            SoundState target = original.withXupmix(1);
            SoundState base = new SoundState(curve.withGain(2, 1), 0);
            EqualizerPresetStore store = new EqualizerPresetStore(activity);
            EqualizerPreset preset = store.add(EqualizerPresetFile
                .parse(EqualizerPresetFile.json(EqualizerPreset.create("Imported", target, "Night", base)).toString()));
            ConnectionShadow.current = original;
            ConnectionShadow.applies = 0;
            FineEqualizer.attach(activity);
            ViewGroup content = activity.findViewById(android.R.id.content);
            ViewGroup entries = (ViewGroup) ((ViewGroup) content.getChildAt(0)).getChildAt(1);
            entries.getChildAt(0).performClick();
            android.app.Dialog editor = org.robolectric.shadows.ShadowDialog.getLatestDialog();
            button(editor.getWindow().getDecorView(), "Presets").performClick();
            choose("Imported");
            choose("Use curve");
            android.widget.Button apply = button(editor.getWindow().getDecorView(), "Apply");
            Assert.assertTrue(apply.isEnabled());
            Assert.assertEquals(0, ConnectionShadow.applies);
            apply.performClick();
            Assert.assertEquals(1, ConnectionShadow.applies);
            Assert.assertEquals(target, ConnectionShadow.current);
            Assert.assertEquals(preset.id, store.current());
            Assert.assertEquals(base, store.load().get(0).base);
            EqualizerSession session = new EqualizerSession(activity, true, (state) -> {
            }, new EqualizerSessionConnection() {
                @Override
                public void read(SoundCallback callback) {
                    callback.completed(ConnectionShadow.current);
                }

                @Override
                public void apply(SoundState before, SoundState state, SoundCallback callback) {
                    ConnectionShadow.current = state;
                    callback.completed(state);
                }
            });
            session.read();
            Assert.assertEquals("Night", session.state().baseName);
            Assert.assertEquals(base, session.state().base);
            session.reset();
            Assert.assertEquals(base, ConnectionShadow.current);
            session.close();
            activity.getSharedPreferences("hx_ultimea_equalizer_presets", 0).edit().clear().commit();
        }
    }

    @Test
    @Config(shadows = { ConnectionShadow.class, AppliedFlagsShadow.class,
            EqualizerPresetTransferTest.BasesShadow.class })
    public void confirmsIdenticalPresetAndSavesResetBase() throws Exception {
        try (ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            activity.setContentView(new FrameLayout(activity));
            activity.getSharedPreferences("hx_ultimea_equalizer_presets", 0).edit().clear().commit();
            SoundState original = new SoundState(new EqualizerCurve(8, new int[10]), 0);
            SoundState base = original.withCurve(original.curve.withGain(2, 1));
            EqualizerPresetStore store = new EqualizerPresetStore(activity);
            EqualizerPreset preset = store.add(EqualizerPresetFile.parse(
                    EqualizerPresetFile.json(EqualizerPreset.create("Identical", original, "Night", base)).toString()));
            ConnectionShadow.current = original;
            ConnectionShadow.applies = 0;
            FineEqualizer.attach(activity);
            ViewGroup content = activity.findViewById(android.R.id.content);
            ((ViewGroup) ((ViewGroup) content.getChildAt(0)).getChildAt(1)).getChildAt(0).performClick();
            android.app.Dialog editor = org.robolectric.shadows.ShadowDialog.getLatestDialog();
            button(editor.getWindow().getDecorView(), "Presets").performClick();
            choose("Identical");
            choose("Use curve");
            android.widget.Button apply = button(editor.getWindow().getDecorView(), "Apply");
            Assert.assertTrue(apply.isEnabled());
            apply.performClick();
            Assert.assertEquals(original, ConnectionShadow.current);
            Assert.assertEquals(preset.id, store.current());
            Assert.assertEquals(base, store.load().get(0).base);
            Assert.assertFalse(apply.isEnabled());
            Assert.assertFalse(button(editor.getWindow().getDecorView(), "Undo").isEnabled());
            activity.getSharedPreferences("hx_ultimea_equalizer_presets", 0).edit().clear().commit();
        }
    }

    @Test
    @Config(shadows = ConnectionShadow.class)
    public void rejectsApplyAfterExternalStateChange() {
        rejectChangedConnectionOrState(false, false);
    }

    @Test
    @Config(shadows = ConnectionShadow.class)
    public void rejectsApplyAfterVerificationReadFails() {
        rejectChangedConnectionOrState(true, false);
    }

    @Test
    @Config(shadows = ConnectionShadow.class)
    public void rejectsApplyOnReplacementConnectionWithIdenticalSound() {
        rejectChangedConnectionOrState(false, true);
    }

    @Test
    @Config(shadows = { ConnectionShadow.class, AppliedFlagsShadow.class, EqualizerPresetTransferTest.BasesShadow.class,
            LocalPresetsTest.PollShadow.class })
    public void keepsEditorWhileImportRunsAcrossPause() throws Exception {
        checkImportLifetime(false);
    }

    @Test
    @Config(shadows = { ConnectionShadow.class, AppliedFlagsShadow.class, EqualizerPresetTransferTest.BasesShadow.class,
            LocalPresetsTest.PollShadow.class })
    public void preventsImportFromReopeningChooserAfterEditorCloses() throws Exception {
        checkImportLifetime(true);
    }

    @Test
    @Config(shadows = ConnectionShadow.class)
    public void rejectsUndoOnReplacementConnectionWithIdenticalSound() {
        try (ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            activity.setContentView(new FrameLayout(activity));
            ConnectionShadow.current = new SoundState(new EqualizerCurve(8, new int[10]), 0);
            ConnectionShadow.identity = new Object();
            ConnectionShadow.applies = 0;
            FineEqualizer.attach(activity);
            ViewGroup content = activity.findViewById(android.R.id.content);
            ((ViewGroup) ((ViewGroup) content.getChildAt(0)).getChildAt(1)).getChildAt(0).performClick();
            android.app.Dialog editor = org.robolectric.shadows.ShadowDialog.getLatestDialog();
            button(editor.getWindow().getDecorView(), "+").performClick();
            button(editor.getWindow().getDecorView(), "Apply").performClick();
            Assert.assertEquals(1, ConnectionShadow.applies);
            android.widget.Button undo = button(editor.getWindow().getDecorView(), "Undo");
            Assert.assertTrue(undo.isEnabled());
            ConnectionShadow.identity = new Object();
            undo.performClick();
            Assert.assertEquals(1, ConnectionShadow.applies);
            Assert.assertFalse(undo.isEnabled());
        }
    }

    private void checkImportLifetime(boolean closeEditor) throws Exception {
        java.util.concurrent.CountDownLatch release = new java.util.concurrent.CountDownLatch(1);
        try (ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            activity.setContentView(new FrameLayout(activity));
            android.content.SharedPreferences preferences = activity
                .getSharedPreferences("hx_ultimea_equalizer_presets", 0);
            preferences.edit().clear().commit();
            SoundState original = new SoundState(new EqualizerCurve(8, new int[10]), 0);
            ConnectionShadow.current = original;
            ConnectionShadow.identity = new Object();
            FineEqualizer.attach(activity);
            ViewGroup content = activity.findViewById(android.R.id.content);
            ((ViewGroup) ((ViewGroup) content.getChildAt(0)).getChildAt(1)).getChildAt(0).performClick();
            android.app.Dialog editor = org.robolectric.shadows.ShadowDialog.getLatestDialog();
            button(editor.getWindow().getDecorView(), "Presets").performClick();
            choose("Import");
            int request = org.robolectric.Shadows.shadowOf(activity).getNextStartedActivityForResult().requestCode;
            java.io.File file = java.io.File.createTempFile("preset-lifetime", ".json", activity.getCacheDir());
            try {
                try (java.io.FileOutputStream output = new java.io.FileOutputStream(file)) {
                    output.write(EqualizerPresetFile.json("Imported", original.curve.withGain(1, 1))
                        .toString()
                        .getBytes(StandardCharsets.UTF_8));
                }
                java.io.FileInputStream input = new java.io.FileInputStream(file);
                android.net.Uri uri = android.net.Uri.parse("content://presets/lifetime");
                org.robolectric.Shadows.shadowOf(activity.getContentResolver()).registerInputStream(uri, input);
                LocalPresetsTest.PollShadow.blocked.add(input.getFD());
                LocalPresetsTest.PollShadow.timeout = false;
                LocalPresetsTest.PollShadow.entered = new java.util.concurrent.CountDownLatch(1);
                LocalPresetsTest.PollShadow.release = release;
                LocalPresets.onActivityResult(activity, request, Activity.RESULT_OK,
                        new android.content.Intent().setData(uri));
                Assert.assertTrue(LocalPresetsTest.PollShadow.entered.await(2, java.util.concurrent.TimeUnit.SECONDS));
                if (closeEditor) {
                    button(editor.getWindow().getDecorView(), "Close").performClick();
                } else {
                    controller.pause();
                    Assert.assertTrue(editor.isShowing());
                    controller.resume();
                }
                release.countDown();
                EqualizerPresetStore store = new EqualizerPresetStore(activity);
                java.lang.reflect.Field executor = LocalPresets.class.getDeclaredField("FILES");
                executor.setAccessible(true);
                java.util.concurrent.ThreadPoolExecutor workers = (java.util.concurrent.ThreadPoolExecutor) executor
                    .get(null);
                long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(3);
                while ((store.load().isEmpty() || workers.getActiveCount() != 0) && System.nanoTime() < deadline) {
                    Thread.yield();
                }
                Assert.assertEquals(0, workers.getActiveCount());
                Assert.assertEquals(1, store.load().size());
                org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
                if (closeEditor) {
                    Assert.assertFalse(editor.isShowing());
                    Assert.assertFalse(LocalPresets.isOpen(activity));
                } else {
                    choose("Imported");
                    choose("Use curve");
                    Assert.assertTrue(editor.isShowing());
                    Assert.assertTrue(button(editor.getWindow().getDecorView(), "Apply").isEnabled());
                }
            } finally {
                release.countDown();
                Assert.assertTrue(file.delete());
                preferences.edit().clear().commit();
            }
        }
    }

    private void rejectChangedConnectionOrState(boolean failedRead, boolean replacement) {
        try (ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            activity.setContentView(new FrameLayout(activity));
            SoundState original = new SoundState(new EqualizerCurve(8, new int[10]), 0);
            ConnectionShadow.current = original;
            ConnectionShadow.identity = new Object();
            ConnectionShadow.applies = 0;
            FineEqualizer.attach(activity);
            ViewGroup content = activity.findViewById(android.R.id.content);
            ((ViewGroup) ((ViewGroup) content.getChildAt(0)).getChildAt(1)).getChildAt(0).performClick();
            android.app.Dialog editor = org.robolectric.shadows.ShadowDialog.getLatestDialog();
            button(editor.getWindow().getDecorView(), "+").performClick();
            if (replacement) {
                ConnectionShadow.identity = new Object();
            } else {
                ConnectionShadow.current = failedRead ? null : original.withXupmix(1);
            }
            button(editor.getWindow().getDecorView(), "Apply").performClick();
            Assert.assertEquals(0, ConnectionShadow.applies);
            Assert.assertFalse(button(editor.getWindow().getDecorView(), "Apply").isEnabled());
            Assert.assertFalse(button(editor.getWindow().getDecorView(), "Undo").isEnabled());
        }
    }

    private static android.widget.Button button(View view, String text) {
        if (view instanceof android.widget.Button && text.contentEquals(((android.widget.Button) view).getText())) {
            return (android.widget.Button) view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int index = 0; index < group.getChildCount(); index++) {
                android.widget.Button result = button(group.getChildAt(index), text);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    private static void choose(String title) {
        android.widget.ListView list = org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog().getListView();
        for (int index = 0; index < list.getAdapter().getCount(); index++) {
            if (title.equals(list.getAdapter().getItem(index).toString())) {
                list.performItemClick(null, index, list.getAdapter().getItemId(index));
                return;
            }
        }
        Assert.fail("Missing preset action: " + title);
    }

    @org.robolectric.annotation.Implements(value = EqualizerConnection.class, isInAndroidSdk = false)
    public static class ConnectionShadow {

        private static SoundState current;

        private static int applies;

        private static Object identity = new Object();

        @org.robolectric.annotation.Implementation
        protected static Object connection() {
            return identity;
        }

        @org.robolectric.annotation.Implementation
        protected static void readState(SoundCallback callback) {
            callback.completed(current);
        }

        @org.robolectric.annotation.Implementation
        protected static void applyState(SoundState before, SoundState target, SoundCallback callback) {
            applies++;
            current = target;
            callback.completed(target);
        }

    }

    @org.robolectric.annotation.Implements(value = app.hxreborn.extension.ultimea.AppliedPatches.class,
            isInAndroidSdk = false)
    public static class AppliedFlagsShadow {

        @org.robolectric.annotation.Implementation
        protected static boolean localEqualizerPresets() {
            return true;
        }

    }

}
