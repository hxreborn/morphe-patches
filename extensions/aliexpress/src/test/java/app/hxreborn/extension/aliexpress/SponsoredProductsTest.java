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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class SponsoredProductsTest {

    @Test
    public void removeAdjacentSponsoredProductsWhileKeepingOrganicOrder() {
        Map<String, Object> firstProduct = product("first", null);
        Map<String, Object> lastProduct = product("last", null);
        List<Object> products = new ArrayList<>(Arrays.asList(firstProduct, product("sponsored-first", new HashMap<>()),
                product("sponsored-last", new HashMap<>()), lastProduct));

        Map<String, Object> source = new HashMap<>();
        source.put("listItems", products);
        SponsoredProducts.filter(source, "listItems");

        Assert.assertEquals(Arrays.asList(firstProduct, lastProduct), source.get("listItems"));
    }

    @Test
    public void retainNonProductRowsWithSponsorshipMetadata() {
        Map<String, Object> header = product(null, new HashMap<>());
        header.remove("productId");
        List<Object> products = new ArrayList<>(Arrays.asList(header, null, "unrecognized row"));

        Map<String, Object> source = new HashMap<>();
        source.put("listItems", products);
        SponsoredProducts.filter(source, "listItems");

        Assert.assertEquals(Arrays.asList(header, null, "unrecognized row"), source.get("listItems"));
    }

    @Test
    public void retainProductsWithNonObjectSponsorshipValues() {
        Map<String, Object> product = product("organic", "false");
        List<Object> products = new ArrayList<>(Arrays.asList(product));

        Map<String, Object> source = new HashMap<>();
        source.put("listItems", products);
        SponsoredProducts.filter(source, "listItems");

        Assert.assertEquals(Collections.singletonList(product), source.get("listItems"));
    }

    @Test
    public void preserveOtherArraysContainingSponsoredProducts() {
        List<Object> images = Arrays.asList(product("image", new HashMap<>()));
        Map<String, Object> source = new HashMap<>();
        source.put("images", images);

        SponsoredProducts.filter(source, "images");

        Assert.assertSame(images, source.get("images"));
    }

    @Test
    public void filterImmutableProductLists() {
        Map<String, Object> organic = product("organic", null);
        List<Object> products = Collections
            .unmodifiableList(Arrays.asList(product("sponsored", new HashMap<>()), organic));
        Map<String, Object> source = new HashMap<>();
        source.put("cells", products);

        SponsoredProducts.filter(source, "cells");

        Assert.assertEquals(Collections.singletonList(organic), source.get("cells"));
    }

    private static Map<String, Object> product(String productId, Object sponsorship) {
        Map<String, Object> result = new HashMap<>();
        result.put("productId", productId);
        result.put("p4p", sponsorship);
        return result;
    }

}
