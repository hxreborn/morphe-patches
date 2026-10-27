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
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

final class MdnsBrowser {

    private static final int PORT = 5353;

    private static final String GROUP_HOST = "224.0.0.251";

    private static final int MULTICAST_TTL = 255;

    private static final String ENUMERATION = "_services._dns-sd._udp.local.";

    private static final String DOMAIN = "local";

    private static final String PROTOCOL_TCP = "_tcp";

    private static final String PROTOCOL_UDP = "_udp";

    private static final String LABEL_PREFIX = "_";

    private static final int TYPE_LABELS = 3;

    private static final int MIN_INSTANCE_LABELS = TYPE_LABELS + 1;

    private static final int HEADER_LENGTH = 12;

    private static final int FLAGS_OFFSET = 2;

    private static final int QUESTION_COUNT_OFFSET = 4;

    private static final int ANSWER_COUNT_OFFSET = 6;

    private static final int AUTHORITY_COUNT_OFFSET = 8;

    private static final int ADDITIONAL_COUNT_OFFSET = 10;

    private static final int RESPONSE_FLAG = 0x8000;

    private static final int QUESTION_SUFFIX_LENGTH = 4;

    private static final int TTL_OFFSET = 4;

    private static final int RDATA_LENGTH_OFFSET = 8;

    private static final int RECORD_FIXED_LENGTH = 10;

    private static final int SRV_PORT_OFFSET = 4;

    private static final int SRV_TARGET_OFFSET = 6;

    private static final int IPV4_LENGTH = 4;

    private static final int IPV6_LENGTH = 16;

    private static final int POINTER_FLAG = 0xC0;

    private static final int POINTER_OFFSET_MASK = 0x3F;

    private static final int POINTER_LENGTH = 2;

    private static final int MAX_POINTER_HOPS = 32;

    private static final int MAX_LABEL_LENGTH = 63;

    private static final int BYTE_MASK = 0xff;

    private static final int TYPE_A = 1;

    private static final int TYPE_PTR = 12;

    private static final int TYPE_TXT = 16;

    private static final int TYPE_AAAA = 28;

    private static final int TYPE_SRV = 33;

    private static final int CLASS_IN = 1;

    private static final int UNICAST_RESPONSE = 0x8000;

    private static final int ROUNDS = 3;

    private static final long ROUND_MS = 225;

    private static final long RESOLVE_MS = 6000;

    private static final int MAX_PACKET = 9000;

    private static final InetAddress GROUP = group();

    final InetAddress local;

    private final NetworkInterface networkInterface;

    private final MdnsChangeListener listener;

    private final Object lock = new Object();

    private final Set<String> types = new LinkedHashSet<>();

    private final Map<String, Instance> instances = new LinkedHashMap<>();

    private final Map<String, List<InetAddress>> hosts = new HashMap<>();

    private final Set<String> queriedHosts = new HashSet<>();

    private final List<BonjourService> services = new ArrayList<>();

    private MulticastSocket socket;

    private ScheduledExecutorService scheduler;

    private boolean unicastResponse;

    private int generation;

    MdnsBrowser(InetAddress local, NetworkInterface networkInterface, MdnsChangeListener listener) {
        this.local = local;
        this.networkInterface = networkInterface;
        this.listener = listener;
    }

    List<BonjourService> services() {
        synchronized (this.lock) {
            return new ArrayList<>(this.services);
        }
    }

    void start() throws IOException {
        synchronized (this.lock) {
            if (this.socket != null) {
                return;
            }
            this.socket = openSocket();
            this.scheduler = Executors.newSingleThreadScheduledExecutor(MdnsBrowser::daemon);
            this.generation++;
            this.queriedHosts.clear();
            final MulticastSocket opened = this.socket;
            final int current = this.generation;
            daemon(() -> readLoop(opened, current)).start();
            query(ENUMERATION, TYPE_PTR);
            for (String type : this.types) {
                query(type, TYPE_PTR);
            }
            for (Instance instance : this.instances.values()) {
                if (!instance.isResolved(this.hosts)) {
                    requestInstance(instance);
                }
            }
        }
    }

    void stop() {
        synchronized (this.lock) {
            if (this.socket == null) {
                return;
            }
            this.generation++;
            this.socket.close();
            this.scheduler.shutdownNow();
            this.socket = null;
            this.scheduler = null;
        }
    }

