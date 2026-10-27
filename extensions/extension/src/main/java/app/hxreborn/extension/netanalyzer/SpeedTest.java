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
package app.hxreborn.extension.netanalyzer;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import android.annotation.TargetApi;
import android.os.Build;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

final class SpeedTest {

    static final long PHASE_MS = 15000;

    private static final String SERVERS_URL = "https://www.speedtest.net/api/js/servers?engine=js&limit=20";

    private static final String USER_AGENT = "Mozilla/5.0 (Linux; Android 10; K) "
            + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Mobile Safari/537.36";

    private static final String HELLO_COMMAND = "HI\n";

    private static final String HELLO_REPLY = "HELLO";

    private static final long DOWNLOAD_REQUEST_BYTES = 1000000000L;

    private static final String DOWNLOAD_REQUEST = "DOWNLOAD " + DOWNLOAD_REQUEST_BYTES + "\n";

    private static final String UPLOAD_COMMAND_PREFIX = "UPLOAD ";

    private static final String UPLOAD_COMMAND_SUFFIX = " 0\n";

    private static final String UPLOAD_ACK = "OK";

    private static final String HTTPS_IDENTIFICATION = "HTTPS";

    private static final String JSON_HTTPS_FUNCTIONAL = "https_functional";

    private static final String JSON_HOST = "host";

    private static final int SERVER_LIMIT = 16;

    private static final int PARALLEL_SERVERS = 6;

    private static final int DEFAULT_PORT = 8080;

    private static final int LIST_CONNECT_TIMEOUT_MS = 6000;

    private static final int LIST_READ_TIMEOUT_MS = 10000;

    private static final int CONNECT_TIMEOUT_MS = 3000;

    private static final int READ_TIMEOUT_MS = 3000;

    private static final int UPLOAD_READ_TIMEOUT_MS = 15000;

    private static final int LINE_LIMIT = 4096;

    private static final int LIST_BUFFER_BYTES = 8192;

    private static final long TICK_MS = 250;

    private static final long SAMPLE_WINDOW_MS = 250;

    private static final long STALL_MS = 2500;

    private static final long RETRY_DELAY_MS = 200;

    private static final long SHUTDOWN_WAIT_MS = 2000;

    private static final long MILLIS_PER_SECOND = 1000;

    private static final byte[] DOWNLOAD_COMMAND = DOWNLOAD_REQUEST.getBytes(StandardCharsets.US_ASCII);

    private static final int DOWNLOAD_BUFFER = 512 * 1024;

    private static final int DOWNLOAD_EMIT_BYTES = 16384;

    private static final long FAILED_ATTEMPT_BYTES = 100000;

    private static final int WARMUP_WINDOWS = 2;

    private static final int WARMUP_MIN_SAMPLES = 8;

    private static final double WARMUP_FRACTION = 0.2;

    private static final double DOWNLOAD_TRIM_LOW = 0.25;

    private static final double DOWNLOAD_TRIM_HIGH = 0.75;

    private static final double UPLOAD_TRIM_LOW = 0.15;

    private static final double UPLOAD_TRIM_HIGH = 0.85;

    private static final double[] LIVE_WEIGHTS = { 1, 0.9, 0.7, 0.4, 0.2, 0.1, 0.05 };

    private static final long UPLOAD_ONE_CONNECTION_BELOW = 250000;

    private static final long UPLOAD_TWO_CONNECTIONS_BELOW = 1250000;

    private static final long UPLOAD_FOUR_CONNECTIONS_BELOW = 6000000;

    private static final int UPLOAD_TWO_CONNECTIONS = 2;

    private static final int UPLOAD_FOUR_CONNECTIONS = 4;

    private static final int UPLOAD_PAYLOAD_BYTES = 8 * 1024 * 1024;

    private static final int UPLOAD_PAYLOAD_RESERVE_BYTES = 1024 * 1024;

    private static final int ACK_FIELDS = 2;

    private static final int UPLOAD_CONCURRENT_REQUESTS = 2;

