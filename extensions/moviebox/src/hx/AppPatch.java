/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * See LICENSE for GPLv3.
 * Additional term under GPLv3 Section 7(b): When conveying this material
 * or modified versions, preserve this notice and credit hxreborn for this
 * material in any Appropriate Legal Notices displayed by works containing it.
 */
package hx;

import java.io.IOException;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import hx.gateway.BuildTag;
import hx.gateway.BuildTagInterceptor;
import hx.gateway.CloudFrontPlayInfoClient;
import hx.loopback.DashServer;
import hx.okhttp.ApiClient;
import hx.platform.Notices;
import hx.rewrite.DashInterceptor;
import hx.rewrite.HomeTabs;
import hx.source.MovieBoxSource;
import hx.util.Logs;

@SuppressWarnings("unused")
public final class AppPatch {

    private static final String MANAGER_CLASS = "fh.c";

    private static final String INSTALL_FAILED = "Playback patch failed to load";

    private static final int MANAGER_RETRY_LIMIT = 200;

    private static final long MANAGER_RETRY_DELAY_MS = 50L;

    private AppPatch() {
    }

    public static void install(ClassLoader preferred) {
        new Installer(preferred).start();
    }

    private static Class<?> resolve(String name, ClassLoader preferred) {
        ClassLoader[] loaders = { preferred, AppPatch.class.getClassLoader(),
                Thread.currentThread().getContextClassLoader() };
        for (ClassLoader loader : loaders) {
            Class<?> found = (loader != null) ? loadOrNull(name, loader) : null;
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static Class<?> loadOrNull(String name, ClassLoader loader) {
        try {
            return Class.forName(name, false, loader);
        } catch (ClassNotFoundException ex) {
            return null;
        }
    }

    private static void doInstall(Class<?> manager) throws ReflectiveOperationException {
        AppNetwork network = AppNetwork.locate(manager);
        BuildTag buildTag = BuildTag.fromInstalledApp();
        Object taggedClient = withBuildTagInterceptor(network.loader, network.client, buildTag);
        MovieBoxSource source = new MovieBoxSource(new ApiClient(network.loader, taggedClient, network.apiBase),
                new CloudFrontPlayInfoClient(buildTag));
        DashServer server = bindServer(source);
        boolean routeDownloads = !isMainProcess() || server != null;
        try {
            if (server != null) {
                server.start();
            }
            DashInterceptor dash = new DashInterceptor(network.loader, source, routeDownloads, HomeTabs.readHidden());
            Object dashProxy = ClientInterceptors.proxy(network.loader, dash);
            network.useClient(ClientInterceptors.addFirst(taggedClient, dashProxy));
        } catch (ReflectiveOperationException | RuntimeException ex) {
            if (server != null) {
                server.close();
            }
            throw ex;
        }
        Log.i(Logs.TAG, "DASH interceptor installed");
    }

    private static Object withBuildTagInterceptor(ClassLoader loader, Object client, BuildTag buildTag)
            throws ReflectiveOperationException {
        if (buildTag == null) {
            return client;
        }
        Object proxy = ClientInterceptors.proxy(loader, new BuildTagInterceptor(buildTag));
        Object tagged = ClientInterceptors.addBeforeTerminal(client, proxy);
        Log.i(Logs.TAG, "build tag interceptor installed");
        return tagged;
    }

    private static DashServer bindServer(MovieBoxSource source) {
        if (!isMainProcess()) {
            return null;
        }
        try {
            return DashServer.bind(source);
        } catch (IOException ex) {
            Log.e(Logs.TAG, "loopback server unavailable, resource lists keep their own links", ex);
            return null;
        }
    }

    private static boolean isMainProcess() {
        return ProcessName.isMain(ProcessName.current());
    }

    private static final class Installer implements Runnable {

        private final Handler main = new Handler(Looper.getMainLooper());

        private final ClassLoader preferred;

        private int attempts;

        private boolean laboratoryUnlocked;

        private Throwable failure;

        Installer(ClassLoader preferred) {
            this.preferred = preferred;
        }

        void start() {
            this.main.post(this);
        }

        @Override
        public void run() {
            if (tryInstall()) {
                return;
            }
            if (++this.attempts < MANAGER_RETRY_LIMIT) {
                this.main.postDelayed(this, MANAGER_RETRY_DELAY_MS);
                return;
            }
            reportFailure();
        }

        private boolean tryInstall() {
            Class<?> manager = resolve(MANAGER_CLASS, this.preferred);
            if (manager == null) {
                return false;
            }
            if (!this.laboratoryUnlocked) {
                Laboratory.unlock(manager.getClassLoader());
                this.laboratoryUnlocked = true;
            }
            try {
                doInstall(manager);
                return true;
            } catch (Throwable ex) {
                this.failure = ex;
                return false;
            }
        }

        private void reportFailure() {
            if (this.failure == null) {
                Log.e(Logs.TAG, "network manager class not found");
            } else {
                Log.e(Logs.TAG, "cannot install DASH interceptor", this.failure);
            }
            Notices.show(INSTALL_FAILED);
        }

    }

}
