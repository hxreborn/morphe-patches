/*
 * Copyright (C) 2026 BlazeFTL
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from BlazeFTL/FTL-Patches:
 * https://github.com/BlazeFTL/FTL-Patches/commit/dd13beaf9ad004d59b5107a33304606581b0687f
 * Commit dd13beaf9ad004d59b5107a33304606581b0687f (2026-10-25),
 * extensions/firefox/src/main/java/app/ftl/extension/firefox/OldMenu.java
 */
package app.hxreborn.extension.firefox;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.Window;
import android.view.WindowInsets;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;

import app.hxreborn.extension.shared.Dimensions;

@SuppressWarnings("unused")
public final class OldMenu {

    public static boolean extensionsActive;

    private static final String TAG = "FirefoxPatches";

    private static final String BEHAVIOR = "com.google.android.material.bottomsheet.BottomSheetBehavior";

    private static final int FLAG_DIM_BEHIND = 2;

    private static final ViewOutlineProvider OUTLINE = new ViewOutlineProvider() {
        @Override
        public void getOutline(View view, Outline outline) {
            outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), Dimensions.dpExact(view.getContext(), 12));
        }
    };

    private static boolean bottomToolbar;

    private static Context appContext;

    private static Set<String> longBadges;

    private static WeakReference<Dialog> dialogRef = new WeakReference<>(null);

    private static WeakReference<View> sheetRef = new WeakReference<>(null);

    private static WeakReference<Object> behaviorRef = new WeakReference<>(null);

    private static WeakReference<View> enterPlayedFor = new WeakReference<>(null);

    private OldMenu() {
    }

    public static void onCreateDialog(Dialog dialog) {
        try {
            PatchSettings.latch(dialog.getContext());
            extensionsActive = false;
            bottomToolbar = isBottom(dialog.getContext());
            appContext = dialog.getContext().getApplicationContext();
            if (!PatchSettings.oldMenu()) {
                return;
            }
            Window window = dialog.getWindow();
            if (window == null) {
                return;
            }
            window.clearFlags(FLAG_DIM_BEHIND);
            window.setDimAmount(0f);
            window.setWindowAnimations(animStyle(dialog.getContext()));
        } catch (Throwable ex) {
            Log.e(TAG, "onCreateDialog failed", ex);
        }
    }

    public static void onShowStart(DialogInterface dialog) {
        if (dialog instanceof Dialog) {
            dialogRef = new WeakReference<>((Dialog) dialog);
        }
    }

    static void dismiss() {
        Dialog dialog = dialogRef.get();
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    public static void afterShow() {
        if (!PatchSettings.oldMenu()) {
            return;
        }
        apply(dialogRef.get());
    }

    public static void onViewCreated(Dialog dialog) {
        if (!PatchSettings.oldMenu() || dialog == null) {
            return;
        }
        dialogRef = new WeakReference<>(dialog);
        apply(dialog);
        View sheet = findSheet(dialog);
        if (sheet != null) {
            playEnter(sheet);
        }
    }

    public static int menuWidth(Resources resources, int dp) {
        return (int) (resources.getDisplayMetrics().density * dp);
    }

    public static void setExtensionsActive(Object accessPoint, boolean expanded) {
        boolean browser = accessPoint instanceof Enum && "Browser".equals(((Enum<?>) accessPoint).name());
        extensionsActive = expanded && browser;
        applyWidth(extensionsActive ? 314 : 240);
    }

    public static float navPadding() {
        return bottomToolbar ? 9f : 12f;
    }

    public static float bottomPadding(float stock, boolean expanded) {
        if (stock == 16f || expanded) {
            return bottomToolbar ? 0f : 6f;
        }
        return 48f;
    }

    public static boolean hideBadge(String text) {
        try {
            if (text == null || !PatchSettings.oldMenu() || appContext == null) {
                return false;
            }
            if (longBadges == null) {
                Set<String> set = new HashSet<>();
                Resources res = appContext.getResources();
                String[] names = { "ip_protection_menu_try_vpn_cta", "ip_protection_menu_error",
                        "ip_protection_menu_paused", "ip_protection_menu_connecting", };
                for (String name : names) {
                    int id = res.getIdentifier(name, "string", appContext.getPackageName());
                    if (id != 0) {
                        set.add(res.getString(id));
                    }
                }
                longBadges = set;
            }
            return longBadges.contains(text);
        } catch (Throwable ex) {
            return false;
        }
    }

    public static boolean showBottomDivider() {
        return bottomToolbar && PatchSettings.oldMenu();
    }

    public static int patchesIcon(Context context) {
        return Drawables.id(context, "mozac_ic_tool_24");
    }

    public static Object trailingIcon(Object stock) {
        return PatchSettings.oldMenu() ? null : stock;
    }

    public static String accountSubtitle(String stock) {
        return PatchSettings.oldMenu() ? null : stock;
    }

    private static void apply(Dialog dialog) {
        if (dialog == null) {
            return;
        }
        try {
            Window window = dialog.getWindow();
            if (window != null) {
                window.clearFlags(FLAG_DIM_BEHIND);
                window.setWindowAnimations(animStyle(window.getContext()));
                Drawable bg = window.getDecorView().getBackground();
                if (bg instanceof ColorDrawable && ((ColorDrawable) bg).getAlpha() == 100) {
                    window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                }
            }

            View sheet = findSheet(dialog);
            if (sheet == null) {
                return;
            }

            if (sheetRef.get() != sheet) {
                sheetRef = new WeakReference<>(sheet);
                sheet.getViewTreeObserver().addOnPreDrawListener(() -> {
                    pinSheet(sheet);
                    return true;
                });
                sheet.setOutlineProvider(OUTLINE);
                sheet.setClipToOutline(true);
            }

            Object behavior = behaviorOf(sheet);
            behaviorRef = new WeakReference<>(behavior);
            if (behavior != null) {
                setHideable(behavior, false);
                setBoolean(behavior, "draggable", false);
            }

            float density = sheet.getContext().getResources().getDisplayMetrics().density;
            Drawable bg = sheet.getBackground();
            if (bg instanceof GradientDrawable) {
                ((GradientDrawable) bg).setCornerRadius(12 * density);
            }
            sheet.setElevation(8 * density);

            ViewGroup.LayoutParams lp = sheet.getLayoutParams();
            if (lp != null && lp.getClass()
                .getName()
                .equals("androidx.coordinatorlayout.widget.CoordinatorLayout$LayoutParams")) {
                lp.getClass().getField("gravity").setInt(lp, 0x800035);
                ((ViewGroup.MarginLayoutParams) lp).setMarginEnd((int) (8 * density));
                sheet.setLayoutParams(lp);
            }
        } catch (Throwable ex) {
            Log.e(TAG, "apply failed", ex);
        }
    }

    private static void pinSheet(View sheet) {
        if (!(sheet.getParent() instanceof View)) {
            return;
        }
        int parentHeight = ((View) sheet.getParent()).getHeight();
        if (parentHeight == 0) {
            return;
        }
        float margin = Dimensions.dpExact(sheet.getContext(), 8);
        WindowInsets insets = sheet.getRootWindowInsets();
        int statusBar = (insets != null) ? insets.getSystemWindowInsetTop() : 0;
        capHeight(sheet, parentHeight - statusBar - (int) (2 * margin));
        float translation = bottomToolbar ? -margin : (statusBar + margin - sheet.getTop());
        if (sheet.getTranslationY() != translation) {
            sheet.setTranslationY(translation);
        }
    }

    private static void capHeight(View sheet, int height) {
        try {
            Object behavior = behaviorRef.get();
            if (behavior == null) {
                return;
            }
            Field f = behaviorField("maxHeight");
            if (f.getInt(behavior) != height) {
                f.setInt(behavior, height);
                sheet.requestLayout();
            }
        } catch (Throwable ex) {
            Log.e(TAG, "capHeight failed", ex);
        }
    }

    private static void applyWidth(int dp) {
        try {
            View sheet = sheetRef.get();
            Object behavior = behaviorRef.get();
            if (sheet == null || behavior == null) {
                return;
            }
            Resources res = sheet.getContext().getResources();
            int width = Math.min(menuWidth(res, dp), res.getDisplayMetrics().widthPixels);
            Field f = behaviorField("maxWidth");
            if (f.getInt(behavior) != width) {
                f.setInt(behavior, width);
                sheet.requestLayout();
            }
        } catch (Throwable ex) {
            Log.e(TAG, "applyWidth failed", ex);
        }
    }

    private static View findSheet(Dialog dialog) {
        Context c = dialog.getContext();
        int id = c.getResources().getIdentifier("design_bottom_sheet", "id", c.getPackageName());
        return (id == 0) ? null : dialog.findViewById(id);
    }

    private static Object behaviorOf(View sheet) {
        try {
            Method from = Class.forName(BEHAVIOR).getMethod("from", View.class);
            return from.invoke(null, sheet);
        } catch (Throwable ex) {
            return null;
        }
    }

    private static void setHideable(Object behavior, boolean value) {
        try {
            behavior.getClass().getMethod("setHideable", boolean.class).invoke(behavior, value);
        } catch (Throwable ignored) {
        }
        setBoolean(behavior, "hideable", value);
    }

    private static void setBoolean(Object target, String field, boolean value) {
        try {
            behaviorField(field).setBoolean(target, value);
        } catch (Throwable ignored) {
        }
    }

    private static Field behaviorField(String name) throws ReflectiveOperationException {
        Field field = Class.forName(BEHAVIOR).getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static void playEnter(View sheet) {
        if (enterPlayedFor.get() == sheet || !isBottom(sheet.getContext())) {
            return;
        }
        enterPlayedFor = new WeakReference<>(sheet);

        AnimationSet set = new AnimationSet(true);
        set.addAnimation(new ScaleAnimation(0f, 1f, 0f, 1f, ScaleAnimation.RELATIVE_TO_SELF, 0.95f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.95f));
        set.addAnimation(new AlphaAnimation(0f, 1f));
        set.setInterpolator(new AccelerateDecelerateInterpolator());
        set.setDuration(200);
        sheet.startAnimation(set);
    }

    private static int animStyle(Context context) {
        if (isBottom(context)) {
            return 0;
        }
        return context.getResources()
            .getIdentifier("Mozac_Browser_Menu_Animation_OverflowMenuTop", "style", context.getPackageName());
    }

    private static boolean isBottom(Context context) {
        try {
            Object settings = settings(context);
            return (Boolean) settings.getClass().getMethod("getShouldUseBottomToolbar").invoke(settings);
        } catch (Throwable ex) {
            Log.e(TAG, "toolbar position failed", ex);
            return false;
        }
    }

    private static Object settings(Context context) throws Exception {
        Class<?> contextKt = Class.forName("org.mozilla.fenix.ext.ContextKt");
        Object app = contextKt.getMethod("getApplication", Context.class).invoke(null, context);
        Object components = app.getClass().getMethod("getComponents").invoke(app);
        return components.getClass().getMethod("getSettings").invoke(components);
    }

}
