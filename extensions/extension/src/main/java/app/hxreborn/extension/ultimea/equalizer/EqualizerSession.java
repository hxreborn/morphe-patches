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
import java.util.Collections;
import java.util.List;

import android.content.Context;
import org.json.JSONException;

final class EqualizerSession {

    private static final int MOVIE_MODE = 1;

    private static final int LAST_BASE_MODE = 6;

    private final boolean presetsEnabled;

    private final EqualizerSessionListener listener;

    private final EqualizerPresetStore store;

    private final EqualizerSessionConnection connection;

    private List<EqualizerPreset> presets = Collections.emptyList();

    private SoundState confirmed;

    private SoundState desired;

    private SoundState undoState;

    private Selection confirmedSelection;

    private Selection desiredSelection;

    private Change active;

    private boolean reading;

    private boolean busy;

    private boolean closed;

    private boolean failed;

    private boolean readAfterChange;

    private boolean saveDesired;

    private boolean selectionPending;

    private boolean gesturePending;

    private int readGeneration;

    private String error;

    EqualizerSession(Context context, boolean presetsEnabled, EqualizerSessionListener listener) {
        this(context, presetsEnabled, listener, new EqualizerSessionConnection() {
            @Override
            public void read(SoundCallback callback) {
                EqualizerConnection.readState(callback);
            }

            @Override
            public void apply(SoundState before, SoundState target, SoundCallback callback) {
                EqualizerConnection.applyState(before, target, callback);
            }
        });
    }

    EqualizerSession(Context context, boolean presetsEnabled, EqualizerSessionListener listener,
            EqualizerSessionConnection connection) {
        this.presetsEnabled = presetsEnabled;
        this.listener = listener;
        this.connection = connection;
        this.store = new EqualizerPresetStore(context);
        EqualizerBases.configure(context.getApplicationContext());
    }

    State state() {
        return new State(this);
    }

    void read() {
        if (this.closed || this.reading) {
            return;
        }
        if (this.busy) {
            this.readAfterChange = true;
            return;
        }
        this.reading = true;
        this.readAfterChange = false;
        this.failed = false;
        this.error = null;
        this.undoState = null;
        this.gesturePending = false;
        final int generation = ++this.readGeneration;
        publish();
        this.connection.read((readback) -> {
            if (this.closed || generation != this.readGeneration || !this.reading) {
                return;
            }
            this.reading = false;
            if (readback == null) {
                this.failed = true;
                this.error = EqualizerBases.available() ? "Could not read soundbar. Reconnect, then retry."
                        : "Equalizer base configuration changed";
            } else {
                this.confirmed = readback;
                this.desired = readback;
                this.selectionPending = false;
                this.saveDesired = false;
                restoreSelection();
            }
            publish();
        });
    }

    void selectBase(int mode) {
        if (!canSelect()) {
            return;
        }
        EqualizerBases.Entry entry = (mode >= MOVIE_MODE && mode <= LAST_BASE_MODE) ? EqualizerBases.mode(mode) : null;
        if (entry == null) {
            report("Equalizer base is not available");
            return;
        }
        SoundState base = new SoundState(entry.curve, this.confirmed.xupmix);
        this.undoState = null;
        this.gesturePending = false;
        request(base, new Selection(null, entry.name, base), false);
    }

    void selectStyle(int row, int column) {
        if (!canSelect()) {
            return;
        }
        if (!EqualizerBases.available()) {
            report("Equalizer configuration changed");
            return;
        }
        try {
            EqualizerStyles.Point point = EqualizerStyles.at(row, column);
            SoundState base = new SoundState(point.curve, this.confirmed.xupmix);
            this.undoState = null;
            this.gesturePending = false;
            request(base, new Selection(null, (point.name != null) ? point.name : "Style", base), false);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            report("Equalizer style could not be selected");
        }
    }

    void create() {
        selectBase(MOVIE_MODE);
    }

