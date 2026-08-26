/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.photoeditorpro;

import app.morphe.extension.shared.settings.BooleanSetting;
import app.morphe.extension.shared.settings.EnumSetting;
import app.morphe.extension.shared.settings.StringSetting;

public final class PatchSettings {

    public static final BooleanSetting HIDE_ADS =
            new BooleanSetting("pep_hide_ads", true, true);

    public static final BooleanSetting UNLOCK_PREMIUM =
            new BooleanSetting("pep_unlock_premium", true, true);

    public static final BooleanSetting SHOW_AI_PROGRESS =
            new BooleanSetting("pep_show_ai_progress", true);

    public static final EnumSetting<LiveOverlay.Position> OVERLAY_POSITION =
            new EnumSetting<>("pep_overlay_position", LiveOverlay.Position.TOP);

    public static final EnumSetting<LiveOverlay.Linger> OVERLAY_LINGER =
            new EnumSetting<>("pep_overlay_linger", LiveOverlay.Linger.NORMAL);

    public static final BooleanSetting LOG_ENDPOINTS =
            new BooleanSetting("pep_log_endpoints", false);

    public static final StringSetting RUN_HISTORY =
            new StringSetting("pep_run_history", "");

    private PatchSettings() {
    }
}
