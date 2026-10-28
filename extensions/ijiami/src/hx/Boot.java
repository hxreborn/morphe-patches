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
package hx;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.Signature;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Process;
import android.util.Log;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

final class Boot {
    private static final String TAG = "hxreborn/ijiami";

    private static final String CERTIFICATE_ASSET = "assets/hx.cer";
    private static final String FACTORY_ASSET = "assets/hx.factory";
    private static final String OVERLAY_CLASSES_ASSET = "assets/hx.classes";
    private static final String OVERLAY_CODE_ASSET = "assets/hx.code";
    private static final String NATIVE_LIBRARY = "libhxpatch.so";
    private static final String ARM64_ABI = "arm64-v8a";
    private static final String ARM32_ABI = "armeabi-v7a";
    private static final String APP_HOOK_ASSET = "assets/hx.hook";
    private static final String STRIP_WARNING =
            "Disable \"Optimize for device architecture\" & re-patch";
    private static final String ARCH_WARNING = "This device's architecture is not supported by the patch";
    static final String APP_HOOK_FAILED = "Patch hook failed";
    static final String CODE_OVERLAY_FAILED = "Patch cannot write its code overlay";
    private static final int TOAST_ATTEMPTS = 30;
    private static final long TOAST_RETRY_MILLIS = 1000L;

    private static boolean installed;
    private static boolean appHookStarted;
    private static boolean assetsRead;
    private static String packerFactory = "";
    private static String appHookClass = "";
    private static byte[] certificate = new byte[0];

    private Boot() {
    }

    static synchronized void install(ApplicationInfo info) {
        if (installed) return;
        installed = true;

        readAssets(info);

        try {
            warnIfArchitectureStripped(info);
        } catch (Throwable t) {
            Log.e(TAG, "cannot verify native library architectures", t);
        }
        try {
            loadNativeLibrary(info);
        } catch (Throwable t) {
            Log.e(TAG, "cannot load " + NATIVE_LIBRARY, t);
        }
        try {
            spoofSignature(info.packageName);
        } catch (Throwable t) {
            Log.e(TAG, "cannot spoof signing certificate", t);
        }
        try {
            spoofApplicationInfo(info.packageName);
        } catch (Throwable t) {
            Log.e(TAG, "cannot spoof appComponentFactory", t);
        }
    }

    static synchronized String packerFactory(ApplicationInfo info) {
        readAssets(info);
        return packerFactory;
    }

    static void writeCodeOverlay(ClassLoader loader, ApplicationInfo info) {
        byte[] code;
        String[] classNames;
        try (ZipFile archive = new ZipFile(info.sourceDir)) {
            code = read(archive, OVERLAY_CODE_ASSET);
            classNames = new String(read(archive, OVERLAY_CLASSES_ASSET), StandardCharsets.UTF_8).split("\n");
        } catch (Throwable t) {
            Log.e(TAG, "cannot read code overlay from " + info.sourceDir, t);
            return;
        }
        if (code.length == 0) return;

        int loaded = 0;
        for (String name : classNames) {
            if (name.isEmpty()) continue;
            try {
                Class.forName(name, false, loader);
                loaded++;
            } catch (Throwable t) {
                Log.e(TAG, "cannot load " + name + " before the code overlay", t);
            }
        }

        try {
            int written = writeCode(code);
            Log.i(TAG, "code overlay: loaded " + loaded + " classes, wrote " + written + " code items");
        } catch (Throwable t) {
            Log.e(TAG, "cannot write code overlay", t);
            toast(failureNotice(CODE_OVERLAY_FAILED, t));
        }
    }

    private static native int writeCode(byte[] records);