    void selectPreset(String id) {
        if (!canSelect()) {
            return;
        }
        if (!this.presetsEnabled) {
            report("Local presets are disabled");
            return;
        }
        try {
            this.presets = this.store.load();
            for (EqualizerPreset preset : this.presets) {
                if (preset.id.equals(id)) {
                    this.undoState = null;
                    this.gesturePending = false;
                    SoundState base = resolve(preset.base);
                    request(resolve(preset.state), new Selection(preset, preset.baseName, base), false);
                    return;
                }
            }
            report("Preset no longer exists");
        } catch (JSONException | IllegalArgumentException ex) {
            report("Saved presets could not be read");
        }
    }

    void beginEdit() {
        if (!canEdit()) {
            return;
        }
        this.gesturePending = true;
    }

    private void recordUndoBoundary() {
        if (!this.gesturePending && this.undoState != null) {
            return;
        }
        this.gesturePending = false;
        if (!this.busy) {
            this.undoState = this.confirmed;
        } else if (this.undoState == null) {
            this.undoState = this.active.target;
        }
    }

    void setGain(int band, int tenths) {
        if (!canEdit()) {
            return;
        }
        if (band < 0 || band >= EqualizerCurve.BAND_COUNT) {
            report("Unknown equalizer band");
            return;
        }
        if (tenths == this.desired.curve.gain(band)) {
            return;
        }
        try {
            EqualizerCurve curve = this.desired.curve.withGain(band, tenths);
            recordUndoBoundary();
            request(this.desired.withCurve(curve), this.desiredSelection, true);
        } catch (IllegalArgumentException ex) {
            report(ex.getMessage());
        }
    }

    void step(int band, int delta) {
        if (!canEdit() || delta == 0) {
            return;
        }
        if (band < 0 || band >= EqualizerCurve.BAND_COUNT) {
            report("Unknown equalizer band");
            return;
        }
        long gain = (long) this.desired.curve.gain(band) + delta;
        if (gain < EqualizerCurve.MIN_GAIN_TENTHS || gain > EqualizerCurve.MAX_GAIN_TENTHS) {
            report("Equalizer gain exceeds slider range");
            return;
        }
        beginEdit();
        setGain(band, (int) gain);
    }

    void setXupmix(boolean enabled) {
        if (!canEdit()) {
            return;
        }
        int value = enabled ? 1 : 0;
        if (value == this.desired.xupmix) {
            return;
        }
        beginEdit();
        recordUndoBoundary();
        request(this.desired.withXupmix(value), this.desiredSelection, true);
    }

    void reset() {
        if (!canSelect() || this.confirmedSelection == null) {
            return;
        }
        if (this.confirmedSelection.base.equals(this.desired)) {
            return;
        }
        beginEdit();
        recordUndoBoundary();
        request(this.confirmedSelection.base, this.confirmedSelection, true);
    }

    void undo() {
        if (!canSelect() || this.undoState == null) {
            return;
        }
        SoundState target = this.undoState;
        this.undoState = this.confirmed;
        this.gesturePending = false;
        request(target, this.confirmedSelection, true);
    }

    void rename(String name) {
        if (!canSelect() || !this.presetsEnabled || this.confirmedSelection.preset == null) {
            return;
        }
        if (name == null) {
            report("Use a name between 1 and 80 characters");
            return;
        }
        try {
            this.store.rename(this.confirmedSelection.preset.id, name);
            this.error = null;
            reloadPresets();
            publish();
        } catch (IOException | JSONException | IllegalArgumentException ex) {
            report(message("Preset could not be renamed", ex));
        }
    }

    void delete() {
        if (!canSelect() || !this.presetsEnabled || this.confirmedSelection.preset == null) {
            return;
        }
        try {
            this.store.remove(this.confirmedSelection.preset.id);
            this.confirmedSelection = currentSound(this.confirmed);
            this.desiredSelection = this.confirmedSelection;
            this.error = null;
            this.presets = this.store.load();
            publish();
        } catch (IOException | JSONException | IllegalArgumentException ex) {
            report(message("Preset could not be deleted", ex));
        }
    }

