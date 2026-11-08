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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import org.json.JSONException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23, shadows = EqualizerSessionTest.BasesShadow.class)
public class EqualizerSessionTest {

    private static final String PREFERENCES = "hx_ultimea_equalizer_presets";

    private static final int BAND = 5;

    private static final int[] MOVIE_GAINS = { 30, 40, 20, 10, 0, 10, 10, 0, -20, -20 };

    private Context context;

    private EqualizerPresetStore store;

    private FakeConnection connection;

    private EqualizerSession session;

    private final List<EqualizerSession.State> snapshots = new ArrayList<>();

    @Before
    public void createSession() {
        this.context = RuntimeEnvironment.getApplication();
        this.context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit().clear().commit();
        this.store = new EqualizerPresetStore(this.context);
        this.connection = new FakeConnection(movie());
        this.session = session(true);
    }

    @After
    public void closeSession() {
        this.session.close();
        this.context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit().clear().commit();
    }

    @Test
    public void readsWithoutChangingOrSavingSound() throws JSONException {
        Assert.assertTrue(this.snapshots.isEmpty());
        Assert.assertNull(this.session.state().confirmed);
        read();
        Assert.assertEquals(movie(), this.session.state().confirmed);
        Assert.assertEquals("Movie", this.session.state().baseName);
        Assert.assertTrue(this.session.state().canEdit);
        Assert.assertEquals(0, this.connection.applied);
        Assert.assertTrue(this.store.load().isEmpty());
    }

    @Test
    public void selectsEveryStockStyleAndKeepsXupmix() throws JSONException {
        read();
        for (EqualizerStyles.Point point : EqualizerStyles.points()) {
            this.session.selectStyle(point.row, point.column);
            this.connection.settle();
            Assert.assertEquals(point.curve, this.session.state().confirmed.curve);
            Assert.assertEquals(1, this.session.state().confirmed.xupmix);
            Assert.assertNull(this.session.state().selected);
            Assert.assertNull(this.store.current());
        }
        Assert.assertTrue(this.store.load().isEmpty());
    }

    @Test
    public void editsNamedStyleIntoPresetAndResetsToWholeStyle() throws JSONException {
        read();
        this.session.selectStyle(2, 2);
        this.connection.settle();
        SoundState bass = this.session.state().confirmed;
        this.session.step(BAND, 1);
        this.connection.settle();
        Assert.assertEquals("Bass custom", this.session.state().selected.name);
        Assert.assertEquals(bass, this.session.state().selected.base);
        Assert.assertEquals(7, this.session.state().confirmed.curve.mode());
        this.session.reset();
        this.connection.settle();
        Assert.assertEquals(bass, this.session.state().confirmed);
        Assert.assertEquals(8, this.store.load().get(0).state.curve.mode());
        this.session.undo();
        this.connection.settle();
        Assert.assertEquals(bass.curve.gain(BAND) + 1, this.session.state().confirmed.curve.gain(BAND));
    }

    @Test
    public void rejectsStyleOutsideGridWithoutWriting() {
        read();
        this.session.selectStyle(-1, 0);
        Assert.assertEquals(0, this.connection.applied);
        Assert.assertNotNull(this.session.state().error);
        Assert.assertEquals(movie(), this.session.state().confirmed);
    }

    @Test
    public void identifiesNamedStyleOnFreshRead() {
        this.connection.sound = new SoundState(EqualizerStyles.at(8, 8).curve, 0);
        read();
        Assert.assertEquals("Classical", this.session.state().baseName);
        Assert.assertEquals(0, this.session.state().confirmed.xupmix);
    }

    @Test
    public void coalescesRepeatedReads() {
        this.session.read();
        this.session.read();
        Assert.assertEquals(1, this.connection.reads.size());
        this.connection.replyRead();
        Assert.assertTrue(this.session.state().canEdit);
        Assert.assertFalse(this.session.state().reading);
        Assert.assertNull(this.session.state().error);
    }

