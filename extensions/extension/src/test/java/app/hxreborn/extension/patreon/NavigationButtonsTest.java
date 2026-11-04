package app.hxreborn.extension.patreon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class NavigationButtonsTest {

    @Before
    public void clearPreferences() {
        RuntimeEnvironment.getApplication()
            .getSharedPreferences(PatchSettings.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit();
    }

    @Test
    public void preferencesFileNameIsHxPatreonPatches() {
        assertEquals("hx_patreon_patches", PatchSettings.PREFERENCES_NAME);
    }

    @Test
    public void tabsAreShownByDefault() {
        for (String tab : NavigationButtons.TABS) {
            assertFalse(tab, NavigationButtons.isHidden(tab));
        }
    }

    @Test
    public void hiddenTabIsRemoved() {
        NavigationButtons.setHiddenByUser(NavigationButtons.LIBRARY, true);
        Object library = new Tab(NavigationButtons.LIBRARY);
        Object community = new Tab(NavigationButtons.COMMUNITY);

        assertNull(NavigationButtons.visibleTab(library));
        assertSame(community, NavigationButtons.visibleTab(community));
    }

    @Test
    public void hiddenFeedStartsOnLibrary() {
        NavigationButtons.setHiddenByUser(NavigationButtons.FEED, true);

        assertTrue(NavigationButtons.startsOnLibrary());
        assertFalse(NavigationButtons.startsOnCommunity());
    }

    @Test
    public void hiddenFeedAndLibraryStartOnCommunity() {
        NavigationButtons.setHiddenByUser(NavigationButtons.FEED, true);
        NavigationButtons.setHiddenByUser(NavigationButtons.LIBRARY, true);

        assertFalse(NavigationButtons.startsOnLibrary());
        assertTrue(NavigationButtons.startsOnCommunity());
    }

    @Test
    public void feedStaysWhenLibraryAndCommunityAreHidden() {
        NavigationButtons.setHiddenByUser(NavigationButtons.FEED, true);
        NavigationButtons.setHiddenByUser(NavigationButtons.LIBRARY, true);
        NavigationButtons.setHiddenByUser(NavigationButtons.COMMUNITY, true);

        assertFalse(NavigationButtons.isHidden(NavigationButtons.FEED));
        assertFalse(NavigationButtons.startsOnLibrary());
        assertFalse(NavigationButtons.startsOnCommunity());
    }

    private static final class Tab {

        private final String name;

        Tab(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return this.name;
        }

    }

}
