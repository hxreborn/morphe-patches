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
package app.hxreborn.extension.netanalyzer;

final class MaterialIcons {

    static final String CHEVRON = "M 12.6 12 L 8 7.4 L 9.4 6 l 6 6 l -6 6 L 8 16.6 L 12.6 12 Z";

    static final String BACK = """
            M 7.83 13 l 5.6 5.6 L 12 20 L 4 12 L 12 4 l 1.43 1.4 L 7.83 11 H 20 v 2 H \
            7.83 Z\
            """;

    static final String HISTORY = """
            M 12 21 Q 8.55 21 5.99 18.71 Q 3.43 16.43 3.05 13 H 5.1 q 0.35 2.6 2.31 \
            4.3 T 12 19 q 2.93 0 4.96 -2.04 Q 19 14.93 19 12 Q 19 9.07 16.96 7.04 T \
            12 5 Q 10.28 5 8.78 5.8 T 6.25 8 H 9 v 2 H 3 V 4 H 5 V 6.35 Q 6.28 4.75 \
            8.11 3.88 T 12 3 q 1.88 0 3.51 0.71 t 2.85 1.93 t 1.93 2.85 T 21 12 t \
            -0.71 3.51 t -1.93 2.85 t -2.85 1.93 T 12 21 Z m 2.8 -4.8 L 11 12.4 V 7 \
            h 2 v 4.6 l 3.2 3.2 l -1.4 1.4 Z\
            """;

    static final String MORE = """
            M 12 20 q -0.82 0 -1.41 -0.59 T 10 18 t 0.59 -1.41 T 12 16 t 1.41 0.59 Q 14 \
            17.18 14 18 t -0.59 1.41 T 12 20 Z m 0 -6 q -0.82 0 -1.41 -0.59 T 10 12 t \
            0.59 -1.41 T 12 10 t 1.41 0.59 Q 14 11.18 14 12 t -0.59 1.41 T 12 14 Z M 12 \
            8 Q 11.18 8 10.59 7.41 T 10 6 T 10.59 4.59 T 12 4 t 1.41 0.59 Q 14 5.18 14 \
            6 T 13.41 7.41 T 12 8 Z\
            """;

    static final String DELETE = """
            M 7 21 Q 6.18 21 5.59 20.41 T 5 19 V 6 H 4 V 4 H 9 V 3 h 6 V 4 h 5 V 6 H \
            19 V 19 q 0 0.82 -0.59 1.41 T 17 21 H 7 Z M 17 6 H 7 V 19 H 17 V 6 Z M 9 \
            17 h 2 V 8 H 9 v 9 Z m 4 0 h 2 V 8 H 13 v 9 Z M 7 6 V 19 V 6 Z\
            """;

    static final String ALARM = """
            M 8.49 21.29 Q 6.85 20.58 5.64 19.36 T 3.71 16.51 T 3 13 T 3.71 9.49 T \
            5.64 6.64 T 8.49 4.71 T 12 4 t 3.51 0.71 q 1.64 0.71 2.85 1.93 t 1.93 2.85 \
            T 21 13 t -0.71 3.51 t -1.93 2.85 t -2.85 1.93 T 12 22 T 8.49 21.29 Z M 12 \
            13 Z m 2.8 4.2 l 1.4 -1.4 L 13 12.6 V 8 H 11 v 5.4 l 3.8 3.8 Z M 5.6 2.35 \
            L 7 3.75 L 2.75 8 L 1.35 6.6 L 5.6 2.35 Z m 12.8 0 L 22.65 6.6 L 21.25 8 L \
            17 3.75 l 1.4 -1.4 Z M 12 20 q 2.93 0 4.96 -2.04 Q 19 15.93 19 13 Q 19 \
            10.07 16.96 8.04 T 12 6 Q 9.08 6 7.04 8.04 T 5 13 t 2.04 4.96 T 12 20 Z\
            """;

    static final String SWITCH_FREQUENCY = """
            M 3 14 V 12 q 1.85 0 3.49 -0.71 T 9.35 9.35 T 11.29 6.49 T 12 3 \
            h 2 q 0 2.27 -0.86 4.27 t -2.36 3.5 t -3.5 2.36 T 3 14 Z M 3 10 \
            V 8 Q 5.08 8 6.54 6.54 T 8 3 h 2 Q 10 5.9 7.95 7.95 T 3 10 Z M \
            3 6 V 3 H 6 Q 6 4.25 5.13 5.13 T 3 6 Z m 7 15 q 0 -2.27 0.86 \
            -4.27 t 2.36 -3.5 t 3.5 -2.36 T 21 10 v 2 q -1.85 0 -3.49 0.71 \
            q -1.64 0.71 -2.86 1.94 t -1.94 2.86 T 12 21 H 10 Z m 4 0 q 0 \
            -2.9 2.05 -4.95 T 21 14 v 2 q -2.07 0 -3.54 1.46 Q 16 18.93 16 \
            21 H 14 Z m 4 0 q 0 -1.25 0.88 -2.13 T 21 18 v 3 H 18 Z\
            """;

    static final String CLOSE = """
            M 6.4 19 L 5 17.6 L 10.6 12 L 5 6.4 L 6.4 5 L 12 10.6 L 17.6 5 L 19 6.4 L \
            13.4 12 L 19 17.6 L 17.6 19 L 12 13.4 L 6.4 19 Z\
            """;