    @Test
    public void savesOnlyAfterExactDeviceConfirmation() throws JSONException {
        read();
        this.session.step(BAND, 1);
        Assert.assertTrue(this.store.load().isEmpty());
        Assert.assertNull(this.session.state().selected);
        this.connection.replyRead();
        Assert.assertTrue(this.store.load().isEmpty());
        this.connection.finishApply();
        EqualizerPreset preset = this.store.load().get(0);
        Assert.assertEquals("Movie custom", preset.name);
        Assert.assertEquals(11, preset.state.curve.gain(BAND));
        Assert.assertEquals(7, preset.state.curve.mode());
        Assert.assertEquals(1, preset.state.xupmix);
        Assert.assertEquals(movie(), preset.base);
        Assert.assertEquals(preset.id, this.store.current());
        Assert.assertEquals(preset.id, this.session.state().selected.id);
    }

    @Test
    public void coalescesGainChangesIntoOneSavedPreset() throws JSONException {
        read();
        this.session.beginEdit();
        this.session.setGain(BAND, 11);
        this.session.setGain(BAND, 12);
        this.session.setGain(BAND, 13);
        Assert.assertEquals(1, this.connection.reads.size());
        this.connection.settle();
        Assert.assertEquals(2, this.connection.applied);
        Assert.assertEquals(1, this.store.load().size());
        Assert.assertEquals(13, this.store.load().get(0).state.curve.gain(BAND));
        Assert.assertEquals(13, this.session.state().confirmed.curve.gain(BAND));
        this.session.undo();
        this.connection.settle();
        Assert.assertEquals(movie(), this.session.state().confirmed);
        Assert.assertEquals(movie(), this.store.load().get(0).state);
    }

    @Test
    public void rejectsSelectionDuringPendingWriteAndKeepsCoalescingGains() throws Exception {
        EqualizerPreset other = this.store.add("Other", movie().curve.withGain(BAND, 20));
        read();
        this.session.step(BAND, 1);
        this.session.selectBase(2);
        this.session.selectPreset(other.id);
        Assert.assertEquals("Wait for the soundbar update to finish", this.session.state().error);
        Assert.assertEquals(11, this.session.state().desired.curve.gain(BAND));
        this.session.setGain(BAND, 12);
        this.connection.settle();
        Assert.assertEquals("Movie custom", this.session.state().selected.name);
        Assert.assertEquals(12, this.session.state().confirmed.curve.gain(BAND));
        Assert.assertEquals(2, this.store.load().size());
    }

    @Test
    public void restoresConfirmedSelectionAfterFailedRecall() throws Exception {
        read();
        this.session.step(BAND, 1);
        this.connection.settle();
        String current = this.store.current();
        SoundState original = this.connection.sound;
        EqualizerPreset other = this.store.add("Other", movie().curve.withGain(BAND, 20));
        this.session.selectPreset(other.id);
        Assert.assertEquals(current, this.store.current());
        Assert.assertEquals(current, this.session.state().selected.id);
        this.connection.replyRead();
        this.connection.failApply();
        Assert.assertEquals(current, this.store.current());
        Assert.assertEquals(current, this.session.state().selected.id);
        Assert.assertEquals(original, this.session.state().desired);
        Assert.assertFalse(this.session.state().canEdit);
        Assert.assertEquals("Change could not be saved. Reconnect, then retry.", this.session.state().error);
        read();
        Assert.assertTrue(this.session.state().canEdit);
        Assert.assertEquals(current, this.session.state().selected.id);
    }

    @Test
    public void confirmsSameSoundBeforeChangingCurrentPreset() throws Exception {
        read();
        EqualizerPreset same = this.store.add(EqualizerPresetFile.parse(
                EqualizerPresetFile.json(EqualizerPreset.create("Same movie", movie(), "Movie", movie())).toString()));
        this.session.selectPreset(same.id);
        Assert.assertNull(this.store.current());
        Assert.assertNull(this.session.state().selected);
        this.connection.replyRead();
        Assert.assertNull(this.store.current());
        this.connection.finishApply();
        Assert.assertEquals(same.id, this.store.current());
        Assert.assertEquals(same.id, this.session.state().selected.id);
    }

