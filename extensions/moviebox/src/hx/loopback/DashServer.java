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
package hx.loopback;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.net.Uri;
import android.util.Log;
import hx.dash.DashFile;
import hx.util.Logs;

public final class DashServer implements Runnable {

    static final int PORT = 47291;

    private static final byte[] LOOPBACK = { 127, 0, 0, 1 };

    private static final int BACKLOG = 16;

    private static final int WORKERS = 8;

    private static final int QUEUED_REQUESTS = 16;

    private static final int SOCKET_TIMEOUT_MS = 30000;

    private static final int OUTPUT_BUFFER_BYTES = 1 << 16;

    private final DashFileSource source;

    private final ServerSocket serverSocket;

    private final ThreadPoolExecutor workers = new ThreadPoolExecutor(WORKERS, WORKERS, 0L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<Runnable>(QUEUED_REQUESTS));

    private boolean started;

    private DashServer(DashFileSource source, ServerSocket serverSocket) {
        this.source = source;
        this.serverSocket = serverSocket;
    }

    public static DashServer bind(DashFileSource source) throws IOException {
        return new DashServer(source, new ServerSocket(PORT, BACKLOG, InetAddress.getByAddress(LOOPBACK)));
    }

    public static String buildUrl(String subjectId, int season, int episode, int height, String origin, long originSize,
            String resourceId) {
        return "http://127.0.0.1:" + PORT + "/dash/" + subjectId + "/" + season + "/" + episode + "/" + height + ".mp4"
                + "?" + LoopbackRequest.ORIGIN_PARAMETER + "=" + Uri.encode(origin) + "&"
                + LoopbackRequest.ORIGIN_SIZE_PARAMETER + "=" + originSize + ((resourceId != null)
                        ? "&" + LoopbackRequest.RESOURCE_ID_PARAMETER + "=" + Uri.encode(resourceId) : "");
    }

    public synchronized void start() {
        if (this.started) {
            throw new IllegalStateException("already started");
        }
        this.started = true;
        Thread acceptor = new Thread(this, "hx-dash-server");
        acceptor.setDaemon(true);
        acceptor.start();
    }

    public void close() {
        try {
            this.serverSocket.close();
        } catch (IOException ignored) {
        }
        this.workers.shutdownNow();
    }

    @Override
    public void run() {
        while (!this.serverSocket.isClosed()) {
            Socket client = accept();
            if (client == null) {
                return;
            }
            dispatch(client);
        }
    }

    private Socket accept() {
        try {
            return this.serverSocket.accept();
        } catch (IOException ex) {
            if (!this.serverSocket.isClosed()) {
                Log.e(Logs.TAG, "DASH server stopped", ex);
            }
            close();
            return null;
        }
    }

    private void dispatch(Socket client) {
        try {
            this.workers.execute(() -> serve(client));
        } catch (RejectedExecutionException ex) {
            closeQuietly(client);
        }
    }

    private void serve(Socket client) {
        try {
            client.setSoTimeout(SOCKET_TIMEOUT_MS);
            LoopbackRequest request = LoopbackRequest.parse(LoopbackRequest.readHead(client.getInputStream()));
            OutputStream out = new BufferedOutputStream(client.getOutputStream(), OUTPUT_BUFFER_BYTES);
            respond(request, out);
            out.flush();
        } catch (SocketException ex) {
            Log.i(Logs.TAG, "DASH request ended by the client: " + ex.getMessage());
        } catch (Throwable ex) {
            Log.w(Logs.TAG, "DASH request failed", ex);
        } finally {
            closeQuietly(client);
        }
    }

    private void respond(LoopbackRequest request, OutputStream out) throws IOException {
        if (request == null) {
            LoopbackResponse.writeNotFound(out);
            return;
        }
        DashFile file;
        try {
            file = this.source.open(request.subjectId, request.season, request.episode, request.height, request.origin,
                    request.originSize, request.resourceId, request.startsAtBeginning());
        } catch (RedirectException ex) {
            LoopbackResponse.writeRedirect(out, ex.location);
            return;
        } catch (UnavailableException ex) {
            Log.i(Logs.TAG, "unavailable " + request.target + ": " + ex.getMessage());
            LoopbackResponse.writeNotFound(out);
            return;
        } catch (IOException ex) {
            Log.e(Logs.TAG, "cannot open " + request.target, ex);
            LoopbackResponse.writeUnavailable(out);
            return;
        }
        if (file == null) {
            writeOriginOrNotFound(request, out);
            return;
        }
        writeFile(request, file, out);
    }

    private static void writeOriginOrNotFound(LoopbackRequest request, OutputStream out) throws IOException {
        if (request.origin == null) {
            LoopbackResponse.writeNotFound(out);
        } else {
            LoopbackResponse.writeRedirect(out, request.origin);
        }
    }

    private static void writeFile(LoopbackRequest request, DashFile file, OutputStream out) throws IOException {
        ByteRange range = request.rangeWithin(file.length);
        if (!range.isSatisfiable()) {
            LoopbackResponse.writeUnsatisfiable(out, file.length);
            return;
        }
        LoopbackResponse.writeHeaders(out, range, file.length, request.isPartial());
        if (!request.headOnly) {
            file.write(out, range.start, range.end);
        }
    }

    private static void closeQuietly(Socket socket) {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }

    public static final class RedirectException extends IOException {

        private static final long serialVersionUID = 1L;

        final String location;

        public RedirectException(String location) {
            super("redirect to " + location);
            this.location = location;
        }

    }

    public static final class UnavailableException extends IOException {

        private static final long serialVersionUID = 1L;

        public UnavailableException(String message) {
            super(message);
        }

    }

}
