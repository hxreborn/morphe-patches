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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public final class ProcessNameTest {

    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void readsTheNameBeforeTheTerminator() throws IOException {
        // given
        File commandLine = commandLine("com.community.oneroom:push\0");

        // when
        String name = ProcessName.read(commandLine);

        // then
        Assertions.assertThat(name).isEqualTo("com.community.oneroom:push");
    }

    @Test
    public void readsANameWithoutATerminator() throws IOException {
        // given
        File commandLine = commandLine("com.community.oneroom");

        // when
        String name = ProcessName.read(commandLine);

        // then
        Assertions.assertThat(name).isEqualTo("com.community.oneroom");
    }

    @Test
    public void givesNoNameForAnEmptyCommandLine() throws IOException {
        // given
        File commandLine = commandLine("");

        // when
        String name = ProcessName.read(commandLine);

        // then
        Assertions.assertThat(name).isNull();
    }

    @Test
    public void givesNoNameForAMissingCommandLine() {
        // when
        String name = ProcessName.read(new File(this.folder.getRoot(), "missing"));

        // then
        Assertions.assertThat(name).isNull();
    }

    @Test
    public void readsTheNameOfThisProcess() {
        // when
        String name = ProcessName.current();

        // then
        Assertions.assertThat(name).isNotEmpty();
    }

    @Test
    public void treatsAProcessWithoutASuffixAsMain() {
        // when
        boolean main = ProcessName.isMain("com.community.oneroom");

        // then
        Assertions.assertThat(main).isTrue();
    }

    @Test
    public void treatsAProcessWithASuffixAsNotMain() {
        // when
        boolean main = ProcessName.isMain("com.community.oneroom:push");

        // then
        Assertions.assertThat(main).isFalse();
    }

    @Test
    public void treatsAnUnknownProcessAsNotMain() {
        // when
        boolean main = ProcessName.isMain(null);

        // then
        Assertions.assertThat(main).isFalse();
    }

    private File commandLine(String content) throws IOException {
        File file = this.folder.newFile();
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
        return file;
    }

}
