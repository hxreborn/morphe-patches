/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.netanalyzer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.net.Network;

final class SsdpBrowser {

    private static final String GROUP_HOST = "239.255.255.250";

    private static final int PORT = 1900;

    private static final String ROOT_DEVICE = "upnp:rootdevice";

    private static final int SEARCH_MAX_WAIT_SECONDS = 3;

    private static final byte[] SEARCH = ("M-SEARCH * HTTP/1.1\r\nHOST: " + GROUP_HOST + ":" + PORT + "\r\n"
            + "MAN: \"ssdp:discover\"\r\nMX: " + SEARCH_MAX_WAIT_SECONDS + "\r\nST: " + ROOT_DEVICE + "\r\n\r\n")
        .getBytes(StandardCharsets.UTF_8);

    private static final int SEARCH_ROUNDS = 3;

    private static final long FIRST_SEARCH_DELAY_MS = 100;

    private static final long SEARCH_INTERVAL_MS = 750;

    private static final int SEARCH_TTL = 4;

    private static final String HEADER_USN = "USN";

    private static final String HEADER_LOCATION = "LOCATION";

    private static final String HEADER_NTS = "NTS";

    private static final String NTS_BYEBYE = "ssdp:byebye";

    private static final String USN_SEPARATOR = "::";

    private static final String HTTP_SCHEME = "http";

    private static final int DEFAULT_HTTP_PORT = 80;

    private static final String HTTP_OK = " 200";

    private static final String USER_AGENT = "Network Analyzer";

    private static final int HEX_RADIX = 16;

    private static final int COPY_BUFFER_SIZE = 4096;

    private static final int BYTE_MASK = 0xff;

    private static final int NO_LENGTH = -1;

    private static final int MAX_PACKET = 10000;

    private static final int CONNECT_TIMEOUT_MS = 2000;

    private static final int READ_TIMEOUT_MS = 3000;

    private static final int MAX_BODY = 400000;

    private static final int MAX_LINE = 8192;

    private static final int MAX_HEADERS = 100;

    private static final int FETCH_THREADS = 4;

    private static final int MAX_PENDING_FETCHES = 64;

    private static final int MAX_DEVICES = 256;

    private static final InetAddress GROUP = group();

    final InetAddress local;

    final Network network;

    private final NetworkInterface networkInterface;

    private final Listener listener;

    private final Object lock = new Object();

    private final Map<String, UpnpDevice> devices = new LinkedHashMap<>();

    private final Set<String> fetching = new HashSet<>();

    private MulticastSocket searchSocket;

    private MulticastSocket notifySocket;

    private ScheduledExecutorService scheduler;

    private ExecutorService fetchers;

    private int generation;

    SsdpBrowser(InetAddress local, Network network, NetworkInterface networkInterface, Listener listener) {
        this.local = local;
        this.network = network;
        this.networkInterface = networkInterface;
        this.listener = listener;
    }

    List<UpnpDevice> devices() {
        final List<UpnpDevice> snapshot;
        synchronized (this.lock) {
            snapshot = new ArrayList<>(this.devices.values());
        }
        Collections.sort(snapshot, new DeviceOrder());
        return snapshot;
    }

    void start() throws IOException {
        synchronized (this.lock) {
            if (this.searchSocket != null) {
                return;
            }
            final MulticastSocket search = new MulticastSocket(new InetSocketAddress(this.local, 0));
            search.setTimeToLive(SEARCH_TTL);
            if (this.networkInterface != null) {
                search.setNetworkInterface(this.networkInterface);
            }
            this.searchSocket = search;
            this.notifySocket = openNotifySocket();
            this.generation++;
            this.fetching.clear();
            this.scheduler = Executors.newSingleThreadScheduledExecutor(SsdpBrowser::daemon);
            this.fetchers = new ThreadPoolExecutor(FETCH_THREADS, FETCH_THREADS, 0L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<>(MAX_PENDING_FETCHES), SsdpBrowser::daemon);
            final int current = this.generation;
            daemon(() -> readLoop(search, current)).start();
            if (this.notifySocket != null) {
                final MulticastSocket notify = this.notifySocket;
                daemon(() -> readLoop(notify, current)).start();
            }
            for (int round = 0; round < SEARCH_ROUNDS; round++) {
                this.scheduler.schedule(() -> send(search), FIRST_SEARCH_DELAY_MS + round * SEARCH_INTERVAL_MS,
                        TimeUnit.MILLISECONDS);
            }
        }
    }

