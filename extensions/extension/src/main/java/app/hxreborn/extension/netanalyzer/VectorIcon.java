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

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

final class VectorIcon extends Drawable {

    private static final float VIEWPORT = 24f;

    private final Path source;

    private final Path scaled = new Path();

    private final Matrix matrix = new Matrix();

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    VectorIcon(String pathData, int color) {
        this.source = parse(pathData);
        this.paint.setColor(color);
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        this.matrix.setScale(bounds.width() / VIEWPORT, bounds.height() / VIEWPORT);
        this.matrix.postTranslate(bounds.left, bounds.top);
        this.source.transform(this.matrix, this.scaled);
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawPath(this.scaled, this.paint);
    }

    @Override
    public void setAlpha(int alpha) {
        this.paint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        this.paint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public int getIntrinsicWidth() {
        return -1;
    }

    @Override
    public int getIntrinsicHeight() {
        return -1;
    }

    private static Path parse(String pathData) {
        final Path path = new Path();
        final PathTokens tokens = new PathTokens(pathData);
        float x = 0;
        float y = 0;
        float startX = 0;
        float startY = 0;
        float controlX = 0;
        float controlY = 0;
        char command = 0;
        char previous = 0;
        while (tokens.hasMore()) {
            if (tokens.atCommand()) {
                command = tokens.command();
            }
            final boolean relative = Character.isLowerCase(command);
            final float originX = relative ? x : 0;
            final float originY = relative ? y : 0;
            switch (Character.toUpperCase(command)) {
                case 'M' -> {
                    x = originX + tokens.number();
                    y = originY + tokens.number();
                    path.moveTo(x, y);
                    startX = x;
                    startY = y;
                    command = relative ? 'l' : 'L';
                }
                case 'L' -> {
                    x = originX + tokens.number();
                    y = originY + tokens.number();
                    path.lineTo(x, y);
                }
                case 'H' -> {
                    x = originX + tokens.number();
                    path.lineTo(x, y);
                }
                case 'V' -> {
                    y = originY + tokens.number();
                    path.lineTo(x, y);
                }
                case 'C' -> {
                    final float x1 = originX + tokens.number();
                    final float y1 = originY + tokens.number();
                    controlX = originX + tokens.number();
                    controlY = originY + tokens.number();
                    x = originX + tokens.number();
                    y = originY + tokens.number();
                    path.cubicTo(x1, y1, controlX, controlY, x, y);
                }
                case 'S' -> {
                    final boolean smooth = "CcSs".indexOf(previous) >= 0;
                    final float x1 = smooth ? 2 * x - controlX : x;
                    final float y1 = smooth ? 2 * y - controlY : y;
                    controlX = originX + tokens.number();
                    controlY = originY + tokens.number();
                    x = originX + tokens.number();
                    y = originY + tokens.number();
                    path.cubicTo(x1, y1, controlX, controlY, x, y);
                }
                case 'Q' -> {
                    controlX = originX + tokens.number();
                    controlY = originY + tokens.number();
                    x = originX + tokens.number();
                    y = originY + tokens.number();
                    path.quadTo(controlX, controlY, x, y);
                }
                case 'T' -> {
                    final boolean smooth = "QqTt".indexOf(previous) >= 0;
                    controlX = smooth ? 2 * x - controlX : x;
                    controlY = smooth ? 2 * y - controlY : y;
                    x = originX + tokens.number();
                    y = originY + tokens.number();
                    path.quadTo(controlX, controlY, x, y);
                }
                case 'Z' -> {
                    path.close();
                    x = startX;
                    y = startY;
                }
                default -> throw new IllegalArgumentException("Unsupported path command " + command);
            }
            previous = command;
        }
        return path;
    }

    private static final class PathTokens {

        private final String pathData;

        private int position;

        PathTokens(String pathData) {
            this.pathData = pathData;
        }

        boolean hasMore() {
            skipSeparators();
            return this.position < this.pathData.length();
        }

        boolean atCommand() {
            skipSeparators();
            return Character.isLetter(this.pathData.charAt(this.position));
        }

        char command() {
            return this.pathData.charAt(this.position++);
        }

        float number() {
            skipSeparators();
            final int start = this.position;
            boolean seenDot = false;
            if (this.position < this.pathData.length() && "+-".indexOf(this.pathData.charAt(this.position)) >= 0) {
                this.position++;
            }
            while (this.position < this.pathData.length()) {
                final char current = this.pathData.charAt(this.position);
                if (current == '.' && !seenDot) {
                    seenDot = true;
                } else if (!Character.isDigit(current)) {
                    break;
                }
                this.position++;
            }
            return Float.parseFloat(this.pathData.substring(start, this.position));
        }

        private void skipSeparators() {
            while (this.position < this.pathData.length() && (this.pathData.charAt(this.position) == ','
                    || Character.isWhitespace(this.pathData.charAt(this.position)))) {
                this.position++;
            }
        }

    }

}
