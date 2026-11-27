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
package app.hxreborn.extension.viber;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;

@SuppressWarnings("unused")
public final class ContactSearch {

    private static final String FOLD_ACCENTS_FUNCTION = "hx_fold_accents";

    private static final char FIRST_COMBINING_DIACRITIC = 0x0300;

    private static final char LAST_COMBINING_DIACRITIC = 0x036F;

    private ContactSearch() {
    }

    public static void addFoldAccentsFunction(Map<String, Object> scalarFunctions) {
        scalarFunctions.put(FOLD_ACCENTS_FUNCTION, new ContactSearch());
    }

    public static String searchedColumn(String column) {
        return PatchSettings.ignoreAccentsInContactSearch() ? FOLD_ACCENTS_FUNCTION + "(" + column + ")" : column;
    }

    public static String searchedText(String text) {
        return PatchSettings.ignoreAccentsInContactSearch() ? foldAccents(text) : text;
    }

    public static String foldAccents(String text) {
        if (text == null) {
            return null;
        }
        String caseFolded = text.toUpperCase(Locale.ROOT).toLowerCase(Locale.ROOT);
        String decomposed = Normalizer.normalize(caseFolded, Normalizer.Form.NFD);
        StringBuilder withoutDiacritics = new StringBuilder(decomposed.length());
        for (int i = 0; i < decomposed.length(); i++) {
            char character = decomposed.charAt(i);
            if (character < FIRST_COMBINING_DIACRITIC || character > LAST_COMBINING_DIACRITIC) {
                withoutDiacritics.append(character);
            }
        }
        return Normalizer.normalize(withoutDiacritics, Normalizer.Form.NFC);
    }

}
