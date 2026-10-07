/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.raindrop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import android.app.Application;
import android.database.Cursor;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class LinkCheckerPageTextTest {

    private static final int MAX_PAGE_BYTES = 2 * 1024 * 1024;

    private static final int MAX_TEXT_CHARS = 100000;

    private static final String HTML = "text/html; charset=UTF-8";

    private static final AtomicLong ACCOUNTS = new AtomicLong(20000);

    private static final AtomicInteger PAGES = new AtomicInteger();

    private static PageServer server;

    private LinkStore store;

    private static byte[] utf8(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] repeat(String text, int times) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < times; i++) {
            builder.append(text);
        }
        return utf8(builder.toString());
    }

    private static byte[] pageOfLength(int length, String tail) {
        StringBuilder builder = new StringBuilder("<p>head</p>");
        while (builder.length() < length - tail.length()) {
            builder.append(' ');
        }
        return utf8(builder.append(tail).toString());
    }

    @BeforeClass
    public static void startServer() throws IOException {
        server = new PageServer();
    }

    @AfterClass
    public static void stopServer() throws IOException {
        server.close();
    }

    @Before
    public void selectAccount() {
        Application application = RuntimeEnvironment.getApplication();
        long account = ACCOUNTS.incrementAndGet();
        LinkChecker.init(application);
        LinkChecker.useAccount(account);
        this.store = new LinkStore(application, account);
    }

    @After
    public void closeStore() {
        this.store.close();
    }

    private String storedText(String contentType, byte[] body) throws JSONException, InterruptedException {
        String link = server.publish(contentType, body);
        LinkChecker.checkInBackground(Collections.singletonList(new JSONObject().put("link", link)), false);
        long deadline = System.nanoTime() + 10_000_000_000L;
        while (this.store.isHealthCheckDue(link)) {
            assertTrue("health check of " + link + " did not finish", System.nanoTime() < deadline);
            Thread.sleep(10);
        }
        try (Cursor cursor = this.store.getReadableDatabase()
            .rawQuery("SELECT body FROM page_text WHERE link = ?", new String[] { link })) {
            return cursor.moveToFirst() ? cursor.getString(0) : null;
        }
    }

    private String textOf(String html) throws JSONException, InterruptedException {
        return storedText(HTML, utf8(html));
    }

    @Test
    public void stripsTagsCollapsingWhitespace() throws Exception {
        assertEquals("Hello world", textOf("<p>Hello</p>\n\n<p>  <b>world</b> </p>"));
        assertEquals("a b", textOf("a<br>b"));
        assertEquals("", textOf(""));
        assertEquals("", textOf("<div></div>\n<span> </span>"));
    }

    @Test
    public void dropsScriptStyleNoscriptTemplateContent() throws Exception {
        assertEquals("before after", textOf("before <script>var a = 1 < 2;</script> after"));
        assertEquals("before after", textOf("before <style>p > a { color: red }</style> after"));
        assertEquals("before after", textOf("before <noscript><p>enable js</p></noscript> after"));
        assertEquals("before after", textOf("before <template><li>row</li></template> after"));
    }

    @Test
    public void removesScriptsDespiteCaseAttributesOrLineBreaks() throws Exception {
        assertEquals("a b", textOf("a <SCRIPT type=\"text/javascript\">\nvar x;\n</Script > b"));
        assertEquals("a b", textOf("a <script\nsrc=\"x.js\">\n</script> b"));
    }

    @Test
    public void removesEachScriptBlockSeparately() throws Exception {
        assertEquals("one two three", textOf("one <script>a()</script> two <script>b()</script> three"));
    }

    @Test
    public void decodesEntitiesAfterTagsAreStripped() throws Exception {
        assertEquals("Fish & Chips", textOf("<p>Fish &amp; Chips</p>"));
        assertEquals("<script>x</script>", textOf("&lt;script&gt;x&lt;/script&gt;"));
        assertEquals("a b", textOf("a&nbsp;&nbsp;b"));
    }

    @Test
    public void decodesTextInTheCharsetOfTheResponse() throws Exception {
        byte[] latin1 = "café".getBytes(StandardCharsets.ISO_8859_1);
        assertEquals("café", storedText("text/html; charset=ISO-8859-1", latin1));
        assertEquals("caf�", storedText("text/html; charset=UTF-8", latin1));
        assertEquals("caf�", storedText("text/html", latin1));
        assertEquals("caf�", storedText("text/html; charset=bogus-charset", latin1));
        assertEquals("café", storedText("text/html; charset=UTF-16LE", "café".getBytes(Charset.forName("UTF-16LE"))));
    }

    @Test
    public void truncatesLongTextToTheLimit() throws Exception {
        // when
        String text = storedText(HTML, repeat("ab ", MAX_TEXT_CHARS));

        // then
        assertEquals(MAX_TEXT_CHARS, text.length());
        assertTrue(text.startsWith("ab ab ab"));
    }

    @Test
    public void capsOnlyTextLongerThanTheLimit() throws Exception {
        assertEquals(MAX_TEXT_CHARS, storedText(HTML, repeat("a", MAX_TEXT_CHARS)).length());
        assertEquals(MAX_TEXT_CHARS, storedText(HTML, repeat("a", MAX_TEXT_CHARS + 1)).length());
        assertEquals(MAX_TEXT_CHARS - 1, storedText(HTML, repeat("a", MAX_TEXT_CHARS - 1)).length());
    }

    @Test
    public void extractsTheSameTextFromTheSameHtmlTwice() throws Exception {
        String html = "<title>T</title><script>x()</script><p>Body &amp; soul</p>";
        assertEquals(textOf(html), textOf(html));
        assertEquals("T Body & soul", textOf(html));
    }

    @Test
    public void ignoresPagesThatAreNotHtml() throws Exception {
        assertNull(storedText("text/plain", utf8("plain words")));
        assertNull(storedText("application/json", utf8("{\"a\":1}")));
    }

    @Test
    public void readsWholePagesUnderTheCap() throws Exception {
        String text = new String(repeat("0123456789", 5000), StandardCharsets.UTF_8);
        assertEquals(text, textOf(text));
        assertEquals("", textOf(""));
    }

    @Test
    public void stopsReadingAtTheCap() throws Exception {
        assertEquals("head", storedText(HTML, pageOfLength(MAX_PAGE_BYTES + 12345, "<p>tail</p>")));
        assertEquals("head tail", storedText(HTML, pageOfLength(MAX_PAGE_BYTES + 1, "tail!")));
    }

    @Test
    public void readsAPageExactlyAtTheCapCompletely() throws Exception {
        assertEquals("head tail", storedText(HTML, pageOfLength(MAX_PAGE_BYTES, "<p>tail</p>")));
        assertEquals("head tail", storedText(HTML, pageOfLength(MAX_PAGE_BYTES, "tail")));
    }

    private static final class PageServer {

        private final ServerSocket socket;

        private final Map<String, Page> pages = new ConcurrentHashMap<>();

        PageServer() throws IOException {
            this.socket = new ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"));
            Thread acceptor = new Thread(this::acceptConnections, "page-server");
            acceptor.setDaemon(true);
            acceptor.start();
        }

        String publish(String contentType, byte[] body) {
            String path = "/page-" + PAGES.incrementAndGet();
            this.pages.put(path, new Page(contentType, body));
            return "http://127.0.0.1:" + this.socket.getLocalPort() + path;
        }

        void close() throws IOException {
            this.socket.close();
        }

        private void acceptConnections() {
            while (!this.socket.isClosed()) {
                try {
                    Socket client = this.socket.accept();
                    Thread responder = new Thread(() -> respond(client), "page-response");
                    responder.setDaemon(true);
                    responder.start();
                } catch (IOException ex) {
                    return;
                }
            }
        }

        private void respond(Socket client) {
            try (Socket connection = client) {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.ISO_8859_1));
                String path = reader.readLine().split(" ")[1];
                String line = reader.readLine();
                while (line != null && !line.isEmpty()) {
                    line = reader.readLine();
                }
                Page page = this.pages.get(path);
                OutputStream output = connection.getOutputStream();
                if (page == null) {
                    output.write(utf8("HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"));
                } else {
                    output.write(utf8("HTTP/1.1 200 OK\r\nContent-Type: " + page.contentType + "\r\nContent-Length: "
                            + page.body.length + "\r\nConnection: close\r\n\r\n"));
                    output.write(page.body);
                }
                output.flush();
            } catch (IOException ex) {
                return;
            }
        }

    }

    private static final class Page {

        private final String contentType;

        private final byte[] body;

        Page(String contentType, byte[] body) {
            this.contentType = contentType;
            this.body = body;
        }

    }

}
