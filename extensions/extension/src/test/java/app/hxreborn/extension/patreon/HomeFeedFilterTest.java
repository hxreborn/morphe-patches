package app.hxreborn.extension.patreon;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class HomeFeedFilterTest {

    @Before
    public void clearPreferences() {
        RuntimeEnvironment.getApplication()
            .getSharedPreferences(PatchSettings.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit();
    }

    @Test
    public void filterIsEnabledByDefault() {
        assertTrue(HomeFeedFilter.isEnabled());
    }

    @Test
    public void disabledFilterKeepsEveryItem() {
        PatchSettings.setEnabled(HomeFeedFilter.HIDE_RECOMMENDED_CREATORS, false);
        List<Object> items = Collections.singletonList(new Object());

        assertSame(items, HomeFeedFilter.removeNonMemberItems(items));
        assertSame(items, HomeFeedFilter.removeExploreSections(items));
    }

}
