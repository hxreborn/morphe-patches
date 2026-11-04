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
package app.hxreborn.extension.liftoff;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.text.TextPaint;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

@SuppressWarnings("unused")
public final class PatchesMenu {

    public static final String SETTINGS_ROW_TITLE = "hxreborn patches";

    private static final String ICON_NAME = "hx_liftoff_ic_entry";

    private static final int HOST_ROW_CHILD_COUNT = 3;

    private static final Set<String> RESTORE_LABELS = new HashSet<>(
            Arrays.asList("Restore Purchases", "Restaurer les achats", "Restaurar Compras", "Käufe wiederherstellen",
                    "購入を復元", "구매 복원", "Obnovit nákupy", "Återställ köp", "Ανάκτηση αγορών", "Ripristina acquisti",
                    "Восстановить покупки", "Przywracanie zakupów", "Aankopen herstellen"));

    private static final Set<String> DISCORD_LABELS = new HashSet<>(Arrays.asList("Join Our Discord",
            "Rejoins notre Discord", "Únete a Nuestro Discord", "Tritt unserem Discord bei", "Discordに参加",
            "Discord 참여하기", "Junte-se ao Nosso Discord", "Přidej Se na Náš Discord", "Gå Med I Vår Discord",
            "Μπες στο Discord μας", "Entra nel nostro Discord", "Вступай в наш Discord", "Dołącz na naszego Discorda",
            "Word Lid van Onze Discord"));

    private PatchesMenu() {
    }

    public static void onScrollLayout(ScrollView scroll) {
        Activity activity = PatchContext.resumedActivity();
        if (activity == null || !scroll.isShown() || scroll.getChildCount() != 1
                || !(scroll.getChildAt(0) instanceof ViewGroup)) {
            return;
        }
        ViewGroup content = (ViewGroup) scroll.getChildAt(0);
        if (content.findViewWithTag(scroll) != null) {
            return;
        }
        TextView template = findLabel(content, RESTORE_LABELS);
        if (template == null || findLabel(content, DISCORD_LABELS) == null) {
            return;
        }
        View anchor = contentChild(content, template);
        if (!(anchor instanceof ViewGroup)) {
            return;
        }
        scroll.post(() -> {
            if (scroll.isAttachedToWindow() && content.findViewWithTag(scroll) == null) {
                new SettingsEntry(activity, scroll, content, template, (ViewGroup) anchor).attach();
            }
        });
    }

