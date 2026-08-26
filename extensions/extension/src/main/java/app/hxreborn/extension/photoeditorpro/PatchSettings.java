/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.photoeditorpro;

import app.morphe.extension.shared.settings.BooleanSetting;

public final class PatchSettings {

    public static final BooleanSetting HIDE_ADS =
            new BooleanSetting("pep_hide_ads", true, true);

    private PatchSettings() {
    }
}