    void stop() {
        synchronized (this.lock) {
            if (this.searchSocket == null) {
                return;
            }
            this.generation++;
            this.searchSocket.close();
            if (this.notifySocket != null) {
                this.notifySocket.close();
            }
            this.scheduler.shutdownNow();
            this.fetchers.shutdownNow();
            this.searchSocket = null;
            this.notifySocket = null;
            this.scheduler = null;
            this.fetchers = null;
        }
    }

    private MulticastSocket openNotifySocket() {
        MulticastSocket opened = null;
        try {
            opened = new MulticastSocket(null);
            opened.setReuseAddress(true);
            opened.bind(new InetSocketAddress(PORT));
            opened.joinGroup(new InetSocketAddress(GROUP, PORT), this.networkInterface);
            return opened;
        } catch (IOException ex) {
            if (opened != null) {
                opened.close();
            }
            return null;
        }
    }

    private static void send(MulticastSocket out) {
        try {
            out.send(new DatagramPacket(SEARCH, SEARCH.length, GROUP, PORT));
        } catch (IOException ex) {
            return;
        }
    }

    private void readLoop(MulticastSocket source, int current) {
        final byte[] buffer = new byte[MAX_PACKET];
        final DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        while (true) {
            try {
                packet.setLength(buffer.length);
                source.receive(packet);
            } catch (IOException ex) {
                return;
            }
            updateDevices(parseHeaders(new String(buffer, 0, packet.getLength(), StandardCharsets.UTF_8)), current);
        }
    }

    static Map<String, String> parseHeaders(String text) {
        final Map<String, String> headers = new HashMap<>();
        for (String raw : text.split("\n")) {
            final String line = raw.trim();
            if (line.isEmpty()) {
                break;
            }
            final int colon = line.indexOf(':');
            if (colon > 0) {
                headers.put(line.substring(0, colon).trim().toUpperCase(Locale.ROOT), line.substring(colon + 1).trim());
            }
        }
        return headers;
    }

    private void updateDevices(Map<String, String> headers, int current) {
        final String usn = headers.get(HEADER_USN);
        if (usn == null) {
            return;
        }
        final int separator = usn.indexOf(USN_SEPARATOR);
        final String uuid = (separator >= 0) ? usn.substring(0, separator) : usn;
        final String usnType = (separator >= 0 && separator + USN_SEPARATOR.length() < usn.length())
                ? usn.substring(separator + USN_SEPARATOR.length()) : null;
        final String location = headers.get(HEADER_LOCATION);
        boolean changed = false;
        synchronized (this.lock) {
            if (current != this.generation) {
                return;
            }
            if (location != null) {
                if (!this.devices.containsKey(uuid) && this.devices.size() + this.fetching.size() < MAX_DEVICES
                        && this.fetching.add(uuid)) {
                    try {
                        this.fetchers.execute(() -> fetch(uuid, location, current));
                    } catch (RejectedExecutionException ex) {
                        this.fetching.remove(uuid);
                    }
                }
            } else if (NTS_BYEBYE.equals(headers.get(HEADER_NTS))) {
                final UpnpDevice known = this.devices.get(uuid);
                if (known != null
                        && (usnType == null || ROOT_DEVICE.equals(usnType) || usnType.equals(known.deviceType))) {
                    this.devices.remove(uuid);
                    changed = true;
                }
            }
        }
        if (changed) {
            this.listener.onChanged();
        }
    }

    private void fetch(String uuid, String location, int current) {
        UpnpDevice device = null;
        try {
            device = describe(location);
        } catch (IOException | URISyntaxException ex) {
            device = null;
        }
        synchronized (this.lock) {
            this.fetching.remove(uuid);
            if (device == null || current != this.generation) {
                return;
            }
            this.devices.put(uuid, device);
        }
        this.listener.onChanged();
    }

    private UpnpDevice describe(String location) throws IOException, URISyntaxException {
        final URI uri = new URI(location);
        if (!HTTP_SCHEME.equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
            return null;
        }
        final int port = (uri.getPort() >= 0) ? uri.getPort() : DEFAULT_HTTP_PORT;
        final String path = (uri.getRawPath() == null || uri.getRawPath().isEmpty()) ? "/" : uri.getRawPath();
        final String file = (uri.getRawQuery() != null) ? path + "?" + uri.getRawQuery() : path;
        try (Socket socket = new Socket()) {
            this.network.bindSocket(socket);
            socket.connect(new InetSocketAddress(this.network.getByName(uri.getHost()), port), CONNECT_TIMEOUT_MS);
            socket.setSoTimeout(READ_TIMEOUT_MS);
            final OutputStream out = socket.getOutputStream();
            out.write(("GET " + file + " HTTP/1.1\r\nHost: " + uri.getHost() + "\r\nAccept: */*\r\n" + "User-Agent: "
                    + USER_AGENT + "\r\nAccept-Language: en-us\r\nConnection: close\r\n\r\n")
                .getBytes(StandardCharsets.UTF_8));
            out.flush();
            final String body = readResponse(socket.getInputStream());
            return (body != null) ? UpnpDevice.parse(body, socket.getInetAddress()) : null;
        }
    }

