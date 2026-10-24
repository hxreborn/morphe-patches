/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.shared;

import java.security.MessageDigest;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;

@SuppressWarnings("unused")
public final class InstalledCertificate {

    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

    private InstalledCertificate() {

    }

    public static String[] appendSha256Hex(Context context, String[] digests) {
        if (context == null || digests == null) {
            return digests;
        }
        try {
            Signature[] signers = signers(context);
            String[] combined = new String[digests.length + signers.length];
            System.arraycopy(digests, 0, combined, 0, digests.length);
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            for (int i = 0; i < signers.length; i++) {
                combined[digests.length + i] = hex(sha256.digest(signers[i].toByteArray()));
            }
            return combined;
        } catch (Exception ex) {
            return digests;
        }
    }

    @SuppressWarnings("deprecation")
    private static Signature[] signers(Context context) throws PackageManager.NameNotFoundException {
        PackageManager packageManager = context.getPackageManager();
        String packageName = context.getPackageName();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageInfo info = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES);
            return info.signingInfo.getApkContentsSigners();
        }
        PackageInfo info = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
        return info.signatures;
    }

    private static String hex(byte[] bytes) {
        char[] chars = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            chars[i * 2] = HEX_DIGITS[(bytes[i] >> 4) & 0xf];
            chars[i * 2 + 1] = HEX_DIGITS[bytes[i] & 0xf];
        }
        return new String(chars);
    }

}
