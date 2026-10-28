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
package hx.source;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import android.net.Uri;
import android.os.SystemClock;
import android.util.Log;
import hx.dash.CdnCookies;
import hx.dash.DashFile;
import hx.gateway.PlayInfoLookup;
import hx.loopback.DashFileSource;
import hx.loopback.DashServer;
import hx.okhttp.ApiClient;
import hx.platform.Notices;
import hx.util.Logs;
import hx.util.RemoteFile;
import hx.util.Tasks;

public final class MovieBoxSource implements DashFileSource {

    private static final int FILE_RETENTION = 8;

    private static final int RESOURCE_RETENTION = 32;

    private static final int PLAY_INFO_ATTEMPTS = 5;

    private static final long PLAY_INFO_RETRY_DELAY_MS = 2000L;

    private static final String NO_STREAM = "No stream for this title";

    private final ApiClient api;

    private final DirectFiles directFiles;

    private final PlayInfoLookup replacementLookup;

    private final Map<String, SignedResource> resources = new LruMap<>(RESOURCE_RETENTION);

    private final Map<String, Long> originLengths = new LruMap<>(RESOURCE_RETENTION);

    private final Map<String, Boolean> handedOff = new LruMap<>(RESOURCE_RETENTION);

    private final Map<String, FutureTask<DashFile>> files = new LruMap<>(FILE_RETENTION);

    public MovieBoxSource(ApiClient api, PlayInfoLookup replacementLookup) {
        this.api = api;
        this.directFiles = new DirectFiles(api);
        this.replacementLookup = replacementLookup;
    }

    @Override
    public DashFile open(String subjectId, int season, int episode, int height, String origin, long originSize,
            String resourceId, boolean fromStart) throws IOException {
        String label = SignedResource.key(subjectId, season, episode) + "@" + height;
        String directFileUrl = this.directFiles.knownUrl(resourceId);
        if (directFileUrl != null) {
            throw new DashServer.RedirectException(directFileUrl);
        }
        SignedResource signed;
        try {
            signed = resource(subjectId, season, episode, null);
        } catch (NoSignedResourceException ex) {
            if (origin == null) {
                throw ex;
            }
            redirectToDirectFile(label, subjectId, resourceId, origin);
            if (matchesOrigin(label, origin, originSize)) {
                return null;
            }
            Notices.showOnce(NO_STREAM);
            throw new DashServer.UnavailableException(label + ": no signed DASH resource and no direct file");
        }
        if (matchesOrigin(label, origin, originSize)) {
            return null;
        }
        if (isHandedOff(label, subjectId, signed, resourceId, origin, fromStart)) {
            throw new DashServer.UnavailableException(label + ": DASH on " + Uri.parse(signed.manifestUrl).getHost()
                    + ", leaving the download to the app's direct file");
        }
        return getOrCreateFile(subjectId, season, episode, height);
    }

    public SignedResource observePlayInfo(String requestUrl, String body, String apiHost) {
        String key = SignedResource.key(Uri.parse(requestUrl));
        SignedResource resource = SignedResource.fromPlayInfo(body);
        if (resource == null) {
            Log.w(Logs.TAG, "play-info " + key + ": no signed DASH resource");
            return null;
        }
        Log.i(Logs.TAG, "play-info " + key + ": DASH on " + Uri.parse(resource.manifestUrl).getHost()
                + ((apiHost != null) ? ", app API host " + apiHost : ""));
        if (key == null) {
            return null;
        }
        synchronized (this.resources) {
            this.resources.put(key, resource);
        }
        return resource;
    }

    public String fetchReplacementPlayInfo(String pathAndQuery) {
        return this.replacementLookup.fetchPlayInfo(pathAndQuery, MovieBoxSource::isUnpacedPlayInfo);
    }

    public String directFilePlayInfo(String playInfo, String subjectId, int season, int episode) {
        return this.directFiles.playInfoFor(playInfo, subjectId, season, episode);
    }

    public String withDirectFileSizes(String resourceList) {
        return this.directFiles.withSizes(resourceList);
    }

    private static boolean isUnpacedPlayInfo(String body) {
        SignedResource resource = SignedResource.fromPlayInfo(body);
        if (resource != null && DashFile.isUnpacedCdn(resource.manifestUrl)) {
            return true;
        }
        Log.i(Logs.TAG, "alternative play-info " + ((resource != null)
                ? "is on " + Uri.parse(resource.manifestUrl).getHost() : "has no signed DASH resource"));
        return false;
    }

    private boolean matchesOrigin(String label, String origin, long originSize) {
        if (origin == null || originSize <= 0 || originLength(origin) != originSize) {
            return false;
        }
        Log.i(Logs.TAG, label + ": redirect to origin, size " + originSize + " matches");
        return true;
    }

    private void redirectToDirectFile(String label, String subjectId, String resourceId, String origin)
            throws DashServer.RedirectException {
        String directFileUrl = this.directFiles.resolveUrl(subjectId, resourceId, origin);
        if (directFileUrl != null) {
            Log.i(Logs.TAG, label + ": redirect to direct file on " + Uri.parse(directFileUrl).getHost());
            throw new DashServer.RedirectException(directFileUrl);
        }
    }

