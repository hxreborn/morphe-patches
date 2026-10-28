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
package app.hxreborn.extension.protonmail;

import android.content.Context;
import app.morphe.extension.shared.Utils;

import app.hxreborn.extension.proton.UpsellingVisibility;

@SuppressWarnings("unused")
public final class AutoDeleteBanner {

    private static final String BANNER_BOTH = "hx_scheduled_deletion_banner";

    private static final String BANNER_TRASH = "hx_scheduled_deletion_banner_trash";

    private static final String BANNER_SPAM = "hx_scheduled_deletion_banner_spam";

    private static final String UPSELL_STATE_NAME = "AutoDeleteUpsell";

    private static final String DISABLED_STATE_NAME = "AutoDeleteDisabled";

    private static final String ENABLED_STATE_NAME = "AutoDeleteEnabled";

    private AutoDeleteBanner() {

    }

    public static boolean isAutoDeleteEnabled(boolean enabledByProton) {
        return enabledByProton || isScheduled();
    }

    public static int textResource(int original) {
        if (!isScheduled()) {
            return original;
        }

        final Context context = Utils.getContext();
        final int resource = context.getResources().getIdentifier(bannerName(), "string", context.getPackageName());
        return (resource != 0) ? resource : original;
    }

    private static boolean isScheduled() {
        final Context context = Utils.getContext();
        final String shown = ScheduledDeletion.shownLabel();
        return (shown != null) ? ScheduledDeletionSettings.isActive(context, shown)
                : ScheduledDeletionSettings.isActive(context);
    }

    private static String bannerName() {
        final String shown = ScheduledDeletion.shownLabel();
        if (ScheduledDeletion.TRASH.equals(shown)) {
            return BANNER_TRASH;
        }
        if (ScheduledDeletion.SPAM.equals(shown)) {
            return BANNER_SPAM;
        }

        final Context context = Utils.getContext();
        final boolean trash = ScheduledDeletionSettings.isActive(context, ScheduledDeletion.TRASH);
        final boolean spam = ScheduledDeletionSettings.isActive(context, ScheduledDeletion.SPAM);
        if (trash && spam) {
            return BANNER_BOTH;
        }
        return (trash) ? BANNER_TRASH : BANNER_SPAM;
    }

    public static Object resolveState(Object currentState) {
        try {
            final String currentStateName = ((Enum<?>) currentState).name();
            if (isScheduled()) {
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