    @Test
    public void rereadsAfterQueuedEditsFinish() {
        read();
        this.session.step(BAND, 1);
        this.session.setGain(BAND, 12);
        this.session.read();
        this.connection.settle();
        Assert.assertEquals(2, this.connection.applied);
        Assert.assertEquals(12, this.session.state().confirmed.curve.gain(BAND));
        Assert.assertFalse(this.session.state().reading);
        Assert.assertFalse(this.session.state().busy);
        Assert.assertNull(this.session.state().error);
    }

    @Test
    public void stopsBeforeSetterWhenClosedDuringComparisonRead() throws JSONException {
        read();
        this.session.step(BAND, 1);
        int notifications = this.snapshots.size();
        this.session.close();
        this.connection.replyRead();
        Assert.assertEquals(0, this.connection.applied);
        Assert.assertTrue(this.store.load().isEmpty());
        Assert.assertEquals(notifications, this.snapshots.size());
    }

    @Test
    public void savesDispatchedChangeAfterCloseAndDropsQueuedChange() throws JSONException {
        read();
        this.session.step(BAND, 1);
        this.connection.replyRead();
        this.session.setGain(BAND, 12);
        int notifications = this.snapshots.size();
        this.session.close();
        this.connection.finishApply();
        Assert.assertEquals(1, this.connection.applied);
        Assert.assertEquals(11, this.store.load().get(0).state.curve.gain(BAND));
        Assert.assertTrue(this.connection.reads.isEmpty());
        Assert.assertEquals(notifications, this.snapshots.size());
    }

    @Test
    public void editsWithoutPersistingWhenPresetsAreDisabled() throws JSONException {
        this.session.close();
        this.session = session(false);
        read();
        this.session.step(BAND, 1);
        this.connection.settle();
        Assert.assertEquals(11, this.session.state().confirmed.curve.gain(BAND));
        Assert.assertNull(this.session.state().selected);
        Assert.assertTrue(this.store.load().isEmpty());
        Assert.assertNull(this.store.current());
    }

    @Test
    public void resetsAndUndoesExactModeGainsAndXupmix() {
        read();
        this.session.setXupmix(false);
        this.connection.settle();
        Assert.assertEquals(1, this.session.state().confirmed.curve.mode());
        Assert.assertEquals(0, this.session.state().confirmed.xupmix);
        Assert.assertTrue(this.session.state().canReset);
        this.session.reset();
        this.connection.settle();
        Assert.assertEquals(movie(), this.session.state().confirmed);
        this.session.undo();
        this.connection.settle();
        Assert.assertEquals(1, this.session.state().confirmed.curve.mode());
        Assert.assertEquals(0, this.session.state().confirmed.xupmix);
    }

    @Test
    public void createsMoviePresetOnlyAfterFirstConfirmedEdit() throws JSONException {
        this.connection.sound = new SoundState(new EqualizerCurve(2, new int[10]), 0);
        read();
        this.session.create();
        this.connection.settle();
        Assert.assertEquals(1, this.session.state().confirmed.curve.mode());
        Assert.assertEquals(0, this.session.state().confirmed.xupmix);
        Assert.assertTrue(this.store.load().isEmpty());
        this.session.step(BAND, -1);
        this.connection.settle();
        Assert.assertEquals("Movie custom", this.session.state().selected.name);
        Assert.assertEquals(9, this.session.state().confirmed.curve.gain(BAND));
    }

    @Test
    public void rejectsInvalidGainsWithoutDeviceOperation() {
        read();
        this.session.setGain(BAND, 61);
        Assert.assertEquals("Equalizer gain exceeds slider range", this.session.state().error);
        this.session.step(BAND, Integer.MAX_VALUE);
        Assert.assertEquals(0, this.connection.applied);
        Assert.assertTrue(this.connection.reads.isEmpty());
        Assert.assertEquals(movie(), this.session.state().confirmed);
    }

    @Test
    public void renamesAndDeletesWithoutChangingSound() throws JSONException {
        read();
        this.session.step(BAND, 1);
        this.connection.settle();
        EqualizerSession.State original = this.session.state();
        int applies = this.connection.applied;
        this.session.rename("Dialogue");
        Assert.assertEquals("Dialogue", this.session.state().selected.name);
        Assert.assertEquals("Movie custom", original.selected.name);
        Assert.assertEquals(original.confirmed, this.session.state().confirmed);
        this.session.delete();
        Assert.assertNull(this.session.state().selected);
        Assert.assertTrue(this.store.load().isEmpty());
        Assert.assertEquals(applies, this.connection.applied);
        Assert.assertEquals(original.confirmed, this.connection.sound);
    }

