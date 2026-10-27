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
package hx.gateway;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public final class DigestsTest {

    @Test
    public void hashesWithMd5() throws NoSuchAlgorithmException {
        // when
        String digest = Digests.md5Hex("abc".getBytes(StandardCharsets.UTF_8));

        // then
        Assertions.assertThat(digest).isEqualTo("900150983cd24fb0d6963f7d28e17f72");
    }

    @Test
    public void hashesWithSha256() throws NoSuchAlgorithmException {
        // when
        String digest = Digests.sha256Hex("abc".getBytes(StandardCharsets.UTF_8));

        // then
        Assertions.assertThat(digest).isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    public void padsEveryByteToTwoHexDigits() throws NoSuchAlgorithmException {
        // when
        String digest = Digests.md5Hex(new byte[0]);

        // then
        Assertions.assertThat(digest).isEqualTo("d41d8cd98f00b204e9800998ecf8427e");
    }

}