    private static String readResponse(InputStream in) throws IOException {
        final String status = readLine(in);
        if (status == null || !status.contains(HTTP_OK)) {
            return null;
        }
        int length = NO_LENGTH;
        boolean chunked = false;
        String line;
        int headers = 0;
        while ((line = readLine(in)) != null && !line.isEmpty()) {
            if (++headers > MAX_HEADERS) {
                throw new IOException("More than " + MAX_HEADERS + " response headers");
            }
            final int colon = line.indexOf(':');
            if (colon <= 0) {
                continue;
            }
            final String key = line.substring(0, colon).trim().toLowerCase(Locale.ROOT);
            final String value = line.substring(colon + 1).trim();
            if ("content-length".equals(key)) {
                try {
                    length = Integer.parseInt(value);
                } catch (NumberFormatException ex) {
                    return null;
                }
            } else if ("transfer-encoding".equals(key) && value.toLowerCase(Locale.ROOT).contains("chunked")) {
                chunked = true;
            }
        }
        final ByteArrayOutputStream body = new ByteArrayOutputStream();
        if (chunked) {
            readChunks(in, body);
        } else if (length == 0 || length > MAX_BODY) {
            return null;
        } else {
            copy(in, body, (length != NO_LENGTH) ? length : MAX_BODY);
        }
        if (body.size() == 0 || body.size() >= MAX_BODY) {
            return null;
        }
        return new String(body.toByteArray(), StandardCharsets.UTF_8);
    }

    private static void readChunks(InputStream in, ByteArrayOutputStream body) throws IOException {
        while (body.size() < MAX_BODY) {
            final String sizeLine = readLine(in);
            if (sizeLine == null) {
                return;
            }
            final int semicolon = sizeLine.indexOf(';');
            final int size;
            try {
                size = Integer.parseInt(((semicolon >= 0) ? sizeLine.substring(0, semicolon) : sizeLine).trim(),
                        HEX_RADIX);
            } catch (NumberFormatException ex) {
                return;
            }
            if (size == 0) {
                return;
            }
            copy(in, body, Math.min(size, MAX_BODY - body.size()));
            readLine(in);
        }
    }

    private static void copy(InputStream in, ByteArrayOutputStream out, int count) throws IOException {
        final byte[] buffer = new byte[COPY_BUFFER_SIZE];
        int remaining = count;
        while (remaining > 0) {
            final int read = in.read(buffer, 0, Math.min(buffer.length, remaining));
            if (read < 0) {
                return;
            }
            out.write(buffer, 0, read);
            remaining -= read;
        }
    }

    private static String readLine(InputStream in) throws IOException {
        final StringBuilder line = new StringBuilder();
        int value;
        while ((value = in.read()) >= 0) {
            if (value == '\n') {
                return line.toString().trim();
            }
            if (line.length() >= MAX_LINE) {
                throw new IOException("Response line longer than " + MAX_LINE + " bytes");
            }
            line.append((char) value);
        }
        return (line.length() == 0) ? null : line.toString().trim();
    }

    private static Thread daemon(Runnable runnable) {
        final Thread thread = new Thread(runnable, "ssdp");
        thread.setDaemon(true);
        return thread;
    }

    private static InetAddress group() {
        try {
            return InetAddress.getByName(GROUP_HOST);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static final class DeviceOrder implements Comparator<UpnpDevice> {

        @Override
        public int compare(UpnpDevice a, UpnpDevice b) {
            final int byAddress = Long.compare(key(a), key(b));
            return (byAddress != 0) ? byAddress : a.friendlyName.compareTo(b.friendlyName);
        }

        private static long key(UpnpDevice device) {
            if (!(device.address instanceof Inet4Address)) {
                return Long.MAX_VALUE;
            }
            long value = 0;
            for (byte part : device.address.getAddress()) {
                value = (value << Byte.SIZE) | (part & BYTE_MASK);
            }
            return value;
        }

    }

    interface Listener {

        void onChanged();

    }

}