    @Test
    public void refreshesImportedPresetsWithoutChangingSound() throws Exception {
        read();
        EqualizerPreset imported = this.store.add("Imported", movie().curve.withGain(BAND, -1));
        this.session.refreshPresets();
        Assert.assertEquals(imported.id, this.session.state().presets.get(0).id);
        Assert.assertNull(this.session.state().selected);
        Assert.assertEquals(0, this.connection.applied);
    }

    @Test
    public void retainsStorageReadErrorAfterSuccessfulDeviceRead() {
        this.context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit().putString("presets", "{").commit();
        read();
        Assert.assertEquals(movie(), this.session.state().confirmed);
        Assert.assertEquals("Saved presets could not be read", this.session.state().error);
        Assert.assertFalse(this.session.state().canEdit);
    }

    @Test
    public void preservesUndoAfterUnchangedGesture() {
        read();
        this.session.step(BAND, 1);
        this.connection.settle();
        int applies = this.connection.applied;
        this.session.beginEdit();
        this.session.setGain(BAND, 11);
        Assert.assertTrue(this.session.state().canUndo);
        Assert.assertEquals(applies, this.connection.applied);
        this.session.undo();
        this.connection.settle();
        Assert.assertEquals(movie(), this.session.state().confirmed);
    }

    @Test
    public void preservesUndoAfterRejectedBoundaryStep() {
        read();
        this.session.setGain(BAND, EqualizerCurve.MAX_GAIN_TENTHS);
        this.connection.settle();
        int applies = this.connection.applied;
        this.session.step(BAND, 1);
        Assert.assertTrue(this.session.state().canUndo);
        Assert.assertEquals(applies, this.connection.applied);
        this.session.undo();
        this.connection.settle();
        Assert.assertEquals(movie(), this.session.state().confirmed);
    }

    @Test
    public void recordsNewUndoBoundaryOnFirstChangedGestureValue() {
        read();
        this.session.step(BAND, 1);
        this.connection.settle();
        SoundState previous = this.session.state().confirmed;
        this.session.beginEdit();
        this.session.setGain(BAND, 11);
        this.session.setGain(BAND, 12);
        this.connection.settle();
        this.session.undo();
        this.connection.settle();
        Assert.assertEquals(previous, this.session.state().confirmed);
    }

    @Test
    public void keepsMovieUndoBoundaryForGainQueuedDuringBaseSelection() throws JSONException {
        this.connection.sound = new SoundState(new EqualizerCurve(2, new int[10]), 0);
        read();
        this.session.create();
        this.session.beginEdit();
        this.session.setGain(BAND, 11);
        this.connection.settle();
        Assert.assertEquals(1, this.store.load().size());
        Assert.assertEquals("Movie custom", this.session.state().selected.name);
        this.session.undo();
        this.connection.settle();
        Assert.assertEquals(movie().withXupmix(0), this.session.state().confirmed);
    }

    @Test
    public void rejectsMalformedActionsAndBoundarySteps() {
        read();
        this.session.selectBase(Integer.MIN_VALUE);
        this.session.selectPreset(null);
        this.session.setGain(-1, 0);
        this.session.step(EqualizerCurve.BAND_COUNT, 1);
        Assert.assertEquals(0, this.connection.applied);
        Assert.assertTrue(this.connection.reads.isEmpty());
        this.session.setGain(BAND, EqualizerCurve.MAX_GAIN_TENTHS);
        this.connection.settle();
        int applies = this.connection.applied;
        this.session.step(BAND, 1);
        this.session.rename(null);
        Assert.assertEquals(applies, this.connection.applied);
        Assert.assertEquals(EqualizerCurve.MAX_GAIN_TENTHS, this.session.state().confirmed.curve.gain(BAND));
        Assert.assertEquals("Use a name between 1 and 80 characters", this.session.state().error);
    }