    private static TextView findLabel(View view, Set<String> labels) {
        if (view instanceof TextView && labels.contains(((TextView) view).getText().toString())) {
            return (TextView) view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int index = 0; index < group.getChildCount(); index++) {
                TextView found = findLabel(group.getChildAt(index), labels);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static View contentChild(View content, View descendant) {
        View current = descendant;
        while (current != null && current.getParent() != content) {
            current = (current.getParent() instanceof View) ? (View) current.getParent() : null;
        }
        return current;
    }

    private static final class SettingsEntry
            implements ViewTreeObserver.OnGlobalLayoutListener, View.OnAttachStateChangeListener {

        private final ScrollView scroll;

        private final ViewGroup content;

        private final TextView template;

        private final ViewGroup anchor;

        private final FrameLayout row;

        private final ImageView icon;

        private final TextView title;

        private final ImageView chevron;

        private final Rect hostBounds = new Rect();

        private final int originalBottomPadding;

        private int appliedColor;

        private CharSequence templateText;

        SettingsEntry(Activity activity, ScrollView scroll, ViewGroup content, TextView template, ViewGroup anchor) {
            this.scroll = scroll;
            this.content = content;
            this.template = template;
            this.anchor = anchor;
            this.originalBottomPadding = scroll.getPaddingBottom();
            this.row = new FrameLayout(activity);
            this.row.setTag(scroll);
            this.row.setLayoutDirection(template.getLayoutDirection());
            this.row.setAccessibilityDelegate(new View.AccessibilityDelegate() {
                @Override
                public void onInitializeAccessibilityNodeInfo(View view, AccessibilityNodeInfo info) {
                    super.onInitializeAccessibilityNodeInfo(view, info);
                    info.setClassName(Button.class.getName());
                    info.setText(SETTINGS_ROW_TITLE);
                }
            });
            this.row.setOnClickListener((view) -> new PatchesSettingsDialog(activity, this.template).show());
            this.icon = new ImageView(activity);
            this.icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            this.icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            this.title = new TextView(activity);
            this.title.setText(SETTINGS_ROW_TITLE);
            this.title.setGravity(Gravity.CENTER_VERTICAL);
            this.title.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            this.chevron = new ImageView(activity);
            this.chevron.setScaleType(ImageView.ScaleType.FIT_XY);
            this.chevron.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            this.row.addView(this.icon, new FrameLayout.LayoutParams(0, 0));
            this.row.addView(this.title, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            this.row.addView(this.chevron, new FrameLayout.LayoutParams(0, 0));
            applyHostStyle();
        }

        void attach() {
            this.content.addView(this.row, new ViewGroup.LayoutParams(0, 0));
            this.scroll.addOnAttachStateChangeListener(this);
            this.scroll.getViewTreeObserver().addOnGlobalLayoutListener(this);
            update();
        }

        @Override
        public void onGlobalLayout() {
            update();
        }

        private void update() {
            if (this.templateText != this.template.getText()
                    || this.appliedColor != this.template.getCurrentTextColor()) {
                applyHostStyle();
            }
            if (this.anchor.getParent() != this.content || this.anchor.getHeight() == 0) {
                this.row.setVisibility(View.GONE);
                return;
            }
            int rowHeight = this.anchor.getHeight();
            int gap = gapAboveAnchor();
            int shift = rowHeight + gap;
            shiftFollowingRows(shift);
            int bottomPadding = this.originalBottomPadding + shift;
            if (this.scroll.getPaddingBottom() != bottomPadding) {
                this.scroll.setPadding(this.scroll.getPaddingLeft(), this.scroll.getPaddingTop(),
                        this.scroll.getPaddingRight(), bottomPadding);
            }
            layoutHostMetrics();
            this.row.setVisibility(View.VISIBLE);
            int left = this.anchor.getLeft();
            int top = this.anchor.getBottom() + gap;
            this.row.measure(View.MeasureSpec.makeMeasureSpec(this.anchor.getWidth(), View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(rowHeight, View.MeasureSpec.EXACTLY));
            this.row.layout(left, top, left + this.anchor.getWidth(), top + rowHeight);
        }

        private int gapAboveAnchor() {
            int index = this.content.indexOfChild(this.anchor);
            if (index <= 0) {
                return 0;
            }
            return Math.max(0, this.anchor.getTop() - this.content.getChildAt(index - 1).getBottom());
        }

        private void shiftFollowingRows(int shift) {
            for (int index = this.content.indexOfChild(this.anchor) + 1; index < this.content
                .getChildCount(); index++) {
                View following = this.content.getChildAt(index);
                if (following != this.row && following.getTranslationY() != shift) {
                    following.setTranslationY(shift);
                }
            }
        }

        private void layoutHostMetrics() {
            View hostIconView = hostIconView();
            if (hostIconView != null) {
                this.hostBounds.set(0, 0, hostIconView.getWidth(), hostIconView.getHeight());
                this.anchor.offsetDescendantRectToMyCoords(hostIconView, this.hostBounds);
                place(this.icon, this.hostBounds.left, this.hostBounds.width(), this.hostBounds.height());
            }
            this.template.getDrawingRect(this.hostBounds);
            this.anchor.offsetDescendantRectToMyCoords(this.template, this.hostBounds);
            place(this.title, this.hostBounds.left, ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            View hostChevronView = hostChevronView();
            if (hostChevronView != null) {
                place(this.chevron, hostChevronView.getLeft(), hostChevronView.getWidth(), hostChevronView.getHeight());
            }
        }

        private void place(View view, int left, int width, int height) {
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) view.getLayoutParams();
            if (params.leftMargin != left || params.width != width || params.height != height) {
                params.leftMargin = left;
                params.width = width;
                params.height = height;
                params.gravity = Gravity.START | Gravity.CENTER_VERTICAL;
                view.setLayoutParams(params);
            }
        }

        private View hostIconView() {
            View slot = this.anchor.getChildAt(0);
            if (slot instanceof ViewGroup && ((ViewGroup) slot).getChildCount() > 0) {
                return ((ViewGroup) slot).getChildAt(0);
            }
            return null;
        }

        private View hostChevronView() {
            int count = this.anchor.getChildCount();
            return (count >= HOST_ROW_CHILD_COUNT) ? this.anchor.getChildAt(count - 1) : null;
        }

        private void applyHostStyle() {
            TextPaint hostText = SettingsStyle.hostTextPaint(this.template);
            this.title.setTextSize(TypedValue.COMPLEX_UNIT_PX, hostText.getTextSize());
            this.title.setTypeface(hostText.getTypeface());
            this.title.setTextColor(hostText.getColor());
            SettingsStyle style = new SettingsStyle(this.row.getContext(), this.template);
            this.row.setBackground(style.ripple(0));
            this.icon.setImageDrawable(style.hostIcon(ICON_NAME, hostText.getColor()));
            drawHostChevron();
            this.appliedColor = this.template.getCurrentTextColor();
            this.templateText = this.template.getText();
        }

        private void drawHostChevron() {
            View hostChevronView = hostChevronView();
            if (hostChevronView == null || hostChevronView.getWidth() == 0 || hostChevronView.getHeight() == 0) {
                return;
            }
            Bitmap bitmap = Bitmap.createBitmap(hostChevronView.getWidth(), hostChevronView.getHeight(),
                    Bitmap.Config.ARGB_8888);
            hostChevronView.draw(new Canvas(bitmap));
            this.chevron.setImageBitmap(bitmap);
        }

        @Override
        public void onViewAttachedToWindow(View view) {
        }

        @Override
        public void onViewDetachedFromWindow(View view) {
            this.scroll.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            this.scroll.removeOnAttachStateChangeListener(this);
            shiftFollowingRows(0);
            this.content.removeView(this.row);
            this.scroll.setPadding(this.scroll.getPaddingLeft(), this.scroll.getPaddingTop(),
                    this.scroll.getPaddingRight(), this.originalBottomPadding);
        }

    }

}
