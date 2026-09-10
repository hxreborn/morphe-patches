/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// aircompressor 0.27 port without Unsafe
package app.morphe.patches.shared.misc.jiagu.zstd;

import static java.lang.String.format;

public final class Zstd
{
    private Zstd()
    {
    }

    /**
     * Decompresses one whole frame, sized from its declared {@code Frame_Content_Size}.
     *
     * @throws MalformedInputException if the frame is corrupted or declares no content size
     */
    public static byte[] decompress(byte[] input, int offset, int length)
    {
        if (offset < 0 || length < 0 || offset + length > input.length) {
            throw new IllegalArgumentException(
                    format("Invalid offset or length (%s, %s) in array of length %s", offset, length, input.length));
        }

        long inputLimit = offset + length;

        long contentSize = ZstdFrameDecompressor.getDecompressedSize(input, offset, inputLimit);
        if (contentSize < 0) {
            throw new MalformedInputException(offset, "Frame declares no content size");
        }
        if (contentSize > Integer.MAX_VALUE - 8) {
            throw new MalformedInputException(offset, "Frame content size too large: " + contentSize);
        }

        byte[] output = new byte[(int) contentSize];
        int written = new ZstdFrameDecompressor().decompress(input, offset, inputLimit, output, 0, output.length);
        if (written != output.length) {
            throw new MalformedInputException(offset, format("Expected %s bytes, decoded %s", output.length, written));
        }
        return output;
    }
}