    @Test
    public void retainsConfirmedHardwareStateWhenPresetLimitBlocksAutosave() throws Exception {
        for (int index = 0; index < 64; index++) {
            this.store.add("Preset " + index, movie().curve.asCustom());
        }
        read();
        this.session.step(BAND, 1);
        this.connection.settle();
        Assert.assertEquals(11, this.session.state().confirmed.curve.gain(BAND));
        Assert.assertEquals(64, this.store.load().size());
        Assert.assertNull(this.session.state().selected);
        Assert.assertFalse(this.session.state().canEdit);
        Assert.assertTrue(this.session.state().error.contains("Local preset limit reached"));
    }

    @Test
    public void rejectsChangedDeviceBeforeSendingSetter() {
        read();
        this.session.step(BAND, 1);
        this.connection.sound = movie().withXupmix(0);
        this.connection.replyRead();
        Assert.assertEquals(0, this.connection.applied);
        Assert.assertFalse(this.session.state().canEdit);
        Assert.assertEquals(movie(), this.session.state().confirmed);
    }

    @Test
    public void ignoresInitialReadAfterClose() {
        this.session.read();
        int notifications = this.snapshots.size();
        this.session.close();
        this.connection.replyRead();
        Assert.assertNull(this.session.state().confirmed);
        Assert.assertEquals(notifications, this.snapshots.size());
    }

    private EqualizerSession session(boolean presetsEnabled) {
        return new EqualizerSession(this.context, presetsEnabled, this.snapshots::add, this.connection);
    }

    private void read() {
        this.session.read();
        this.connection.replyRead();
    }

    private static SoundState movie() {
        return new SoundState(new EqualizerCurve(1, MOVIE_GAINS), 1);
    }

    @Implements(EqualizerBases.class)
    public static class BasesShadow {

        @Implementation
        protected static String definitions() {
            try (InputStream input = EqualizerSessionTest.class.getResourceAsStream("/ultimea/equalizer/styles.json")) {
                if (input == null) {
                    throw new IllegalStateException("Missing stock style fixture");
                }
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    output.write(buffer, 0, count);
                }
                return new String(output.toByteArray(), StandardCharsets.UTF_8);
            } catch (IOException ex) {
                throw new IllegalStateException("Stock style fixture could not be read", ex);
            }
        }

        @Implementation
        protected static boolean available() {
            return true;
        }

        @Implementation
        protected static EqualizerBases.Entry mode(int mode) {
            if (mode == 1) {
                return new EqualizerBases.Entry("Movie", movie().curve);
            }
            if (mode == 2) {
                return new EqualizerBases.Entry("Music", new EqualizerCurve(2, new int[10]));
            }
            return null;
        }

    }

    private static final class FakeConnection implements EqualizerSessionConnection {

        private final ArrayDeque<SoundCallback> reads = new ArrayDeque<>();

        private final ArrayDeque<PendingApply> applies = new ArrayDeque<>();

        private SoundState sound;

        private int applied;

        FakeConnection(SoundState initial) {
            this.sound = initial;
        }

        @Override
        public void read(SoundCallback callback) {
            this.reads.add(callback);
        }

        @Override
        public void apply(SoundState before, SoundState target, SoundCallback callback) {
            Assert.assertEquals(this.sound, before);
            this.applied++;
            this.applies.add(new PendingApply(target, callback));
        }

        void replyRead() {
            Assert.assertFalse(this.reads.isEmpty());
            this.reads.remove().completed(this.sound);
        }

        void finishApply() {
            PendingApply apply = this.applies.remove();
            this.sound = apply.target;
            apply.callback.completed(this.sound);
        }

        void failApply() {
            this.applies.remove().callback.completed(null);
        }

        void settle() {
            int remaining = 20;
            while (!this.reads.isEmpty() || !this.applies.isEmpty()) {
                Assert.assertTrue("Device operations did not settle", remaining-- > 0);
                if (!this.reads.isEmpty()) {
                    replyRead();
                } else {
                    finishApply();
                }
            }
        }

    }

    private static final class PendingApply {

        private final SoundState target;

        private final SoundCallback callback;

        PendingApply(SoundState target, SoundCallback callback) {
            this.target = target;
            this.callback = callback;
        }

    }

}
