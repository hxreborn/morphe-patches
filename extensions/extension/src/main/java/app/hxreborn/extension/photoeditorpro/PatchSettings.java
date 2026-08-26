/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.photoeditorpro;

import app.morphe.extension.shared.settings.BooleanSetting;
import app.morphe.extension.shared.settings.IntegerSetting;

public final class PatchSettings {

    public static final BooleanSetting HIDE_ADS =
            new BooleanSetting("pep_hide_ads", true, true);

    public static final BooleanSetting UNLOCK_PREMIUM =
            new BooleanSetting("pep_unlock_premium", true, true);

    public static final IntegerSetting UPLOAD_MAX_DIMENSION =
            new IntegerSetting("pep_upload_max_dimension", 3600);

    public static final BooleanSetting LOG_ENDPOINTS =
            new BooleanSetting("pep_log_endpoints", false);

    private PatchSettings() {
    }
}