    static final String SETTINGS_ETHERNET = """
            M 17 18 L 15.6 16.6 L 20.15 12 L 15.6 7.4 L 17 6 l 6 6 l -6 6 Z M 7 18 L \
            1 12 L 7 6 L 8.4 7.4 L 3.85 12 L 8.4 16.6 L 7 18 Z M 7.29 12.71 Q 7 \
            12.43 7 12 T 7.29 11.29 T 8 11 t 0.71 0.29 T 9 12 T 8.71 12.71 T 8 13 Q \
            7.58 13 7.29 12.71 Z m 4 0 Q 11 12.43 11 12 t 0.29 -0.71 T 12 11 t 0.71 \
            0.29 T 13 12 t -0.29 0.71 T 12 13 T 11.29 12.71 Z m 4 0 Q 15 12.43 15 12 \
            t 0.29 -0.71 T 16 11 q 0.43 0 0.71 0.29 T 17 12 t -0.29 0.71 T 16 13 T \
            15.29 12.71 Z\
            """;

    static final String TEXT_SNIPPET = """
            M 5 21 Q 4.18 21 3.59 20.41 T 3 19 V 5 Q 3 4.17 3.59 3.59 T 5 3 H 15 l 6 \
            6 V 19 q 0 0.82 -0.59 1.41 T 19 21 H 5 Z M 7 17 H 17 V 15 H 7 v 2 Z M 7 \
            13 H 17 V 11 H 7 v 2 Z M 7 9 h 7 V 7 H 7 V 9 Z\
            """;

    static final String TABLE_CHART = """
            M 3 8 V 5 Q 3 4.17 3.59 3.59 T 5 3 H 19 q 0.83 0 1.41 0.59 T 21 5 V 8 H \
            3 Z M 5 21 Q 4.18 21 3.59 20.41 T 3 19 V 10 H 7.5 V 21 H 5 Z m 11.5 0 V \
            10 H 21 v 9 q 0 0.82 -0.59 1.41 T 19 21 H 16.5 Z m -7 0 V 10 h 5 V 21 h \
            -5 Z\
            """;

    static final String SHARE = """
            M 17 22 q -1.25 0 -2.13 -0.88 T 14 19 q 0 -0.15 0.08 -0.7 L 7.05 14.2 q \
            -0.4 0.38 -0.93 0.59 T 5 15 Q 3.75 15 2.88 14.13 T 2 12 T 2.88 9.88 T 5 \
            9 Q 5.6 9 6.13 9.21 T 7.05 9.8 L 14.08 5.7 Q 14.03 5.52 14.01 5.36 T 14 \
            5 Q 14 3.75 14.88 2.88 T 17 2 t 2.13 0.88 T 20 5 T 19.13 7.13 T 17 8 Q \
            16.4 8 15.88 7.79 T 14.95 7.2 L 7.93 11.3 q 0.05 0.18 0.06 0.34 T 8 12 T \
            7.99 12.36 T 7.93 12.7 l 7.03 4.1 q 0.4 -0.38 0.92 -0.59 T 17 16 q 1.25 \
            0 2.13 0.88 T 20 19 t -0.88 2.13 T 17 22 Z m 0 -2 q 0.43 0 0.71 -0.29 Q \
            18 19.43 18 19 T 17.71 18.29 T 17 18 t -0.71 0.29 T 16 19 t 0.29 0.71 T \
            17 20 Z M 5 13 q 0.43 0 0.71 -0.29 Q 6 12.43 6 12 T 5.71 11.29 T 5 11 Q \
            4.58 11 4.29 11.29 T 4 12 t 0.29 0.71 T 5 13 Z M 17.71 5.71 Q 18 5.43 18 \
            5 T 17.71 4.29 T 17 4 T 16.29 4.29 T 16 5 t 0.29 0.71 T 17 6 T 17.71 \
            5.71 Z M 17 19 Z M 5 12 Z M 17 5 Z\
            """;

    static final String SEARCH = """
            M 19.6 21 L 13.3 14.7 q -0.75 0.6 -1.72 0.95 T 9.5 16 Q 6.78 16 4.89 \
            14.11 Q 3 12.23 3 9.5 Q 3 6.77 4.89 4.89 T 9.5 3 t 4.61 1.89 T 16 9.5 q \
            0 1.1 -0.35 2.07 T 14.7 13.3 L 21 19.6 L 19.6 21 Z M 9.5 14 q 1.88 0 \
            3.19 -1.31 T 14 9.5 T 12.69 6.31 T 9.5 5 T 6.31 6.31 T 5 9.5 t 1.31 3.19 \
            T 9.5 14 Z\
            """;

    static final String SETTINGS = """
            M 9.25 22 L 8.85 18.8 Q 8.53 18.68 8.24 18.5 T 7.68 18.13 L 4.7 19.38 L \
            1.95 14.63 L 4.53 12.68 Q 4.5 12.5 4.5 12.34 q 0 -0.16 0 -0.34 t 0 -0.34 \
            T 4.53 11.33 L 1.95 9.38 L 4.7 4.63 L 7.68 5.88 Q 7.95 5.68 8.25 5.5 T \
            8.85 5.2 L 9.25 2 h 5.5 l 0.4 3.2 q 0.33 0.13 0.61 0.3 t 0.56 0.38 L \
            19.3 4.63 l 2.75 4.75 l -2.57 1.95 q 0.02 0.18 0.02 0.34 t 0 0.34 t 0 \
            0.34 t -0.05 0.34 l 2.57 1.95 l -2.75 4.75 L 16.33 18.13 q -0.27 0.2 \
            -0.57 0.38 t -0.6 0.3 L 14.75 22 H 9.25 Z m 2.8 -6.5 q 1.45 0 2.47 -1.03 \
            T 15.55 12 T 14.53 9.52 T 12.05 8.5 q -1.47 0 -2.49 1.02 T 8.55 12 t \
            1.01 2.47 t 2.49 1.03 Z\
            """;

    private MaterialIcons() {
    }

}
