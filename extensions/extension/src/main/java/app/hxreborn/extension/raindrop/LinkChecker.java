/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.raindrop;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.net.Uri;
import android.os.SystemClock;
import android.util.Log;
import org.json.JSONObject;

final class LinkChecker {

    private static final String TAG = "RaindropPro";

    private static final long RECHECK_AFTER_MILLIS = 7L * 24 * 60 * 60 * 1000;

    private static final long RETRY_AFTER_MILLIS = 60L * 60 * 1000;

    private static final long LOOKUP_INTERVAL_MILLIS = 5000;

    private static final long SAVE_INTERVAL_MILLIS = 6000;

    private static final long API_CHECK_INTERVAL_MILLIS = 60000;

    private static final int TIMEOUT_MILLIS = 15000;

    private static final int SAVE_TIMEOUT_MILLIS = 60000;

    private static final int MAX_PAGE_BYTES = 2 * 1024 * 1024;

    private static final int MAX_TEXT_CHARS = 100000;

    private static final String WAYBACK_URL = "https://web.archive.org/";

    private static final Pattern WAYBACK_CAPTURE_PATH = Pattern.compile("/web/\\d{14}/.*");

    private static final String USER_AGENT = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 "
            + "(KHTML, like Gecko) Chrome/129.0 Mobile Safari/537.36";

    private static final Pattern SCRIPT_OR_STYLE = Pattern
        .compile("(?is)<(script|style|noscript|template)\\b[^>]*>.*?</\\1\\s*>");

    private static final Pattern TAG_PATTERN = Pattern.compile("(?s)<[^>]*>");

    private static final Pattern ENTITY = Pattern.compile("&(#?[a-zA-Z0-9]+);");

    private static final Pattern CHARSET = Pattern.compile("(?i)charset=\"?([\\w.:-]+)");

    private static final Pattern IP_LITERAL = Pattern.compile("[0-9.]+|[0-9a-f:.]*:[0-9a-f:.]*");

    private static final String[] PRIVATE_HOST_SUFFIXES = { ".local", ".localhost", ".internal", ".lan", ".home",
            ".home.arpa", ".corp", ".intranet" };

    private static final Object API_CHECK_LOCK = new Object();

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private static final ExecutorService healthChecker = Executors.newSingleThreadExecutor();

    private static final ExecutorService archiver = Executors.newSingleThreadExecutor();

    private static final Set<String> healthQueue = Collections.synchronizedSet(new HashSet<String>());

    private static final Set<String> archiveQueue = Collections.synchronizedSet(new HashSet<String>());

    private static volatile LinkStore store;

    private static Context applicationContext;

    private static long storeAccount;

    private static long apiCheckedAt;

    private static boolean isApiUp;

    private LinkChecker() {
    }

    static synchronized void init(Context context) {
        applicationContext = context.getApplicationContext();
    }

    static synchronized void useAccount(long account) {
        if (applicationContext == null || account == 0 || (store != null && account == storeAccount)) {
            return;
        }
        store = new LinkStore(applicationContext, account);
        storeAccount = account;
    }

    static boolean isBroken(String link) {
        LinkStore links = store;
        return links != null && links.isBroken(link);
    }

    static boolean isArchived(String link) {
        LinkStore links = store;
        return links != null && links.isArchived(link);
    }

    static Set<String> linksContaining(String query) {
        LinkStore links = store;
        return (links != null) ? links.linksContaining(query) : Collections.<String>emptySet();
    }

    static String resultStamp() {
        LinkStore links = store;
        return (links != null) ? links.resultStamp() : "";
    }

    static String archiveUrl(String link) {
        return WAYBACK_URL + "web/2/" + link;
    }

    static void checkInBackground(List<JSONObject> bookmarks, boolean isWholeLibrary) {
        final LinkStore links = store;
        if (links == null) {
            return;
        }
        final List<String> all = new ArrayList<>();
        for (JSONObject bookmark : bookmarks) {
            final String link = bookmark.optString("link");
            if (!link.startsWith("http://") && !link.startsWith("https://")) {
                continue;
            }
            all.add(link);
            if (healthQueue.add(link)) {
                healthChecker.execute(() -> {
                    try {
                        if (links.isHealthCheckDue(link)) {
                            checkHealth(links, link);
                        }
                        if (isArchivable(link) && archiveQueue.add(link)) {
                            archiver.execute(() -> archiveIfDue(links, link));
                        }
                    } catch (RuntimeException ex) {
                        Log.w(TAG, "Link check failed for " + link, ex);
                    } finally {
                        healthQueue.remove(link);
                    }
                });
            }
        }
        if (isWholeLibrary) {
            healthChecker.execute(() -> links.retainOnly(all));
        }
    }

