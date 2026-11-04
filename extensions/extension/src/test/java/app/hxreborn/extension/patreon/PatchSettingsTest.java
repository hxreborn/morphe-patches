package app.hxreborn.extension.patreon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class PatchSettingsTest {

    private SharedPreferences preferences;

    @Before
    public void clearPreferences() {
        this.preferences = RuntimeEnvironment.getApplication()
            .getSharedPreferences(PatchSettings.PREFERENCES_NAME, Context.MODE_PRIVATE);
        this.preferences.edit().clear().commit();
    }

    @Test
    public void resetToDefaultsClearsStoredSwitchValues() {
        // given
        PatchSettings.setEnabled(HomeFeedFilter.HIDE_RECOMMENDED_CREATORS, false);
        PatchSettings.setEnabled(HomeTab.HIDE_HOME_TAB, false);
        for (String tab : NavigationButtons.TABS) {
            NavigationButtons.setHiddenByUser(tab, true);
        }

        // when
        PatchSettings.resetToDefaults();

        // then
        assertEquals(0, this.preferences.getAll().size());
        assertTrue(HomeFeedFilter.isEnabled());
        assertTrue(HomeTab.isHidden());
        for (String tab : NavigationButtons.TABS) {
            assertFalse(tab, NavigationButtons.isHiddenByUser(tab));
        }
    }

}
