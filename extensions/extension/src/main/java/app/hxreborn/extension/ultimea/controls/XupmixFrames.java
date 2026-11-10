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

final class XupmixFrames {

    private static final String D80_BLUETOOTH_NAME = "Poseidon D80 BLE";

    private static final int LEGACY_CAPABILITY_BYTES = 18;

    private static final int XUPMIX_CAPABILITY_INDEX = 24;

    private static final int FRAME_HEADER_BYTES = 5;

    private static final int FRAME_OVERHEAD_BYTES = 6;

    private static final int SET_CATEGORY = 2;

    private static final int SET_COMMAND = 0x16;

    private static final int GET_CATEGORY = 1;

    private static final int GET_COMMAND = 0x18;

    private static final int FRAME_LENGTH = 7;

    private static final int REQUEST_MARKER = 0xaa;

    private static final int RESPONSE_MARKER = 0xbb;

    private static final int LENGTH_OFFSET = 1;

    private static final int CATEGORY_OFFSET = 3;

    private static final int COMMAND_OFFSET = 4;

    private static final int VALUE_OFFSET = 5;

    private static final int CHECKSUM_OFFSET = 6;

    private static final int UNSIGNED_BYTE_MASK = 0xff;

    private XupmixFrames() {
    }

    static boolean isD80(String bluetoothName) {
        return D80_BLUETOOTH_NAME.equals(bluetoothName);
    }

    static boolean isLegacyCapabilities(byte[] frame) {
        if (frame == null || frame.length != LEGACY_CAPABILITY_BYTES + FRAME_OVERHEAD_BYTES
                || (frame[0] & UNSIGNED_BYTE_MASK) != RESPONSE_MARKER || frame[LENGTH_OFFSET] != LEGACY_CAPABILITY_BYTES
                || frame[LENGTH_OFFSET + 1] != 0 || frame[CATEGORY_OFFSET] != 0 || frame[COMMAND_OFFSET] != 0) {
            return false;
        }
        int checksum = REQUEST_MARKER;
        for (int index = FRAME_HEADER_BYTES; index < frame.length - 1; index++) {
            int capability = frame[index] & UNSIGNED_BYTE_MASK;
            if (capability > 1) {
                return false;
            }
            checksum += capability;
        }
        return (frame[frame.length - 1] & UNSIGNED_BYTE_MASK) == (checksum & UNSIGNED_BYTE_MASK);
    }

    static byte[] exposeD80Xupmix(String bluetoothName, byte[] frame) {
        if (!isD80(bluetoothName) || !isLegacyCapabilities(frame)) {
            return frame;
        }
        int payloadLength = XUPMIX_CAPABILITY_INDEX + 1;
        byte[] result = new byte[payloadLength + FRAME_OVERHEAD_BYTES];
        System.arraycopy(frame, 0, result, 0, frame.length - 1);
        result[LENGTH_OFFSET] = (byte) payloadLength;
        result[FRAME_HEADER_BYTES + XUPMIX_CAPABILITY_INDEX] = 1;
        int checksum = REQUEST_MARKER;
        for (int index = FRAME_HEADER_BYTES; index < result.length - 1; index++) {
            checksum += result[index] & UNSIGNED_BYTE_MASK;
        }
        result[result.length - 1] = (byte) checksum;
        return result;
    }

    static int requestedState(byte[] frame) {
        return matches(frame, REQUEST_MARKER, SET_CATEGORY, SET_COMMAND) ? frame[VALUE_OFFSET] : -1;
    }

    static int readState(byte[] frame) {
        return matches(frame, RESPONSE_MARKER, GET_CATEGORY, GET_COMMAND) ? frame[VALUE_OFFSET] : -1;
    }

    static boolean isSetterReply(byte[] frame) {
        return matches(frame, RESPONSE_MARKER, SET_CATEGORY, SET_COMMAND);
    }

    static byte[] confirmedSetterReply(int requestedState, byte[] readback) {
        if (!matches(readback, RESPONSE_MARKER, GET_CATEGORY, GET_COMMAND)
                || readback[VALUE_OFFSET] != requestedState) {
            return null;
        }
        byte[] result = readback.clone();
        result[CATEGORY_OFFSET] = SET_CATEGORY;
        result[COMMAND_OFFSET] = SET_COMMAND;
        result[CHECKSUM_OFFSET] = (byte) (REQUEST_MARKER + SET_CATEGORY + SET_COMMAND + requestedState);
        return result;
    }

    private static boolean matches(byte[] frame, int marker, int category, int command) {
        if (frame == null || frame.length != FRAME_LENGTH || (frame[0] & UNSIGNED_BYTE_MASK) != marker
                || frame[LENGTH_OFFSET] != 1 || frame[LENGTH_OFFSET + 1] != 0 || frame[CATEGORY_OFFSET] != category
                || frame[COMMAND_OFFSET] != command || (frame[VALUE_OFFSET] != 0 && frame[VALUE_OFFSET] != 1)) {
            return false;
        }
        int checksum = REQUEST_MARKER + category + command + frame[VALUE_OFFSET];
        return (frame[CHECKSUM_OFFSET] & UNSIGNED_BYTE_MASK) == (checksum & UNSIGNED_BYTE_MASK);
    }

}