    private MulticastSocket openSocket() throws IOException {
        MulticastSocket shared = null;
        try {
            shared = new MulticastSocket(null);
            shared.setReuseAddress(true);
            shared.bind(new InetSocketAddress(PORT));
            configure(shared);
            shared.joinGroup(new InetSocketAddress(GROUP, PORT), this.networkInterface);
            this.unicastResponse = false;
            return shared;
        } catch (IOException ex) {
            if (shared != null) {
                shared.close();
            }
        }
        final MulticastSocket ephemeral = new MulticastSocket(new InetSocketAddress(this.local, 0));
        configure(ephemeral);
        this.unicastResponse = true;
        return ephemeral;
    }

    private void configure(MulticastSocket target) throws IOException {
        target.setTimeToLive(MULTICAST_TTL);
        if (this.networkInterface != null) {
            target.setNetworkInterface(this.networkInterface);
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
            final List<Record> records;
            try {
                records = parse(Arrays.copyOf(buffer, packet.getLength()));
            } catch (RuntimeException | UnknownHostException ex) {
                continue;
            }
            mergeRecords(records, current);
        }
    }

    private void mergeRecords(List<Record> records, int current) {
        boolean changed = false;
        synchronized (this.lock) {
            if (current != this.generation) {
                return;
            }
            for (Record record : records) {
                storeRecord(record);
            }
            for (Instance instance : this.instances.values()) {
                changed |= refresh(instance);
            }
        }
        if (changed) {
            this.listener.onChanged();
        }
    }

    private void storeRecord(Record record) {
        switch (record.type) {
            case TYPE_PTR:
                if (ENUMERATION.equalsIgnoreCase(record.name)) {
                    addType(record.target);
                } else if (record.ttl > 0) {
                    instanceFor(record.target);
                }
                break;
            case TYPE_SRV:
                final Instance withSrv = instanceFor(record.name);
                if (withSrv != null) {
                    withSrv.server = record.target;
                    withSrv.port = record.port;
                    withSrv.hasSrv = true;
                    if (this.queriedHosts.add(lower(record.target))) {
                        query(record.target, TYPE_A, TYPE_AAAA);
                    }
                }
                break;
            case TYPE_TXT:
                final Instance withTxt = instanceFor(record.name);
                if (withTxt != null) {
                    withTxt.txt = record.txt;
                    withTxt.hasTxt = true;
                }
                break;
            case TYPE_A:
            case TYPE_AAAA:
                final String host = lower(record.name);
                List<InetAddress> known = this.hosts.get(host);
                if (known == null) {
                    known = new ArrayList<>();
                    this.hosts.put(host, known);
                }
                if (!known.contains(record.address)) {
                    known.add(record.address);
                }
                break;
            default:
                break;
        }
    }

    private void addType(String type) {
        final String key = lower(type);
        if (isType(key) && this.types.add(key)) {
            query(key, TYPE_PTR);
        }
    }

    private Instance instanceFor(String fullName) {
        final String key = lower(fullName);
        Instance instance = this.instances.get(key);
        if (instance != null) {
            return instance;
        }
        final String[] parts = split(fullName);
        if (parts == null) {
            return null;
        }
        instance = new Instance(key, fullName, parts[0], parts[1]);
        this.instances.put(key, instance);
        addType(parts[1]);
        requestInstance(instance);
        return instance;
    }

