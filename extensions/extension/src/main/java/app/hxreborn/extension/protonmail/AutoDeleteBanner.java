/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.protonmail;

import android.content.Context;
import app.morphe.extension.shared.Utils;

@SuppressWarnings("unused")
public final class AutoDeleteBanner {

    private static final String UPSELL_STATE_NAME = "AutoDeleteUpsell";
    private static final String DISABLED_STATE_NAME = "AutoDeleteDisabled";
    private static final String ENABLED_STATE_NAME = "AutoDeleteEnabled";

    private AutoDeleteBanner() {}

    public static boolean isAutoDeleteEnabled(boolean enabledByProton) {
        return enabledByProton || ScheduledDeletionSettings.isActive(Utils.getContext());
    }

    public static int textResource(int original) {
        final Context context = Utils.getContext();
        if (!ScheduledDeletionSettings.isActive(context)) return original;

        final int resource = context.getResources().getIdentifier(
                "hx_scheduled_deletion_banner", "string", context.getPackageName());
        return resource == 0 ? original : resource;
    }

    public static Object resolveState(Object currentState) {
        try {
            final String currentStateName = ((Enum<?>) currentState).name();
            if (ScheduledDeletionSettings.isActive(Utils.getContext())) {
                return enumConstant(currentState, ENABLED_STATE_NAME);
            }
            if (UpsellingVisibility.isHidden() && UPSELL_STATE_NAME.equals(currentStateName)) {
                return enumConstant(currentState, DISABLED_STATE_NAME);
            }
        } catch (Throwable ignored) {
        }
        return currentState;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static Object enumConstant(Object currentState, String name) {
        return Enum.valueOf((Class) ((Enum<?>) currentState).getDeclaringClass(), name);
    }
}
