/*
 * Copyright (C) 2026 BlazeFTL
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from BlazeFTL/FTL-Patches:
 * https://github.com/BlazeFTL/FTL-Patches/commit/dd13beaf9ad004d59b5107a33304606581b0687f
 * Commit dd13beaf9ad004d59b5107a33304606581b0687f (2026-10-25),
 * extensions/firefox/src/main/java/app/ftl/extension/firefox/ExtensionPin.java
 */
package app.hxreborn.extension.firefox;

import java.io.ByteArrayOutputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;
import kotlin.jvm.functions.Function1;

@SuppressWarnings({ "unused", "rawtypes", "unchecked" })
public final class ExtensionPin {

    private static final String TAG = "FirefoxPatches";

    private static final String PREFS = "hx_pinned_ext";

    private static final String BROWSER_STATE = "mozilla.components.browser.state.state.BrowserState";

    private static final String SELECTORS = "mozilla.components.browser.state.selector.SelectorsKt";

    private static final String TOOLBAR_PACKAGE = "mozilla.components.compose.browser.toolbar.";

    private static final String ACTION_BUTTON = TOOLBAR_PACKAGE + "concept.Action$ActionButton";

    private static final String BUTTON_STATE = ACTION_BUTTON + "$State";

    private static final String INTERACTION = TOOLBAR_PACKAGE + "store.BrowserToolbarInteraction";

    private static final String ENDS_UPDATED = TOOLBAR_PACKAGE
            + "store.BrowserDisplayToolbarAction$BrowserActionsEndUpdated";

    private static Context appCtx;

    private static WeakReference<Object> bstore;

    private static WeakReference<Object> store;

    private static WeakReference<Object> mw;

    private static final ConcurrentHashMap<String, PinState> cache = new ConcurrentHashMap<>();

    private static Handler handler;

    private static int lastHash;

    private static String raw;

    private static boolean started;

    private ExtensionPin() {
    }

    static String[] ids(Context context) {
        if (raw == null) {
            raw = prefs(context).getString("ids", "");
        }
        return raw.isEmpty() ? new String[0] : raw.split("\n");
    }

    static void toggle(Context context, Object item) {
        try {
            String id = (String) Reflection.read(item, "id");
            if (isPinned(context, id)) {
                unpin(context, id);
                refresh();
                return;
            }
            if (visibleCount(context) >= maxPins()) {
                Toast
                    .makeText(context, "Up to 5 pinned extensions. The Expanded toolbar layout fits more.",
                            Toast.LENGTH_LONG)
                    .show();
                return;
            }
            pin(context, item);
            kick();
        } catch (Throwable ex) {
            Log.e(TAG, "toggle failed", ex);
        }
    }

    static void kick() {
        if (handler == null) {
            sync();
        } else {
            handler.post(ExtensionPin::sync);
        }
    }

    static int px(Context context) {
        return (int) (context.getResources().getDisplayMetrics().density * 24f + 0.5f);
    }

