/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.bereal;

import android.os.Bundle;
import app.morphe.extension.shared.settings.BooleanSetting;

final class SettingsSnapshot {

    private static final BooleanSetting[] FLAGS = { PatchSettings.GALLERY_PHOTOS };

    private static final String FLAGS_KEY = "undo_flags";

    private final boolean[] flags = new boolean[FLAGS.length];

    static SettingsSnapshot capture() {
        final SettingsSnapshot snapshot = new SettingsSnapshot();
        for (int i = 0; i < FLAGS.length; i++) {
            snapshot.flags[i] = FLAGS[i].get();
        }
        return snapshot;
    }

    static void resetToDefaults() {
        for (BooleanSetting flag : FLAGS) {
            flag.resetToDefault();
        }
    }

    static SettingsSnapshot readFrom(Bundle state) {
        final boolean[] flags = state.getBooleanArray(FLAGS_KEY);
        if (flags == null || flags.length != FLAGS.length) {
            return null;
        }
        final SettingsSnapshot snapshot = new SettingsSnapshot();
        System.arraycopy(flags, 0, snapshot.flags, 0, flags.length);
        return snapshot;
    }

    void writeTo(Bundle state) {
        state.putBooleanArray(FLAGS_KEY, this.flags.clone());
    }

    void restore() {
        for (int i = 0; i < FLAGS.length; i++) {
            FLAGS[i].save(this.flags[i]);
        }
    }

}