    private static final int UPLOAD_MIN_REQUEST_BYTES = 4096;

    private static final int UPLOAD_FIRST_REQUEST_MAX_BYTES = 131072;

    private static final int UPLOAD_REQUEST_MAX_BYTES = 16 * 1024 * 1024;

    private static final double UPLOAD_REQUEST_SECONDS = 0.5;

    private static final int UPLOAD_FIRST_CHUNK_BYTES = 4096;

    private static final int UPLOAD_CHUNK_MAX_BYTES = 1024 * 1024;

    private static final double UPLOAD_CHUNK_SECONDS = 0.05;

    private static final long UPLOAD_ESTIMATE_MIN_BYTES_PER_SECOND = 16000;

    private static final long UPLOAD_ESTIMATE_MAX_BYTES_PER_SECOND = 4000000;

    private static final double UPLOAD_ESTIMATE_DIVISOR = 4.0;

    private static final int SEND_BUFFER_MIN_BYTES = 65536;

    private static final int SEND_BUFFER_MAX_BYTES = 524288;

    private static final long SEND_BUFFER_DIVISOR = 2;

    private static final long MIN_ACK_BYTES_PER_SECOND = 1000;

    private static final double MIN_ACK_SECONDS = 0.001;

    private final SpeedTestListener listener;

    private final long phaseMillis;

    private final Random random = new Random();

    private final Set<Socket> sockets = Collections.newSetFromMap(new ConcurrentHashMap<Socket, Boolean>());

    private final List<Parked> parked = new ArrayList<>();

    private volatile boolean cancelled;

    private volatile Thread coordinator;

    private volatile ExecutorService pool;

    private volatile HttpURLConnection listRequest;

    SpeedTest(SpeedTestListener listener) {
        this(listener, PHASE_MS);
    }

    SpeedTest(SpeedTestListener listener, long phaseMillis) {
        this.listener = listener;
        this.phaseMillis = phaseMillis;
    }

    void start() {
        final Thread thread = new Thread(this::measure, "speedtest");
        this.coordinator = thread;
        thread.start();
    }

    void cancel() {
        this.cancelled = true;
        final Thread thread = this.coordinator;
        if (thread != null) {
            thread.interrupt();
        }
        final HttpURLConnection request = this.listRequest;
        if (request != null) {
            request.disconnect();
        }
        final ExecutorService current = this.pool;
        if (current != null) {
            current.shutdownNow();
        }
        closeAll();
    }