    static Bitmap savedIcon(Context context, String id) {
        try {
            String encoded = prefs(context).getString("i:" + id, null);
            if (encoded == null) {
                return null;
            }
            byte[] bytes = Base64.decode(encoded, 0);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (Throwable ex) {
            return null;
        }
    }

    static PinState state(String id) {
        PinState state = cache.get(id);
        if (state == null) {
            state = new PinState();
            state.id = id;
            cache.put(id, state);
        }
        return state;
    }

    private static Object action(Object extensionState) throws Exception {
        if (extensionState == null) {
            return null;
        }
        Object action = Reflection.read(extensionState, "browserAction");
        return (action != null) ? action : Reflection.read(extensionState, "pageAction");
    }

    private static Object value(Object action, String name) throws Exception {
        return (action != null) ? Reflection.read(action, name) : null;
    }

    private static Object pick(Object tabAction, Object globalAction, String name) throws Exception {
        Object value = value(tabAction, name);
        return (value != null) ? value : value(globalAction, name);
    }

    public static void start(Object middleware) {
        if (started) {
            return;
        }
        started = true;
        try {
            Context activity = (Context) Reflection.read(middleware, "uiContext");
            appCtx = activity.getApplicationContext();
            ids(appCtx);
            Object browserStore = Reflection.read(middleware, "browserStore");
            bstore = new WeakReference<>(browserStore);
            handler = new Handler(Looper.getMainLooper());
            Method observe = browserStore.getClass().getMethod("observeManually", Function1.class);
            Object subscription = observe.invoke(browserStore, new PinObserver());
            subscription.getClass().getMethod("resume").invoke(subscription);
            kick();
        } catch (Throwable ex) {
            Log.e(TAG, "start failed", ex);
        }
    }

    public static void remember(Object toolbarStore, Object middleware) {
        try {
            if (PatchSettings.pinEnabled()) {
                start(middleware);
            }
            if (store != null && store.get() == toolbarStore) {
                return;
            }
            store = new WeakReference<>(toolbarStore);
            mw = new WeakReference<>(middleware);
        } catch (Throwable ex) {
            Log.e(TAG, "remember failed", ex);
        }
    }

    static void onState(Object browserState) throws Exception {
        if (raw == null || raw.isEmpty() || appCtx == null) {
            return;
        }
        Map extensions = (Map) Reflection.read(browserState, "extensions");
        Map tabExtensions = tabExtensionState(browserState);
        String[] ids = ids(appCtx);
        int hash = 17;
        for (String id : ids) {
            hash = hash * 31 + System.identityHashCode(extensions.get(id));
            hash = hash * 31 + System.identityHashCode((tabExtensions != null) ? tabExtensions.get(id) : null);
        }
        hash = hash * 31 + ids.length;
        if (hash == lastHash) {
            return;
        }
        lastHash = hash;
        if (handler != null) {
            handler.post(ExtensionPin::sync);
        }
    }

    private static Map tabExtensionState(Object browserState) throws Exception {
        Class<?> state = Class.forName(BROWSER_STATE);
        Object tab = Class.forName(SELECTORS).getMethod("getSelectedTab", state).invoke(null, browserState);
        if (tab == null) {
            return null;
        }
        return (Map) tab.getClass().getMethod("getExtensionState").invoke(tab);
    }

    static void sync() {
        try {
            Context context = appCtx;
            if (context == null || bstore == null) {
                return;
            }
            Object browserStore = bstore.get();
            if (browserStore == null) {
                return;
            }
            Object browserState = browserStore.getClass().getMethod("getState").invoke(browserStore);
            Map extensions = (Map) Reflection.read(browserState, "extensions");
            Map tabExtensions = tabExtensionState(browserState);
            for (String id : ids(context)) {
                syncOne(context, id, extensions, tabExtensions);
            }
            refresh();
        } catch (Throwable ex) {
            Log.e(TAG, "sync failed", ex);
        }
    }

    static void syncOne(Context context, String id, Map extensions, Map tabExtensions) {
        try {
            Object extension = extensions.get(id);
            PinState st = state(id);
            if (extension == null) {
                if (!extensions.isEmpty()) {
                    st.hidden = true;
                }
                return;
            }
            if (!(Boolean) Reflection.read(extension, "enabled")) {
                st.hidden = true;
                return;
            }
            st.hidden = false;

            Object tabAction = (tabExtensions != null) ? action(tabExtensions.get(id)) : null;
            Object globalAction = action(extension);

            String title = (String) pick(tabAction, globalAction, "title");
            if (title == null) {
                title = (String) Reflection.read(extension, "name");
            }
            st.title = title;
            st.enabled = (Boolean) pick(tabAction, globalAction, "enabled");
            st.text = (String) pick(tabAction, globalAction, "badgeText");
            st.bg = (Integer) pick(tabAction, globalAction, "badgeBackgroundColor");
            st.fg = (Integer) pick(tabAction, globalAction, "badgeTextColor");

            Object loader = pick(tabAction, globalAction, "loadIcon");
            if (loader != null && loader != st.loader) {
                st.loader = loader;
                Class<?> function2 = Class.forName("kotlin.jvm.functions.Function2");
                Object result = function2.getMethod("invoke", Object.class, Object.class)
                    .invoke(loader, px(context), new PinIconContinuation(st));
                Object suspended = Class.forName("kotlin.coroutines.intrinsics.CoroutineSingletons")
                    .getField("COROUTINE_SUSPENDED")
                    .get(null);
                if (result != suspended && result instanceof Bitmap) {
                    st.bitmap = (Bitmap) result;
                }
            }
        } catch (Throwable ex) {
            Log.e(TAG, "syncOne failed", ex);
        }
    }

    static void drawBadge(Canvas canvas, Paint paint, int size, String text, Integer bg, Integer fg) {
        float s = size;
        int background = ((bg != null) ? bg : 0xFF5E5E5E) | 0xFF000000;
        int foreground = ((fg != null) ? fg : -1) | 0xFF000000;
        paint.setTextSize(0.42f * s);
        paint.setFakeBoldText(true);
        paint.setTextAlign(Paint.Align.CENTER);
        float width = paint.measureText(text) + 0.12f * s * 2f;
        float height = s * 0.5f;
        if (width < height) {
            width = height;
        }
        if (width > s) {
            width = s;
        }
        float left = s - width;
        float top = s - height;
        float radius = height * 0.5f;
        paint.setColor(background);
        canvas.drawRoundRect(left, top, s, s, radius, radius, paint);
        paint.setColor(foreground);
        Paint.FontMetrics fm = paint.getFontMetrics();
        float baseline = height * 0.5f - (fm.ascent + fm.descent) * 0.5f + top;
        canvas.drawText(text, left + width * 0.5f, baseline, paint);
    }

    static Bitmap render(Context context, Bitmap icon, String text, Integer bg, Integer fg, Boolean enabled) {
        int size = px(context);
        Bitmap out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setFilterBitmap(true);
        if (Boolean.FALSE.equals(enabled)) {
            paint.setAlpha(0x66);
        }
        Rect bounds = new Rect(0, 0, size, size);
        if (icon != null) {
            canvas.drawBitmap(icon, null, bounds, paint);
        } else {
            int id = context.getResources()
                .getIdentifier("mozac_ic_extension_fill_24", "drawable", context.getPackageName());
            if (id == 0) {
                id = context.getResources()
                    .getIdentifier("mozac_ic_extension_24", "drawable", context.getPackageName());
            }
            Drawable drawable = context.getDrawable(id);
            if (drawable != null) {
                drawable.setBounds(bounds);
                drawable.draw(canvas);
            }
        }
        if (text != null && !text.isEmpty()) {
            paint.setAlpha(0xFF);
            drawBadge(canvas, paint, size, text, bg, fg);
        }
        return out;
    }

    static Object makeButton(Context context, String id) throws Exception {
        PinState st = cache.get(id);
        Bitmap bitmap = null;
        String title = null;
        String text = null;
        Integer bg = null;
        Integer fg = null;
        Boolean enabled = null;
        if (st != null) {
            if (st.hidden) {
                return null;
            }
            bitmap = st.bitmap;
            title = st.title;
            text = st.text;
            bg = st.bg;
            fg = st.fg;
            enabled = st.enabled;
        }
        if (bitmap == null) {
            bitmap = savedIcon(context, id);
        }
        if (title == null) {
            title = prefs(context).getString("l:" + id, id);
        }

        Drawable drawable = new NoTintDrawable(context.getResources(), render(context, bitmap, text, bg, fg, enabled));
        Class<?> button = Class.forName(ACTION_BUTTON);
        Class<?> buttonState = Class.forName(BUTTON_STATE);
        Class<?> interaction = Class.forName(INTERACTION);
        Constructor<?> constructor = button.getDeclaredConstructor(Drawable.class, boolean.class, String.class,
                buttonState, boolean.class, interaction, interaction, String.class);
        constructor.setAccessible(true);
        return constructor.newInstance(drawable, false, title, buttonState.getField("DEFAULT").get(null), false,
                new PinEvent(id, false), new PinEvent(id, true), null);
    }

    public static void addPinned(ArrayList list, Object middleware) {
        try {
            if (!PatchSettings.pinEnabled()) {
                return;
            }
            Context context = (Context) Reflection.read(middleware, "uiContext");
            String[] ids = ids(context);
            int max = maxOf(middleware);
            int added = 0;
            for (int i = 0; i < ids.length && added < max; i++) {
                Object button = makeButton(context, ids[i]);
                if (button != null) {
                    list.add(button);
                    added++;
                }
            }
        } catch (Throwable ex) {
            Log.e(TAG, "addPinned failed", ex);
        }
    }

    static void click(Object middleware, String id) throws Exception {
        Object browserStore = Reflection.read(middleware, "browserStore");
        Object browserState = browserStore.getClass().getMethod("getState").invoke(browserStore);
        Map extensions = (Map) Reflection.read(browserState, "extensions");
        Object action = action(extensions.get(id));
        Map tabExtensions = tabExtensionState(browserState);
        Object tabAction = (tabExtensions != null) ? action(tabExtensions.get(id)) : null;
        if (tabAction != null) {
            action = tabAction;
        }
        if (action != null) {
            invokeFunction0(Reflection.read(action, "onClick"));
        }
    }

    static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, 0);
    }

    static boolean isPinned(Context context, String id) {
        for (String pinned : ids(context)) {
            if (pinned.equals(id)) {
                return true;
            }
        }
        return false;
    }

    static void pin(Context context, Object item) throws Exception {
        String id = (String) Reflection.read(item, "id");
        if (isPinned(context, id)) {
            return;
        }
        SharedPreferences prefs = prefs(context);
        String current = prefs.getString("ids", "");
        String updated = current.isEmpty() ? id : current + "\n" + id;
        raw = updated;
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("ids", updated);
        editor.putString("l:" + id, (String) Reflection.read(item, "label"));
        Bitmap icon = (Bitmap) Reflection.read(item, "icon");
        if (icon != null) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            icon.compress(Bitmap.CompressFormat.PNG, 100, out);
            editor.putString("i:" + id, Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP));
        }
        editor.apply();
    }

    static void unpin(Context context, String id) {
        StringBuilder builder = new StringBuilder();
        for (String pinned : ids(context)) {
            if (pinned.equals(id)) {
                continue;
            }
            if (builder.length() != 0) {
                builder.append("\n");
            }
            builder.append(pinned);
        }
        raw = builder.toString();
        prefs(context).edit().putString("ids", raw).remove("l:" + id).remove("i:" + id).apply();
    }

    public static Object wrap(Object base, Object item) {
        if (!PatchSettings.pinEnabled() || appCtx == null) {
            return base;
        }
        return new PinClick(base, new PinLongClick(appCtx, item));
    }

    public static Object longOf(Object function, Object stock) {
        return (function instanceof PinClick) ? ((PinClick) function).onLong : stock;
    }

    static void refresh() {
        try {
            if (store == null || mw == null) {
                return;
            }
            Object toolbarStore = store.get();
            Object middleware = mw.get();
            if (toolbarStore == null || middleware == null) {
                return;
            }

            ArrayList actions = new ArrayList();
            addPinned(actions, middleware);

            Object state = toolbarStore.getClass().getMethod("getState").invoke(toolbarStore);
            Object display = Reflection.read(state, "displayState");
            List current = (List) Reflection.read(display, "browserActionsEnd");
            Class<?> buttonClass = Class.forName(ACTION_BUTTON);
            for (Iterator it = current.iterator(); it.hasNext();) {
                Object entry = it.next();
                if (buttonClass.isInstance(entry) && Reflection.read(entry, "onClick") instanceof PinEvent) {
                    continue;
                }
                actions.add(entry);
            }

            Object update = Class.forName(ENDS_UPDATED).getConstructor(List.class).newInstance(actions);
            Class<?> action = Class.forName("mozilla.components.lib.state.Action");
            toolbarStore.getClass().getMethod("dispatch", action).invoke(toolbarStore, update);
        } catch (Throwable ex) {
            Log.e(TAG, "refresh failed", ex);
        }
    }

    static void applySetting() {
        try {
            Object middleware = (mw != null) ? mw.get() : null;
            if (PatchSettings.pinEnabled() && middleware != null) {
                start(middleware);
                kick();
            } else {
                refresh();
            }
        } catch (Throwable ex) {
            Log.e(TAG, "applySetting failed", ex);
        }
    }

    public static void onPinEvent(Object event, Object middleware) {
        try {
            PinEvent pinEvent = (PinEvent) event;
            if (pinEvent.isLong) {
                confirmUnpin((Context) Reflection.read(middleware, "uiContext"), pinEvent.id);
            } else {
                click(middleware, pinEvent.id);
            }
        } catch (Throwable ex) {
            Log.e(TAG, "pin event failed", ex);
        }
    }

    static int maxOf(Object middleware) {
        try {
            Object settings = Reflection.read(middleware, "settings");
            boolean expanded = (Boolean) settings.getClass().getMethod("getShouldUseExpandedToolbar").invoke(settings);
            return expanded ? Integer.MAX_VALUE : 5;
        } catch (Throwable ex) {
            return 5;
        }
    }

    static int maxPins() {
        Object middleware = (mw != null) ? mw.get() : null;
        return (middleware != null) ? maxOf(middleware) : 5;
    }

    static int visibleCount(Context context) {
        int count = 0;
        for (String id : ids(context)) {
            PinState st = cache.get(id);
            if (st == null || !st.hidden) {
                count++;
            }
        }
        return count;
    }

    static void confirmUnpin(final Context context, final String id) {
        PinState st = cache.get(id);
        String title = (st != null) ? st.title : null;
        if (title == null) {
            title = prefs(context).getString("l:" + id, id);
        }
        showMaterialDialog(dialogContext(context), title, null, null, "Unpin", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                unpin(context, id);
                refresh();
            }
        });
    }

    static void showDialog(final Context context, final Object item) {
        try {
            String id = (String) Reflection.read(item, "id");
            boolean pinned = isPinned(context, id);
            showMaterialDialog(dialogContext(context), (String) Reflection.read(item, "label"),
                    new CharSequence[] { pinned ? "Unpin from toolbar" : "Pin to toolbar" },
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            OldMenu.dismiss();
                            toggle(context, item);
                        }
                    }, null, null);
        } catch (Throwable ex) {
            Log.e(TAG, "showDialog failed", ex);
        }
    }

    private static void showMaterialDialog(Context context, CharSequence title, CharSequence[] items,
            DialogInterface.OnClickListener itemListener, CharSequence positive,
            DialogInterface.OnClickListener positiveListener) {
        try {
            Object builder = Class.forName("com.google.android.material.dialog.MaterialAlertDialogBuilder")
                .getConstructor(Context.class, int.class)
                .newInstance(context, 0);
            Object params = builder.getClass().getField("P").get(builder);
            Class<?> type = params.getClass();
            type.getField("mTitle").set(params, title);
            if (items != null) {
                type.getField("mItems").set(params, items);
                type.getField("mOnClickListener").set(params, itemListener);
            }
            if (positive != null) {
                type.getField("mPositiveButtonText").set(params, positive);
                type.getField("mPositiveButtonListener").set(params, positiveListener);
                type.getField("mNegativeButtonText").set(params, "Cancel");
            }
            ((Dialog) builder.getClass().getMethod("create").invoke(builder)).show();
        } catch (Throwable ex) {
            Log.e(TAG, "material dialog failed", ex);
            AlertDialog.Builder fallback = new AlertDialog.Builder(context).setTitle(title);
            if (items != null) {
                fallback.setItems(items, itemListener);
            }
            if (positive != null) {
                fallback.setPositiveButton(positive, positiveListener).setNegativeButton("Cancel", null);
            }
            fallback.show();
        }
    }

    private static Context dialogContext(Context fallback) {
        try {
            Object middleware = (mw != null) ? mw.get() : null;
            if (middleware != null) {
                Context context = (Context) Reflection.read(middleware, "uiContext");
                if (context instanceof Activity && !((Activity) context).isFinishing()) {
                    return context;
                }
            }
        } catch (Throwable ex) {
            Log.e(TAG, "ui context failed", ex);
        }
        return fallback;
    }

    static Object invokeFunction0(Object function) {
        try {
            if (function == null) {
                return null;
            }
            return Class.forName("kotlin.jvm.functions.Function0").getMethod("invoke").invoke(function);
        } catch (Throwable ex) {
            Log.e(TAG, "invoke failed", ex);
            return null;
        }
    }

}
