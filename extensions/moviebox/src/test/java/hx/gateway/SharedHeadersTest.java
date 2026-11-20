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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public final class SharedHeadersTest {

    @Test
    public void findsTheStaticHeaderMapOfTheStore() throws ReflectiveOperationException {
        // when
        Map<String, String> headers = SharedHeaders.of(HeaderStore.class);

        // then
        Assertions.assertThat(headers).isSameAs(HeaderStore.HEADERS);
    }

    @Test
    public void rejectsAClassWithoutAHeaderMap() {
        // when
        Throwable failure = Assertions.catchThrowable(() -> SharedHeaders.of(String.class));

        // then
        Assertions.assertThat(failure).isInstanceOf(NoSuchFieldException.class)
            .hasMessage("no static header map on java.lang.String");
    }

    private static final class HeaderStore {

        private static final String NAME = "store";

        private static final Map<String, String> HEADERS = new ConcurrentHashMap<>();

        private final Map<String, String> instanceHeaders = new ConcurrentHashMap<>();

    }

}
