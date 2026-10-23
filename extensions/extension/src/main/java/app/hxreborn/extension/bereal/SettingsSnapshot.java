/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.bereal;

import android.os.Bundle;
import app.morphe.extension.shared.settings.BooleanSetting;
import app.morphe.extension.shared.settings.StringSetting;

final class SettingsSnapshot {

    private static final BooleanSetting[] FLAGS = { PatchSettings.GALLERY_PHOTOS, PatchSettings.CUSTOM_LOCATION,
            PatchSettings.CUSTOM_RETAKE_COUNT, PatchSettings.REMOVE_LATE_FLAG, PatchSettings.REMOVE_FRIEND_SUGGESTIONS,
            PatchSettings.REMOVE_FRIENDS_OF_FRIENDS_PROMPTS, PatchSettings.REMOVE_FRIENDS_OF_FRIENDS_TAB,
            PatchSettings.DISABLE_VIEW_RECEIPTS, PatchSettings.REMOVE_LATE_LABEL, PatchSettings.REMOVE_STREAK_PROMPTS,
            PatchSettings.REMOVE_PROMOTIONS };

    private static final StringSetting[] TEXTS = { PatchSettings.LOCATION_LATITUDE, PatchSettings.LOCATION_LONGITUDE,
            PatchSettings.LOCATION_LABEL };

    private static final String FLAGS_KEY = "undo_flags";

    private static final String TEXTS_KEY = "undo_texts";

    private final boolean[] flags = new boolean[FLAGS.length];

    private final String[] texts = new String[TEXTS.length];

    static SettingsSnapshot capture() {
        final SettingsSnapshot snapshot = new SettingsSnapshot();
        for (int i = 0; i < FLAGS.length; i++) {
            snapshot.flags[i] = FLAGS[i].get();
        }
        for (int i = 0; i < TEXTS.length; i++) {
            snapshot.texts[i] = TEXTS[i].get();
        }
        return snapshot;
    }

    static void resetToDefaults() {
        for (BooleanSetting flag : FLAGS) {
            flag.resetToDefault();
        }
        for (StringSetting text : TEXTS) {
            text.resetToDefault();
        }
    }

    static SettingsSnapshot readFrom(Bundle state) {
        final boolean[] flags = state.getBooleanArray(FLAGS_KEY);
        final String[] texts = state.getStringArray(TEXTS_KEY);
        if (flags == null || texts == null || flags.length != FLAGS.length || texts.length != TEXTS.length) {
            return null;
        }
        final SettingsSnapshot snapshot = new SettingsSnapshot();
        System.arraycopy(flags, 0, snapshot.flags, 0, flags.length);
        System.arraycopy(texts, 0, snapshot.texts, 0, texts.length);
        return snapshot;
    }

    void writeTo(Bundle state) {
        state.putBooleanArray(FLAGS_KEY, this.flags.clone());
        state.putStringArray(TEXTS_KEY, this.texts.clone());
    }

    void restore() {
        for (int i = 0; i < FLAGS.length; i++) {
            FLAGS[i].save(this.flags[i]);
        }
        for (int i = 0; i < TEXTS.length; i++) {
            TEXTS[i].save(this.texts[i]);
        }
    }

}