    private void requestInstance(Instance instance) {
        query(instance.fullName, TYPE_SRV, TYPE_TXT);
        final int current = this.generation;
        try {
            this.scheduler.schedule(() -> force(instance.key, current), RESOLVE_MS, TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException ex) {
            return;
        }
    }

    private void force(String key, int current) {
        boolean changed = false;
        synchronized (this.lock) {
            final Instance instance = this.instances.get(key);
            if (current != this.generation || instance == null) {
                return;
            }
            instance.forced = true;
            changed = refresh(instance);
        }
        if (changed) {
            this.listener.onChanged();
        }
    }

    private boolean refresh(Instance instance) {
        if (!instance.hasSrv && !instance.hasTxt) {
            return false;
        }
        if (!instance.isResolved(this.hosts)) {
            return false;
        }
        final List<InetAddress> addresses = instance.hasSrv ? this.hosts.get(lower(instance.server)) : null;
        Inet4Address address4 = null;
        Inet6Address address6 = null;
        if (addresses != null) {
            for (InetAddress address : addresses) {
                if (address instanceof Inet4Address && address4 == null) {
                    address4 = (Inet4Address) address;
                } else if (address instanceof Inet6Address && address6 == null) {
                    address6 = (Inet6Address) address;
                }
            }
        }
        final String application = instance.type.substring(LABEL_PREFIX.length(), instance.type.indexOf('.'));
        final BonjourService service = new BonjourService(instance.name, DOMAIN, instance.server, application,
                instance.type, instance.port, address4, address6, instance.txt);
        if (instance.published == null) {
            this.services.add(service);
        } else if (instance.published.sameAs(service)) {
            return false;
        } else {
            this.services.set(this.services.indexOf(instance.published), service);
        }
        instance.published = service;
        return true;
    }

    private void query(String name, int... questionTypes) {
        final byte[] packet = buildQuery(name, this.unicastResponse, questionTypes);
        final MulticastSocket out = this.socket;
        try {
            for (int round = 0; round < ROUNDS; round++) {
                this.scheduler.schedule(() -> send(out, packet), round * ROUND_MS, TimeUnit.MILLISECONDS);
            }
        } catch (RejectedExecutionException ex) {
            return;
        }
    }

    private static void send(MulticastSocket out, byte[] packet) {
        try {
            out.send(new DatagramPacket(packet, packet.length, GROUP, PORT));
        } catch (IOException ex) {
            return;
        }
    }

    static byte[] buildQuery(String name, boolean unicast, int... questionTypes) {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeShort(out, 0);
        writeShort(out, 0);
        writeShort(out, questionTypes.length);
        writeShort(out, 0);
        writeShort(out, 0);
        writeShort(out, 0);
        for (int type : questionTypes) {
            writeName(out, name);
            writeShort(out, type);
            writeShort(out, CLASS_IN | (unicast ? UNICAST_RESPONSE : 0));
        }
        return out.toByteArray();
    }

    private static void writeShort(ByteArrayOutputStream out, int value) {
        out.write((value >> Byte.SIZE) & BYTE_MASK);
        out.write(value & BYTE_MASK);
    }

    private static void writeName(ByteArrayOutputStream out, String name) {
        for (String label : labels(name)) {
            final byte[] bytes = label.getBytes(StandardCharsets.UTF_8);
            out.write(Math.min(bytes.length, MAX_LABEL_LENGTH));
            out.write(bytes, 0, Math.min(bytes.length, MAX_LABEL_LENGTH));
        }
        out.write(0);
    }

    static List<Record> parse(byte[] packet) throws UnknownHostException {
        final List<Record> records = new ArrayList<>();
        if ((u16(packet, FLAGS_OFFSET) & RESPONSE_FLAG) == 0) {
            return records;
        }
        final int questions = u16(packet, QUESTION_COUNT_OFFSET);
        final int total = u16(packet, ANSWER_COUNT_OFFSET) + u16(packet, AUTHORITY_COUNT_OFFSET)
                + u16(packet, ADDITIONAL_COUNT_OFFSET);
        final int[] cursor = { HEADER_LENGTH };
        for (int i = 0; i < questions; i++) {
            readName(packet, cursor);
            cursor[0] += QUESTION_SUFFIX_LENGTH;
        }
        for (int i = 0; i < total; i++) {
            final String name = readName(packet, cursor);
            final int type = u16(packet, cursor[0]);
            final long ttl = ((long) u16(packet, cursor[0] + TTL_OFFSET) << Short.SIZE)
                    | u16(packet, cursor[0] + TTL_OFFSET + Short.BYTES);
            final int length = u16(packet, cursor[0] + RDATA_LENGTH_OFFSET);
            final int start = cursor[0] + RECORD_FIXED_LENGTH;
            cursor[0] = start + length;
            if (cursor[0] > packet.length) {
                break;
            }
            final Record record = new Record(name, type, ttl);
            if (type == TYPE_PTR) {
                record.target = readName(packet, new int[] { start });
            } else if (type == TYPE_SRV) {
                record.port = u16(packet, start + SRV_PORT_OFFSET);
                record.target = readName(packet, new int[] { start + SRV_TARGET_OFFSET });
            } else if (type == TYPE_TXT) {
                record.txt = parseTxt(packet, start, length);
            } else if (type == TYPE_A && length == IPV4_LENGTH || type == TYPE_AAAA && length == IPV6_LENGTH) {
                record.address = InetAddress.getByAddress(Arrays.copyOfRange(packet, start, start + length));
            } else {
                continue;
            }
            records.add(record);
        }
        return records;
    }

    private static Map<String, String> parseTxt(byte[] packet, int start, int length) {
        final Map<String, String> txt = new LinkedHashMap<>();
        int pos = start;
        final int end = start + length;
        while (pos < end) {
            final int size = packet[pos++] & BYTE_MASK;
            if (size == 0) {
                continue;
            }
            final String entry = new String(packet, pos, Math.min(size, end - pos), StandardCharsets.UTF_8);
            pos += size;
            final int equals = entry.indexOf('=');
            if (equals < 0) {
                txt.put(entry, "true");
            } else if (equals > 0) {
                txt.put(entry.substring(0, equals), entry.substring(equals + 1));
            }
        }
        return txt;
    }

    private static String readName(byte[] packet, int[] cursor) {
        final StringBuilder name = new StringBuilder();
        int pos = cursor[0];
        boolean jumped = false;
        int hops = 0;
        while (true) {
            final int length = packet[pos] & BYTE_MASK;
            if (length == 0) {
                pos++;
                break;
            }
            if ((length & POINTER_FLAG) == POINTER_FLAG) {
                if (!jumped) {
                    cursor[0] = pos + POINTER_LENGTH;
                    jumped = true;
                }
                if (++hops > MAX_POINTER_HOPS) {
                    throw new IllegalArgumentException(
                            "DNS name has more than " + MAX_POINTER_HOPS + " compression pointers");
                }
                pos = ((length & POINTER_OFFSET_MASK) << Byte.SIZE) | (packet[pos + 1] & BYTE_MASK);
                continue;
            }
            final String label = new String(packet, pos + 1, length, StandardCharsets.UTF_8);
            name.append(label.replace("\\", "\\\\").replace(".", "\\.")).append('.');
            pos += 1 + length;
        }
        if (!jumped) {
            cursor[0] = pos;
        }
        return (name.length() != 0) ? name.toString() : ".";
    }

    private static int u16(byte[] packet, int pos) {
        return ((packet[pos] & BYTE_MASK) << Byte.SIZE) | (packet[pos + 1] & BYTE_MASK);
    }

    static List<String> labels(String name) {
        final List<String> labels = new ArrayList<>();
        final StringBuilder current = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            final char c = name.charAt(i);
            if (c == '\\' && i + 1 < name.length()) {
                current.append(name.charAt(++i));
            } else if (c == '.') {
                if (current.length() > 0) {
                    labels.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0) {
            labels.add(current.toString());
        }
        return labels;
    }

    private static boolean isType(String type) {
        final List<String> labels = labels(type);
        return labels.size() == TYPE_LABELS && isTypeLabels(labels.get(0), labels.get(1), labels.get(2));
    }

    private static boolean isTypeLabels(String app, String protocol, String domain) {
        final String lowerProtocol = protocol.toLowerCase(Locale.ROOT);
        return app.startsWith(LABEL_PREFIX) && app.length() > LABEL_PREFIX.length()
                && (PROTOCOL_TCP.equals(lowerProtocol) || PROTOCOL_UDP.equals(lowerProtocol))
                && DOMAIN.equalsIgnoreCase(domain);
    }

    private static String[] split(String fullName) {
        final List<String> labels = labels(fullName);
        final int count = labels.size();
        if (count < MIN_INSTANCE_LABELS) {
            return null;
        }
        final int typeStart = count - TYPE_LABELS;
        final String app = labels.get(typeStart);
        final String protocol = labels.get(typeStart + 1).toLowerCase(Locale.ROOT);
        if (!isTypeLabels(app, protocol, labels.get(typeStart + 2))) {
            return null;
        }
        final StringBuilder name = new StringBuilder();
        for (int i = 0; i < typeStart; i++) {
            if (i > 0) {
                name.append('.');
            }
            name.append(labels.get(i));
        }
        return new String[] { name.toString(), (app + "." + protocol + "." + DOMAIN + ".").toLowerCase(Locale.ROOT) };
    }

    private static String lower(String value) {
        return value.toLowerCase(Locale.ROOT);
    }

    private static Thread daemon(Runnable runnable) {
        final Thread thread = new Thread(runnable, "mdns");
        thread.setDaemon(true);
        return thread;
    }

    private static InetAddress group() {
        try {
            return InetAddress.getByName(GROUP_HOST);
        } catch (UnknownHostException ex) {
            throw new IllegalStateException(ex);
        }
    }

    static final class Record {

        final String name;

        final int type;

        final long ttl;

        String target;

        int port;

        Map<String, String> txt;

        InetAddress address;

        Record(String name, int type, long ttl) {
            this.name = name;
            this.type = type;
            this.ttl = ttl;
        }

    }

    private static final class Instance {

        final String key;

        final String fullName;

        final String name;

        final String type;

        String server;

        int port;

        Map<String, String> txt = new LinkedHashMap<>();

        boolean hasSrv;

        boolean hasTxt;

        boolean forced;

        BonjourService published;

        Instance(String key, String fullName, String name, String type) {
            this.key = key;
            this.fullName = fullName;
            this.name = name;
            this.type = type;
        }

        boolean isResolved(Map<String, List<InetAddress>> hosts) {
            if (this.forced) {
                return true;
            }
            final List<InetAddress> addresses = this.hasSrv ? hosts.get(this.server.toLowerCase(Locale.ROOT)) : null;
            return this.hasSrv && this.hasTxt && addresses != null && !addresses.isEmpty();
        }

    }

}
