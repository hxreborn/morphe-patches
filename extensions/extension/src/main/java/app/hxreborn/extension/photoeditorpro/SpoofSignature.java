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
package app.hxreborn.extension.photoeditorpro;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.Signature;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Base64;

import java.lang.reflect.Field;

import app.morphe.extension.shared.Utils;

public class SpoofSignature extends Application {
    private static final String PACKAGE_NAME = "photo.editor.photoeditor.photoeditorpro";
    private static final String CERTIFICATE_DATA = """
            MIIDKzCCAhOgAwIBAgIEdFFbvjANBgkqhkiG9w0BAQsFADBFMRUwEwYDVQQKEwxDb2xsYWdlTWFrZXIxFTATBgNVBAsT\
            DENvbGxhZ2VNYWtlcjEVMBMGA1UEAxMMQ29sbGFnZU1ha2VyMCAXDTE2MDkyMDEwMjQ1NVoYDzIxMTYwODI3MTAyNDU1\
            WjBFMRUwEwYDVQQKEwxDb2xsYWdlTWFrZXIxFTATBgNVBAsTDENvbGxhZ2VNYWtlcjEVMBMGA1UEAxMMQ29sbGFnZU1h\
            a2VyMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA4DHiMYyWMA7OWFDvJGEi2RrHyZKTyBr+iQc2PQKUeZ08\
            JMCnb5kpeS4yIuq8lrsTLFQ3lniwRuDHb6G9bdS3YEZo4E8R/ixkU+dLtc9Xka5OKixb0gEWO+3qeo5ohmxL5Y8ha0XD\
            PQupU+U7O9jAc2+wyluqtTDZhrHNz6Uo3x4kRzZUz6v3DS/uEezBRjYwS7YQo2AdyK0wLdoq3VYbwqQjL2RSDHoexA91\
            QwwqqHO8EeNRlBEgDbM4o/U7jOy/6/6XKkZHYgk3KuD1XXvn/9truF1OBEfGqxe0cBZ++5MQrfieNJydqGAR4SCkM4me\
            we9g89/bksurSz9pFqe4RwIDAQABoyEwHzAdBgNVHQ4EFgQUiNbnaOBNU6fYphidNsYL3UzinCkwDQYJKoZIhvcNAQEL\
            BQADggEBAK5vY/4TwNO9f66qQ8EfH/JLZSG+nK4FXGlcnT/SGp92C8MZasJ6CfF2rU5rFv8Ruj3NiN7P0Izgt7JSYfh7\
            zsheAybXXZpO+aHQn+DyYGU3bS9jpfatPPf2hJg8t7LxLgZT1aDlkVByADiVN87X63zABkUvCYZ5W4hNgmwIlnCnujDG\
            w698IfjkvEey158pD/Nuk9XhS1iZ5aIFU+sZs6aSDW5RF4Ib9/yHpu/UHLbr6Cdh6XJ+da0d9236xWRFhlxFSXGmQCJK\
            oYspzFIJk0Fct0N6N14S97GO/W690BZVCBkZgwbGjLpP11pbwuMeo3bBVMymlhTZyQWIsooy2M8=""";

    static {
        spoofPackageInfoCreator();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        Utils.setContext(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        PatchPanel.install(this);
    }

    private static void spoofPackageInfoCreator() {
        Signature spoofed = new Signature(Base64.decode(CERTIFICATE_DATA, Base64.DEFAULT));
        Parcelable.Creator<PackageInfo> original = PackageInfo.CREATOR;

        Parcelable.Creator<PackageInfo> creator = new Parcelable.Creator<>() {
            @Override
            public PackageInfo createFromParcel(Parcel source) {
                PackageInfo packageInfo = original.createFromParcel(source);
                spoofSignatures(packageInfo, spoofed);

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
    private static void spoofSignatures(PackageInfo packageInfo, Signature spoofed) {
        if (!PACKAGE_NAME.equals(packageInfo.packageName)) {
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
