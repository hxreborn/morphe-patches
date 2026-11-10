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
package app.hxreborn.extension.ultimea.controls;

import org.junit.Assert;
import org.junit.Test;

public class XupmixFramesTest {

    private static final int HEX_RADIX = 16;

    private static final int HEX_DIGITS_PER_BYTE = 2;

    @Test
    public void readsEnableRequest() {
        Assert.assertEquals(1, XupmixFrames.requestedState(hex("aa0100021601c3")));
    }

    @Test
    public void readsDisableRequest() {
        Assert.assertEquals(0, XupmixFrames.requestedState(hex("aa0100021600c2")));
    }

    @Test
    public void rejectsQueryAsSetter() {
        Assert.assertEquals(-1, XupmixFrames.requestedState(hex("aa00000118c3")));
    }

    @Test
    public void confirmsEnabledReadback() {
        Assert.assertArrayEquals(hex("bb0100021601c3"), XupmixFrames.confirmedSetterReply(1, hex("bb0100011801c4")));
    }

    @Test
    public void confirmsDisabledReadback() {
        Assert.assertArrayEquals(hex("bb0100021600c2"), XupmixFrames.confirmedSetterReply(0, hex("bb0100011800c3")));
    }

    @Test
    public void rejectsOppositeReadback() {
        Assert.assertNull(XupmixFrames.confirmedSetterReply(1, hex("bb0100011800c3")));
    }

    @Test
    public void rejectsBrightnessNotification() {
        Assert.assertNull(XupmixFrames.confirmedSetterReply(1, hex("bb0100020c05bd")));
    }

    @Test
    public void rejectsSetterAcknowledgementAsReadback() {
        Assert.assertNull(XupmixFrames.confirmedSetterReply(1, hex("bb0100021601c3")));
    }

    @Test
    public void rejectsIncorrectChecksum() {
        Assert.assertNull(XupmixFrames.confirmedSetterReply(1, hex("bb0100011801c3")));
    }

    @Test
    public void rejectsIncorrectLength() {
        Assert.assertNull(XupmixFrames.confirmedSetterReply(1, hex("bb0200011801c4")));
    }

    @Test
    public void rejectsTruncatedReadback() {
        Assert.assertNull(XupmixFrames.confirmedSetterReply(1, hex("bb0100011801")));
    }

    @Test
    public void rejectsInvalidState() {
        Assert.assertNull(XupmixFrames.confirmedSetterReply(1, hex("bb0100011802c5")));
    }

    @Test
    public void preservesReadbackPacket() {
        byte[] readback = hex("bb0100011801c4");
        XupmixFrames.confirmedSetterReply(1, readback);
        Assert.assertArrayEquals(hex("bb0100011801c4"), readback);
    }

    @Test
    public void recognizesSetterReply() {
        Assert.assertTrue(XupmixFrames.isSetterReply(hex("bb0100021601c3")));
    }

    @Test
    public void extendsD80CapabilitiesWithXupmixOnly() {
        byte[] original = hex("bb12000000000101010101010101010100010100010101b9");
        byte[] extended = XupmixFrames.exposeD80Xupmix("Poseidon D80 BLE", original);
        Assert.assertArrayEquals(hex("bb1900000000010101010101010101010001010001010100000000000001ba"), extended);
        Assert.assertArrayEquals(hex("bb12000000000101010101010101010100010100010101b9"), original);
    }

    @Test
    public void preservesOtherModelCapabilities() {
        byte[] original = hex("bb12000000000101010101010101010100010100010101b9");
        Assert.assertSame(original, XupmixFrames.exposeD80Xupmix("Poseidon D60 BLE", original));
        Assert.assertSame(original, XupmixFrames.exposeD80Xupmix(null, original));
    }

    @Test
    public void rejectsCorruptedCapabilities() {
        byte[] original = hex("bb12000000000101010101010101010100010100010101b8");
        Assert.assertSame(original, XupmixFrames.exposeD80Xupmix("Poseidon D80 BLE", original));
    }

    @Test
    public void preservesOtherFirmwareCapabilityLayouts() {
        byte[] original = hex("bb0100000000aa");
        Assert.assertSame(original, XupmixFrames.exposeD80Xupmix("Poseidon D80 BLE", original));
    }

    private static byte[] hex(String value) {
        byte[] bytes = new byte[value.length() / HEX_DIGITS_PER_BYTE];
        for (int index = 0; index < bytes.length; index++) {
            bytes[index] = (byte) Integer.parseInt(
                    value.substring(index * HEX_DIGITS_PER_BYTE, index * HEX_DIGITS_PER_BYTE + HEX_DIGITS_PER_BYTE),
                    HEX_RADIX);
        }
        return bytes;
    }

}
