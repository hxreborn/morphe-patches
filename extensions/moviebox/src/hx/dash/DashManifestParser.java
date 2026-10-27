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
package hx.dash;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Xml;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

final class DashManifestParser {

    private static final int MAX_DEPTH = 32;

    private static final Pattern ISO_DURATION = Pattern
        .compile("P(?:(\\d+)D)?(?:T(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+(?:\\.\\d+)?)S)?)?");

    private static final long SECONDS_PER_MINUTE = 60L;

    private static final long SECONDS_PER_HOUR = 3600L;

    private static final long SECONDS_PER_DAY = 86400L;

    private final XmlPullParser parser;

    private final DashManifest manifest = new DashManifest();

    private final String[] baseUrls = new String[MAX_DEPTH];

    private String setType;

    private DashManifest.Template setTemplate;

    private DashManifest.Template template;

    private DashManifest.Representation representation;

    private DashManifestParser(byte[] xml, String manifestUrl) throws XmlPullParserException {
        this.parser = Xml.newPullParser();
        this.parser.setInput(new ByteArrayInputStream(xml), null);
        this.baseUrls[0] = manifestUrl.substring(0, manifestUrl.lastIndexOf('/') + 1);
    }

    static DashManifest parse(byte[] xml, String manifestUrl) throws IOException {
        try {
            return new DashManifestParser(xml, manifestUrl).read();
        } catch (XmlPullParserException | NumberFormatException | MalformedURLException ex) {
            throw new IOException("cannot parse manifest " + manifestUrl, ex);
        }
    }

    private static double parseDurationSeconds(String iso8601Duration) {
        if (iso8601Duration == null) {
            return 0;
        }
        Matcher matcher = ISO_DURATION.matcher(iso8601Duration);
        if (!matcher.matches()) {
            throw new NumberFormatException("duration " + iso8601Duration);
        }
        double seconds = 0;
        if (matcher.group(1) != null) {
            seconds += Long.parseLong(matcher.group(1)) * SECONDS_PER_DAY;
        }
        if (matcher.group(2) != null) {
            seconds += Long.parseLong(matcher.group(2)) * SECONDS_PER_HOUR;
        }
        if (matcher.group(3) != null) {
            seconds += Long.parseLong(matcher.group(3)) * SECONDS_PER_MINUTE;
        }
        if (matcher.group(4) != null) {
            seconds += Double.parseDouble(matcher.group(4));
        }
        return seconds;
    }

    private DashManifest read() throws XmlPullParserException, IOException {
        for (int event = this.parser.getEventType(); event != XmlPullParser.END_DOCUMENT; event = this.parser.next()) {
            int depth = this.parser.getDepth();
            if (depth >= MAX_DEPTH) {
                throw new IOException("manifest nested deeper than " + MAX_DEPTH);
            }
            if (event == XmlPullParser.START_TAG) {
                startTag(this.parser.getName(), depth);
            } else if (event == XmlPullParser.END_TAG) {
                endTag(this.parser.getName(), depth);
            }
        }
        return this.manifest;
    }

    private void startTag(String name, int depth) throws XmlPullParserException, IOException {
        this.baseUrls[depth] = this.baseUrls[depth - 1];
        if ("MPD".equals(name)) {
            this.manifest.presentationSeconds = parseDurationSeconds(attribute("mediaPresentationDuration"));
        } else if ("BaseURL".equals(name)) {
            readBaseUrl(depth);
        } else if ("AdaptationSet".equals(name)) {
            this.setType = readContentType();
            this.setTemplate = null;
        } else if ("Representation".equals(name)) {
            startRepresentation();
        } else if ("SegmentTemplate".equals(name)) {
            startTemplate();
        } else if ("S".equals(name) && this.template != null) {
            this.template.timeline
                .add(new DashManifest.Interval(longAttribute("t", -1), longAttribute("d", 0), longAttribute("r", 0)));
        }
    }

    private void endTag(String name, int depth) {
        if ("Representation".equals(name) && this.representation != null) {
            finishRepresentation(depth);
        } else if ("SegmentTemplate".equals(name)) {
            this.template = null;
        }
    }

    private void readBaseUrl(int depth) throws XmlPullParserException, IOException {
        URL base = new URL(this.baseUrls[depth - 1]);
        this.baseUrls[depth - 1] = new URL(base, this.parser.nextText().trim()).toString();
        this.baseUrls[depth] = this.baseUrls[depth - 1];
    }

    private void startRepresentation() {
        this.representation = new DashManifest.Representation();
        this.representation.id = attribute("id");
        this.representation.height = intAttribute("height", 0);
        this.representation.bandwidth = intAttribute("bandwidth", 0);
        String ownType = readContentType();
        if (ownType != null) {
            this.representation.type = ownType;
        } else {
            this.representation.type = (this.representation.height > 0) ? "video" : this.setType;
        }
    }

    private void startTemplate() {
        this.template = new DashManifest.Template();
        this.template.timescale = intAttribute("timescale", 1);
        this.template.duration = longAttribute("duration", 0);
        this.template.initialization = attribute("initialization");
        this.template.media = attribute("media");
        this.template.startNumber = intAttribute("startNumber", 1);
        if (this.representation == null) {
            this.setTemplate = this.template;
        } else {
            this.representation.template = this.template;
        }
    }

    private void finishRepresentation(int depth) {
        if (this.representation.template == null) {
            this.representation.template = this.setTemplate;
        }
        this.representation.baseUrl = this.baseUrls[depth];
        if (this.representation.template != null && this.representation.template.media != null) {
            this.manifest.representations.add(this.representation);
        }
        this.representation = null;
    }

    private String readContentType() {
        String type = attribute("contentType");
        if (type != null) {
            return type;
        }
        String mime = attribute("mimeType");
        return (mime != null) ? mime.substring(0, Math.max(mime.indexOf('/'), 0)) : null;
    }

    private String attribute(String name) {
        return this.parser.getAttributeValue(null, name);
    }

    private int intAttribute(String name, int fallback) {
        String value = attribute(name);
        return (value != null) ? Integer.parseInt(value) : fallback;
    }

    private long longAttribute(String name, long fallback) {
        String value = attribute(name);
        return (value != null) ? Long.parseLong(value) : fallback;
    }

}
