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

        assertSame(items, HomeFeedFilter.removeNonMemberItems(new Object(), items));
        assertSame(items, HomeFeedFilter.removePostViewerItems(items));
        assertSame(items, HomeFeedFilter.removeExploreSections(items));
    }

    @Test
    public void postViewerFilterKeepsItemsWithoutPostIds() {
        HomeFeedFilter.recordViewedPost(new Object());
        List<Object> items = Collections.singletonList(new Object());

        assertSame(items, HomeFeedFilter.removePostViewerItems(items));
    }

    @Test
    public void itemStateOutsidePostViewerStays() {
        Object state = new Object();

        assertSame(state, HomeFeedFilter.removeNonMemberItemState(state, new Object(), new Object()));
    }

    @Test
    public void unpatchedQuipsKeepEveryItem() {
        List<Object> items = Collections.singletonList(new Object());

        assertSame(items, HomeFeedFilter.removeQuipCards(items));
    }

}