    void refreshPresets() {
        if (this.closed || !this.presetsEnabled) {
            return;
        }
        if (this.busy || this.reading) {
            report("Wait for the soundbar update to finish");
            return;
        }
        try {
            this.error = null;
            reloadPresets();
            publish();
        } catch (JSONException | IllegalArgumentException ex) {
            report("Saved presets could not be read");
        }
    }

    void close() {
        this.closed = true;
        this.gesturePending = false;
        this.readGeneration++;
        this.reading = false;
        this.readAfterChange = false;
        if (this.active == null || !this.active.sent) {
            this.active = null;
            this.busy = false;
            this.desired = this.confirmed;
            this.desiredSelection = this.confirmedSelection;
        } else {
            this.desired = this.active.target;
            this.desiredSelection = this.active.selection;
        }
    }

    private void request(SoundState target, Selection selection, boolean save) {
        this.desired = target;
        this.desiredSelection = selection;
        this.saveDesired = save;
        this.selectionPending = !save;
        this.error = null;
        publish();
        send();
    }

    private void send() {
        if (this.closed || this.busy || this.failed || this.desired == null
                || this.desired.equals(this.confirmed) && !this.selectionPending) {
            return;
        }
        this.busy = true;
        Change change = new Change(this);
        this.active = change;
        publish();
        this.connection.read((latest) -> {
            if (this.active != change || this.closed) {
                return;
            }
            if (latest == null || !latest.equals(change.before)) {
                complete(change, null);
                return;
            }
            change.sent = true;
            this.connection.apply(change.before, change.target, (readback) -> complete(change, readback));
        });
    }

    private void complete(Change change, SoundState readback) {
        if (this.active != change) {
            return;
        }
        this.active = null;
        this.busy = false;
        this.selectionPending = false;
        if (readback == null || !readback.equals(change.target)) {
            this.failed = true;
            this.desired = this.confirmed;
            restoreSelection();
            if (this.error == null) {
                this.error = "Change could not be saved. Reconnect, then retry.";
            }
            publish();
            if (this.readAfterChange && !this.closed) {
                read();
            }
            return;
        }
        this.confirmed = readback;
        this.confirmedSelection = change.selection;
        try {
            saveConfirmed(change);
        } catch (IOException | JSONException | IllegalArgumentException ex) {
            this.failed = true;
            this.desired = readback;
            this.desiredSelection = this.confirmedSelection;
            this.error = message("Soundbar confirmed. Preset could not be saved", ex);
            publish();
            return;
        }
        this.error = null;
        publish();
        if (!this.closed) {
            send();
            if (!this.busy && this.readAfterChange) {
                read();
            }
        }
    }

    private void saveConfirmed(Change change) throws IOException, JSONException {
        if (!this.presetsEnabled) {
            return;
        }
        Selection selection = change.selection;
        if (change.save || selection.preset != null) {
            if (selection.preset == null) {
                selection.preset = this.store.createCustom(selection.baseName, selection.base, change.target);
            } else {
                EqualizerPreset preset = selection.preset;
                this.store.update(preset.id, change.target);
                selection.preset = new EqualizerPreset(preset.id, preset.name, change.target, preset.baseName,
                        preset.base);
            }
        }
        this.store.setCurrent((selection.preset != null) ? selection.preset.id : null);
        this.presets = this.store.load();
    }

    private void restoreSelection() {
        this.confirmedSelection = currentSound(this.confirmed);
        if (this.presetsEnabled) {
            try {
                this.presets = this.store.load();
                String current = this.store.current();
                for (EqualizerPreset preset : this.presets) {
                    if (preset.id.equals(current) && matches(preset.state, this.confirmed)) {
                        this.confirmedSelection = new Selection(preset, preset.baseName, resolve(preset.base));
                        break;
                    }
                }
            } catch (JSONException | IllegalArgumentException ex) {
                this.failed = true;
                this.error = "Saved presets could not be read";
            }
        }
        this.desiredSelection = this.confirmedSelection;
    }

