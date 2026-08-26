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
package app.hxreborn.extension.protonmail;

import android.app.Application;
import android.content.pm.PackageInfo;
import android.content.pm.Signature;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Base64;

import java.lang.reflect.Field;

public class SpoofSignature extends Application {
    private static final String PACKAGE_NAME = "ch.protonmail.android";
    private static final String CERTIFICATE_DATA = """
            MIIDxTCCAq2gAwIBAgIEB/UCODANBgkqhkiG9w0BAQsFADCBkjELMAkGA1UEBhMCQ0gxDzANBgNVBAgTBkdlbmV2\
            YTEPMA0GA1UEBxMGR2VuZXZhMR8wHQYDVQQKExZQcm90b24gVGVjaG5vbG9naWVzIEFHMR8wHQYDVQQLExZQ\
            cm90b24gVGVjaG5vbG9naWVzIEFHMR8wHQYDVQQDExZQcm90b24gVGVjaG5vbG9naWVzIEFHMB4XDTE1MDMx\
            ODE5MjMyNFoXDTQyMDgwMzE5MjMyNFowgZIxCzAJBgNVBAYTAkNIMQ8wDQYDVQQIEwZHZW5ldmExDzANBgNV\
            BAcTBkdlbmV2YTEfMB0GA1UEChMWUHJvdG9uIFRlY2hub2xvZ2llcyBBRzEfMB0GA1UECxMWUHJvdG9uIFRl\
            Y2hub2xvZ2llcyBBRzEfMB0GA1UEAxMWUHJvdG9uIFRlY2hub2xvZ2llcyBBRzCCASIwDQYJKoZIhvcNAQEB\
            BQADggEPADCCAQoCggEBAJAtCCo9WigQLdn4AljaS5uMfQQft0ZWEJcCLufQQp6zoceZPUcuUBhi4Xpsmw\
            Zq/aWkanUrsm75yKl+konoCIo7TdadAztxL/iGLAlGY2DKDKvALIqpvQsg3Hp1dJHDUExWAMf2d0P0Ysqb\
            4I1IFMVgm5/bFoOkirhijuxJJFgJUxtplO/qSCUutTqHqbRKmrH4VH65a9jqGWvXsyhr+tDp5aH4nBxR+z\
            d3SeThs4DSCcSb+SXtOcRL/4qmT2cRkhVbfzTVq7xQJ1kI1woYIp3sEHia8+plB1buS+CYjjES+Dcj6QT9\
            VHyfdyC2QY/6g6BusjjFrPvr1OOmwNZUOLUCAwEAAaMhMB8wHQYDVR0OBBYEFLC4uoH4KQg2dgnUSVdi3h\
            b5d5heMA0GCSqGSIb3DQEBCwUAA4IBAQCJe1HQTaHuV/cg9+Og+zxd3VtbsVID8ufxV3dqg8P2z/U0qfEh\
            srdRkn946/0mi4Wy6D4bl5eokV8YDGWl+oADKCc2N4BVyOYReCjIDwynx/1KkjOvqE2w2hhRUYMfdHFMp/\
            pmtKjYnH7OSpV7GZyxtYkRUSg4CrLyPBGzl0ZgPFSQLzKYNMJU9Awy2tg2Y2p2gxZnkQxztd4twQh/oBTk\
            qBv+PD0kFIBgqJ7o8oMqHlqlEQW/T7j8SDDSKZULqdgM5vuRlp22XDBIUZbuK6HOnJCgX98b8Q9Tzrv67A\
            yJ8Lbsis5uTr0JCc415OKAuyHzeISqoKI5PEDbHOSn5Vol""";

    static {
        spoofPackageInfoCreator();
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