    static long trimmedMean(long[] values, double low, double high) {
        final int count = values.length;
        final long[] kept;
        if (count >= WARMUP_MIN_SAMPLES) {
            final List<Long> filtered = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                if (i > WARMUP_FRACTION * count) {
                    filtered.add(values[i]);
                }
            }
            kept = new long[filtered.size()];
            for (int i = 0; i < kept.length; i++) {
                kept[i] = filtered.get(i);
            }
        } else {
            kept = values.clone();
        }
        if (kept.length == 0) {
            return 0;
        }
        Arrays.sort(kept);
        final int size = kept.length;
        double sum = 0;
        int used = 0;
        for (int k = 0; k < size; k++) {
            if (k > low * size && k < high * size) {
                sum += kept[size - 1 - k];
                used++;
            }
        }
        if (used == 0) {
            for (long value : kept) {
                sum += value;
            }
            used = size;
        }
        return Math.round(sum / used);
    }

    private static long now() {
        return System.nanoTime() / 1000000L;
    }

    private static void closeQuietly(Socket socket) {
        if (socket == null) {
            return;
        }
        try {
            socket.setSoLinger(true, 0);
        } catch (IOException ex) {
        }
        try {
            socket.close();
        } catch (IOException ex) {
        }
    }

    private static String readLine(InputStream in) throws IOException {
        final StringBuilder line = new StringBuilder();
        while (line.length() < LINE_LIMIT) {
            final int next = in.read();
            if (next < 0) {
                throw new EOFException();
            }
            if (next == '\n') {
                return line.toString();
            }
            line.append((char) next);
        }
        throw new IOException("Server reply line exceeds " + LINE_LIMIT + " characters");
    }

    private void measure() {
        try {
            this.listener.onPhase(Phase.PREPARING);
            final List<Server> servers;
            try {
                servers = fetchServers();
            } catch (IOException | JSONException ex) {
                if (!this.cancelled) {
                    this.listener.onFailed("Could not load the server list.");
                }
                return;
            }
            if (this.cancelled) {
                return;
            }
            if (servers.isEmpty()) {
                this.listener.onFailed("No test servers available.");
                return;
            }
            final long download = runDownload(servers);
            if (this.cancelled) {
                return;
            }
            if (download <= 0) {
                this.listener.onFailed("Could not reach the test servers.");
                return;
            }
            this.listener.onPhaseResult(Phase.DOWNLOAD, download);
            final long upload = runUpload(servers, download);
            if (this.cancelled) {
                return;
            }
            this.listener.onPhaseResult(Phase.UPLOAD, upload);
            this.listener.onFinished(download, upload);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } finally {
            shutdown();
        }
    }

    private void shutdown() {
        final ExecutorService current = this.pool;
        if (current != null) {
            current.shutdownNow();
        }
        closeAll();
        if (current != null) {
            try {
                current.awaitTermination(SHUTDOWN_WAIT_MS, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private List<Server> fetchServers() throws IOException, JSONException {
        final HttpURLConnection connection = (HttpURLConnection) new URL(SERVERS_URL).openConnection();
        this.listRequest = connection;
        try {
            connection.setConnectTimeout(LIST_CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(LIST_READ_TIMEOUT_MS);
            connection.setRequestProperty("User-Agent", USER_AGENT);
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP " + connection.getResponseCode());
            }
            final ByteArrayOutputStream body = new ByteArrayOutputStream();
            try (InputStream in = connection.getInputStream()) {
                final byte[] buffer = new byte[LIST_BUFFER_BYTES];
                int read;
                while ((read = in.read(buffer)) >= 0) {
                    body.write(buffer, 0, read);
                }
            }
            final JSONArray array = new JSONArray(new String(body.toByteArray(), StandardCharsets.UTF_8));
            final List<Server> servers = new ArrayList<>();
            for (int i = 0; i < array.length() && servers.size() < SERVER_LIMIT; i++) {
                final JSONObject item = array.optJSONObject(i);
                if (item == null || item.optInt(JSON_HTTPS_FUNCTIONAL, 0) != 1) {
                    continue;
                }
                final String host = item.optString(JSON_HOST, "");
                if (host.isEmpty()) {
                    continue;
                }
                final int colon = host.indexOf(':');
                int port = DEFAULT_PORT;
                if (colon >= 0) {
                    try {
                        port = Integer.parseInt(host.substring(colon + 1));
                    } catch (NumberFormatException ex) {
                        port = 0;
                    }
                    if (port <= 0) {
                        port = DEFAULT_PORT;
                    }
                }
                servers.add(new Server((colon >= 0) ? host.substring(0, colon) : host, port));
            }
            return servers;
        } finally {
            connection.disconnect();
            this.listRequest = null;
        }
    }

    private long runDownload(List<Server> servers) throws InterruptedException {
        final Sampler sampler = new Sampler(Phase.DOWNLOAD, DOWNLOAD_TRIM_LOW, DOWNLOAD_TRIM_HIGH);
        final ExecutorService phasePool = Executors.newCachedThreadPool();
        this.pool = phasePool;
        this.listener.onPhase(Phase.DOWNLOAD);
        sampler.begin();
        final int count = Math.min(PARALLEL_SERVERS, servers.size());
        for (int i = 0; i < count; i++) {
            final Server server = servers.get(i);
            phasePool.execute(() -> downloadLoop(server, servers, sampler));
            phasePool.execute(() -> preConnect(server, sampler));
        }
        try {
            waitPhase(sampler, null);
        } finally {
            endPhase(sampler, phasePool);
        }
        return sampler.result();
    }

    private long runUpload(List<Server> servers, long downloadBytesPerSecond) throws InterruptedException {
        final int wanted;
        if (downloadBytesPerSecond < UPLOAD_ONE_CONNECTION_BELOW) {
            wanted = 1;
        } else if (downloadBytesPerSecond < UPLOAD_TWO_CONNECTIONS_BELOW) {
            wanted = UPLOAD_TWO_CONNECTIONS;
        } else if (downloadBytesPerSecond < UPLOAD_FOUR_CONNECTIONS_BELOW) {
            wanted = UPLOAD_FOUR_CONNECTIONS;
        } else {
            wanted = PARALLEL_SERVERS;
        }
        final int count = Math.min(wanted, Math.min(PARALLEL_SERVERS, servers.size()));
        final byte[] payload = new byte[UPLOAD_PAYLOAD_BYTES];
        this.random.nextBytes(payload);
        final Sampler sampler = new Sampler(Phase.UPLOAD, UPLOAD_TRIM_LOW, UPLOAD_TRIM_HIGH);
        final List<UploadConnection> connections = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            final Server server = servers.get(i);
            connections.add(new UploadConnection(server, servers, downloadBytesPerSecond, sampler, payload,
                    takeParked(server)));
        }
        closeParked();
        final ExecutorService phasePool = Executors.newCachedThreadPool();
        this.pool = phasePool;
        this.listener.onPhase(Phase.UPLOAD);
        sampler.begin();
        for (UploadConnection connection : connections) {
            phasePool.execute(connection);
        }
        try {
            waitPhase(sampler, connections);
        } finally {
            endPhase(sampler, phasePool);
        }
        return sampler.result();
    }

    private void waitPhase(Sampler sampler, List<UploadConnection> connections) throws InterruptedException {
        final long start = sampler.start;
        final long end = start + this.phaseMillis;
        long current = now();
        while (!this.cancelled && current < end) {
            this.listener.onProgress(sampler.phase, Math.min(1f, (float) (current - start) / this.phaseMillis));
            if (connections != null) {
                for (UploadConnection connection : connections) {
                    connection.checkStall();
                }
            }
            Thread.sleep(Math.min(TICK_MS, end - current));
            current = now();
        }
        if (!this.cancelled) {
            this.listener.onProgress(sampler.phase, 1f);
        }
    }

    private void endPhase(Sampler sampler, ExecutorService phasePool) throws InterruptedException {
        sampler.close();
        phasePool.shutdownNow();
        closeActive();
        phasePool.awaitTermination(SHUTDOWN_WAIT_MS, TimeUnit.MILLISECONDS);
    }

    private void downloadLoop(Server first, List<Server> servers, Sampler sampler) {
        final byte[] buffer = new byte[DOWNLOAD_BUFFER];
        Server server = first;
        while (sampler.isOpen() && !this.cancelled) {
            long total = 0;
            Socket socket = null;
            try {
                socket = connect(server, sampler);
                final OutputStream out = socket.getOutputStream();
                out.write(DOWNLOAD_COMMAND);
                out.flush();
                final InputStream in = socket.getInputStream();
                long pending = 0;
                while (total < DOWNLOAD_REQUEST_BYTES) {
                    final int read = in.read(buffer);
                    if (read < 0) {
                        break;
                    }
                    total += read;
                    pending += read;
                    if (pending > DOWNLOAD_EMIT_BYTES) {
                        sampler.add(pending);
                        pending = 0;
                    }
                }
                if (pending > 0) {
                    sampler.add(pending);
                }
            } catch (IOException ex) {
                total = Math.min(total, FAILED_ATTEMPT_BYTES - 1);
            } finally {
                release(socket);
            }
            if (total < FAILED_ATTEMPT_BYTES && sampler.isOpen()) {
                server = servers.get(this.random.nextInt(servers.size()));
                pause(RETRY_DELAY_MS);
            }
        }
    }

    private void preConnect(Server server, Sampler sampler) {
        Socket socket = null;
        boolean kept = false;
        try {
            socket = connect(server, sampler);
            synchronized (this.parked) {
                this.sockets.remove(socket);
                this.parked.add(new Parked(server, socket));
                kept = true;
            }
        } catch (IOException ex) {
        } finally {
            if (!kept) {
                release(socket);
            }
        }
    }

    private Socket takeParked(Server server) {
        synchronized (this.parked) {
            for (int i = 0; i < this.parked.size(); i++) {
                final Parked candidate = this.parked.get(i);
                if (candidate.server.key().equals(server.key())) {
                    this.parked.remove(i);
                    this.sockets.add(candidate.socket);
                    return candidate.socket;
                }
            }
        }
        return null;
    }

    private void closeParked() {
        synchronized (this.parked) {
            for (Parked entry : this.parked) {
                closeQuietly(entry.socket);
            }
            this.parked.clear();
        }
    }

    private void closeActive() {
        for (Socket socket : this.sockets) {
            closeQuietly(socket);
        }
        this.sockets.clear();
    }

    private void closeAll() {
        closeActive();
        closeParked();
    }

    private void release(Socket socket) {
        if (socket != null) {
            this.sockets.remove(socket);
            closeQuietly(socket);
        }
    }

    private void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private Socket connect(Server server, Sampler sampler) throws IOException {
        final InetAddress[] addresses = InetAddress.getAllByName(server.host);
        final List<InetAddress> candidates = new ArrayList<>();
        for (InetAddress address : addresses) {
            if (address instanceof Inet4Address) {
                candidates.add(address);
                break;
            }
        }
        for (InetAddress address : addresses) {
            if (!(address instanceof Inet4Address)) {
                candidates.add(address);
                break;
            }
        }
        IOException failure = new IOException("No address for " + server.host);
        for (InetAddress address : candidates) {
            final Socket plain = new Socket();
            this.sockets.add(plain);
            try {
                checkOpen(sampler);
                plain.connect(new InetSocketAddress(address, server.port), CONNECT_TIMEOUT_MS);
                plain.setTcpNoDelay(true);
                final SSLSocket secure = (SSLSocket) ((SSLSocketFactory) SSLSocketFactory.getDefault())
                    .createSocket(plain, server.host, server.port, true);
                this.sockets.remove(plain);
                this.sockets.add(secure);
                try {
                    checkOpen(sampler);
                    secure.setSoTimeout(READ_TIMEOUT_MS);
                    requireHostnameCheck(secure);
                    secure.startHandshake();
                    final OutputStream out = secure.getOutputStream();
                    out.write(HELLO_COMMAND.getBytes(StandardCharsets.US_ASCII));
                    out.flush();
                    if (!readLine(secure.getInputStream()).startsWith(HELLO_REPLY)) {
                        throw new IOException("Server " + server.host + " did not answer " + HELLO_COMMAND.trim());
                    }
                    return secure;
                } catch (IOException ex) {
                    release(secure);
                    throw ex;
                }
            } catch (IOException ex) {
                this.sockets.remove(plain);
                closeQuietly(plain);
                failure = ex;
            }
        }
        throw failure;
    }

    @TargetApi(Build.VERSION_CODES.N)
    private static void requireHostnameCheck(SSLSocket socket) {
        final SSLParameters parameters = socket.getSSLParameters();
        parameters.setEndpointIdentificationAlgorithm(HTTPS_IDENTIFICATION);
        socket.setSSLParameters(parameters);
    }

    private void checkOpen(Sampler sampler) throws IOException {
        if (this.cancelled) {
            throw new IOException("Speed test cancelled");
        }
        if (!sampler.isOpen()) {
            throw new IOException("Speed test phase ended");
        }
    }

    enum Phase {

        PREPARING, DOWNLOAD, UPLOAD

    }

    static final class Server {

        final String host;

        final int port;

        Server(String host, int port) {
            this.host = host;
            this.port = port;
        }

        String key() {
            return this.host + ":" + this.port;
        }

    }

    private static final class Parked {

        final Server server;

        final Socket socket;

        Parked(Server server, Socket socket) {
            this.server = server;
            this.socket = socket;
        }

    }

    private final class Sampler {

        final Phase phase;

        final double low;

        final double high;

        private final List<Long> samples = new ArrayList<>();

        private volatile boolean open = true;

        private long start;

        private long windowStart;

        private long bytes;

        private int windows;

        Sampler(Phase phase, double low, double high) {
            this.phase = phase;
            this.low = low;
            this.high = high;
        }

        synchronized void begin() {
            this.start = now();
            this.windowStart = this.start;
            this.bytes = 0;
            this.windows = 0;
        }

        boolean isOpen() {
            return this.open;
        }

        void close() {
            this.open = false;
        }

        synchronized void add(long count) {
            if (!this.open) {
                return;
            }
            this.bytes += count;
            final long current = now();
            final long elapsed = current - this.windowStart;
            if (elapsed <= SAMPLE_WINDOW_MS) {
                return;
            }
            final long perSecond = this.bytes * MILLIS_PER_SECOND / elapsed;
            this.bytes = 0;
            this.windowStart = current;
            this.windows++;
            if (this.windows > WARMUP_WINDOWS) {
                this.samples.add(perSecond);
                SpeedTest.this.listener.onSample(this.phase, perSecond, live(), current - this.start);
            }
        }

        synchronized long result() {
            final long[] values = new long[this.samples.size()];
            for (int i = 0; i < values.length; i++) {
                values[i] = this.samples.get(i);
            }
            return trimmedMean(values, this.low, this.high);
        }

        private long live() {
            double sum = 0;
            double weights = 0;
            final int size = this.samples.size();
            for (int i = 0; i < LIVE_WEIGHTS.length && i < size; i++) {
                sum += LIVE_WEIGHTS[i] * this.samples.get(size - 1 - i);
                weights += LIVE_WEIGHTS[i];
            }
            return Math.round(sum / weights);
        }

    }

    private final class UploadConnection implements Runnable {

        private final Server first;

        private final List<Server> servers;

        private final long downloadBytesPerSecond;

        private final Sampler sampler;

        private final byte[] payload;

        private volatile Socket initial;

        private volatile Socket current;

        private volatile long lastProgress = now();

        private volatile double speed;

        private int offset;

        UploadConnection(Server first, List<Server> servers, long downloadBytesPerSecond, Sampler sampler,
                byte[] payload, Socket initial) {
            this.first = first;
            this.servers = servers;
            this.downloadBytesPerSecond = downloadBytesPerSecond;
            this.sampler = sampler;
            this.payload = payload;
            this.initial = initial;
        }

        @Override
        public void run() {
            Server target = this.first;
            while (this.sampler.isOpen() && !SpeedTest.this.cancelled) {
                Socket socket = this.initial;
                this.initial = null;
                long moved = 0;
                try {
                    if (socket == null) {
                        socket = connect(target, this.sampler);
                    }
                    moved = transfer(socket);
                } catch (IOException ex) {
                } catch (InterruptedException ex) {
                    release(socket);
                    return;
                } finally {
                    release(socket);
                    this.current = null;
                }
                if (this.sampler.isOpen()) {
                    if (moved == 0) {
                        target = this.servers.get(SpeedTest.this.random.nextInt(this.servers.size()));
                    }
                    pause(RETRY_DELAY_MS);
                }
            }
        }

        void checkStall() {
            final Socket socket = this.current;
            if (socket != null && now() - this.lastProgress > STALL_MS) {
                closeQuietly(socket);
            }
        }

        private long transfer(Socket socket) throws IOException, InterruptedException {
            socket.setSoTimeout(UPLOAD_READ_TIMEOUT_MS);
            socket.setSendBufferSize((int) Math.min(SEND_BUFFER_MAX_BYTES,
                    Math.max(SEND_BUFFER_MIN_BYTES, this.downloadBytesPerSecond / SEND_BUFFER_DIVISOR)));
            this.current = socket;
            this.lastProgress = now();
            final Session session = new Session(socket);
            final ExecutorService phasePool = SpeedTest.this.pool;
            phasePool.execute(() -> readAcks(session));
            final OutputStream out = socket.getOutputStream();
            final double minimum = Math.min(UPLOAD_ESTIMATE_MAX_BYTES_PER_SECOND, Math
                .max(UPLOAD_ESTIMATE_MIN_BYTES_PER_SECOND, this.downloadBytesPerSecond / UPLOAD_ESTIMATE_DIVISOR));
            try {
                while (this.sampler.isOpen() && !session.failed) {
                    session.permits.acquire();
                    if (session.failed) {
                        break;
                    }
                    final double known = this.speed;
                    final double estimate = (known == 0) ? minimum : known;
                    final long maxSize = (known == 0) ? UPLOAD_FIRST_REQUEST_MAX_BYTES : UPLOAD_REQUEST_MAX_BYTES;
                    final int size = (int) Math.min(maxSize,
                            Math.max(UPLOAD_MIN_REQUEST_BYTES, estimate * UPLOAD_REQUEST_SECONDS));
                    final byte[] command = (UPLOAD_COMMAND_PREFIX + size + UPLOAD_COMMAND_SUFFIX)
                        .getBytes(StandardCharsets.US_ASCII);
                    final int body = size - command.length - 1;
                    final int chunk = (known == 0) ? UPLOAD_FIRST_CHUNK_BYTES : (int) Math.min(UPLOAD_CHUNK_MAX_BYTES,
                            Math.max(UPLOAD_MIN_REQUEST_BYTES, UPLOAD_CHUNK_SECONDS * known));
                    session.pending.add(new int[] { size, body });
                    out.write(command);
                    if (this.offset + UPLOAD_PAYLOAD_RESERVE_BYTES >= this.payload.length) {
                        this.offset = 0;
                    }
                    int written = 0;
                    while (written < body) {
                        final int length = Math.min(Math.min(chunk, body - written), this.payload.length - this.offset);
                        out.write(this.payload, this.offset, length);
                        this.offset = (this.offset + length) % this.payload.length;
                        written += length;
                        this.lastProgress = now();
                    }
                    out.write('\n');
                    out.flush();
                    this.lastProgress = now();
                }
            } finally {
                session.failed = true;
                closeQuietly(socket);
            }
            return session.credited;
        }

        private void readAcks(Session session) {
            long previous = now();
            try {
                final InputStream in = session.socket.getInputStream();
                while (true) {
                    final String[] parts = readLine(in).split(" ");
                    final int[] head = session.pending.peek();
                    if (head == null || parts.length < ACK_FIELDS || !UPLOAD_ACK.equals(parts[0])
                            || !String.valueOf(head[0]).equals(parts[1])) {
                        break;
                    }
                    session.pending.poll();
                    final long arrived = now();
                    this.speed = Math.max(MIN_ACK_BYTES_PER_SECOND,
                            head[0] / Math.max(MIN_ACK_SECONDS, (double) (arrived - previous) / MILLIS_PER_SECOND));
                    previous = arrived;
                    this.lastProgress = arrived;
                    session.credited += head[1];
                    this.sampler.add(head[1]);
                    session.permits.release();
                }
            } catch (IOException ex) {
            } finally {
                session.failed = true;
                session.permits.release(UPLOAD_CONCURRENT_REQUESTS);
                closeQuietly(session.socket);
            }
        }

        private final class Session {

            final Socket socket;

            final Semaphore permits = new Semaphore(UPLOAD_CONCURRENT_REQUESTS);

            final BlockingQueue<int[]> pending = new LinkedBlockingQueue<>();

            volatile boolean failed;

            volatile long credited;

            Session(Socket socket) {
                this.socket = socket;
            }

        }

    }

}
