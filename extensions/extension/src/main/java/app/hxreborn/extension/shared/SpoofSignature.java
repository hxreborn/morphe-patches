/*
 * Copyright (C) 2026 Morphe
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from MorpheApp/morphe-patches:
 * https://github.com/MorpheApp/morphe-patches/commit/99d0f41e5ca1a043d8b512556b077acf35ba5db6
 * Commit 99d0f41e5ca1a043d8b512556b077acf35ba5db6 (2026-07-25),
 * extensions/reddit/src/main/java/app/morphe/extension/reddit/patches/SpoofSignaturePatch.java
 *
 * Adapted from:
 * https://github.com/L-JINBIN/ApkSignatureKillerEx/blob/3e6a8dc7de1b108dc70647f62bc499d7b68852b2/killer/src/main/java/bin/mt/signature/KillerApplication.java
 */
package app.hxreborn.extension.shared;

import java.lang.reflect.Field;
import java.util.Map;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.util.Base64;

@SuppressWarnings("unused")
public class SpoofSignature extends Application {

    private static PackageManagerBinder packageManagerBinder;

    private static Object spoofedPackageManager;

    private static final Signature spoofedSignature;

    static {
        String packageName = "<package-name>";
        String certificate = "<certificate>";

        spoofedSignature = new Signature(Base64.decode(certificate, Base64.DEFAULT));
        try {
            spoofPackageInfoCreator(packageName, spoofedSignature);
        } catch (ReflectiveOperationException ex) {
            spoofPackageManagerBinder(packageName, spoofedSignature);
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        PackageManager packageManager = base.getPackageManager();
        if (packageManagerBinder != null) {
            replacePackageManagerProxy(packageManager);
            packageManagerBinder.learnPackageInfoTransactionCode(packageManager);
        }
        spoofCachedPackageInfo(packageManager, base.getPackageName());
    }

    private static void spoofPackageInfoCreator(String packageName, Signature spoofed)
            throws ReflectiveOperationException {
        Parcelable.Creator<PackageInfo> original = PackageInfo.CREATOR;

        Parcelable.Creator<PackageInfo> creator = new Parcelable.Creator<>() {
            @Override
            public PackageInfo createFromParcel(Parcel source) {
                PackageInfo packageInfo = original.createFromParcel(source);
                spoofSignatures(packageInfo, packageName, spoofed);

                return packageInfo;
            }

            @Override
            public PackageInfo[] newArray(int size) {
                return original.newArray(size);
            }
        };

        Field creatorField = PackageInfo.class.getDeclaredField("CREATOR");
        creatorField.setAccessible(true);
        creatorField.set(null, creator);
    }

    @SuppressWarnings("deprecation")
    private static void spoofCachedPackageInfo(PackageManager packageManager, String packageName) {
        spoofCachedPackageInfo(packageManager, packageName, PackageManager.GET_SIGNATURES);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            spoofCachedPackageInfo(packageManager, packageName, PackageManager.GET_SIGNING_CERTIFICATES);
            spoofCachedPackageInfo(packageManager, packageName,
                    PackageManager.GET_SIGNATURES | PackageManager.GET_SIGNING_CERTIFICATES);
        }
    }

    private static void spoofCachedPackageInfo(PackageManager packageManager, String packageName, int flags) {
        try {
            spoofSignatures(packageManager.getPackageInfo(packageName, flags), packageName, spoofedSignature);
        } catch (PackageManager.NameNotFoundException ex) {
            throw new IllegalStateException("Could not read the package info of " + packageName, ex);
        }
    }

    @SuppressWarnings("unchecked")
    private static void spoofPackageManagerBinder(String packageName, Signature spoofed) {
        try {
            Class<?> activityThread = Class.forName("android.app.ActivityThread");
            IInterface original = (IInterface) activityThread.getDeclaredMethod("getPackageManager").invoke(null);
            PackageManagerBinder binder = new PackageManagerBinder(original.asBinder(), packageName, spoofed);
            Object packageManager = Class.forName("android.content.pm.IPackageManager$Stub")
                .getDeclaredMethod("asInterface", IBinder.class)
                .invoke(null, binder);

            Field packageManagerField = activityThread.getDeclaredField("sPackageManager");
            packageManagerField.setAccessible(true);
            packageManagerField.set(null, packageManager);

            Field cacheField = Class.forName("android.os.ServiceManager").getDeclaredField("sCache");
            cacheField.setAccessible(true);
            ((Map<String, IBinder>) cacheField.get(null)).put("package", binder);

            packageManagerBinder = binder;
            spoofedPackageManager = packageManager;
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Could not wrap the package manager binder", ex);
        }
    }

    private static void replacePackageManagerProxy(PackageManager packageManager) {
        try {
            Field binderField = packageManager.getClass().getDeclaredField("mPM");
            binderField.setAccessible(true);
            binderField.set(packageManager, spoofedPackageManager);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Could not replace ApplicationPackageManager.mPM", ex);
        }
    }

    @SuppressWarnings("deprecation")
    private static void spoofSignatures(PackageInfo packageInfo, String packageName, Signature spoofed) {
        if (!packageName.equals(packageInfo.packageName)) {
            return;
        }

        replaceFirst(packageInfo.signatures, spoofed);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && packageInfo.signingInfo != null) {
            replaceFirst(packageInfo.signingInfo.getApkContentsSigners(), spoofed);
        }
    }

    private static void replaceFirst(Signature[] signatures, Signature spoofed) {
        if (signatures != null && signatures.length > 0) {
            signatures[0] = spoofed;
        }
    }

    private static final class PackageManagerBinder extends Binder {

        private final IBinder original;

        private final String packageName;

        private final Signature spoofed;

        private volatile Thread learningThread;

        private volatile int packageInfoTransactionCode = -1;

        PackageManagerBinder(IBinder original, String packageName, Signature spoofed) {
            this.original = original;
            this.packageName = packageName;
            this.spoofed = spoofed;
        }

        @SuppressWarnings("deprecation")
        void learnPackageInfoTransactionCode(PackageManager packageManager) {
            this.learningThread = Thread.currentThread();
            try {
                packageManager.getPackageInfo(this.packageName,
                        PackageManager.GET_CONFIGURATIONS | PackageManager.GET_GIDS);
            } catch (PackageManager.NameNotFoundException ex) {
                throw new IllegalStateException("Could not read the package info of " + this.packageName, ex);
            } finally {
                this.learningThread = null;
            }

            if (this.packageInfoTransactionCode < 0) {
                throw new IllegalStateException("Could not learn the getPackageInfo transaction code");
            }
        }

        @Override
        protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (this.packageInfoTransactionCode < 0 && Thread.currentThread() == this.learningThread) {
                this.packageInfoTransactionCode = code;
            }

            boolean handled = this.original.transact(code, data, reply, flags);
            if (handled && reply != null && code == this.packageInfoTransactionCode) {
                spoofReply(reply);
            }

            return handled;
        }

        private void spoofReply(Parcel reply) {
            reply.setDataPosition(0);
            try {
                reply.readException();
            } catch (RuntimeException ex) {
                return;
            }

            int resultStart = reply.dataPosition();
            PackageInfo packageInfo = reply.readTypedObject(PackageInfo.CREATOR);
            if (packageInfo == null || !this.packageName.equals(packageInfo.packageName)) {
                return;
            }

            spoofSignatures(packageInfo, this.packageName, this.spoofed);
            reply.setDataSize(resultStart);
            reply.setDataPosition(resultStart);
            reply.writeTypedObject(packageInfo, Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
        }

    }

}
