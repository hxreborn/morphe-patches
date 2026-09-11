/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.Signature;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.Collections;
import java.util.Locale;
import java.util.List;

@SuppressWarnings("unused")
public class Boot {
    private static final String STUB = "com.stub.StubApp";
    private static final String STUB_CONTEXT_FIELD = "d";

    private static final String CONFIGURATION_ASSET = "hcfg";
    private static final String CERTIFICATE_ASSET = "hc";
    private static final String TELEMETRY_FLAG = "telemetry";

    private static final String[] TELEMETRY_HOSTS = {
            "umeng.com", "umengcloud.com", "snssdk.com", "ctobsnssdk.com",
            "bytedance.com", "bytedance.net", "pangle.cn", "pangle.io",
            "domob.cn", "dmrtb.com", "sigmob.cn", "wkanx.com", "m.taobao.com",
    };

    static {
        String flags = "";
        try {
            flags = new String(asset(CONFIGURATION_ASSET), "UTF-8");
        } catch (Throwable ignored) {
        }

        try {
            spoofSignature(context().getPackageName(), asset(CERTIFICATE_ASSET));
        } catch (Throwable ignored) {
        }

        if (flags.contains(TELEMETRY_FLAG)) {
            try {
                blockTelemetry();
            } catch (Throwable ignored) {
            }
        }
    }

    private static Context context() throws Exception {
        Field field = Class.forName(STUB).getDeclaredField(STUB_CONTEXT_FIELD);
        field.setAccessible(true);
        return (Context) field.get(null);
    }

    private static byte[] asset(String name) throws Exception {
        InputStream in = context().getAssets().open(name);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) > 0) out.write(buffer, 0, read);
        in.close();
        return out.toByteArray();
    }




    private static boolean isTelemetry(String host) {
        if (host == null) return false;
        String lower = host.toLowerCase(Locale.ROOT);
        for (String domain : TELEMETRY_HOSTS) {
            if (lower.equals(domain) || lower.endsWith("." + domain)) return true;
        }
        return false;
    }

    private static void blockTelemetry() {
        final ProxySelector original = ProxySelector.getDefault();
        final List<Proxy> deadEnd = Collections.singletonList(
                new Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved("127.0.0.1", 1)));

        ProxySelector.setDefault(new ProxySelector() {
            @Override
            public List<Proxy> select(URI uri) {
                if (uri != null && isTelemetry(uri.getHost())) return deadEnd;
                return original == null ? Collections.singletonList(Proxy.NO_PROXY) : original.select(uri);
            }

            @Override
            public void connectFailed(URI uri, SocketAddress address, IOException failure) {
                if (original != null) original.connectFailed(uri, address, failure);
            }
        });
    }

    private static void spoofSignature(final String packageName, byte[] certificate) throws Exception {
        final Signature stock = new Signature(certificate);
        final Parcelable.Creator<PackageInfo> original = PackageInfo.CREATOR;

        Parcelable.Creator<PackageInfo> creator = new Parcelable.Creator<PackageInfo>() {
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
        };

        Field field = PackageInfo.class.getDeclaredField("CREATOR");
        field.setAccessible(true);
        field.set(null, creator);
    }
}
