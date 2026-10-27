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
package hx.platform;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import android.content.Context;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public final class HotPatchesTest {

    private final Context context = RuntimeEnvironment.getApplication();

    @Test
    public void removesTheAppliedPatchDirectory() throws IOException {
        // given
        File patches = givenAppliedPatch();

        // when
        boolean removed = HotPatches.removeApplied(this.context);

        // then
        Assertions.assertThat(removed).isTrue();
        Assertions.assertThat(patches).doesNotExist();
    }

    @Test
    public void reportsNothingRemovedWithoutAPatchDirectory() {
        // when
        boolean removed = HotPatches.removeApplied(this.context);

        // then
        Assertions.assertThat(removed).isFalse();
    }

    private File givenAppliedPatch() throws IOException {
        File patches = new File(this.context.getApplicationInfo().dataDir, "tinker");
        File version = new File(patches, "patch-1a2b3c4d");
        Assertions.assertThat(version.mkdirs()).isTrue();
        Files.write(new File(version, "patch-1a2b3c4d.apk").toPath(), "patch".getBytes(StandardCharsets.UTF_8));
        Files.write(new File(patches, "patch.info").toPath(), "info".getBytes(StandardCharsets.UTF_8));
        return patches;
    }

}
