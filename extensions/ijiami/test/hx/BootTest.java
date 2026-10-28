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
package hx;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import android.content.pm.ApplicationInfo;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadows.ShadowToast;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public final class BootTest {

    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void showsTheReasonWhenTheCodeOverlayCannotBeWritten() throws IOException {
        // given
        ApplicationInfo info = givenAppWithCodeOverlay();

        // when
        Boot.writeCodeOverlay(BootTest.class.getClassLoader(), info);

        // then
        ShadowLooper.idleMainLooper();
        Assertions.assertThat(ShadowToast.getTextOfLatestToast())
            .startsWith(Boot.CODE_OVERLAY_FAILED + ": UnsatisfiedLinkError");
    }

    @Test
    public void namesTheCauseOfAFailedHookCall() {
        // given
        Throwable failure = new InvocationTargetException(new NoSuchFieldException("no header map"));

        // when
        String notice = Boot.failureNotice(Boot.APP_HOOK_FAILED, failure);

        // then
        Assertions.assertThat(notice).isEqualTo(Boot.APP_HOOK_FAILED + ": NoSuchFieldException: no header map");
    }

    @Test
    public void namesAFailureWithoutAMessage() {
        // when
        String notice = Boot.failureNotice(Boot.APP_HOOK_FAILED, new IllegalStateException());

        // then
        Assertions.assertThat(notice).isEqualTo(Boot.APP_HOOK_FAILED + ": IllegalStateException");
    }

    private ApplicationInfo givenAppWithCodeOverlay() throws IOException {
        File apk = this.folder.newFile("base.apk");
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(apk))) {
            out.putNextEntry(new ZipEntry("assets/hx.code"));
            out.write(new byte[] { 1, 2, 3, 4 });
            out.closeEntry();
            out.putNextEntry(new ZipEntry("assets/hx.classes"));
            out.closeEntry();
        }
        ApplicationInfo info = new ApplicationInfo();
        info.sourceDir = apk.getPath();
        return info;
    }

}