    private void reloadPresets() throws JSONException {
        this.presets = this.store.load();
        if (this.confirmedSelection == null || this.confirmedSelection.preset == null) {
            return;
        }
        String id = this.confirmedSelection.preset.id;
        for (EqualizerPreset preset : this.presets) {
            if (preset.id.equals(id) && matches(preset.state, this.confirmed)) {
                this.confirmedSelection = new Selection(preset, preset.baseName, resolve(preset.base));
                this.desiredSelection = this.confirmedSelection;
                return;
            }
        }
        this.confirmedSelection = currentSound(this.confirmed);
        this.desiredSelection = this.confirmedSelection;
    }

    private SoundState resolve(SoundState state) {
        SoundState playable = EqualizerBases.playable(state);
        return (playable.xupmix < 0) ? playable.withXupmix(this.confirmed.xupmix) : playable;
    }

    private static boolean matches(SoundState preset, SoundState state) {
        return preset.curve.equals(state.curve) && (preset.xupmix < 0 || preset.xupmix == state.xupmix);
    }

    private static Selection currentSound(SoundState state) {
        EqualizerBases.Entry entry = EqualizerBases.mode(state.curve.mode());
        if (entry != null) {
            return new Selection(null, entry.name, state);
        }
        try {
            EqualizerStyles.Point point = EqualizerBases.available() ? EqualizerStyles.match(state.curve) : null;
            if (point != null) {
                return new Selection(null, (point.name != null) ? point.name : "Style", state);
            }
        } catch (IllegalStateException ex) {
            return new Selection(null, "Current sound", state);
        }
        return new Selection(null, "Current sound", state);
    }

    private boolean canEdit() {
        return !this.closed && !this.reading && !this.failed && this.confirmed != null;
    }

    private boolean canSelect() {
        if (this.closed) {
            return false;
        }
        if (this.busy || this.reading) {
            report("Wait for the soundbar update to finish");
            return false;
        }
        return canEdit();
    }

    private void report(String error) {
        this.error = error;
        publish();
    }

    private static String message(String operation, Exception ex) {
        return (ex.getMessage() != null) ? operation + ": " + ex.getMessage() : operation;
    }

    private void publish() {
        if (!this.closed) {
            this.listener.changed(state());
        }
    }

    static final class State {

        final SoundState confirmed;

        final SoundState desired;

        final EqualizerPreset selected;

        final SoundState base;

        final String baseName;

        final List<EqualizerPreset> presets;

        final boolean busy;

        final boolean reading;

        final boolean closed;

        final boolean presetsEnabled;

        final boolean canEdit;

        final boolean canUndo;

        final boolean canReset;

        final String error;

        State(EqualizerSession session) {
            this.confirmed = session.confirmed;
            this.desired = session.desired;
            this.selected = (session.confirmedSelection != null) ? session.confirmedSelection.preset : null;
            this.base = (session.confirmedSelection != null) ? session.confirmedSelection.base : null;
            this.baseName = (session.confirmedSelection != null) ? session.confirmedSelection.baseName
                    : "Current sound";
            this.presets = session.presets;
            this.busy = session.busy;
            this.reading = session.reading;
            this.closed = session.closed;
            this.presetsEnabled = session.presetsEnabled;
            this.canEdit = session.canEdit();
            this.canUndo = this.canEdit && !this.busy && session.undoState != null
                    && !session.undoState.equals(this.confirmed);
            this.canReset = this.canEdit && !this.busy && this.selected != null && !this.base.equals(this.confirmed);
            this.error = session.error;
        }

    }

    private static final class Selection {

        private EqualizerPreset preset;

        private final String baseName;

        private final SoundState base;

        Selection(EqualizerPreset preset, String baseName, SoundState base) {
            this.preset = preset;
            this.baseName = baseName;
            this.base = base;
        }

    }

    private static final class Change {

        private final SoundState before;

        private final SoundState target;

        private final Selection selection;

        private final boolean save;

        private boolean sent;

        Change(EqualizerSession session) {
            this.before = session.confirmed;
            this.target = session.desired;
            this.selection = session.desiredSelection;
            this.save = session.saveDesired;
        }

    }

}
