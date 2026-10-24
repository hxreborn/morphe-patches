/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.shared;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public final class LoadingIndicatorShapesTest {

    private static final float TOLERANCE = 1e-4f;

    private static final int START_ANCHOR_X = 0;

    private static final int END_ANCHOR_X = 6;

    private static final int[] MORPH_ENDS = { 0, 1 };

    private static float coordinate(float[] morph, int cubic, int coordinate, int morphEnd) {
        return morph[cubic * LoadingIndicatorShapes.FLOATS_PER_CUBIC + coordinate * 2 + morphEnd];
    }

    @Test
    public void decodesEveryMorphAtItsDeclaredSize() {
        // when
        float[][] morphs = LoadingIndicatorShapes.decodeMorphs();

        // then
        assertEquals(LoadingIndicatorShapes.DETERMINATE_MORPH_INDEX + 1, morphs.length);
        for (int i = 0; i < LoadingIndicatorShapes.INDETERMINATE_CUBIC_COUNTS.length; i++) {
            assertEquals(LoadingIndicatorShapes.INDETERMINATE_CUBIC_COUNTS[i] * LoadingIndicatorShapes.FLOATS_PER_CUBIC,
                    morphs[i].length);
        }
        assertEquals(LoadingIndicatorShapes.DETERMINATE_CUBIC_COUNT * LoadingIndicatorShapes.FLOATS_PER_CUBIC,
                morphs[LoadingIndicatorShapes.DETERMINATE_MORPH_INDEX].length);
    }

    @Test
    public void joinsEveryCubicToTheNextAtBothMorphEnds() {
        for (float[] morph : LoadingIndicatorShapes.decodeMorphs()) {
            final int cubics = morph.length / LoadingIndicatorShapes.FLOATS_PER_CUBIC;
            for (int cubic = 0; cubic < cubics; cubic++) {
                final int next = (cubic + 1) % cubics;
                for (int morphEnd : MORPH_ENDS) {
                    assertEquals(coordinate(morph, next, START_ANCHOR_X, morphEnd),
                            coordinate(morph, cubic, END_ANCHOR_X, morphEnd), TOLERANCE);
                    assertEquals(coordinate(morph, next, START_ANCHOR_X + 1, morphEnd),
                            coordinate(morph, cubic, END_ANCHOR_X + 1, morphEnd), TOLERANCE);
                }
            }
        }
    }

    @Test
    public void keepsEveryAnchorInsideTheUnitCircle() {
        for (float[] morph : LoadingIndicatorShapes.decodeMorphs()) {
            for (int cubic = 0; cubic < morph.length / LoadingIndicatorShapes.FLOATS_PER_CUBIC; cubic++) {
                for (int morphEnd : MORPH_ENDS) {
                    final float x = coordinate(morph, cubic, START_ANCHOR_X, morphEnd);
                    final float y = coordinate(morph, cubic, START_ANCHOR_X + 1, morphEnd);
                    assertTrue(Math.hypot(x, y) <= 1 + TOLERANCE);
                }
            }
        }
    }

}
