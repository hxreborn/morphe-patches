package app.morphe.patches.shared.misc.png;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class PngImageTest {
    private final Random random = new Random(7);

    @Test
    void decodesEveryImageTypeImageIoWrites() throws IOException {
        int[] types = {
                BufferedImage.TYPE_INT_ARGB,
                BufferedImage.TYPE_INT_RGB,
                BufferedImage.TYPE_BYTE_GRAY,
                BufferedImage.TYPE_USHORT_GRAY,
                BufferedImage.TYPE_BYTE_BINARY,
                BufferedImage.TYPE_BYTE_INDEXED,
        };
        for (int type : types) {
            assertDecodesLike(save(new BufferedImage(13, 7, type)), "type " + type);
        }
        assertDecodesLike(save(new BufferedImage(13, 7, BufferedImage.TYPE_BYTE_INDEXED, translucentPalette())), "indexed with tRNS");
    }

    @Test
    void writtenPixelsSurviveARoundTrip() throws IOException {
        PngImage original = PngImage.read(save(new BufferedImage(13, 7, BufferedImage.TYPE_INT_ARGB)));
        File written = File.createTempFile("png-image", ".png");
        written.deleteOnExit();

        original.write(written);

        assertArrayEquals(original.argb, PngImage.read(written).argb);
        assertArrayEquals(original.argb, expectedArgb(ImageIO.read(written)));
    }

    @Test
    void decodesACorpusOfRealFilesLikeImageIo() throws IOException {
        String corpus = System.getenv("PNG_CORPUS");
        assumeTrue(corpus != null, "PNG_CORPUS is not set");

        try (Stream<Path> paths = Files.walk(new File(corpus).toPath())) {
            List<File> files = paths.map(Path::toFile)
                    .filter(file -> file.getName().endsWith(".png") && !file.getName().endsWith(".9.png"))
                    .toList();
            assertFalse(files.isEmpty(), "PNG_CORPUS holds no PNG files");
            for (File file : files) {
                assertDecodesLike(file, file.getPath());
            }
        }
    }

    private void assertDecodesLike(File file, String label) throws IOException {
        BufferedImage expected = ImageIO.read(file);
        PngImage actual = PngImage.read(file);

        assertEquals(expected.getWidth(), actual.width, label);
        assertEquals(expected.getHeight(), actual.height, label);
        assertArrayEquals(expectedArgb(expected), actual.argb, label);
    }

    private static int[] expectedArgb(BufferedImage image) {
        if (image.getColorModel().getColorSpace().getType() != ColorSpace.TYPE_GRAY) {
            return image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
        }
        Raster raster = image.getRaster();
        int max = (1 << image.getColorModel().getComponentSize(0)) - 1;
        int[] argb = new int[image.getWidth() * image.getHeight()];
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int tone = raster.getSample(x, y, 0) * 255 / max;
                int alpha = raster.getNumBands() > 1 ? raster.getSample(x, y, 1) * 255 / max : 255;
                argb[y * image.getWidth() + x] = (alpha << 24) | (tone << 16) | (tone << 8) | tone;
            }
        }
        return argb;
    }

    private File save(BufferedImage image) throws IOException {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                image.setRGB(x, y, random.nextInt());
            }
        }
        File file = File.createTempFile("png-image", ".png");
        file.deleteOnExit();
        ImageIO.write(image, "png", file);
        return file;
    }

    private IndexColorModel translucentPalette() {
        int size = 16;
        byte[][] channels = new byte[4][size];
        for (byte[] channel : channels) random.nextBytes(channel);
        return new IndexColorModel(4, size, channels[0], channels[1], channels[2], channels[3]);
    }
}
