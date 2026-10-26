/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.firefox;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.om.FabricatedOverlay;
import android.content.om.OverlayInfo;
import android.content.om.OverlayManager;
import android.content.om.OverlayManagerTransaction;
import android.content.res.Resources;
import android.content.res.loader.ResourcesLoader;
import android.content.res.loader.ResourcesProvider;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.RequiresApi;

import app.hxreborn.extension.shared.ActivityCallbacksAdapter;

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
final class ResourceOverlay {

    private static final String TAG = "FirefoxPatches";

    private ResourceOverlay() {
    }

    static void install(Application application, String name, String overlayable, Entries entries) {
        ResourcesLoader loader = load(application, name, overlayable, entries);
        if (loader != null) {
            attach(application, name, loader);
        }
    }

    static ResourcesLoader load(Application application, String name, String overlayable, Entries entries) {
        try {
            OverlayManager overlayManager = application.getSystemService(OverlayManager.class);
            if (overlayManager == null) {
                return null;
            }
            if (entries == null) {
                unregister(application, overlayManager, name);
                return null;
            }
            register(application, overlayManager, name, overlayable, entries);
            OverlayInfo overlayInfo = find(application, overlayManager, name);
            if (overlayInfo == null) {
                return null;
            }
            ResourcesLoader loader = new ResourcesLoader();
            loader.addProvider(ResourcesProvider.loadOverlay(overlayInfo));
            return loader;
        } catch (Throwable ex) {
            Log.e(TAG, name + " overlay failed", ex);
            return null;
        }
    }

    static void attach(Application application, final String name, final ResourcesLoader loader) {
        addLoader(application, loader, name);
        application.registerActivityLifecycleCallbacks(new ActivityCallbacksAdapter() {
            @Override
            public void onActivityPreCreated(Activity activity, Bundle savedInstanceState) {
                addLoader(activity, loader, name);
            }
        });
    }

    static void addLoader(Context context, ResourcesLoader loader, String name) {
        try {
            context.getResources().addLoaders(loader);
        } catch (Throwable ex) {
            Log.e(TAG, name + " overlay load failed", ex);
        }
    }

    private static void register(Context context, OverlayManager overlayManager, String name, String overlayable,
            Entries entries) {
        FabricatedOverlay overlay = new FabricatedOverlay(name, context.getPackageName());
        overlay.setTargetOverlayable(overlayable);
        entries.write(overlay, context.getResources());
        OverlayManagerTransaction transaction = OverlayManagerTransaction.newInstance();
        transaction.registerFabricatedOverlay(overlay);
        overlayManager.commit(transaction);
    }

    private static void unregister(Context context, OverlayManager overlayManager, String name) {
        if (find(context, overlayManager, name) == null) {
            return;
        }
        FabricatedOverlay overlay = new FabricatedOverlay(name, context.getPackageName());
        OverlayManagerTransaction transaction = OverlayManagerTransaction.newInstance();
        transaction.unregisterFabricatedOverlay(overlay.getIdentifier());
        overlayManager.commit(transaction);
    }

    private static OverlayInfo find(Context context, OverlayManager overlayManager, String name) {
        for (OverlayInfo overlayInfo : overlayManager.getOverlayInfosForTarget(context.getPackageName())) {
            if (name.equals(overlayInfo.getOverlayName())) {
                return overlayInfo;
            }
        }
        return null;
    }

    interface Entries {

        void write(FabricatedOverlay overlay, Resources resources);

    }

}
