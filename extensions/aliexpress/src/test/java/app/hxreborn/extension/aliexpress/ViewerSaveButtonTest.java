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
package app.hxreborn.extension.aliexpress;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowLooper;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.Q, manifest = Config.NONE,
        shadows = { ViewerSaveButtonTest.TaggedImageUrls.class, ViewerSaveButtonTest.ImageSavingEnabled.class })
public class ViewerSaveButtonTest {

    private static final int PAGE_SIZE_PX = 400;

    private static final int THUMBNAIL_SIZE_PX = 40;

    private static final int SHARE_ICON_SIZE_PX = 24;

    private static final int SHARE_WIDTH_PX = 48;

    private static final int SHARE_HEIGHT_PX = 56;

    private static final String CURRENT_PAGE = "https://ae01.alicdn.com/kf/current.jpg";

    private static final String NEXT_PAGE = "https://ae01.alicdn.com/kf/next.jpg";

    private static final String THUMBNAIL = "https://ae01.alicdn.com/kf/thumbnail.jpg";

    @Test
    public void returnOnScreenPageInsteadOfOffScreenNeighbour() {
        // given
        FrameLayout viewer = attachedViewer();
        addImage(viewer, NEXT_PAGE, PAGE_SIZE_PX, PAGE_SIZE_PX * 2);
        addImage(viewer, CURRENT_PAGE, PAGE_SIZE_PX, 0);
        ShadowLooper.idleMainLooper();

        // when
        String url = ViewerSaveButton.visibleImageUrl(viewer);

        // then
        Assert.assertEquals(CURRENT_PAGE, url);
    }

    @Test
    public void preferLargestVisibleImage() {
        // given
        FrameLayout viewer = attachedViewer();
        addImage(viewer, THUMBNAIL, THUMBNAIL_SIZE_PX, 0);
        addImage(viewer, CURRENT_PAGE, PAGE_SIZE_PX, 0);
        ShadowLooper.idleMainLooper();

        // when
        String url = ViewerSaveButton.visibleImageUrl(viewer);

        // then
        Assert.assertEquals(CURRENT_PAGE, url);
    }

    @Test
    public void returnNullWithoutRemoteImage() {
        // given
        FrameLayout viewer = attachedViewer();
        viewer.addView(new View(viewer.getContext()), new FrameLayout.LayoutParams(PAGE_SIZE_PX, PAGE_SIZE_PX));
        ShadowLooper.idleMainLooper();

        // when
        String url = ViewerSaveButton.visibleImageUrl(viewer);

        // then
        Assert.assertNull(url);
    }

    @Test
    public void copyShareIconSizeTintAndScale() {
        // given
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        RelativeLayout bar = new RelativeLayout(activity);
        RelativeLayout share = new RelativeLayout(activity);
        share.setId(View.generateViewId());
        ImageView shareIcon = new ImageView(activity);
        shareIcon.setImageTintList(ColorStateList.valueOf(Color.WHITE));
        shareIcon.setScaleType(ImageView.ScaleType.CENTER);
        share.addView(shareIcon, new RelativeLayout.LayoutParams(SHARE_ICON_SIZE_PX, SHARE_ICON_SIZE_PX));
        bar.addView(share, new RelativeLayout.LayoutParams(SHARE_WIDTH_PX, SHARE_HEIGHT_PX));

        // when
        ViewerSaveButton.attach(share);

        // then
        ViewGroup save = (ViewGroup) bar.getChildAt(bar.getChildCount() - 1);
        ImageView saveIcon = (ImageView) save.getChildAt(0);
        Assert.assertEquals(SHARE_WIDTH_PX, save.getLayoutParams().width);
        Assert.assertEquals(SHARE_ICON_SIZE_PX, saveIcon.getLayoutParams().width);
        Assert.assertEquals(SHARE_ICON_SIZE_PX, saveIcon.getLayoutParams().height);
        Assert.assertEquals(shareIcon.getImageTintList(), saveIcon.getImageTintList());
        Assert.assertEquals(ImageView.ScaleType.CENTER, saveIcon.getScaleType());
    }

    private static FrameLayout attachedViewer() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        FrameLayout viewer = new FrameLayout(activity);
        activity.setContentView(viewer, new FrameLayout.LayoutParams(PAGE_SIZE_PX, PAGE_SIZE_PX));
        return viewer;
    }

    private static void addImage(FrameLayout viewer, String url, int sizePx, int leftMarginPx) {
        ImageView image = new ImageView(viewer.getContext());
        image.setTag(url);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(sizePx, sizePx);
        params.leftMargin = leftMarginPx;
        viewer.addView(image, params);
    }

    @Implements(value = ViewerSaveButton.class, isInAndroidSdk = false)
    public static class TaggedImageUrls {

        @Implementation
        protected static String imageUrlOf(View view) {
            return (view.getTag() instanceof String) ? (String) view.getTag() : null;
        }

        @Implementation
        protected static int saveIconId() {
            return android.R.drawable.ic_menu_save;
        }

    }

    @Implements(value = PatchSettings.class, isInAndroidSdk = false)
    public static class ImageSavingEnabled {

        @Implementation
        protected static boolean enableImageSaving() {
            return true;
        }

    }

}
