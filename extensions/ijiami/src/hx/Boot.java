/*
 * Copyright (C) 2026 hxreborn
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
 */
package hx;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.Signature;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Process;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

final class Boot {
    private static final String TAG = "hxreborn/ijiami";

    private static final String CERTIFICATE_ASSET = "assets/hx.cer";
    private static final String FACTORY_ASSET = "assets/hx.factory";
    private static final String NATIVE_LIBRARY = "libhxpatch.so";
    private static final String NATIVE_LIBRARY_ENTRY = "lib/arm64-v8a/" + NATIVE_LIBRARY;
    private static final String APP_HOOK_CLASS = "hx.AppPatch";

    private static boolean installed;
    private static boolean appHookStarted;
    private static boolean assetsRead;
    private static String packerFactory = "";
    private static byte[] certificate = new byte[0];

    private Boot() {
    }

    static synchronized void install(ApplicationInfo info) {
        if (installed) return;
        installed = true;

        readAssets(info);

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

    static void runAppHook(final ClassLoader loader) {
        synchronized (Boot.class) {
            if (appHookStarted) return;
            appHookStarted = true;
        }

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Class.forName(APP_HOOK_CLASS, true, Boot.class.getClassLoader())
                            .getMethod("install", ClassLoader.class)
                            .invoke(null, loader);
                } catch (ClassNotFoundException noHook) {
                } catch (Throwable t) {
                    Log.e(TAG, "app hook failed", t);
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
            assetsRead = certificate.length > 0 && !packerFactory.isEmpty();

            if (!assetsRead) Log.e(TAG, "missing patch assets in " + info.sourceDir);
        } catch (Throwable t) {
            Log.e(TAG, "cannot read patch assets from " + info.sourceDir, t);
        }
    }

    // Original factory name required by the packer's identity check
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

    private static void loadNativeLibrary(ApplicationInfo info) throws Exception {
        try {
            System.load(info.nativeLibraryDir + File.separator + NATIVE_LIBRARY);
            return;
        } catch (Throwable ignored) {
            // Not extracted at install time
        }

        File extracted = new File(info.dataDir + File.separator + "files", NATIVE_LIBRARY);

        try (ZipFile archive = new ZipFile(info.sourceDir)) {
            ZipEntry entry = archive.getEntry(NATIVE_LIBRARY_ENTRY);
            if (entry == null) {
                throw new IllegalStateException("missing APK entry: " + NATIVE_LIBRARY_ENTRY);
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
