/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.payloadtools;

import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.ValueType;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.Annotation;
import com.android.tools.smali.dexlib2.iface.AnnotationElement;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.iface.value.ArrayEncodedValue;
import com.android.tools.smali.dexlib2.iface.value.EncodedValue;
import com.android.tools.smali.dexlib2.iface.value.TypeEncodedValue;
import com.android.tools.smali.dexlib2.immutable.ImmutableAnnotation;
import com.android.tools.smali.dexlib2.immutable.ImmutableAnnotationElement;
import com.android.tools.smali.dexlib2.immutable.ImmutableClassDef;
import com.android.tools.smali.dexlib2.immutable.value.ImmutableArrayEncodedValue;
import com.android.tools.smali.dexlib2.writer.io.FileDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public final class StripPayloadClasses {

    private StripPayloadClasses() {
    }

    public static void main(String[] arguments) throws IOException {
        final Path resources = new File(arguments[0]).toPath();
        final Path extensions = new File(arguments[1]).toPath();
        final Path output = new File(arguments[2]).toPath();
        final Map<Path, List<String>> removed = removedClasses(extensions);
        try (Stream<Path> files = Files.walk(resources)) {
            for (Path file : (Iterable<Path>) files.filter((path) -> path.toString().endsWith(".mpe"))::iterator) {
                final Path resource = resources.relativize(file);
                final Path stripped = output.resolve(resource);
                Files.createDirectories(stripped.getParent());
                Files.write(stripped, rewrite(Files.readAllBytes(file), removed.getOrDefault(resource, List.of())));
            }
        }
    }

    private static Map<Path, List<String>> removedClasses(Path extensions) throws IOException {
        final Map<Path, List<String>> lists = new HashMap<>();
        try (Stream<Path> files = Files.find(extensions, 3, (path, attributes) -> attributes.isRegularFile()
                && path.getFileName().toString().equals("removed.list"))) {
            for (Path list : (Iterable<Path>) files::iterator) {
                final Path group = list.getParent();
                final Path resource = Path.of("extensions", "hxreborn", group.getParent().getFileName() + "-" + group.getFileName() + ".mpe");
                lists.put(resource, Files.readAllLines(list).stream().filter((line) -> !line.isBlank()).toList());
            }
        }
        return lists;
    }

    private static byte[] rewrite(byte[] dex, List<String> removedNames) throws IOException {
        final DexBackedDexFile file = new DexBackedDexFile(Opcodes.getDefault(), ByteBuffer.wrap(dex));
        final DexPool pool = new DexPool(Opcodes.getDefault());
        for (ClassDef classDef : file.getClasses()) {
            final String type = classDef.getType();
            if (type.endsWith("/R;") || isRemoved(type, removedNames)) {
                continue;
            }
            pool.internClass(removedNames.isEmpty() ? classDef : withoutRemovedTypes(classDef, removedNames));
        }
        final File temporary = File.createTempFile("payload", ".dex");
        try {
            pool.writeTo(new FileDataStore(temporary));
            final byte[] written = Files.readAllBytes(temporary.toPath());
            requireNoReference(written, removedNames);
            return written;
        } finally {
            temporary.delete();
        }
    }

    private static boolean isRemoved(String type, List<String> removedNames) {
        for (String name : removedNames) {
            if (type.equals("L" + name + ";") || type.startsWith("L" + name + "$")) {
                return true;
            }
        }
        return false;
    }

    private static ClassDef withoutRemovedTypes(ClassDef classDef, List<String> removedNames) {
        final List<Annotation> annotations = new ArrayList<>();
        for (Annotation annotation : classDef.getAnnotations()) {
            final List<AnnotationElement> elements = new ArrayList<>();
            for (AnnotationElement element : annotation.getElements()) {
                final EncodedValue value = withoutRemovedTypes(element.getValue(), removedNames);
                if (value != null) {
                    elements.add(new ImmutableAnnotationElement(element.getName(), value));
                }
            }
            if (elements.isEmpty() && !annotation.getElements().isEmpty()) {
                continue;
            }
            annotations.add(new ImmutableAnnotation(annotation.getVisibility(), annotation.getType(), elements));
        }
        final ImmutableClassDef immutable = ImmutableClassDef.of(classDef);
        return new ImmutableClassDef(immutable.getType(), immutable.getAccessFlags(), immutable.getSuperclass(),
                new ArrayList<>(immutable.getInterfaces()), immutable.getSourceFile(), annotations,
                immutable.getFields(), immutable.getMethods());
    }

    private static EncodedValue withoutRemovedTypes(EncodedValue value, List<String> removedNames) {
        if (value.getValueType() == ValueType.TYPE) {
            return isRemoved(((TypeEncodedValue) value).getValue(), removedNames) ? null : value;
        }
        if (value.getValueType() != ValueType.ARRAY) {
            return value;
        }
        final List<EncodedValue> kept = new ArrayList<>();
        for (EncodedValue element : ((ArrayEncodedValue) value).getValue()) {
            final EncodedValue replaced = withoutRemovedTypes(element, removedNames);
            if (replaced != null) {
                kept.add(replaced);
            }
        }
        return kept.isEmpty() ? null : new ImmutableArrayEncodedValue(kept);
    }

    private static void requireNoReference(byte[] dex, List<String> removedNames) throws IOException {
        final DexBackedDexFile file = new DexBackedDexFile(Opcodes.getDefault(), ByteBuffer.wrap(dex));
        for (String name : removedNames) {
            final String simpleName = name.substring(name.lastIndexOf('/') + 1);
            for (String string : file.getStringSection()) {
                if (string.contains(simpleName)) {
                    throw new IOException("Removed class " + name + " is still referenced by " + string);
                }
            }
        }
    }

}
