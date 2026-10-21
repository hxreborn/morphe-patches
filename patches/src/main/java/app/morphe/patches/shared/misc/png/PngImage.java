package app.morphe.patches.shared.misc.png;

import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.ImageLineHelper;
import ar.com.hjg.pngj.ImageLineInt;
import ar.com.hjg.pngj.PngReader;
import ar.com.hjg.pngj.PngWriter;
import ar.com.hjg.pngj.chunks.ChunkRaw;
import ar.com.hjg.pngj.chunks.PngChunk;
import ar.com.hjg.pngj.chunks.PngChunkPLTE;
import ar.com.hjg.pngj.chunks.PngChunkTRNS;
import ar.com.hjg.pngj.chunks.PngChunkUNKNOWN;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class PngImage {
    private static final int OPAQUE = 255;
    private static final int[] NO_ALPHA = new int[0];
    private static final Set<String> NINE_PATCH_CHUNKS = new HashSet<>(Arrays.asList("npTc", "npLb", "npOl"));

    public final int width;
    public final int height;
    public final int[] argb;
    private final Map<String, byte[]> ninePatchChunks;

    public PngImage(int width, int height, int[] argb) {
        this(width, height, argb, Collections.emptyMap());
    }

    private PngImage(int width, int height, int[] argb, Map<String, byte[]> ninePatchChunks) {
        this.width = width;
        this.height = height;
        this.argb = argb;
        this.ninePatchChunks = ninePatchChunks;
    }

    public static PngImage read(File file) {
        PngReader reader = new PngReader(file);
        try {
            ImageInfo info = reader.imgInfo;
            PngChunkPLTE palette = reader.getMetadata().getPLTE();
            PngChunkTRNS transparency = reader.getMetadata().getTRNS();
            int[] argb = new int[info.cols * info.rows];
            for (int y = 0; y < info.rows; y++) {
                int[] samples = ((ImageLineInt) reader.readRow()).getScanline();
                for (int x = 0; x < info.cols; x++) {
                    argb[y * info.cols + x] = info.indexed
                            ? indexedPixel(samples[x], palette, transparency)
                            : pixel(samples, x * info.channels, info, transparency);
                }
            }
            Map<String, byte[]> ninePatchChunks = new LinkedHashMap<>();
            for (PngChunk chunk : reader.getChunksList().getChunks()) {
                if (chunk instanceof PngChunkUNKNOWN && NINE_PATCH_CHUNKS.contains(chunk.id)) {
                    ninePatchChunks.put(chunk.id, ((PngChunkUNKNOWN) chunk).getData());
                }
            }
            return new PngImage(info.cols, info.rows, argb, ninePatchChunks);
        } finally {
            reader.close();
        }
    }

    public void write(File file) {
        PngWriter writer = new PngWriter(file, new ImageInfo(width, height, 8, true), true);
        for (Map.Entry<String, byte[]> entry : ninePatchChunks.entrySet()) {
            writer.getMetadata().queueChunk(new RawChunk(entry.getKey(), entry.getValue(), writer.imgInfo));
        }
        ImageLineInt line = new ImageLineInt(writer.imgInfo);
        int[] row = new int[width];
        for (int y = 0; y < height; y++) {
            System.arraycopy(argb, y * width, row, 0, width);
            ImageLineHelper.setPixelsRGBA8(line, row);
            writer.writeRow(line);
        }
        writer.end();
    }

    private static int indexedPixel(int index, PngChunkPLTE palette, PngChunkTRNS transparency) {
        int[] alphas = transparency == null ? NO_ALPHA : transparency.getPalletteAlpha();
        int alpha = index < alphas.length ? alphas[index] : OPAQUE;
        return (alpha << 24) | palette.getEntry(index);
    }

    private static int pixel(int[] samples, int offset, ImageInfo info, PngChunkTRNS transparency) {
        int max = (1 << info.bitDepth) - 1;
        int red = samples[offset];
        int green = info.greyscale ? red : samples[offset + 1];
        int blue = info.greyscale ? red : samples[offset + 2];
        int alpha = info.alpha ? samples[offset + info.channels - 1] : max;
        if (transparency != null && !info.alpha && isColourKey(samples, offset, info, transparency)) {
            alpha = 0;
        }
        return pack(scale(alpha, max), scale(red, max), scale(green, max), scale(blue, max));
    }

    private static boolean isColourKey(int[] samples, int offset, ImageInfo info, PngChunkTRNS transparency) {
        if (info.greyscale) return samples[offset] == transparency.getGray();

        int[] key = transparency.getRGB();
        return samples[offset] == key[0] && samples[offset + 1] == key[1] && samples[offset + 2] == key[2];
    }

    private static int scale(int sample, int max) {
        return max == OPAQUE ? sample : sample * OPAQUE / max;
    }

    private static int pack(int alpha, int red, int green, int blue) {
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    public static final class RawChunk extends PngChunkUNKNOWN {
        private final byte[] data;

        public RawChunk(String id, byte[] data, ImageInfo info) {
            super(id, info);
            this.data = data;
            setPriority(true);
        }

        @Override
        public ChunkRaw createRawChunk() {
            ChunkRaw raw = createEmptyChunk(data.length, true);
            System.arraycopy(data, 0, raw.data, 0, data.length);
            return raw;
        }
    }
}
