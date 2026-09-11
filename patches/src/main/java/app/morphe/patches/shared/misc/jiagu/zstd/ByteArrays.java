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

import static app.morphe.patches.shared.misc.jiagu.zstd.Constants.SIZE_OF_INT;

/**
 * Stands in for the sun.misc.Unsafe accessors of the original. Addresses are plain array
 * indexes and multi-byte values are little endian.
 */
final class ByteArrays
{
    private ByteArrays()
    {
    }

    public static byte getByte(byte[] base, long address)
    {
        return base[(int) address];
    }

    public static short getShort(byte[] base, long address)
    {
        int index = (int) address;
        return (short) (byteAt(base, index)
                | byteAt(base, index + 1) << 8);
    }

    public static int getInt(byte[] base, long address)
    {
        int index = (int) address;
        return byteAt(base, index)
                | byteAt(base, index + 1) << 8
                | byteAt(base, index + 2) << 16
                | byteAt(base, index + 3) << 24;
    }

    public static long getLong(byte[] base, long address)
    {
        return (getInt(base, address) & 0xFFFF_FFFFL)
                | (long) getInt(base, address + SIZE_OF_INT) << 32;
    }

    public static void putByte(byte[] base, long address, byte value)
    {
        base[(int) address] = value;
    }

    public static void putInt(byte[] base, long address, int value)
    {
        int index = (int) address;
        base[index] = (byte) value;
        base[index + 1] = (byte) (value >>> 8);
        base[index + 2] = (byte) (value >>> 16);
        base[index + 3] = (byte) (value >>> 24);
    }

    public static void putLong(byte[] base, long address, long value)
    {
        putInt(base, address, (int) value);
        putInt(base, address + SIZE_OF_INT, (int) (value >>> 32));
    }

    // Every call site copies between two different arrays
    public static void copyMemory(byte[] source, long sourceAddress, byte[] destination, long destinationAddress, long size)
    {
        System.arraycopy(source, (int) sourceAddress, destination, (int) destinationAddress, (int) size);
    }

    // Wide reads run past the end of a buffer by design
    private static int byteAt(byte[] base, int index)
    {
        if (index >= base.length) {
            return 0;
        }
        return base[index] & 0xFF;
    }
}