    private static void checkHealth(LinkStore links, String link) {
        Boolean broken = null;
        String text = null;
        try {
            HttpURLConnection connection = open(link, TIMEOUT_MILLIS);
            try {
                int code = connection.getResponseCode();
                if (code == HttpURLConnection.HTTP_NOT_FOUND || code == HttpURLConnection.HTTP_GONE) {
                    broken = true;
                } else if (code >= 200 && code < 300) {
                    broken = false;
                }
                String type = connection.getContentType();
                if (code == HttpURLConnection.HTTP_OK && type != null && type.contains("html")) {
                    try (InputStream input = connection.getInputStream()) {
                        text = extractText(readCapped(input), charsetOf(type));
                    }
                }
            } finally {
                connection.disconnect();
            }
        } catch (UnknownHostException | ConnectException ex) {
            if (isApiReachable()) {
                broken = true;
            }
        } catch (IOException ex) {
            Log.w(TAG, "Link check inconclusive for " + link, ex);
        }
        long retryAfter = (broken != null) ? RECHECK_AFTER_MILLIS : RETRY_AFTER_MILLIS;
        links.recordHealth(link, broken, System.currentTimeMillis() + retryAfter, text);
    }

    private static void archiveIfDue(LinkStore links, String link) {
        try {
            if (!links.isArchiveCheckDue(link)) {
                return;
            }
            if (!resolvesToPublicAddresses(Uri.parse(link).getHost())) {
                links.recordArchive(link, false, System.currentTimeMillis() + RECHECK_AFTER_MILLIS);
                return;
            }
            Boolean captured = lookUpCapture(link);
            boolean archived = Boolean.TRUE.equals(captured)
                    || (captured != null && !links.isBroken(link) && save(link));
            long retryAfter = archived ? RECHECK_AFTER_MILLIS : RETRY_AFTER_MILLIS;
            links.recordArchive(link, archived, System.currentTimeMillis() + retryAfter);
        } catch (RuntimeException ex) {
            Log.w(TAG, "Archive failed for " + link, ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } finally {
            archiveQueue.remove(link);
        }
    }

    private static Boolean lookUpCapture(String link) throws InterruptedException {
        Thread.sleep(LOOKUP_INTERVAL_MILLIS);
        try {
            HttpURLConnection lookup = open(archiveUrl(link), TIMEOUT_MILLIS);
            try {
                lookup.setInstanceFollowRedirects(false);
                lookup.setRequestMethod("HEAD");
                int code = lookup.getResponseCode();
                if (code == HttpURLConnection.HTTP_NOT_FOUND) {
                    return false;
                }
                String location = lookup.getHeaderField("Location");
                String path = (location != null) ? Uri.parse(location).getPath() : null;
                if (code / 100 == 3 && path != null && WAYBACK_CAPTURE_PATH.matcher(path).matches()) {
                    return true;
                }
                Log.w(TAG, "Wayback lookup returned HTTP " + code + " for " + link);
                return null;
            } finally {
                lookup.disconnect();
            }
        } catch (IOException ex) {
            Log.w(TAG, "Wayback lookup failed for " + link + ": " + ex);
            return null;
        }
    }

    private static boolean save(String link) throws InterruptedException {
        try {
            HttpURLConnection save = open(WAYBACK_URL + "save/" + link, SAVE_TIMEOUT_MILLIS);
            try {
                int code = save.getResponseCode();
                String capture = save.getURL().getPath();
                if (code == HttpURLConnection.HTTP_OK && WAYBACK_CAPTURE_PATH.matcher(capture).matches()) {
                    return true;
                }
                Log.w(TAG, "Wayback save returned HTTP " + code + " at " + capture + " for " + link);
                return false;
            } finally {
                save.disconnect();
            }
        } catch (IOException ex) {
            Log.w(TAG, "Wayback save failed for " + link + ": " + ex);
            return false;
        } finally {
            Thread.sleep(SAVE_INTERVAL_MILLIS);
        }
    }

    private static boolean isArchivable(String link) {
        Uri uri = Uri.parse(link);
        String host = uri.getHost();
        return uri.getEncodedQuery() == null && uri.getEncodedUserInfo() == null && host != null && isPublicHost(host);
    }

    private static boolean resolvesToPublicAddresses(String host) {
        try {
            for (InetAddress address : InetAddress.getAllByName(host)) {
                if (!isPublicAddress(address)) {
                    return false;
                }
            }
            return true;
        } catch (UnknownHostException ex) {
            return false;
        }
    }

    static boolean isPublicAddress(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                || address.isSiteLocalAddress() || address.isMulticastAddress()) {
            return false;
        }
        byte[] bytes = address.getAddress();
        if (bytes.length == 16) {
            return (bytes[0] & 0xfe) != 0xfc;
        }
        int first = bytes[0] & 0xff;
        int second = bytes[1] & 0xff;
        return first != 0 && !(first == 100 && second >= 64 && second < 128);
    }

