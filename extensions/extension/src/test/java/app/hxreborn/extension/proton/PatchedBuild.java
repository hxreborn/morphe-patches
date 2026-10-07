/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.proton;

import android.content.Context;
import app.morphe.extension.shared.Utils;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

public final class PatchedBuild {

    private PatchedBuild() {

    }

    public static Context useApplicationContext() {
        final Context context = RuntimeEnvironment.getApplication();
        Utils.setContext(context);
        return context;
    }

    public static void setAccentPreset(String preset) {
        AccentColor.setPreset(preset);
    }

    public static void setAmoledEnabled(boolean enabled) {
        AmoledTheme.setEnabled(enabled);
    }

    public static void setUpgradePromotionsHidden(boolean hidden) {
        UpsellingVisibility.setHidden(hidden);
    }

    @Implements(AmoledTheme.class)
    public static final class Amoled {

        @Implementation
        public static boolean isPatched() {
            return true;
        }

    }

    @Implements(AccentColor.class)
    public static final class Accent {

        @Implementation
        public static boolean isPatched() {
            return true;
        }

    }

    @Implements(UpsellingVisibility.class)
    public static final class Upselling {

        @Implementation
        public static boolean isPatched() {
            return true;
        }

    }

    @Implements(AppliedPatches.class)
    public static final class Applied {

        @Implementation
        public static boolean hidePromotionalMessages() {
            return true;
        }

        @Implementation
        public static boolean removeSentFromSignature() {
            return true;
        }

        @Implementation
        public static boolean removeFreeAccountsLimit() {
            return true;
        }

        @Implementation
        public static boolean scheduledDeletion() {
            return true;
        }

        @Implementation
        public static boolean unlockCustomTimePicker() {
            return true;
        }

        @Implementation
        public static boolean removeServerChangeDelay() {
            return true;
        }

        @Implementation
        public static boolean unlockSplitTunneling() {
            return true;
        }

        @Implementation
        public static boolean unlockLanConnections() {
            return true;
        }

        @Implementation
        public static boolean unlockCustomDns() {
            return true;
        }

        @Implementation
        public static boolean unlockNetShield() {
            return true;
        }

        @Implementation
        public static boolean unlockConnectionPreferences() {
            return true;
        }

        @Implementation
        public static boolean unlockProfiles() {
            return true;
        }

        @Implementation
        public static boolean showFreeServerLocations() {
            return true;
        }

        @Implementation
        public static boolean disableTelemetry() {
            return true;
        }

    }

}