    static void runAppHook(final ClassLoader loader) {
        final String hookClass;
        synchronized (Boot.class) {
            if (appHookStarted || appHookClass.isEmpty()) return;
            appHookStarted = true;
            hookClass = appHookClass;
        }

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Class.forName(hookClass, true, Boot.class.getClassLoader())
                            .getMethod("install", ClassLoader.class)
                            .invoke(null, loader);
                } catch (Throwable t) {
                    Log.e(TAG, "app hook failed", t);
                    toast(failureNotice(APP_HOOK_FAILED, t));
                }
            }
        }, "hxreborn-apphook");
        thread.setDaemon(true);
        thread.start();
    }

    private static synchronized void readAssets(ApplicationInfo info) {
        if (assetsRead) return;

        try (ZipFile archive = new ZipFile(info.sourceDir)) {
            certificate = read(archive, CERTIFICATE_ASSET);
            packerFactory = new String(read(archive, FACTORY_ASSET), StandardCharsets.UTF_8).trim();
            appHookClass = new String(read(archive, APP_HOOK_ASSET), StandardCharsets.UTF_8).trim();
            assetsRead = certificate.length > 0 && !packerFactory.isEmpty();

            if (!assetsRead) Log.e(TAG, "missing patch assets in " + info.sourceDir);
        } catch (Throwable t) {
            Log.e(TAG, "cannot read patch assets from " + info.sourceDir, t);
        }
    }

    static void restoreFactoryName(ApplicationInfo info) {
        String factory = packerFactory(info);
        if (factory.isEmpty()) return;

        info.appComponentFactory = factory;

        try {
            Class<?> activityThread = Class.forName("android.app.ActivityThread");
            Object thread = activityThread.getMethod("currentActivityThread").invoke(null);
            Field boundField = activityThread.getDeclaredField("mBoundApplication");
            boundField.setAccessible(true);
            Object bound = boundField.get(thread);
            Field loadedApkField = bound.getClass().getDeclaredField("info");
            loadedApkField.setAccessible(true);
            Object loadedApk = loadedApkField.get(bound);
            Field applicationInfoField = loadedApk.getClass().getDeclaredField("mApplicationInfo");
            applicationInfoField.setAccessible(true);
            ApplicationInfo bare = (ApplicationInfo) applicationInfoField.get(loadedApk);
            if (bare != null) bare.appComponentFactory = factory;
        } catch (Throwable t) {
            Log.e(TAG, "cannot restore bound ApplicationInfo.appComponentFactory", t);
        }
    }

    private static void warnIfArchitectureStripped(ApplicationInfo info) throws Exception {
        Set<String> present = new HashSet<>();
        try (ZipFile archive = new ZipFile(info.sourceDir)) {
            Enumeration<? extends ZipEntry> entries = archive.entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (!name.startsWith("lib/")) continue;
                int slash = name.indexOf('/', 4);
                if (slash > 4) present.add(name.substring(4, slash));
            }
        }

        if (!present.contains(ARM64_ABI) || present.contains(ARM32_ABI)) return;

        Log.e(TAG, ARM32_ABI + " native libraries absent from lib/; present " + present);
        toast(STRIP_WARNING);
    }

    static String failureNotice(String summary, Throwable t) {
        Throwable cause = (t instanceof InvocationTargetException && t.getCause() != null) ? t.getCause() : t;
        String message = cause.getMessage();
        return summary + ": " + cause.getClass().getSimpleName() + (message == null ? "" : ": " + message);
    }

    private static void toast(String message) {
        toast(message, 1);
    }

    private static void toast(final String message, final int attempt) {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    Object application = Class.forName("android.app.ActivityThread")
                            .getMethod("currentApplication").invoke(null);
                    if (application != null) {
                        Toast.makeText((Context) application, message, Toast.LENGTH_LONG).show();
                    } else if (attempt < TOAST_ATTEMPTS) {
                        toast(message, attempt + 1);
                    } else {
                        Log.e(TAG, "no application context to show \"" + message + "\"");
                    }
                } catch (Throwable t) {
                    Log.e(TAG, "cannot show \"" + message + "\"", t);
                }
            }
        }, (attempt == 1) ? 0L : TOAST_RETRY_MILLIS);
    }

    private static void loadNativeLibrary(ApplicationInfo info) throws Exception {
        String libraryEntry = "lib/" + (Process.is64Bit() ? ARM64_ABI : ARM32_ABI) + "/" + NATIVE_LIBRARY;

        try {
            System.load(info.nativeLibraryDir + File.separator + NATIVE_LIBRARY);
            return;
        } catch (Throwable ignored) {
        }

        File extracted = new File(info.dataDir + File.separator + "files", NATIVE_LIBRARY);

        try (ZipFile archive = new ZipFile(info.sourceDir)) {
            ZipEntry entry = archive.getEntry(libraryEntry);
            if (entry == null) {
                toast(ARCH_WARNING);
                throw new IllegalStateException("missing APK entry: " + libraryEntry);
            }

            if (!matches(extracted, entry)) {
                File parent = extracted.getParentFile();
                if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
                    throw new IllegalStateException("cannot create " + parent);
                }

                File partial = new File(extracted.getPath() + "." + Process.myPid() + ".partial");
                try (InputStream in = archive.getInputStream(entry);
                     OutputStream out = new FileOutputStream(partial)) {
                    copy(in, out);
                }
                if (!partial.renameTo(extracted)) {
                    partial.delete();
                    throw new IllegalStateException("cannot rename " + partial + " to " + extracted);
                }
            }
        }

        System.load(extracted.getPath());
    }

    private static boolean matches(File file, ZipEntry entry) {
        if (file.length() != entry.getSize()) return false;

        CRC32 crc = new CRC32();
        byte[] buffer = new byte[8192];
        try (InputStream in = new FileInputStream(file)) {
            int count;
            while ((count = in.read(buffer)) != -1) crc.update(buffer, 0, count);
        } catch (Throwable t) {
            return false;
        }
        return crc.getValue() == entry.getCrc();
    }

    private static byte[] read(ZipFile archive, String name) throws Exception {
        ZipEntry entry = archive.getEntry(name);
        if (entry == null) return new byte[0];

        try (InputStream in = archive.getInputStream(entry)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            copy(in, out);
            return out.toByteArray();
        }
    }

    private static void copy(InputStream in, OutputStream out) throws Exception {
        byte[] buffer = new byte[8192];
        int count;
        while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
    }

    private static void spoofSignature(final String packageName) throws Exception {
        if (certificate.length == 0) return;

        final Signature stock = new Signature(certificate);
        final Parcelable.Creator<PackageInfo> original = PackageInfo.CREATOR;

        replaceCreator(PackageInfo.class, new Parcelable.Creator<PackageInfo>() {
            @Override
            public PackageInfo createFromParcel(Parcel source) {
                PackageInfo info = original.createFromParcel(source);
                if (!packageName.equals(info.packageName)) return info;

                if (info.signatures != null && info.signatures.length > 0) {
                    info.signatures[0] = stock;
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && info.signingInfo != null) {
                    Signature[] signers = info.signingInfo.getApkContentsSigners();
                    if (signers != null && signers.length > 0) signers[0] = stock;
                }
                return info;
            }

            @Override
            public PackageInfo[] newArray(int size) {
                return original.newArray(size);
            }
        });
    }

    private static void spoofApplicationInfo(final String packageName) throws Exception {
        final String factory = packerFactory;
        if (factory.isEmpty()) return;

        final Parcelable.Creator<ApplicationInfo> original = ApplicationInfo.CREATOR;

        replaceCreator(ApplicationInfo.class, new Parcelable.Creator<ApplicationInfo>() {
            @Override
            public ApplicationInfo createFromParcel(Parcel source) {
                ApplicationInfo info = original.createFromParcel(source);
                if (packageName.equals(info.packageName)) info.appComponentFactory = factory;
                return info;
            }

            @Override
            public ApplicationInfo[] newArray(int size) {
                return original.newArray(size);
            }
        });
    }

    private static void replaceCreator(Class<?> owner, Object creator) throws Exception {
        Field field = owner.getDeclaredField("CREATOR");
        field.setAccessible(true);
        field.set(null, creator);
    }
}
