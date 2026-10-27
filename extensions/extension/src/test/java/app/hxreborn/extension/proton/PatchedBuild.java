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

    public static void setDimRaisedSurfacesEnabled(boolean enabled) {
        AmoledTheme.setDimRaisedSurfacesEnabled(enabled);
    }

    public static void setUpgradePromotionsHidden(boolean hidden) {
        UpsellingVisibility.setHidden(hidden);
    }

    public static void setMaterialSwitchesEnabled(boolean enabled) {
        MaterialSwitches.setEnabled(enabled);
    }

    public static void setLoadingIndicatorEnabled(boolean enabled) {
        LoadingIndicator.setEnabled(enabled);
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

    @Implements(MaterialSwitches.class)
    public static final class Switches {

        @Implementation
        public static boolean isPatched() {
            return true;
        }

    }

    @Implements(LoadingIndicator.class)
    public static final class Indicator {

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
        public static boolean swipeToSwitchAccounts() {
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
