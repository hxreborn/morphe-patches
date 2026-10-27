/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package hx;

import java.nio.charset.StandardCharsets;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public final class DigestsTest {

    @Test
    public void hashesWithMd5() throws Exception {
        // when
        String digest = Digests.md5Hex("abc".getBytes(StandardCharsets.UTF_8));

        // then
        Assertions.assertThat(digest).isEqualTo("900150983cd24fb0d6963f7d28e17f72");
    }

    @Test
    public void hashesWithSha256() throws Exception {
        // when
        String digest = Digests.sha256Hex("abc".getBytes(StandardCharsets.UTF_8));

        // then
        Assertions.assertThat(digest).isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    public void padsEveryByteToTwoHexDigits() throws Exception {
        // when
        String digest = Digests.md5Hex(new byte[0]);

        // then
        Assertions.assertThat(digest).isEqualTo("d41d8cd98f00b204e9800998ecf8427e");
    }

}
