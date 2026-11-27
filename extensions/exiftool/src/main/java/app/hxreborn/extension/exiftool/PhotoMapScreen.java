/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.exiftool;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

import android.view.View;
import android.view.ViewGroup;

public final class PhotoMapScreen {

    private static final int MATCH_PARENT = ViewGroup.LayoutParams.MATCH_PARENT;

    private static final WeakHashMap<Object, WeakReference<MapPickerView>> MAPS = new WeakHashMap<>();

    private PhotoMapScreen() {
    }

    public static void attach(Object owner, View root, int containerId) {
        if (root == null) {
            return;
        }
        ViewGroup container = (ViewGroup) root.findViewById(containerId);
        if (container == null) {
            return;
        }
        MapPickerView map = map(owner);
        if (map != null && map.getParent() == container) {
            map.bringToFront();
            return;
        }
        map = new MapPickerView(container.getContext());
        map.setPhotoListener((photo) -> open(owner, photo));
        container.addView(map, new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));
        MAPS.put(owner, new WeakReference<>(map));
    }

    public static void update(Object owner) {
        throw new IllegalStateException("Photo map update stub was not replaced");
    }

    public static void clear(Object owner) {
        MapPickerView map = map(owner);
        if (map != null) {
            map.clearPhotos();
        }
    }

    public static void addPhoto(Object owner, Object photo, double latitude, double longitude) {
        MapPickerView map = map(owner);
        if (map != null) {
            map.addPhoto(new PhotoMapItem(photo, latitude, longitude));
        }
    }

    public static void finish(Object owner) {
        MapPickerView map = map(owner);
        if (map != null) {
            map.bringToFront();
            map.fitPhotos();
        }
    }

    public static void open(Object owner, Object photo) {
        throw new IllegalStateException("Photo map open stub was not replaced");
    }

    private static MapPickerView map(Object owner) {
        WeakReference<MapPickerView> reference = MAPS.get(owner);
        return (reference != null) ? reference.get() : null;
    }

}
