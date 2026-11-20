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
package app.hxreborn.extension.ultimea.equalizer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import org.json.JSONException;

final class EqualizerPresetTransfer {

    private final EqualizerPresetStore store;

    EqualizerPresetTransfer(Context context) {
        this.store = new EqualizerPresetStore(context);
    }

    List<EqualizerPreset> importDocument(String document) throws JSONException, IOException {
        return importDocumentWithReport(document).imported;
    }

    ImportResult importDocumentWithReport(String document) throws JSONException, IOException {
        EqualizerPresetBundleFile.ReadResult read = EqualizerPresetBundleFile.readReport(document);
        List<EqualizerPresetFile> normalized = new ArrayList<>();
        int adapted = 0;
        for (EqualizerPresetFile file : read.presets) {
            EqualizerPresetFile playable = file.playable();
            if (!playable.state.equals(file.state) || !playable.base.equals(file.base)) {
                adapted++;
            }
            normalized.add(playable);
        }
        List<EqualizerPreset> imported = normalized.isEmpty() ? Collections.emptyList() : this.store.addAll(normalized);
        return new ImportResult(imported, read.rejected, read.ignoredFields, adapted);
    }

    String exportDocument() throws JSONException {
        return EqualizerPresetBundleFile.json(this.store.load()).toString(2);
    }

    static final class ImportResult {

        final List<EqualizerPreset> imported;

        final List<EqualizerPresetBundleFile.RejectedPreset> rejected;

        final int ignoredFields;

        final int adapted;

        ImportResult(List<EqualizerPreset> imported, List<EqualizerPresetBundleFile.RejectedPreset> rejected,
                int ignoredFields, int adapted) {
            this.imported = Collections.unmodifiableList(new ArrayList<>(imported));
            this.rejected = Collections.unmodifiableList(new ArrayList<>(rejected));
            this.ignoredFields = ignoredFields;
            this.adapted = adapted;
        }

    }

}
