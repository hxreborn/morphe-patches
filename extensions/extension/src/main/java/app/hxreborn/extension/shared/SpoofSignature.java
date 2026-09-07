/*
 * SPDX-FileCopyrightText: 2026 hxreborn
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

import android.app.Application;
import android.content.pm.PackageInfo;
import android.content.pm.Signature;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Base64;

import java.lang.reflect.Field;

@SuppressWarnings("unused")
public class SpoofSignature extends Application {
    static {
        String packageName = "<package-name>";
        String certificate = "<certificate>";

        spoofPackageInfoCreator(packageName, certificate);
    }

    private static void spoofPackageInfoCreator(String packageName, String certificate) {
        Signature spoofed = new Signature(Base64.decode(certificate, Base64.DEFAULT));
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

        try {
            Field creatorField = PackageInfo.class.getDeclaredField("CREATOR");
            creatorField.setAccessible(true);
            creatorField.set(null, creator);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not replace PackageInfo.CREATOR", e);
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
}
