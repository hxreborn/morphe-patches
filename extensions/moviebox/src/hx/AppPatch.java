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

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import hx.gateway.BuildTag;
import hx.gateway.BuildTagInterceptor;
import hx.gateway.CloudFrontPlayInfoClient;
import hx.gateway.SharedHeaders;
import hx.loopback.DashServer;
import hx.okhttp.ApiClient;
import hx.platform.HotPatches;
import hx.platform.InstalledApp;
import hx.platform.Notices;
import hx.rewrite.DashInterceptor;
import hx.rewrite.HomeTabs;
import hx.source.MovieBoxSource;
import hx.util.Logs;

@SuppressWarnings("unused")
public final class AppPatch {

    private static final String MANAGER_CLASS = "fh.c";

    private static final String SHARED_HEADERS_CLASS = "eh.b";

    private static final String INSTALL_FAILED = "Playback patch failed to load";

    private static final String CERTIFICATE_UNREADABLE = "Playback patch cannot read its certificate";

    private static final String SHARED_HEADERS_MISSING = "Playback patch cannot tag every request";

    private static final RetrySchedule INSTALL_RETRIES = new RetrySchedule(200, 50L, 300, 1000L);

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

    private static void installInterceptors(Class<?> manager, BuildTag buildTag, BuildTagInterceptor tagger)
            throws ReflectiveOperationException {
        AppNetwork network = AppNetwork.locate(manager);
        Object taggedClient = withBuildTagInterceptor(network.loader, network.client, tagger);
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

    private static Object withBuildTagInterceptor(ClassLoader loader, Object client, BuildTagInterceptor tagger)
            throws ReflectiveOperationException {
        Object tagged = ClientInterceptors.addBeforeTerminal(client, ClientInterceptors.proxy(loader, tagger));
        Log.i(Logs.TAG, "build tag interceptor installed");
        return tagged;
    }

    private static Map<String, String> sharedHeaders(ClassLoader loader) {
        try {
            Map<String, String> headers = SharedHeaders.of(Class.forName(SHARED_HEADERS_CLASS, false, loader));
            Log.i(Logs.TAG, "shared request headers found");
            return headers;
        } catch (ReflectiveOperationException | RuntimeException ex) {
            Log.e(Logs.TAG, "shared request headers not found", ex);
            Notices.showOnce(SHARED_HEADERS_MISSING);
            return new ConcurrentHashMap<>();
        }
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

        private boolean hotPatchesChecked;

        private BuildTag buildTag;

        private BuildTagInterceptor tagger;

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
            this.attempts++;
            if (INSTALL_RETRIES.reportsAfter(this.attempts)) {
                reportFailure();
            }
            long delay = INSTALL_RETRIES.delayAfter(this.attempts);
            if (delay == RetrySchedule.STOP) {
                Log.e(Logs.TAG, "playback patch not installed after " + this.attempts + " attempts", this.failure);
                return;
            }
            this.main.postDelayed(this, delay);
        }

        private boolean tryInstall() {
            if (!this.hotPatchesChecked) {
                this.hotPatchesChecked = removeHotPatches();
            }
            Class<?> manager = resolve(MANAGER_CLASS, this.preferred);
            if (manager == null) {
                return false;
            }
            if (!this.laboratoryUnlocked) {
                Laboratory.unlock(manager.getClassLoader());
                this.laboratoryUnlocked = true;
            }
            if (this.tagger == null && !createTagger(manager.getClassLoader())) {
                return false;
            }
            this.tagger.tagSharedHeaders();
            try {
                installInterceptors(manager, this.buildTag, this.tagger);
                return true;
            } catch (Throwable ex) {
                this.failure = ex;
                return false;
            }
        }

        private boolean removeHotPatches() {
            try {
                Context context = InstalledApp.context();
                if (context == null) {
                    return false;
                }
                if (HotPatches.removeApplied(context)) {
                    Log.i(Logs.TAG, "applied hot patches removed");
                }
                return true;
            } catch (ReflectiveOperationException | RuntimeException ex) {
                Log.w(Logs.TAG, "hot patches not checked: " + ex);
                return false;
            }
        }

        private boolean createTagger(ClassLoader loader) {
            BuildTag tag = BuildTag.fromInstalledApp();
            if (tag == null) {
                return false;
            }
            this.buildTag = tag;
            this.tagger = new BuildTagInterceptor(tag, sharedHeaders(loader));
            return true;
        }

        private void reportFailure() {
            if (this.laboratoryUnlocked && this.tagger == null) {
                Log.e(Logs.TAG, "certificate unreadable, requests go out untagged");
                Notices.show(CERTIFICATE_UNREADABLE);
                return;
            }
            if (this.failure == null) {
                Log.e(Logs.TAG, "network manager class not found");
            } else {
                Log.e(Logs.TAG, "cannot install DASH interceptor", this.failure);
            }
            Notices.show(INSTALL_FAILED);
        }

    }

}
