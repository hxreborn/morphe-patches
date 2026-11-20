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
package app.hxreborn.extension.readera;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import android.util.Log;

final class AccentResourceTable {

    static final String TEMPORARY_SUFFIX = ".tmp";

    private static final String TABLE_ENTRY = "resources.arsc";

    private static final String MANIFEST_ENTRY = "AndroidManifest.xml";

    private static final int SDK_QUALIFIER = 21;

    private static final short RES_STRING_POOL_TYPE = 0x0001;

    private static final short RES_TABLE_TYPE = 0x0002;

    private static final short RES_XML_TYPE = 0x0003;

    private static final short RES_XML_START_ELEMENT_TYPE = 0x0102;

    private static final short RES_XML_END_ELEMENT_TYPE = 0x0103;

    private static final short RES_TABLE_PACKAGE_TYPE = 0x0200;

    private static final short RES_TABLE_TYPE_TYPE = 0x0201;

    private static final short RES_TABLE_TYPE_SPEC_TYPE = 0x0202;

    private static final int CHUNK_HEADER_SIZE = 8;

    private static final int TABLE_HEADER_SIZE = 12;

    private static final int STRING_POOL_HEADER_SIZE = 28;

    private static final int PACKAGE_HEADER_SIZE = 288;

    private static final int PACKAGE_NAME_CHARS = 128;

    private static final int TYPE_SPEC_HEADER_SIZE = 16;

    private static final int CONFIG_SIZE = 64;

    private static final int CONFIG_BYTES_BEFORE_SDK_VERSION = 20;

    private static final int CONFIG_BYTES_AFTER_SDK_VERSION = 36;

    private static final int TYPE_FIELDS_SIZE = 20;

    private static final int TYPE_HEADER_SIZE = TYPE_FIELDS_SIZE + CONFIG_SIZE;

    private static final int ENTRY_SIZE = 8;

    private static final int VALUE_SIZE = 8;

    private static final int XML_NODE_HEADER_SIZE = 16;

    private static final int XML_ATTRIBUTE_EXTENSION_SIZE = 20;

    private static final int XML_ATTRIBUTE_SIZE = 20;

    private static final int XML_END_ELEMENT_EXTENSION_SIZE = 8;

    private static final int XML_LINE_NUMBER = 1;

    private static final short ENTRY_FLAG_PUBLIC = 0x0002;

    private static final int SPEC_PUBLIC = 0x40000000;

    private static final int NO_ENTRY = 0xFFFFFFFF;

    private static final int NO_INDEX = 0xFFFFFFFF;

    private static final int UTF8_FLAG = 0x100;

    private static final byte TYPE_STRING = 0x03;

    private static final byte TYPE_INT_COLOR_ARGB8 = 0x1C;

    private static final int PACKAGE_ID_SHIFT = 24;

    private static final int TYPE_ID_SHIFT = 16;

    private static final int TYPE_ID_MASK = 0xFF;

    private static final int ENTRY_INDEX_MASK = 0xFFFF;

    private static final int MAX_SHORT_STRING_LENGTH = 0x7F;

    private static final int CHUNK_ALIGNMENT = 4;

    private static final int TYPES_IN_SPEC = 1;

    private static final int PACKAGES_IN_TABLE = 1;

    private static final int FIRST_TYPE_ID = 1;

    private static final int RESERVED = 0;

    private static final int LAST_PUBLIC_STRING = 0;

    private static final int TYPE_FLAGS = 0;

    private static final int CONFIG_SDK_MINOR_VERSION = 0;

    private static final int SPEC_PRIVATE = 0;

    private static final int FIRST_ENTRY_OFFSET = 0;

    private static final int FIRST_KEY_STRING = 0;

    private static final int NO_STYLES = 0;

    private static final int UTF16_FLAG = 0;

    private static final int NO_SPECIAL_ATTRIBUTE = 0;

    private static final int UTF8_LENGTH_BYTES = 2;

    private static final int UTF16_LENGTH_CHARS = 1;

    private static final int TERMINATOR_LENGTH = 1;

    private static final String PLACEHOLDER_TYPE_PREFIX = "?";

