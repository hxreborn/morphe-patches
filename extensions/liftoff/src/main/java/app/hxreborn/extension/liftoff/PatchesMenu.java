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
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

@SuppressWarnings("unused")
public final class PatchesMenu {

    public static final String SETTINGS_ROW_TITLE = "hxreborn patches";

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
                || !(scroll.getChildAt(0) instanceof ViewGroup content)) {
            return;
        }
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
                new PatchesEntryRow(activity, scroll, content, template, (ViewGroup) anchor).attach();
            }
        });
    }

    private static TextView findLabel(View view, Set<String> labels) {
        if (view instanceof TextView && labels.contains(((TextView) view).getText().toString())) {
            return (TextView) view;
        }
        if (view instanceof ViewGroup group) {
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

}