    private boolean isHandedOff(String label, String subjectId, SignedResource signed, String resourceId, String origin,
            boolean fromStart) {
        synchronized (this.handedOff) {
            Boolean decided = this.handedOff.get(label);
            if (decided != null) {
                return decided;
            }
        }
        boolean handOff = fromStart && !DashFile.isUnpacedCdn(signed.manifestUrl)
                && this.directFiles.resolveUrl(subjectId, resourceId, origin) != null;
        synchronized (this.handedOff) {
            Boolean decided = this.handedOff.get(label);
            if (decided != null) {
                return decided;
            }
            this.handedOff.put(label, handOff);
            return handOff;
        }
    }

    private long originLength(String origin) {
        synchronized (this.originLengths) {
            Long known = this.originLengths.get(origin);
            if (known != null) {
                return known;
            }
        }
        long length;
        try {
            length = RemoteFile.probeLength(origin);
        } catch (IOException | NumberFormatException ex) {
            Log.w(Logs.TAG, "origin " + origin + " not probed", ex);
            return -1;
        }
        Log.i(Logs.TAG, "origin " + origin + ": " + length + " bytes");
        synchronized (this.originLengths) {
            this.originLengths.put(origin, length);
        }
        return length;
    }

    private DashFile getOrCreateFile(String subjectId, int season, int episode, int height) throws IOException {
        String key = SignedResource.key(subjectId, season, episode) + "/" + height;
        FutureTask<DashFile> task;
        boolean owner = false;
        synchronized (this.files) {
            task = this.files.get(key);
            if (task == null) {
                task = new FutureTask<>(() -> openFile(subjectId, season, episode, height, key));
                this.files.put(key, task);
                owner = true;
            }
        }
        if (owner) {
            task.run();
        }
        return awaitFile(key, task);
    }

    private DashFile awaitFile(String key, FutureTask<DashFile> task) throws IOException {
        try {
            return task.get();
        } catch (ExecutionException ex) {
            synchronized (this.files) {
                if (this.files.get(key) == task) {
                    this.files.remove(key);
                }
            }
            throw Tasks.failureOf(ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException(ex);
        }
    }

    private DashFile openFile(String subjectId, int season, int episode, int height, String key) throws IOException {
        SignedResource signed = resource(subjectId, season, episode, null);
        long started = SystemClock.elapsedRealtime();
        DashFile file = DashFile.open(signed.manifestUrl, height,
                new PinnedCookies(signed, subjectId, season, episode));
        Log.i(Logs.TAG,
                "DASH file " + key + ": " + file.length + " bytes from " + Uri.parse(signed.manifestUrl).getHost()
                        + ", ready in " + (SystemClock.elapsedRealtime() - started) + " ms, " + file.readPattern());
        return file;
    }

    private SignedResource resource(String subjectId, int season, int episode, String rejectedCookie)
            throws IOException {
        String key = SignedResource.key(subjectId, season, episode);
        SignedResource cached;
        synchronized (this.resources) {
            cached = this.resources.get(key);
        }
        if (cached != null && !cached.cookie.equals(rejectedCookie)) {
            return cached;
        }
        String pathAndQuery = Endpoints.PLAY_INFO + "?subjectId=" + subjectId + "&se=" + season + "&ep=" + episode
                + "&isVip=true";
        String url = this.api.url(pathAndQuery);
        String body = getPlayInfoWithRetry(url, key);
        if (SignedResource.isEdgeCachePlayInfo(body)) {
            String replacement = fetchReplacementPlayInfo("/" + pathAndQuery);
            if (replacement != null) {
                body = replacement;
            }
        }
        SignedResource fetched = observePlayInfo(url, body, null);
        if (fetched == null) {
            throw new NoSignedResourceException("play-info has no signed DASH resource for " + key);
        }
        return fetched;
    }

    private String getPlayInfoWithRetry(String url, String key) throws IOException {
        for (int attempt = 1;; attempt++) {
            try {
                return this.api.get(url);
            } catch (IOException ex) {
                if (attempt == PLAY_INFO_ATTEMPTS) {
                    throw ex;
                }
                Log.w(Logs.TAG, "play-info " + key + " attempt " + attempt + " of " + PLAY_INFO_ATTEMPTS + " failed",
                        ex);
                SystemClock.sleep(PLAY_INFO_RETRY_DELAY_MS);
            }
        }
    }

    private static final class NoSignedResourceException extends IOException {

        private static final long serialVersionUID = 1L;

        NoSignedResourceException(String message) {
            super(message);
        }

    }

    private final class PinnedCookies implements CdnCookies {

        private final String manifestUrl;

        private final String subjectId;

        private final int season;

        private final int episode;

        private String cookie;

        PinnedCookies(SignedResource signed, String subjectId, int season, int episode) {
            this.manifestUrl = signed.manifestUrl;
            this.cookie = signed.cookie;
            this.subjectId = subjectId;
            this.season = season;
            this.episode = episode;
        }

        @Override
        public synchronized String current() {
            return this.cookie;
        }

        @Override
        public synchronized String refresh(String rejected) throws IOException {
            if (!this.cookie.equals(rejected)) {
                return this.cookie;
            }
            SignedResource fresh = resource(this.subjectId, this.season, this.episode, rejected);
            if (!fresh.manifestUrl.equals(this.manifestUrl)) {
                throw new IOException("signed DASH resource moved to " + Uri.parse(fresh.manifestUrl).getHost());
            }
            this.cookie = fresh.cookie;
            return this.cookie;
        }

    }

}
