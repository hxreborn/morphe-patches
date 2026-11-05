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

import com.ahe.android.hybridengine.widget.AHEWidgetNode;
import com.aliexpress.android.aehome.ahe.ahenode.HomeAHEAEImageViewWidgetNode;

import org.junit.Assert;
import org.junit.Test;

public class TemplateNodesTest {

    private static final int WHITE = 0xFFFFFFFF;

    private static final int BRAND_RED = 0xFFFD384F;

    private static final int TRANSPARENT = 0;

    @Test
    public void darkenFrameWithHomeImageChild() {
        AHEWidgetNode frame = new AHEWidgetNode(WHITE);
        frame.add(new HomeAHEAEImageViewWidgetNode());

        Assert.assertFalse(TemplateNodes.keepsLightColors(frame));
    }

    @Test
    public void keepLightColorsOfFrameWithHomeImageInChildFrame() {
        AHEWidgetNode frame = new AHEWidgetNode(WHITE);
        AHEWidgetNode inner = frame.add(new AHEWidgetNode(TRANSPARENT));
        inner.add(new HomeAHEAEImageViewWidgetNode());

        Assert.assertTrue(TemplateNodes.keepsLightColors(frame));
    }

    @Test
    public void darkenFrameWithHomeImageThreeLevelsDown() {
        AHEWidgetNode frame = new AHEWidgetNode(WHITE);
        AHEWidgetNode inner = frame.add(new AHEWidgetNode(TRANSPARENT));
        AHEWidgetNode deeper = inner.add(new AHEWidgetNode(TRANSPARENT));
        deeper.add(new HomeAHEAEImageViewWidgetNode());

        Assert.assertFalse(TemplateNodes.keepsLightColors(frame));
    }

    @Test
    public void darkenFrameWithoutHomeImage() {
        AHEWidgetNode frame = new AHEWidgetNode(WHITE);
        frame.add(new AHEWidgetNode(TRANSPARENT));

        Assert.assertFalse(TemplateNodes.keepsLightColors(frame));
    }

    @Test
    public void ignoreHomeImageOnSibling() {
        AHEWidgetNode root = new AHEWidgetNode(TRANSPARENT);
        root.add(new HomeAHEAEImageViewWidgetNode());
        AHEWidgetNode frame = root.add(new AHEWidgetNode(WHITE));

        Assert.assertFalse(TemplateNodes.keepsLightColors(frame));
    }

    @Test
    public void drawTextOnLightSurfaceAsNight() {
        AHEWidgetNode card = new AHEWidgetNode(WHITE);
        AHEWidgetNode row = card.add(new AHEWidgetNode(TRANSPARENT));
        AHEWidgetNode text = row.add(new AHEWidgetNode(TRANSPARENT));

        Assert.assertTrue(TemplateNodes.drawnOnNightSurface(text));
    }

    @Test
    public void keepTextWithoutSurfaceBehind() {
        AHEWidgetNode row = new AHEWidgetNode(TRANSPARENT);
        AHEWidgetNode text = row.add(new AHEWidgetNode(TRANSPARENT));

        Assert.assertFalse(TemplateNodes.drawnOnNightSurface(text));
    }

    @Test
    public void keepTextOnBrandSurface() {
        AHEWidgetNode banner = new AHEWidgetNode(BRAND_RED);
        AHEWidgetNode text = banner.add(new AHEWidgetNode(TRANSPARENT));

        Assert.assertFalse(TemplateNodes.drawnOnNightSurface(text));
    }

    @Test
    public void keepTextOnSurfaceThatKeepsLightColors() {
        AHEWidgetNode pill = new AHEWidgetNode(WHITE);
        AHEWidgetNode search = pill.add(new AHEWidgetNode(TRANSPARENT));
        search.add(new HomeAHEAEImageViewWidgetNode());
        AHEWidgetNode text = pill.add(new AHEWidgetNode(TRANSPARENT));

        Assert.assertFalse(TemplateNodes.drawnOnNightSurface(text));
    }

    @Test
    public void drawTextOnSurfaceWithBackdropImage() {
        AHEWidgetNode strip = new AHEWidgetNode(WHITE);
        strip.add(new HomeAHEAEImageViewWidgetNode());
        AHEWidgetNode text = strip.add(new AHEWidgetNode(TRANSPARENT));

        Assert.assertTrue(TemplateNodes.drawnOnNightSurface(text));
    }

    @Test
    public void recogniseHomeImageOnLightFrameAsBackdrop() {
        AHEWidgetNode strip = new AHEWidgetNode(WHITE);
        HomeAHEAEImageViewWidgetNode image = new HomeAHEAEImageViewWidgetNode();
        strip.add(image);

        Assert.assertTrue(TemplateNodes.isBackdropImage(image));
    }

    @Test
    public void ignoreHomeImageOnFrameWithoutSurface() {
        AHEWidgetNode row = new AHEWidgetNode(TRANSPARENT);
        HomeAHEAEImageViewWidgetNode image = new HomeAHEAEImageViewWidgetNode();
        row.add(image);

        Assert.assertFalse(TemplateNodes.isBackdropImage(image));
    }

    @Test
    public void ignoreHomeImageOnBrandFrame() {
        AHEWidgetNode banner = new AHEWidgetNode(BRAND_RED);
        HomeAHEAEImageViewWidgetNode image = new HomeAHEAEImageViewWidgetNode();
        banner.add(image);

        Assert.assertFalse(TemplateNodes.isBackdropImage(image));
    }

    @Test
    public void recogniseNodeOnLightSurface() {
        Assert.assertTrue(TemplateNodes.hasLightSurface(new AHEWidgetNode(WHITE)));
    }

    @Test
    public void ignoreNodeWithoutSurface() {
        Assert.assertFalse(TemplateNodes.hasLightSurface(new AHEWidgetNode(TRANSPARENT)));
    }

    @Test
    public void ignoreNodeOnBrandSurface() {
        Assert.assertFalse(TemplateNodes.hasLightSurface(new AHEWidgetNode(BRAND_RED)));
    }

    @Test
    public void ignoreNodeThatIsNotHomeImage() {
        AHEWidgetNode strip = new AHEWidgetNode(WHITE);
        AHEWidgetNode text = strip.add(new AHEWidgetNode(TRANSPARENT));

        Assert.assertFalse(TemplateNodes.isBackdropImage(text));
    }

    @Test
    public void useNearestSurface() {
        AHEWidgetNode page = new AHEWidgetNode(WHITE);
        AHEWidgetNode banner = page.add(new AHEWidgetNode(BRAND_RED));
        AHEWidgetNode text = banner.add(new AHEWidgetNode(TRANSPARENT));

        Assert.assertFalse(TemplateNodes.drawnOnNightSurface(text));
    }

}