    static boolean isPublicHost(String host) {
        String name = host.toLowerCase(Locale.ROOT);
        if (name.startsWith("[") && name.endsWith("]")) {
            name = name.substring(1, name.length() - 1);
        }
        if (IP_LITERAL.matcher(name).matches()) {
            try {
                return isPublicAddress(InetAddress.getByName(name));
            } catch (UnknownHostException ex) {
                return false;
            }
        }
        if (name.indexOf('.') < 0) {
            return false;
        }
        for (String suffix : PRIVATE_HOST_SUFFIXES) {
            if (name.endsWith(suffix)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isApiReachable() {
        synchronized (API_CHECK_LOCK) {
            return probeApi();
        }
    }

    private static boolean probeApi() {
        long now = SystemClock.elapsedRealtime();
        if (apiCheckedAt != 0 && now - apiCheckedAt < API_CHECK_INTERVAL_MILLIS) {
            return isApiUp;
        }
        apiCheckedAt = now;
        try {
            HttpURLConnection connection = open(RaindropApi.API_URL, TIMEOUT_MILLIS);
            try {
                connection.getResponseCode();
                isApiUp = true;
            } finally {
                connection.disconnect();
            }
        } catch (IOException ex) {
            isApiUp = false;
        }
        return isApiUp;
    }

    private static HttpURLConnection open(String url, int readTimeoutMillis) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setInstanceFollowRedirects(true);
        connection.setConnectTimeout(TIMEOUT_MILLIS);
        connection.setReadTimeout(readTimeoutMillis);
        connection.setRequestProperty("User-Agent", USER_AGENT);
        return connection;
    }

    private static byte[] readCapped(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[16384];
        int read;
        while (output.size() < MAX_PAGE_BYTES
                && (read = input.read(buffer, 0, Math.min(buffer.length, MAX_PAGE_BYTES - output.size()))) != -1) {
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    static Charset charsetOf(String contentType) {
        Matcher charset = CHARSET.matcher(contentType);
        if (charset.find()) {
            try {
                return Charset.forName(charset.group(1));
            } catch (IllegalArgumentException ex) {
                return StandardCharsets.UTF_8;
            }
        }
        return StandardCharsets.UTF_8;
    }

    private static String extractText(byte[] html, Charset charset) {
        String text = new String(html, charset);
        text = SCRIPT_OR_STYLE.matcher(text).replaceAll(" ");
        text = TAG_PATTERN.matcher(text).replaceAll(" ");
        text = decodeEntities(text);
        text = WHITESPACE.matcher(text).replaceAll(" ").trim();
        return (text.length() > MAX_TEXT_CHARS) ? text.substring(0, MAX_TEXT_CHARS) : text;
    }

    static String decodeEntities(String text) {
        Matcher entity = ENTITY.matcher(text);
        StringBuffer decoded = new StringBuffer();
        while (entity.find()) {
            entity.appendReplacement(decoded, Matcher.quoteReplacement(decodeEntity(entity.group(1))));
        }
        entity.appendTail(decoded);
        return decoded.toString();
    }

    private static String decodeEntity(String name) {
        switch (name.toLowerCase(Locale.ROOT)) {
            case "amp":
                return "&";
            case "lt":
                return "<";
            case "gt":
                return ">";
            case "quot":
                return "\"";
            case "apos":
                return "'";
            default:
                break;
        }
        if (name.startsWith("#")) {
            try {
                boolean isHex = name.length() > 1 && (name.charAt(1) == 'x' || name.charAt(1) == 'X');
                int codePoint = isHex ? Integer.parseInt(name.substring(2), 16) : Integer.parseInt(name.substring(1));
                return new String(Character.toChars(codePoint));
            } catch (IllegalArgumentException ex) {
                return " ";
            }
        }
        return " ";
    }

}
