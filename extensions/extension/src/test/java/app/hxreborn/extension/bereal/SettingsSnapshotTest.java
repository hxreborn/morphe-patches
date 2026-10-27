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
package app.hxreborn.extension.bereal;

import static org.junit.Assert.assertEquals;

import android.content.ContentResolver;
import android.net.Uri;

import app.hxreborn.extension.bereal.download.DownloadFolder;
import app.morphe.extension.shared.Utils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class SettingsSnapshotTest {

    private static final Uri PICKED_FOLDER = Uri
        .parse("content://com.android.externalstorage.documents/tree/primary%3APictures%2FBeReal");

    @Before
    public void setContext() {
        Utils.setContext(RuntimeEnvironment.getApplication());
    }

    @After
    public void restoreDefaults() {
        SettingsSnapshot.resetToDefaults();
    }

    @Test
    public void releasesAFolderPickedAfterTheResetOnUndo() {
        // given
        final SettingsSnapshot snapshot = SettingsSnapshot.capture();
        DownloadFolder.choose(resolver(), PICKED_FOLDER);

        // when
        snapshot.restore(resolver());

        // then
        assertEquals(0, resolver().getPersistedUriPermissions().size());
    }

    private static ContentResolver resolver() {
        return RuntimeEnvironment.getApplication().getContentResolver();
    }

}
