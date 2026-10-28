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
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
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

    private static final long TOAST_WAIT_MILLIS = 5000L;

    private static final long NO_TOAST_WAIT_MILLIS = 500L;

    private static final long POLL_MILLIS = 20L;

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
    public void showsTheReasonWhenTheNamedHookClassIsMissing() throws IOException, InterruptedException {
        // given
        Boot.packerFactory(givenAppNamingHook("hx.MissingHook"));

        // when
        Boot.runAppHook(BootTest.class.getClassLoader());

        // then
        Assertions.assertThat(latestToastWithin(TOAST_WAIT_MILLIS))
            .isEqualTo(Boot.APP_HOOK_FAILED + ": ClassNotFoundException: hx.MissingHook");
    }

    @Test
    public void showsNoToastWhenTheAppNamesNoHook() throws IOException, InterruptedException {
        // given
        Boot.packerFactory(givenAppWithoutHook());

        // when
        Boot.runAppHook(BootTest.class.getClassLoader());

        // then
        Assertions.assertThat(latestToastWithin(NO_TOAST_WAIT_MILLIS)).isNull();
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

    private static String latestToastWithin(long timeoutMillis) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (true) {
            ShadowLooper.idleMainLooper();
            String text = ShadowToast.getTextOfLatestToast();
            if (text != null || System.currentTimeMillis() >= deadline) {
                return text;
            }
            Thread.sleep(POLL_MILLIS);
        }
    }

    private ApplicationInfo givenAppWithCodeOverlay() throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("assets/hx.code", new byte[] { 1, 2, 3, 4 });
        entries.put("assets/hx.classes", new byte[0]);
        return givenApp(entries);
    }

    private ApplicationInfo givenAppNamingHook(String hookClass) throws IOException {
        return givenApp(Collections.singletonMap("assets/hx.hook", hookClass.getBytes(StandardCharsets.UTF_8)));
    }

    private ApplicationInfo givenAppWithoutHook() throws IOException {
        return givenApp(Collections.<String, byte[]>emptyMap());
    }

    private ApplicationInfo givenApp(Map<String, byte[]> entries) throws IOException {
        File apk = this.folder.newFile();
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(apk))) {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                out.putNextEntry(new ZipEntry(entry.getKey()));
                out.write(entry.getValue());
                out.closeEntry();
            }
        }
        ApplicationInfo info = new ApplicationInfo();
        info.sourceDir = apk.getPath();
        return info;
    }

}
