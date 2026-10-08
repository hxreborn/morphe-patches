/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.proton;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.om.FabricatedOverlay;
import android.content.om.OverlayInfo;
import android.content.om.OverlayManager;
import android.content.om.OverlayManagerTransaction;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.loader.ResourcesLoader;
import android.content.res.loader.ResourcesProvider;
import android.os.Build;
import android.util.TypedValue;
import app.morphe.extension.shared.Logger;

@TargetApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
public final class AccentOverlay {

    private static final String OVERLAY_NAME = "hx_accent_color";

    private static final String OVERLAYABLE_NAME = "HxAccentColor";

    private static final String BRAND_COLORS_ARRAY = "hx_accent_brand_colors";

    private static volatile ResourcesLoader loader;

    private AccentOverlay() {

    }

    static void install(Context context) {
        final int brandColors = context.getResources()
            .getIdentifier(BRAND_COLORS_ARRAY, "array", context.getPackageName());
        if (brandColors == 0) {
            return;
        }
        try {
            final OverlayManager overlayManager = context.getSystemService(OverlayManager.class);
            if (overlayManager == null) {
                return;
            }
            if (!AccentColor.hasCustomAccent()) {
                unregister(context, overlayManager);
                return;
            }

            register(context, overlayManager, brandColors);
            final OverlayInfo overlayInfo = findOverlay(context, overlayManager);
            if (overlayInfo == null) {
                return;
            }
            final ResourcesLoader resourcesLoader = new ResourcesLoader();
            resourcesLoader.addProvider(ResourcesProvider.loadOverlay(overlayInfo));
            loader = resourcesLoader;
            context.getResources().addLoaders(resourcesLoader);
        } catch (Throwable ex) {
            Logger.printException(() -> "Could not apply the accent color overlay", ex);
        }
    }

    static void applyTo(Context context) {
        final ResourcesLoader resourcesLoader = loader;
        if (resourcesLoader == null) {
            return;
        }
        try {
            context.getResources().addLoaders(resourcesLoader);
        } catch (Throwable ex) {
            Logger.printException(() -> "Could not load the accent color overlay", ex);
        }
    }

    private static void register(Context context, OverlayManager overlayManager, int brandColors) {
        final Resources resources = context.getResources();
        final FabricatedOverlay overlay = new FabricatedOverlay(OVERLAY_NAME, context.getPackageName());
        overlay.setTargetOverlayable(OVERLAYABLE_NAME);
        final TypedArray colors = resources.obtainTypedArray(brandColors);
        try {
            for (int index = 0; index < colors.length(); index++) {
                final int id = colors.getResourceId(index, 0);
                overlay.setResourceValue(resources.getResourceName(id), TypedValue.TYPE_INT_COLOR_ARGB8,
                        AccentColor.transformBrandColorArgb(resources.getColor(id, null)), null);
            }
        } finally {
            colors.recycle();
        }
        final OverlayManagerTransaction transaction = OverlayManagerTransaction.newInstance();
        transaction.registerFabricatedOverlay(overlay);
        overlayManager.commit(transaction);
    }

    private static void unregister(Context context, OverlayManager overlayManager) {
        if (findOverlay(context, overlayManager) == null) {
            return;
        }
        final FabricatedOverlay overlay = new FabricatedOverlay(OVERLAY_NAME, context.getPackageName());
        final OverlayManagerTransaction transaction = OverlayManagerTransaction.newInstance();
        transaction.unregisterFabricatedOverlay(overlay.getIdentifier());
        overlayManager.commit(transaction);
    }

    private static OverlayInfo findOverlay(Context context, OverlayManager overlayManager) {
        for (OverlayInfo overlayInfo : overlayManager.getOverlayInfosForTarget(context.getPackageName())) {
            if (OVERLAY_NAME.equals(overlayInfo.getOverlayName())) {
                return overlayInfo;
            }
        }
        return null;
    }

}