    private static final String MANIFEST_ELEMENT = "manifest";

    private static final int MANIFEST_ATTRIBUTES = 1;

    private static final String PACKAGE_ATTRIBUTE = "package";

    private AccentResourceTable() {
    }

    static void writeApk(File target, String packageName, int resourceId, String typeName, String entryName,
            int entryCount, int color) throws IOException {
        byte[] table = table(packageName, resourceId, typeName, entryName, entryCount, color);
        byte[] manifest = manifest(packageName);
        File temporary = File.createTempFile(target.getName(), TEMPORARY_SUFFIX, target.getParentFile());
        try {
            try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(temporary))) {
                putStoredEntry(zip, TABLE_ENTRY, table);
                putStoredEntry(zip, MANIFEST_ENTRY, manifest);
            }
            if (!temporary.renameTo(target)) {
                throw new IOException("Cannot rename " + temporary.getName() + " to " + target.getName());
            }
        } finally {
            if (temporary.exists() && !temporary.delete()) {
                Log.w(PatchSettings.LOG_TAG, "Cannot delete " + temporary.getName());
            }
        }
    }

    private static byte[] table(String packageName, int resourceId, String typeName, String entryName, int entryCount,
            int color) {
        if (packageName.length() >= PACKAGE_NAME_CHARS) {
            throw new IllegalArgumentException("Package name exceeds " + (PACKAGE_NAME_CHARS - 1) + " characters");
        }
        int packageId = resourceId >>> PACKAGE_ID_SHIFT;
        int typeId = (resourceId >>> TYPE_ID_SHIFT) & TYPE_ID_MASK;
        int entryIndex = resourceId & ENTRY_INDEX_MASK;
        if (packageId == 0 || typeId == 0) {
            throw new IllegalArgumentException(
                    "Resource id " + Integer.toHexString(resourceId) + " has no package or type");
        }
        if (entryCount <= entryIndex) {
            throw new IllegalArgumentException(
                    "Entry count " + entryCount + " does not cover resource id " + Integer.toHexString(resourceId));
        }
        List<String> typeNames = new ArrayList<>();
        for (int id = FIRST_TYPE_ID; id < typeId; id++) {
            typeNames.add(PLACEHOLDER_TYPE_PREFIX + id);
        }
        typeNames.add(typeName);
        byte[] valueStrings = stringPool(Collections.emptyList(), true);
        byte[] typeStrings = stringPool(typeNames, false);
        byte[] keyStrings = stringPool(Collections.singletonList(entryName), true);
        int typeSpecSize = TYPE_SPEC_HEADER_SIZE + entryCount * Integer.BYTES;
        int typeSize = TYPE_HEADER_SIZE + entryCount * Integer.BYTES + ENTRY_SIZE + VALUE_SIZE;
        int packageSize = PACKAGE_HEADER_SIZE + typeStrings.length + keyStrings.length + typeSpecSize + typeSize;
        int tableSize = TABLE_HEADER_SIZE + valueStrings.length + packageSize;

        ByteBuffer out = ByteBuffer.allocate(tableSize).order(ByteOrder.LITTLE_ENDIAN);
        putChunkHeader(out, RES_TABLE_TYPE, TABLE_HEADER_SIZE, tableSize);
        out.putInt(PACKAGES_IN_TABLE);
        out.put(valueStrings);
        putPackageHeader(out, packageSize, packageId, packageName, typeStrings, keyStrings);
        putTypeSpecChunk(out, typeSpecSize, typeId, entryCount, entryIndex);
        putTypeChunk(out, typeSize, typeId, entryCount, entryIndex, color);
        return out.array();
    }

    private static void putPackageHeader(ByteBuffer out, int packageSize, int packageId, String packageName,
            byte[] typeStrings, byte[] keyStrings) {
        putChunkHeader(out, RES_TABLE_PACKAGE_TYPE, PACKAGE_HEADER_SIZE, packageSize);
        out.putInt(packageId);
        for (int index = 0; index < PACKAGE_NAME_CHARS; index++) {
            out.putChar((index < packageName.length()) ? packageName.charAt(index) : '\0');
        }
        out.putInt(PACKAGE_HEADER_SIZE);
        out.putInt(LAST_PUBLIC_STRING);
        out.putInt(PACKAGE_HEADER_SIZE + typeStrings.length);
        out.putInt(LAST_PUBLIC_STRING);
        out.putInt(RESERVED);
        out.put(typeStrings);
        out.put(keyStrings);
    }

    private static void putTypeSpecChunk(ByteBuffer out, int typeSpecSize, int typeId, int entryCount, int entryIndex) {
        putChunkHeader(out, RES_TABLE_TYPE_SPEC_TYPE, TYPE_SPEC_HEADER_SIZE, typeSpecSize);
        out.put((byte) typeId);
        out.put((byte) RESERVED);
        out.putShort((short) TYPES_IN_SPEC);
        out.putInt(entryCount);
        for (int index = 0; index < entryCount; index++) {
            out.putInt((index == entryIndex) ? SPEC_PUBLIC : SPEC_PRIVATE);
        }
    }

    private static void putTypeChunk(ByteBuffer out, int typeSize, int typeId, int entryCount, int entryIndex,
            int color) {
        putChunkHeader(out, RES_TABLE_TYPE_TYPE, TYPE_HEADER_SIZE, typeSize);
        out.put((byte) typeId);
        out.put((byte) TYPE_FLAGS);
        out.putShort((short) RESERVED);
        out.putInt(entryCount);
        out.putInt(TYPE_HEADER_SIZE + entryCount * Integer.BYTES);
        out.putInt(CONFIG_SIZE);
        out.put(new byte[CONFIG_BYTES_BEFORE_SDK_VERSION]);
        out.putShort((short) SDK_QUALIFIER);
        out.putShort((short) CONFIG_SDK_MINOR_VERSION);
        out.put(new byte[CONFIG_BYTES_AFTER_SDK_VERSION]);
        for (int index = 0; index < entryCount; index++) {
            out.putInt((index == entryIndex) ? FIRST_ENTRY_OFFSET : NO_ENTRY);
        }
        out.putShort((short) ENTRY_SIZE);
        out.putShort(ENTRY_FLAG_PUBLIC);
        out.putInt(FIRST_KEY_STRING);
        out.putShort((short) VALUE_SIZE);
        out.put((byte) RESERVED);
        out.put(TYPE_INT_COLOR_ARGB8);
        out.putInt(color);
    }

    private static byte[] manifest(String packageName) {
        List<String> strings = Arrays.asList(PACKAGE_ATTRIBUTE, MANIFEST_ELEMENT, packageName);
        byte[] stringPool = stringPool(strings, true);
        int elementString = strings.indexOf(MANIFEST_ELEMENT);
        int startSize = XML_NODE_HEADER_SIZE + XML_ATTRIBUTE_EXTENSION_SIZE + XML_ATTRIBUTE_SIZE;
        int endSize = XML_NODE_HEADER_SIZE + XML_END_ELEMENT_EXTENSION_SIZE;
        int documentSize = CHUNK_HEADER_SIZE + stringPool.length + startSize + endSize;
        ByteBuffer out = ByteBuffer.allocate(documentSize).order(ByteOrder.LITTLE_ENDIAN);
        putChunkHeader(out, RES_XML_TYPE, CHUNK_HEADER_SIZE, documentSize);
        out.put(stringPool);
        putManifestStartElement(out, startSize, elementString, strings.indexOf(PACKAGE_ATTRIBUTE),
                strings.indexOf(packageName));
        putXmlNodeHeader(out, RES_XML_END_ELEMENT_TYPE, endSize);
        out.putInt(NO_INDEX);
        out.putInt(elementString);
        return out.array();
    }

    private static void putManifestStartElement(ByteBuffer out, int startSize, int elementString, int attributeString,
            int valueString) {
        putXmlNodeHeader(out, RES_XML_START_ELEMENT_TYPE, startSize);
        out.putInt(NO_INDEX);
        out.putInt(elementString);
        out.putShort((short) XML_ATTRIBUTE_EXTENSION_SIZE);
        out.putShort((short) XML_ATTRIBUTE_SIZE);
        out.putShort((short) MANIFEST_ATTRIBUTES);
        out.putShort((short) NO_SPECIAL_ATTRIBUTE);
        out.putShort((short) NO_SPECIAL_ATTRIBUTE);
        out.putShort((short) NO_SPECIAL_ATTRIBUTE);
        out.putInt(NO_INDEX);
        out.putInt(attributeString);
        out.putInt(valueString);
        out.putShort((short) VALUE_SIZE);
        out.put((byte) RESERVED);
        out.put(TYPE_STRING);
        out.putInt(valueString);
    }

    private static void putXmlNodeHeader(ByteBuffer out, short type, int size) {
        putChunkHeader(out, type, XML_NODE_HEADER_SIZE, size);
        out.putInt(XML_LINE_NUMBER);
        out.putInt(NO_INDEX);
    }

    private static byte[] stringPool(List<String> strings, boolean utf8) {
        List<byte[]> encoded = new ArrayList<>();
        int dataSize = 0;
        for (String value : strings) {
            byte[] bytes = utf8 ? utf8String(value) : utf16String(value);
            encoded.add(bytes);
            dataSize += bytes.length;
        }
        int stringsStart = STRING_POOL_HEADER_SIZE + strings.size() * Integer.BYTES;
        int size = align(stringsStart + dataSize);
        ByteBuffer out = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);
        putChunkHeader(out, RES_STRING_POOL_TYPE, STRING_POOL_HEADER_SIZE, size);
        out.putInt(strings.size());
        out.putInt(NO_STYLES);
        out.putInt(utf8 ? UTF8_FLAG : UTF16_FLAG);
        out.putInt(stringsStart);
        out.putInt(NO_STYLES);
        int offset = 0;
        for (byte[] bytes : encoded) {
            out.putInt(offset);
            offset += bytes.length;
        }
        for (byte[] bytes : encoded) {
            out.put(bytes);
        }
        return out.array();
    }

    private static byte[] utf8String(String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (value.length() > MAX_SHORT_STRING_LENGTH || bytes.length > MAX_SHORT_STRING_LENGTH) {
            throw new IllegalArgumentException("String exceeds " + MAX_SHORT_STRING_LENGTH + " bytes: " + value);
        }
        byte[] encoded = new byte[UTF8_LENGTH_BYTES + bytes.length + TERMINATOR_LENGTH];
        encoded[0] = (byte) value.length();
        encoded[1] = (byte) bytes.length;
        System.arraycopy(bytes, 0, encoded, UTF8_LENGTH_BYTES, bytes.length);
        return encoded;
    }

    private static byte[] utf16String(String value) {
        if (value.length() > MAX_SHORT_STRING_LENGTH) {
            throw new IllegalArgumentException("String exceeds " + MAX_SHORT_STRING_LENGTH + " characters: " + value);
        }
        ByteBuffer out = ByteBuffer
            .allocate((UTF16_LENGTH_CHARS + value.length() + TERMINATOR_LENGTH) * Character.BYTES)
            .order(ByteOrder.LITTLE_ENDIAN);
        out.putShort((short) value.length());
        for (int index = 0; index < value.length(); index++) {
            out.putChar(value.charAt(index));
        }
        out.putChar('\0');
        return out.array();
    }

    private static void putChunkHeader(ByteBuffer out, short type, int headerSize, int size) {
        out.putShort(type);
        out.putShort((short) headerSize);
        out.putInt(size);
    }

    private static int align(int size) {
        return (size + CHUNK_ALIGNMENT - 1) / CHUNK_ALIGNMENT * CHUNK_ALIGNMENT;
    }

    private static void putStoredEntry(ZipOutputStream zip, String name, byte[] content) throws IOException {
        CRC32 crc = new CRC32();
        crc.update(content);
        ZipEntry entry = new ZipEntry(name);
        entry.setMethod(ZipEntry.STORED);
        entry.setSize(content.length);
        entry.setCompressedSize(content.length);
        entry.setCrc(crc.getValue());
        zip.putNextEntry(entry);
        zip.write(content);
        zip.closeEntry();
    }

}
